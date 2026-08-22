package com.wjx.kablade.client.renderer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wjx.kablade.Main;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.20 雷斩 schema-v2 几何动画的 Java 8 实现。
 *
 * <p>这里保留完整四元数直至写入 OpenGL 模型矩阵。不能把 squad 的结果重新拆成
 * “刀尖方向 + roll 欧拉角”，否则翻腕、锁刃与回鞘阶段会改变旋转轴。</p>
 */
public final class RaizanAnimation implements IResourceManagerReloadListener {
    public static final RaizanAnimation INSTANCE = new RaizanAnimation();
    private static final ResourceLocation FILE =
            new ResourceLocation(Main.MODID, "kablade_animations/raizan_cleave.json");
    private static final Vec3d MODEL_AXIS = new Vec3d(0.0D, -1.0D, 0.0D);
    private static final Vec3d ZERO = new Vec3d(0.0D, 0.0D, 0.0D);

    private volatile Animation current = Animation.fallback();

    private RaizanAnimation() {
    }

    public Animation get() {
        return current;
    }

    @Override
    public void onResourceManagerReload(IResourceManager manager) {
        try (InputStream input = manager.getResource(FILE).getInputStream();
             Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            Animation loaded = parse(new JsonParser().parse(reader).getAsJsonObject());
            Validation validation = loaded.validate();
            current = loaded;
            if (Main.logger != null) {
                Main.logger.info("Loaded Raizan animation: {} frames at {} fps; axis error={}, "
                                + "insertion error={}, max 0.1-frame rotation={} degrees",
                        loaded.frames, loaded.fps, validation.maxAxisError,
                        validation.maxInsertionError, validation.maxRotationDegrees);
            }
        } catch (Throwable exception) {
            if (Main.logger != null) {
                Main.logger.warn("Could not load Raizan animation; retaining last valid data", exception);
            }
        }
    }

    private static Animation parse(JsonObject root) {
        float fps = positive(number(root, "reference_fps"), "reference_fps");
        int frames = root.get("frame_count").getAsInt();
        if (frames < 2) {
            throw new IllegalArgumentException("frame_count must be at least 2");
        }

        JsonObject modelJson = root.getAsJsonObject("model");
        Model model = new Model(
                positive(number(modelJson, "scale"), "model.scale"),
                vector(modelJson.getAsJsonArray("pivot"), "model.pivot"),
                vector(modelJson.getAsJsonArray("axis_rotation_deg"), "model.axis_rotation_deg"));

        JsonObject tracksJson = root.getAsJsonObject("tracks");
        Map<String, Track> tracks = new HashMap<String, Track>();
        tracks.put("sheath", parseTrack(tracksJson.get("sheath"), frames, "sheath"));
        tracks.put("blade", parseTrack(tracksJson.get("blade"), frames, "blade"));

        Map<String, LayerWindow> layers = new HashMap<String, LayerWindow>();
        JsonObject layerJson = root.getAsJsonObject("layers");
        if (layerJson != null) {
            for (Map.Entry<String, JsonElement> entry : layerJson.entrySet()) {
                JsonArray values = entry.getValue().getAsJsonArray();
                if (values.size() != 4) {
                    throw new IllegalArgumentException("Layer " + entry.getKey()
                            + " must contain four frames");
                }
                LayerWindow window = new LayerWindow(values.get(0).getAsFloat(),
                        values.get(1).getAsFloat(), values.get(2).getAsFloat(),
                        values.get(3).getAsFloat());
                if (!(window.start <= window.peak && window.peak <= window.fade
                        && window.fade <= window.end)) {
                    throw new IllegalArgumentException("Layer frames are not monotonic: "
                            + entry.getKey());
                }
                layers.put(entry.getKey(), window);
            }
        }
        return new Animation(fps, frames, model,
                Collections.unmodifiableMap(tracks), Collections.unmodifiableMap(layers));
    }

    private static Track parseTrack(JsonElement element, int frameCount, String name) {
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException("Missing geometric track " + name);
        }
        JsonObject object = element.getAsJsonObject();
        JsonArray array = object.getAsJsonArray("segments");
        if (array == null || array.size() < 1) {
            throw new IllegalArgumentException("Track " + name + " needs at least one segment");
        }

        List<Segment> segments = new ArrayList<Segment>();
        float expectedStart = 0.0F;
        for (int i = 0; i < array.size(); i++) {
            Segment segment = parseSegment(array.get(i).getAsJsonObject(), name, i);
            if (Math.abs(segment.start - expectedStart) > 0.011F) {
                throw new IllegalArgumentException("Track " + name + " has a gap before segment " + i);
            }
            if (segment.end <= segment.start
                    || segment.end > frameCount - 1.0F + 0.001F) {
                throw new IllegalArgumentException("Invalid segment range in track " + name);
            }
            segments.add(segment);
            expectedStart = segment.end;
        }
        if (Math.abs(expectedStart - (frameCount - 1.0F)) > 0.011F) {
            throw new IllegalArgumentException("Track " + name + " must finish at frame "
                    + (frameCount - 1));
        }

        Secondary secondary = object.has("secondary_motion")
                ? parseSecondary(object.getAsJsonObject("secondary_motion"), name)
                : Secondary.NONE;
        return new Track(Collections.unmodifiableList(segments), secondary);
    }

    private static Segment parseSegment(JsonObject json, String track, int index) {
        float start = number(json, "start");
        float end = number(json, "end");
        String easing = string(json, "easing", "hermite");

        JsonObject pathJson = json.getAsJsonObject("path");
        String pathType = string(pathJson, "type", "bezier");
        Path path = new Path(pathType);
        if ("line".equals(pathType)) {
            path.p0 = vector(pathJson.getAsJsonArray("p0"), track + ".p0");
            path.p1 = vector(pathJson.getAsJsonArray("p1"), track + ".p1");
        } else if ("bezier".equals(pathType)) {
            path.p0 = vector(pathJson.getAsJsonArray("p0"), track + ".p0");
            path.p1 = vector(pathJson.getAsJsonArray("p1"), track + ".p1");
            path.p2 = vector(pathJson.getAsJsonArray("p2"), track + ".p2");
            path.p3 = vector(pathJson.getAsJsonArray("p3"), track + ".p3");
        } else if ("ellipse".equals(pathType)) {
            path.center = vector(pathJson.getAsJsonArray("center"), track + ".center");
            path.axisU = vector(pathJson.getAsJsonArray("axis_u"), track + ".axis_u");
            path.axisV = vector(pathJson.getAsJsonArray("axis_v"), track + ".axis_v");
            path.drift = optionalVector(pathJson, "drift");
            path.startAngle = Math.toRadians(number(pathJson, "start_angle_deg"));
            path.sweep = Math.toRadians(number(pathJson, "sweep_deg"));
        } else if ("axis_draw".equals(pathType)) {
            path.reference = pathJson.get("reference").getAsString();
            path.distanceStart = number(pathJson, "distance_start");
            path.distanceEnd = number(pathJson, "distance_end");
        } else if ("damped_settle".equals(pathType)) {
            path.p0 = vector(pathJson.getAsJsonArray("p0"), track + ".p0");
            path.p1 = vector(pathJson.getAsJsonArray("p1"), track + ".p1");
            path.axisV = optionalVector(pathJson, "amplitude");
            path.cycles = pathJson.has("cycles") ? number(pathJson, "cycles") : 1.0F;
            path.damping = pathJson.has("damping") ? number(pathJson, "damping") : 2.0F;
        } else {
            throw new IllegalArgumentException("Unknown path type " + pathType
                    + " in " + track + " segment " + index);
        }

        JsonObject orientationJson = json.getAsJsonObject("orientation");
        String mode = orientationJson.get("mode").getAsString();
        OrientationSpec orientation = new OrientationSpec(mode);
        orientation.hasStartTip = orientationJson.has("start_tip");
        orientation.hasEndTip = orientationJson.has("end_tip");
        orientation.startTip = orientation.hasStartTip
                ? normalized(orientationJson.getAsJsonArray("start_tip"), track + ".start_tip")
                : MODEL_AXIS;
        orientation.endTip = orientation.hasEndTip
                ? normalized(orientationJson.getAsJsonArray("end_tip"), track + ".end_tip")
                : orientation.startTip;
        orientation.forward = orientationJson.has("forward")
                ? normalized(orientationJson.getAsJsonArray("forward"), track + ".forward")
                : new Vec3d(0.0D, 0.0D, 1.0D);
        orientation.startRoll = orientationJson.has("start_roll_deg")
                ? Math.toRadians(number(orientationJson, "start_roll_deg")) : 0.0D;
        orientation.endRoll = orientationJson.has("end_roll_deg")
                ? Math.toRadians(number(orientationJson, "end_roll_deg"))
                : orientation.startRoll;
        orientation.tangentWeight = orientationJson.has("tangent_weight")
                ? number(orientationJson, "tangent_weight") : 0.35F;
        orientation.tangentSign = orientationJson.has("tangent_sign")
                ? number(orientationJson, "tangent_sign") : 1.0F;
        orientation.reference = orientationJson.has("reference")
                ? orientationJson.get("reference").getAsString() : path.reference;
        return new Segment(start, end, easing, path, orientation);
    }

    private static Secondary parseSecondary(JsonObject json, String track) {
        Vec3d amplitude = optionalVector(json, "position_amplitude");
        float period = positive(number(json, "period_frames"), track + ".period_frames");
        double phase = json.has("phase_deg")
                ? Math.toRadians(number(json, "phase_deg")) : 0.0D;
        double roll = json.has("roll_amplitude_deg")
                ? Math.toRadians(number(json, "roll_amplitude_deg")) : 0.0D;
        return new Secondary(amplitude, period, phase, roll);
    }

    private static float positive(float value, String field) {
        if (!Float.isFinite(value) || value <= 0.0F) {
            throw new IllegalArgumentException(field + " must be finite and positive");
        }
        return value;
    }

    private static float number(JsonObject object, String name) {
        JsonElement element = object.get(name);
        if (element == null) {
            throw new IllegalArgumentException("Missing number " + name);
        }
        float value = element.getAsFloat();
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
        return value;
    }

    private static String string(JsonObject object, String name, String fallback) {
        return object.has(name) ? object.get(name).getAsString() : fallback;
    }

    private static Vec3d optionalVector(JsonObject object, String name) {
        return object.has(name) ? vector(object.getAsJsonArray(name), name) : ZERO;
    }

    private static Vec3d vector(JsonArray array, String field) {
        if (array == null || array.size() != 3) {
            throw new IllegalArgumentException(field + " must contain three values");
        }
        double x = array.get(0).getAsDouble();
        double y = array.get(1).getAsDouble();
        double z = array.get(2).getAsDouble();
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException(field + " must contain finite values");
        }
        return new Vec3d(x, y, z);
    }

    private static Vec3d normalized(JsonArray array, String field) {
        Vec3d result = vector(array, field);
        if (result.lengthSquared() < 1.0E-12D) {
            throw new IllegalArgumentException("Zero direction in " + field);
        }
        return result.normalize();
    }

    public static final class Animation {
        public final float fps;
        public final int frames;
        public final Model model;
        private final Map<String, Track> tracks;
        private final Map<String, LayerWindow> layers;

        private Animation(float fps, int frames, Model model, Map<String, Track> tracks,
                          Map<String, LayerWindow> layers) {
            this.fps = fps;
            this.frames = frames;
            this.model = model;
            this.tracks = tracks;
            this.layers = layers;
        }

        public Pose sample(String name, float frame) {
            return sampleInternal(name, frame, true, 0);
        }

        public float layer(String name, float frame) {
            LayerWindow window = layers.get(name);
            return window == null ? 0.0F : window.envelope(frame);
        }

        private Pose sampleInternal(String name, float frame, boolean secondary, int depth) {
            if (depth > 4) {
                throw new IllegalStateException("Circular Raizan track reference involving " + name);
            }
            Track track = tracks.get(name);
            if (track == null) {
                throw new IllegalArgumentException("Unknown Raizan animation track: " + name);
            }
            float clamped = MathHelper.clamp(frame, 0.0F, frames - 1.0F);

            // 1.20 的拔刀/回鞘段让刀完整继承刀鞘的次级位移和次级旋转。
            // 普通 reference 采样会关闭次级运动，不能替代这个专用分支。
            if (secondary) {
                Segment coupled = segmentAt(track, clamped);
                if ("axis_draw".equals(coupled.path.type)) {
                    float raw = (clamped - coupled.start)
                            / Math.max(coupled.end - coupled.start, 1.0E-4F);
                    float t = ease(raw, coupled.easing);
                    Pose reference = sampleInternal(coupled.path.reference,
                            clamped, true, depth + 1);
                    double distance = lerp(t, coupled.path.distanceStart,
                            coupled.path.distanceEnd);
                    Vec3d position = reference.position.subtract(
                            reference.direction.scale(distance));
                    return new Pose(position, reference.direction, reference.rollRadians,
                            reference.rotation);
                }
            }

            int index = segmentIndex(track, clamped);
            Segment segment = track.segments.get(index);
            float raw = (clamped - segment.start)
                    / Math.max(segment.end - segment.start, 1.0E-4F);
            float t = ease(raw, segment.easing);
            Vec3d position = pathPosition(name, segment, clamped, t, depth);
            Quat rotation = segmentOrientation(name, track, index, clamped, t, depth);
            Vec3d direction = rotation.transform(MODEL_AXIS).normalize();
            double roll = lerp(t, segment.orientation.startRoll,
                    segment.orientation.endRoll);
            Pose pose = new Pose(position, direction, roll, rotation);
            return !secondary || track.secondary == Secondary.NONE
                    ? pose : applySecondary(pose, track.secondary, clamped);
        }

        private Vec3d pathPosition(String trackName, Segment segment, float frame,
                                   float t, int depth) {
            Path path = segment.path;
            if ("line".equals(path.type)) {
                return lerp(path.p0, path.p1, t);
            }
            if ("bezier".equals(path.type)) {
                return bezier(path.p0, path.p1, path.p2, path.p3, t);
            }
            if ("ellipse".equals(path.type)) {
                double angle = path.startAngle + path.sweep * t;
                return path.center.add(path.axisU.scale(Math.cos(angle)))
                        .add(path.axisV.scale(Math.sin(angle))).add(path.drift.scale(t));
            }
            if ("axis_draw".equals(path.type)) {
                Pose reference = sampleInternal(path.reference, frame, false, depth + 1);
                double distance = lerp(t, path.distanceStart, path.distanceEnd);
                // 刀尖方向是由握点向刀尖，拔出位置必须沿其反方向偏移。
                return reference.position.subtract(reference.direction.scale(distance));
            }
            if ("damped_settle".equals(path.type)) {
                Vec3d base = lerp(path.p0, path.p1, smooth(t));
                double endpoint = Math.sin(t * Math.PI);
                double wave = endpoint * endpoint
                        * Math.sin(t * Math.PI * 2.0D * path.cycles)
                        * Math.exp(-path.damping * t);
                return base.add(path.axisV.scale(wave));
            }
            throw new IllegalStateException("Unhandled path type " + path.type
                    + " for " + trackName);
        }

        private Quat segmentOrientation(String trackName, Track track, int index,
                                        float frame, float t, int depth) {
            Segment segment = track.segments.get(index);
            OrientationSpec spec = segment.orientation;
            double roll = lerp(smooth(t), spec.startRoll, spec.endRoll);
            if ("reference".equals(spec.mode)) {
                return sampleInternal(spec.reference, frame, false, depth + 1).rotation;
            }
            if ("path_tangent".equals(spec.mode) || "forward_tangent".equals(spec.mode)) {
                Vec3d tangent = pathTangent(trackName, segment, frame, t, depth)
                        .scale(spec.tangentSign);
                Vec3d direction;
                if ("forward_tangent".equals(spec.mode)) {
                    direction = spec.forward.scale(1.0D - spec.tangentWeight)
                            .add(tangent.scale(spec.tangentWeight)).normalize();
                    if (spec.hasStartTip) {
                        double entrance = smooth(MathHelper.clamp(t / 0.20F, 0.0F, 1.0F));
                        direction = lerp(spec.startTip, direction, entrance).normalize();
                    }
                    if (spec.hasEndTip) {
                        double exit = smooth(MathHelper.clamp((t - 0.80F) / 0.20F,
                                0.0F, 1.0F));
                        direction = lerp(direction, spec.endTip, exit).normalize();
                    }
                } else {
                    direction = tangent.normalize();
                }
                return Quat.orientation(direction, roll);
            }
            if ("locked".equals(spec.mode)) {
                return Quat.orientation(spec.startTip, spec.startRoll);
            }
            if ("explicit_squad".equals(spec.mode)) {
                return squadOrientation(track, index, t);
            }
            throw new IllegalStateException("Unknown orientation mode " + spec.mode);
        }

        private Vec3d pathTangent(String trackName, Segment segment, float frame,
                                  float t, int depth) {
            float epsilon = 0.0015F;
            float a = MathHelper.clamp(t - epsilon, 0.0F, 1.0F);
            float b = MathHelper.clamp(t + epsilon, 0.0F, 1.0F);
            float frameA = (float) lerp(a, segment.start, segment.end);
            float frameB = (float) lerp(b, segment.start, segment.end);
            Vec3d tangent = pathPosition(trackName, segment, frameB, b, depth)
                    .subtract(pathPosition(trackName, segment, frameA, a, depth));
            return tangent.lengthSquared() < 1.0E-9D ? MODEL_AXIS : tangent.normalize();
        }

        private static Quat squadOrientation(Track track, int index, float t) {
            Segment current = track.segments.get(index);
            Quat q0 = Quat.orientation(current.orientation.startTip,
                    current.orientation.startRoll);
            Quat q1 = Quat.sameHemisphere(q0, Quat.orientation(
                    current.orientation.endTip, current.orientation.endRoll));
            Quat previous = q0;
            Quat next = q1;
            if (index > 0 && "explicit_squad".equals(
                    track.segments.get(index - 1).orientation.mode)) {
                OrientationSpec spec = track.segments.get(index - 1).orientation;
                previous = Quat.sameHemisphere(q0,
                        Quat.orientation(spec.startTip, spec.startRoll));
            }
            if (index + 1 < track.segments.size() && "explicit_squad".equals(
                    track.segments.get(index + 1).orientation.mode)) {
                OrientationSpec spec = track.segments.get(index + 1).orientation;
                next = Quat.sameHemisphere(q1,
                        Quat.orientation(spec.endTip, spec.endRoll));
            }
            Quat a = Quat.squadControl(previous, q0, q1);
            Quat b = Quat.squadControl(q0, q1, next);
            a = Quat.slerp(q0, Quat.sameHemisphere(q0, a), 0.35D);
            b = Quat.slerp(q1, Quat.sameHemisphere(q1, b), 0.35D);
            Quat direct = Quat.slerp(q0, q1, t);
            Quat control = Quat.slerp(a, b, t);
            return Quat.slerp(direct, control, 2.0D * t * (1.0D - t)).normalized();
        }

        private static Pose applySecondary(Pose pose, Secondary secondary, float frame) {
            double endpointWindow = Math.sin(MathHelper.clamp(frame / 66.0F,
                    0.0F, 1.0F) * Math.PI);
            double phase = frame * Math.PI * 2.0D / secondary.period + secondary.phase;
            Vec3d offset = new Vec3d(
                    secondary.amplitude.x * Math.sin(phase),
                    secondary.amplitude.y * Math.sin(phase * 1.31D + 1.1D),
                    secondary.amplitude.z * Math.cos(phase * 0.83D + 0.4D))
                    .scale(endpointWindow);
            double rollOffset = secondary.rollAmplitude * Math.sin(phase * 1.07D)
                    * endpointWindow;
            Quat rotation = pose.rotation.multiply(
                    Quat.axisAngle(0.0D, -1.0D, 0.0D, rollOffset)).normalized();
            Vec3d direction = rotation.transform(MODEL_AXIS).normalize();
            return new Pose(pose.position.add(offset), direction,
                    pose.rollRadians + rollOffset, rotation);
        }

        private Validation validate() {
            double maxAxisError = 0.0D;
            double maxInsertionError = 0.0D;
            double maxRotation = 0.0D;
            Pose previousBlade = null;
            Pose previousSheath = null;
            for (int sample = 0; sample <= 660; sample++) {
                float frame = sample * 0.1F;
                Pose blade = sample("blade", frame);
                Pose sheath = sample("sheath", frame);
                validateFinite(blade, "blade", frame);
                validateFinite(sheath, "sheath", frame);

                if (isAxisCoupledFrame(frame)) {
                    Vec3d axis = sheath.direction.normalize();
                    Vec3d delta = blade.position.subtract(sheath.position);
                    double error = Math.sqrt(delta.subtract(axis.scale(
                            delta.dotProduct(axis))).lengthSquared());
                    maxAxisError = Math.max(maxAxisError, error);
                    if (frame >= 56.5F && frame <= 60.5F) {
                        maxInsertionError = Math.max(maxInsertionError, error);
                    }
                }
                if (previousBlade != null) {
                    double bladeAngle = Quat.angleDegrees(previousBlade.rotation, blade.rotation);
                    double sheathAngle = Quat.angleDegrees(previousSheath.rotation, sheath.rotation);
                    maxRotation = Math.max(maxRotation, Math.max(bladeAngle, sheathAngle));
                    if (bladeAngle > 75.0D || sheathAngle > 75.0D) {
                        throw new IllegalArgumentException(
                                "Raizan quaternion discontinuity near frame " + frame);
                    }
                }
                previousBlade = blade;
                previousSheath = sheath;
            }
            if (maxAxisError > 0.02D || maxInsertionError > 0.015D) {
                throw new IllegalArgumentException("Raizan sheath-axis coupling exceeds tolerance: "
                        + maxAxisError + "/" + maxInsertionError);
            }
            Pose finalPose = sample("blade", 66.0F);
            if (finalPose.position.distanceTo(new Vec3d(-0.68D, 1.24D, -0.04D)) > 1.0E-4D) {
                throw new IllegalArgumentException(
                        "Raizan final blade grip no longer matches the approved pose");
            }
            return new Validation(maxAxisError, maxInsertionError, maxRotation);
        }

        private static boolean isAxisCoupledFrame(float frame) {
            return frame <= 8.6F || (frame >= 25.0F && frame <= 31.2F)
                    || frame >= 56.5F;
        }

        private static void validateFinite(Pose pose, String track, float frame) {
            if (!finite(pose.position) || !finite(pose.direction)
                    || !pose.rotation.isFinite()) {
                throw new IllegalArgumentException("Non-finite " + track
                        + " transform at frame " + frame);
            }
        }

        private static Segment segmentAt(Track track, float frame) {
            return track.segments.get(segmentIndex(track, frame));
        }

        private static int segmentIndex(Track track, float frame) {
            for (int i = 0; i < track.segments.size(); i++) {
                if (frame <= track.segments.get(i).end + 1.0E-4F) {
                    return i;
                }
            }
            return track.segments.size() - 1;
        }

        private static Animation fallback() {
            Path path = new Path("bezier");
            path.p0 = path.p1 = path.p2 = path.p3 = new Vec3d(-0.60D, 1.38D, -0.06D);
            OrientationSpec orientation = new OrientationSpec("locked");
            orientation.startTip = orientation.endTip =
                    new Vec3d(0.18D, -0.27D, -0.94D).normalize();
            orientation.startRoll = orientation.endRoll = Math.toRadians(99.17D);
            Segment segment = new Segment(0.0F, 66.0F, "linear", path, orientation);
            Track track = new Track(Collections.singletonList(segment), Secondary.NONE);
            Map<String, Track> tracks = new HashMap<String, Track>();
            tracks.put("blade", track);
            tracks.put("sheath", track);
            return new Animation(12.76F, 67,
                    new Model(0.82F, new Vec3d(0.0D, -0.59D, 0.0D),
                            new Vec3d(-90.0D, 0.0D, 0.0D)),
                    Collections.unmodifiableMap(tracks),
                    Collections.<String, LayerWindow>emptyMap());
        }
    }

    public static final class Pose {
        public final Vec3d position;
        public final Vec3d direction;
        public final double rollRadians;
        private final Quat rotation;

        private Pose(Vec3d position, Vec3d direction, double rollRadians, Quat rotation) {
            this.position = position;
            this.direction = direction;
            this.rollRadians = rollRadians;
            this.rotation = rotation;
        }

        /** 写入与 PoseStack.mulPose 完全同义的 OpenGL 列主序旋转矩阵。 */
        public void writeRotationMatrix(FloatBuffer buffer) {
            double x = rotation.x;
            double y = rotation.y;
            double z = rotation.z;
            double w = rotation.w;
            double xx = x * x;
            double yy = y * y;
            double zz = z * z;
            double xy = x * y;
            double xz = x * z;
            double yz = y * z;
            double wx = w * x;
            double wy = w * y;
            double wz = w * z;

            // glMultMatrix 读取列主序；每组四项是一列。
            buffer.clear();
            buffer.put((float) (1.0D - 2.0D * (yy + zz)));
            buffer.put((float) (2.0D * (xy + wz)));
            buffer.put((float) (2.0D * (xz - wy)));
            buffer.put(0.0F);
            buffer.put((float) (2.0D * (xy - wz)));
            buffer.put((float) (1.0D - 2.0D * (xx + zz)));
            buffer.put((float) (2.0D * (yz + wx)));
            buffer.put(0.0F);
            buffer.put((float) (2.0D * (xz + wy)));
            buffer.put((float) (2.0D * (yz - wx)));
            buffer.put((float) (1.0D - 2.0D * (xx + yy)));
            buffer.put(0.0F);
            buffer.put(0.0F).put(0.0F).put(0.0F).put(1.0F);
            buffer.flip();
        }
    }

    public static final class Model {
        public final float scale;
        public final Vec3d pivot;
        public final Vec3d axis;

        private Model(float scale, Vec3d pivot, Vec3d axis) {
            this.scale = scale;
            this.pivot = pivot;
            this.axis = axis;
        }
    }

    private static final class Track {
        private final List<Segment> segments;
        private final Secondary secondary;

        private Track(List<Segment> segments, Secondary secondary) {
            this.segments = segments;
            this.secondary = secondary;
        }
    }

    private static final class Segment {
        private final float start;
        private final float end;
        private final String easing;
        private final Path path;
        private final OrientationSpec orientation;

        private Segment(float start, float end, String easing, Path path,
                        OrientationSpec orientation) {
            this.start = start;
            this.end = end;
            this.easing = easing;
            this.path = path;
            this.orientation = orientation;
        }
    }

    private static final class Path {
        private final String type;
        private Vec3d p0 = ZERO;
        private Vec3d p1 = ZERO;
        private Vec3d p2 = ZERO;
        private Vec3d p3 = ZERO;
        private Vec3d center = ZERO;
        private Vec3d axisU = ZERO;
        private Vec3d axisV = ZERO;
        private Vec3d drift = ZERO;
        private double startAngle;
        private double sweep;
        private String reference;
        private double distanceStart;
        private double distanceEnd;
        private double cycles;
        private double damping;

        private Path(String type) {
            this.type = type;
        }
    }

    private static final class OrientationSpec {
        private final String mode;
        private Vec3d startTip = MODEL_AXIS;
        private Vec3d endTip = MODEL_AXIS;
        private double startRoll;
        private double endRoll;
        private Vec3d forward = new Vec3d(0.0D, 0.0D, 1.0D);
        private double tangentWeight = 0.35D;
        private double tangentSign = 1.0D;
        private String reference;
        private boolean hasStartTip;
        private boolean hasEndTip;

        private OrientationSpec(String mode) {
            this.mode = mode;
        }
    }

    private static final class Secondary {
        private static final Secondary NONE =
                new Secondary(ZERO, 1.0F, 0.0D, 0.0D);
        private final Vec3d amplitude;
        private final float period;
        private final double phase;
        private final double rollAmplitude;

        private Secondary(Vec3d amplitude, float period, double phase, double rollAmplitude) {
            this.amplitude = amplitude;
            this.period = period;
            this.phase = phase;
            this.rollAmplitude = rollAmplitude;
        }
    }

    private static final class LayerWindow {
        private final float start;
        private final float peak;
        private final float fade;
        private final float end;

        private LayerWindow(float start, float peak, float fade, float end) {
            this.start = start;
            this.peak = peak;
            this.fade = fade;
            this.end = end;
        }

        private float envelope(float frame) {
            if (frame <= start || frame >= end) return 0.0F;
            if (frame < peak) {
                return smooth((frame - start) / Math.max(peak - start, 1.0E-4F));
            }
            if (frame <= fade) return 1.0F;
            return 1.0F - smooth((frame - fade) / Math.max(end - fade, 1.0E-4F));
        }
    }

    private static final class Validation {
        private final double maxAxisError;
        private final double maxInsertionError;
        private final double maxRotationDegrees;

        private Validation(double maxAxisError, double maxInsertionError,
                           double maxRotationDegrees) {
            this.maxAxisError = maxAxisError;
            this.maxInsertionError = maxInsertionError;
            this.maxRotationDegrees = maxRotationDegrees;
        }
    }

    /** Java 8 四元数，公式逐项对应 1.20/JOML 的 rotationTo、slerp 与 squad。 */
    private static final class Quat {
        private final double x;
        private final double y;
        private final double z;
        private final double w;

        private Quat(double x, double y, double z, double w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        private static Quat orientation(Vec3d direction, double rollRadians) {
            Vec3d target = direction.normalize();
            double dot = MathHelper.clamp(MODEL_AXIS.dotProduct(target), -1.0D, 1.0D);
            Quat base;
            if (dot < -0.999999D) {
                // JOML rotationTo 对 (0,-1,0) 的反向退化轴为 X。
                base = new Quat(1.0D, 0.0D, 0.0D, 0.0D);
            } else {
                Vec3d cross = MODEL_AXIS.crossProduct(target);
                base = new Quat(cross.x, cross.y, cross.z, 1.0D + dot).normalized();
            }
            return base.multiply(axisAngle(0.0D, -1.0D, 0.0D, rollRadians)).normalized();
        }

        private static Quat axisAngle(double ax, double ay, double az, double angle) {
            double half = angle * 0.5D;
            double sine = Math.sin(half);
            return new Quat(ax * sine, ay * sine, az * sine, Math.cos(half)).normalized();
        }

        private Quat multiply(Quat other) {
            return new Quat(
                    w * other.x + x * other.w + y * other.z - z * other.y,
                    w * other.y - x * other.z + y * other.w + z * other.x,
                    w * other.z + x * other.y - y * other.x + z * other.w,
                    w * other.w - x * other.x - y * other.y - z * other.z);
        }

        private Quat conjugate() {
            return new Quat(-x, -y, -z, w);
        }

        private Quat normalized() {
            double length = Math.sqrt(x * x + y * y + z * z + w * w);
            if (length < 1.0E-12D) {
                return new Quat(0.0D, 0.0D, 0.0D, 1.0D);
            }
            return new Quat(x / length, y / length, z / length, w / length);
        }

        private Vec3d transform(Vec3d vector) {
            Vec3d u = new Vec3d(x, y, z);
            return u.scale(2.0D * u.dotProduct(vector))
                    .add(vector.scale(w * w - u.dotProduct(u)))
                    .add(u.crossProduct(vector).scale(2.0D * w));
        }

        private boolean isFinite() {
            return Double.isFinite(x) && Double.isFinite(y)
                    && Double.isFinite(z) && Double.isFinite(w);
        }

        private static Quat sameHemisphere(Quat reference, Quat value) {
            double dot = reference.x * value.x + reference.y * value.y
                    + reference.z * value.z + reference.w * value.w;
            return dot < 0.0D
                    ? new Quat(-value.x, -value.y, -value.z, -value.w) : value;
        }

        private static Quat slerp(Quat first, Quat value, double t) {
            Quat second = sameHemisphere(first, value);
            double dot = MathHelper.clamp(first.x * second.x + first.y * second.y
                    + first.z * second.z + first.w * second.w, -1.0D, 1.0D);
            if (dot > 0.9995D) {
                return new Quat(
                        first.x + (second.x - first.x) * t,
                        first.y + (second.y - first.y) * t,
                        first.z + (second.z - first.z) * t,
                        first.w + (second.w - first.w) * t).normalized();
            }
            double theta = Math.acos(dot);
            double sine = Math.sin(theta);
            return new Quat(
                    (first.x * Math.sin((1.0D - t) * theta)
                            + second.x * Math.sin(t * theta)) / sine,
                    (first.y * Math.sin((1.0D - t) * theta)
                            + second.y * Math.sin(t * theta)) / sine,
                    (first.z * Math.sin((1.0D - t) * theta)
                            + second.z * Math.sin(t * theta)) / sine,
                    (first.w * Math.sin((1.0D - t) * theta)
                            + second.w * Math.sin(t * theta)) / sine).normalized();
        }

        private static Quat squadControl(Quat previous, Quat current, Quat next) {
            Quat inverse = current.conjugate().normalized();
            Quat before = logarithm(inverse.multiply(previous).normalized());
            Quat after = logarithm(inverse.multiply(next).normalized());
            Quat average = new Quat(
                    -(before.x + after.x) * 0.25D,
                    -(before.y + after.y) * 0.25D,
                    -(before.z + after.z) * 0.25D, 0.0D);
            return current.multiply(exponential(average)).normalized();
        }

        private static Quat logarithm(Quat quaternion) {
            double angle = Math.acos(MathHelper.clamp(quaternion.w, -1.0D, 1.0D));
            double sine = Math.sin(angle);
            double scale = Math.abs(sine) < 1.0E-6D ? 1.0D : angle / sine;
            return new Quat(quaternion.x * scale, quaternion.y * scale,
                    quaternion.z * scale, 0.0D);
        }

        private static Quat exponential(Quat quaternion) {
            double angle = Math.sqrt(quaternion.x * quaternion.x
                    + quaternion.y * quaternion.y + quaternion.z * quaternion.z);
            double scale = angle < 1.0E-6D ? 1.0D : Math.sin(angle) / angle;
            return new Quat(quaternion.x * scale, quaternion.y * scale,
                    quaternion.z * scale, Math.cos(angle));
        }

        private static double angleDegrees(Quat first, Quat second) {
            double dot = Math.abs(first.x * second.x + first.y * second.y
                    + first.z * second.z + first.w * second.w);
            return Math.toDegrees(2.0D * Math.acos(MathHelper.clamp(dot, 0.0D, 1.0D)));
        }
    }

    private static Vec3d bezier(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, float t) {
        double u = 1.0D - t;
        return p0.scale(u * u * u)
                .add(p1.scale(3.0D * u * u * t))
                .add(p2.scale(3.0D * u * t * t))
                .add(p3.scale(t * t * t));
    }

    private static Vec3d lerp(Vec3d first, Vec3d second, double t) {
        return first.add(second.subtract(first).scale(t));
    }

    private static double lerp(double t, double first, double second) {
        return first + (second - first) * t;
    }

    private static float ease(float value, String easing) {
        float t = MathHelper.clamp(value, 0.0F, 1.0F);
        if ("linear".equals(easing)) return t;
        if ("accelerate".equals(easing)) return (float) Math.pow(t, 1.35D);
        if ("decelerate".equals(easing)) {
            return 1.0F - (float) Math.pow(1.0F - t, 1.45D);
        }
        if ("hermite".equals(easing) || "smooth".equals(easing)) return smooth(t);
        if ("smoother".equals(easing)) {
            return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
        }
        throw new IllegalArgumentException("Unknown Raizan easing: " + easing);
    }

    private static float smooth(float value) {
        float t = MathHelper.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static boolean finite(Vec3d value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y)
                && Double.isFinite(value.z);
    }
}

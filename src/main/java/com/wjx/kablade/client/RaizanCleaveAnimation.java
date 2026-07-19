package com.wjx.kablade.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.renderer.RaizanCleavePostPipeline;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Reloadable geometric rigid-body animation for Raizan's floating blade and sheath. */
public final class RaizanCleaveAnimation implements ResourceManagerReloadListener {

    public static final RaizanCleaveAnimation INSTANCE = new RaizanCleaveAnimation();
    private static final ResourceLocation RESOURCE =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "kablade_animations/raizan_cleave.json");
    private static final Vector3f MODEL_AXIS = new Vector3f(0.0F, -1.0F, 0.0F);
    private static final Vec3 ZERO = Vec3.ZERO;

    private volatile Animation animation = fallback();

    private RaizanCleaveAnimation() {
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        RaizanCleavePostPipeline.invalidateResources();
        try (BufferedReader reader = manager.getResourceOrThrow(RESOURCE).openAsReader()) {
            Animation loaded = parse(JsonParser.parseReader(reader).getAsJsonObject());
            this.animation = loaded;
            Main.LOGGER.info("Loaded Raizan Cleave geometric animation: {} frames at {} fps",
                    loaded.frameCount, loaded.referenceFps);
        } catch (RuntimeException | IOException exception) {
            Main.LOGGER.error("Could not reload {}; retaining the previous valid Raizan animation.",
                    RESOURCE, exception);
        }
    }

    public Animation current() {
        return this.animation;
    }

    private static Animation parse(JsonObject root) {
        float fps = positive(root.get("reference_fps").getAsFloat(), "reference_fps");
        int frames = root.get("frame_count").getAsInt();
        if (frames < 2) {
            throw new IllegalArgumentException("frame_count must be at least 2");
        }

        JsonObject modelJson = root.getAsJsonObject("model");
        ModelSettings model = new ModelSettings(
                positive(modelJson.get("scale").getAsFloat(), "model.scale"),
                vec3(modelJson.getAsJsonArray("pivot"), "model.pivot"),
                vec3f(modelJson.getAsJsonArray("axis_rotation_deg"), "model.axis_rotation_deg"));

        JsonObject tracksJson = root.getAsJsonObject("tracks");
        Map<String, Track> tracks = new HashMap<>();
        tracks.put("sheath", parseTrack(tracksJson.get("sheath"), frames, "sheath"));
        tracks.put("blade", parseTrack(tracksJson.get("blade"), frames, "blade"));

        Map<String, LayerWindow> layers = new HashMap<>();
        JsonObject layersJson = root.getAsJsonObject("layers");
        for (Map.Entry<String, JsonElement> entry : layersJson.entrySet()) {
            JsonArray values = entry.getValue().getAsJsonArray();
            if (values.size() != 4) {
                throw new IllegalArgumentException("Layer " + entry.getKey() + " must contain four frames");
            }
            LayerWindow window = new LayerWindow(values.get(0).getAsFloat(), values.get(1).getAsFloat(),
                    values.get(2).getAsFloat(), values.get(3).getAsFloat());
            if (!(window.start <= window.peak && window.peak <= window.fade
                    && window.fade <= window.end)) {
                throw new IllegalArgumentException("Layer frames are not monotonic: " + entry.getKey());
            }
            layers.put(entry.getKey(), window);
        }
        Animation result = new Animation(fps, frames, model, Map.copyOf(tracks), Map.copyOf(layers));
        Validation validation = result.validate();
        Main.LOGGER.info("Validated Raizan animation: max axis error={} blocks, "
                        + "insertion error={} blocks, max 0.1-frame rotation={} degrees",
                validation.maxAxisError, validation.maxInsertionError,
                validation.maxRotationDegrees);
        return result;
    }

    private static Track parseTrack(JsonElement element, int frameCount, String name) {
        if (element == null) {
            throw new IllegalArgumentException("Missing track " + name);
        }
        if (element.isJsonArray()) {
            return new Track(parseLegacyKeys(element.getAsJsonArray(), frameCount, name),
                    List.of(), SecondaryMotion.NONE);
        }

        JsonObject object = element.getAsJsonObject();
        JsonArray array = object.getAsJsonArray("segments");
        if (array == null || array.size() < 1) {
            throw new IllegalArgumentException("Track " + name + " needs at least one segment");
        }
        List<Segment> segments = new ArrayList<>();
        float expectedStart = 0.0F;
        for (int i = 0; i < array.size(); i++) {
            Segment segment = parseSegment(array.get(i).getAsJsonObject(), name, i);
            if (Math.abs(segment.start - expectedStart) > 0.011F) {
                throw new IllegalArgumentException("Track " + name + " has a gap before segment " + i);
            }
            if (segment.end <= segment.start || segment.end > frameCount - 1.0F + 0.001F) {
                throw new IllegalArgumentException("Invalid segment range in track " + name);
            }
            segments.add(segment);
            expectedStart = segment.end;
        }
        if (Math.abs(expectedStart - (frameCount - 1.0F)) > 0.011F) {
            throw new IllegalArgumentException("Track " + name + " must finish at frame " + (frameCount - 1));
        }
        SecondaryMotion secondary = object.has("secondary_motion")
                ? parseSecondary(object.getAsJsonObject("secondary_motion"), name)
                : SecondaryMotion.NONE;
        return new Track(List.of(), Collections.unmodifiableList(segments), secondary);
    }

    private static List<Keyframe> parseLegacyKeys(JsonArray array, int frameCount, String name) {
        if (array.size() < 2) {
            throw new IllegalArgumentException("Track " + name + " needs at least two keyframes");
        }
        List<Keyframe> keys = new ArrayList<>();
        float previous = -Float.MAX_VALUE;
        for (JsonElement element : array) {
            JsonObject key = element.getAsJsonObject();
            float frame = key.get("frame").getAsFloat();
            if (!Float.isFinite(frame) || frame < previous || frame < 0.0F || frame > frameCount - 1) {
                throw new IllegalArgumentException("Invalid or unsorted frame in track " + name + ": " + frame);
            }
            Vector3f direction = normalized(key.getAsJsonArray("tip_direction"), name + ".tip_direction");
            String curve = key.has("curve") ? key.get("curve").getAsString() : "linear";
            String easing = key.has("easing") ? key.get("easing").getAsString() : "smooth";
            keys.add(new Keyframe(frame, vec3(key.getAsJsonArray("grip"), name + ".grip"),
                    direction, key.get("roll_deg").getAsFloat() * Mth.DEG_TO_RAD, curve, easing));
            previous = frame;
        }
        return Collections.unmodifiableList(keys);
    }

    private static Segment parseSegment(JsonObject json, String track, int index) {
        float start = json.get("start").getAsFloat();
        float end = json.get("end").getAsFloat();
        String easing = json.has("easing") ? json.get("easing").getAsString() : "hermite";
        JsonObject pathJson = json.getAsJsonObject("path");
        String type = pathJson.get("type").getAsString();
        PathSpec path = switch (type) {
            case "line" -> new PathSpec(type,
                    vec3(pathJson.getAsJsonArray("p0"), track + ".p0"),
                    vec3(pathJson.getAsJsonArray("p1"), track + ".p1"), ZERO, ZERO,
                    ZERO, ZERO, ZERO, ZERO, 0.0F, 0.0F,
                    null, 0.0F, 0.0F, 0.0F, 0.0F);
            case "bezier" -> new PathSpec(type,
                    vec3(pathJson.getAsJsonArray("p0"), track + ".p0"),
                    vec3(pathJson.getAsJsonArray("p1"), track + ".p1"),
                    vec3(pathJson.getAsJsonArray("p2"), track + ".p2"),
                    vec3(pathJson.getAsJsonArray("p3"), track + ".p3"),
                    ZERO, ZERO, ZERO, ZERO, 0.0F, 0.0F,
                    null, 0.0F, 0.0F, 0.0F, 0.0F);
            case "ellipse" -> new PathSpec(type, ZERO, ZERO, ZERO, ZERO,
                    vec3(pathJson.getAsJsonArray("center"), track + ".center"),
                    vec3(pathJson.getAsJsonArray("axis_u"), track + ".axis_u"),
                    vec3(pathJson.getAsJsonArray("axis_v"), track + ".axis_v"),
                    optionalVec3(pathJson, "drift"),
                    pathJson.get("start_angle_deg").getAsFloat() * Mth.DEG_TO_RAD,
                    pathJson.get("sweep_deg").getAsFloat() * Mth.DEG_TO_RAD,
                    null, 0.0F, 0.0F, 0.0F, 0.0F);
            case "axis_draw" -> new PathSpec(type, ZERO, ZERO, ZERO, ZERO,
                    ZERO, ZERO, ZERO, ZERO, 0.0F, 0.0F,
                    pathJson.get("reference").getAsString(),
                    pathJson.get("distance_start").getAsFloat(),
                    pathJson.get("distance_end").getAsFloat(), 0.0F, 0.0F);
            case "damped_settle" -> new PathSpec(type,
                    vec3(pathJson.getAsJsonArray("p0"), track + ".p0"),
                    vec3(pathJson.getAsJsonArray("p1"), track + ".p1"), ZERO, ZERO,
                    ZERO, ZERO, optionalVec3(pathJson, "amplitude"), ZERO,
                    0.0F, 0.0F, null, 0.0F, 0.0F,
                    pathJson.has("cycles") ? pathJson.get("cycles").getAsFloat() : 1.0F,
                    pathJson.has("damping") ? pathJson.get("damping").getAsFloat() : 2.0F);
            default -> throw new IllegalArgumentException("Unknown path type " + type
                    + " in " + track + " segment " + index);
        };

        JsonObject orientationJson = json.getAsJsonObject("orientation");
        String orientationMode = orientationJson.get("mode").getAsString();
        boolean hasStartTip = orientationJson.has("start_tip");
        boolean hasEndTip = orientationJson.has("end_tip");
        Vector3f startTip = orientationJson.has("start_tip")
                ? normalized(orientationJson.getAsJsonArray("start_tip"), track + ".start_tip")
                : new Vector3f(0.0F, -1.0F, 0.0F);
        Vector3f endTip = orientationJson.has("end_tip")
                ? normalized(orientationJson.getAsJsonArray("end_tip"), track + ".end_tip")
                : new Vector3f(startTip);
        Vector3f forward = orientationJson.has("forward")
                ? normalized(orientationJson.getAsJsonArray("forward"), track + ".forward")
                : new Vector3f(0.0F, 0.0F, 1.0F);
        float startRoll = orientationJson.has("start_roll_deg")
                ? orientationJson.get("start_roll_deg").getAsFloat() * Mth.DEG_TO_RAD : 0.0F;
        float endRoll = orientationJson.has("end_roll_deg")
                ? orientationJson.get("end_roll_deg").getAsFloat() * Mth.DEG_TO_RAD : startRoll;
        float tangentWeight = orientationJson.has("tangent_weight")
                ? orientationJson.get("tangent_weight").getAsFloat() : 0.35F;
        float tangentSign = orientationJson.has("tangent_sign")
                ? orientationJson.get("tangent_sign").getAsFloat() : 1.0F;
        String reference = orientationJson.has("reference")
                ? orientationJson.get("reference").getAsString() : path.reference;
        OrientationSpec orientation = new OrientationSpec(orientationMode, startTip, endTip,
                startRoll, endRoll, forward, tangentWeight, tangentSign, reference,
                hasStartTip, hasEndTip);
        return new Segment(start, end, easing, path, orientation);
    }

    private static SecondaryMotion parseSecondary(JsonObject json, String track) {
        Vec3 amplitude = optionalVec3(json, "position_amplitude");
        float period = positive(json.get("period_frames").getAsFloat(), track + ".period_frames");
        float phase = json.has("phase_deg") ? json.get("phase_deg").getAsFloat() * Mth.DEG_TO_RAD : 0.0F;
        float roll = json.has("roll_amplitude_deg")
                ? json.get("roll_amplitude_deg").getAsFloat() * Mth.DEG_TO_RAD : 0.0F;
        return new SecondaryMotion(amplitude, period, phase, roll);
    }

    private static float positive(float value, String field) {
        if (!Float.isFinite(value) || value <= 0.0F) {
            throw new IllegalArgumentException(field + " must be finite and positive");
        }
        return value;
    }

    private static Vec3 optionalVec3(JsonObject object, String name) {
        return object.has(name) ? vec3(object.getAsJsonArray(name), name) : ZERO;
    }

    private static Vec3 vec3(JsonArray array, String field) {
        if (array == null || array.size() != 3) {
            throw new IllegalArgumentException(field + " must contain three values");
        }
        double x = array.get(0).getAsDouble();
        double y = array.get(1).getAsDouble();
        double z = array.get(2).getAsDouble();
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException(field + " must contain finite values");
        }
        return new Vec3(x, y, z);
    }

    private static Vector3f vec3f(JsonArray array, String field) {
        Vec3 value = vec3(array, field);
        return new Vector3f((float) value.x, (float) value.y, (float) value.z);
    }

    private static Vector3f normalized(JsonArray array, String field) {
        Vector3f value = vec3f(array, field);
        if (value.lengthSquared() < 1.0E-6F) {
            throw new IllegalArgumentException("Zero direction in " + field);
        }
        return value.normalize();
    }

    private static Animation fallback() {
        Vector3f direction = new Vector3f(0.18F, -0.27F, -0.94F).normalize();
        Track track = new Track(List.of(
                new Keyframe(0.0F, new Vec3(-0.60D, 1.38D, -0.06D),
                        new Vector3f(direction), 99.17F * Mth.DEG_TO_RAD, "linear", "smooth"),
                new Keyframe(66.0F, new Vec3(-0.68D, 1.24D, -0.04D),
                        new Vector3f(0.12F, -0.58F, -0.80F).normalize(),
                        5.73F * Mth.DEG_TO_RAD, "linear", "smooth")),
                List.of(), SecondaryMotion.NONE);
        return new Animation(12.76F, 67,
                new ModelSettings(0.82F, new Vec3(0.0D, -0.59D, 0.0D),
                        new Vector3f(-90.0F, 0.0F, 0.0F)),
                Map.of("blade", track, "sheath", track), Map.of());
    }

    public record ModelSettings(float scale, Vec3 pivot, Vector3f axisRotationDegrees) {
    }

    public record WeaponPose(Vec3 grip, Vector3f tipDirection, float rollRadians,
                             Quaternionf orientation) {
    }

    private record Keyframe(float frame, Vec3 grip, Vector3f direction,
                            float rollRadians, String curve, String easing) {
    }

    private record Track(List<Keyframe> keys, List<Segment> segments, SecondaryMotion secondary) {
    }

    private record Segment(float start, float end, String easing, PathSpec path,
                           OrientationSpec orientation) {
    }

    private record PathSpec(String type, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                            Vec3 center, Vec3 axisU, Vec3 axisV, Vec3 drift,
                            float startAngle, float sweep, String reference,
                            float distanceStart, float distanceEnd, float cycles, float damping) {
    }

    private record OrientationSpec(String mode, Vector3f startTip, Vector3f endTip,
                                   float startRoll, float endRoll, Vector3f forward,
                                   float tangentWeight, float tangentSign, String reference,
                                   boolean hasStartTip, boolean hasEndTip) {
    }

    private record SecondaryMotion(Vec3 amplitude, float period, float phase, float rollAmplitude) {
        private static final SecondaryMotion NONE = new SecondaryMotion(ZERO, 1.0F, 0.0F, 0.0F);
    }

    private record LayerWindow(float start, float peak, float fade, float end) {
        private float envelope(float frame) {
            if (frame <= start || frame >= end) return 0.0F;
            if (frame < peak) return smooth((frame - start) / Math.max(peak - start, 1.0E-4F));
            if (frame <= fade) return 1.0F;
            return 1.0F - smooth((frame - fade) / Math.max(end - fade, 1.0E-4F));
        }
    }

    private record Validation(double maxAxisError, double maxInsertionError,
                              double maxRotationDegrees) {
    }

    public static final class Animation {
        private final float referenceFps;
        private final int frameCount;
        private final ModelSettings model;
        private final Map<String, Track> tracks;
        private final Map<String, LayerWindow> layers;

        private Animation(float referenceFps, int frameCount, ModelSettings model,
                          Map<String, Track> tracks, Map<String, LayerWindow> layers) {
            this.referenceFps = referenceFps;
            this.frameCount = frameCount;
            this.model = model;
            this.tracks = tracks;
            this.layers = layers;
        }

        public float referenceFps() {
            return referenceFps;
        }

        public int frameCount() {
            return frameCount;
        }

        public ModelSettings model() {
            return model;
        }

        public float envelope(String layer, float frame) {
            LayerWindow window = layers.get(layer);
            return window == null ? 0.0F : window.envelope(frame);
        }

        public WeaponPose sample(String name, float frame) {
            return sampleInternal(name, frame, true, 0);
        }

        private Validation validate() {
            double maxAxisError = 0.0D;
            double maxInsertionError = 0.0D;
            double maxRotation = 0.0D;
            WeaponPose previousBlade = null;
            WeaponPose previousSheath = null;
            for (int sample = 0; sample <= 660; sample++) {
                float frame = sample * 0.1F;
                WeaponPose blade = sample("blade", frame);
                WeaponPose sheath = sample("sheath", frame);
                validateFinite(blade, "blade", frame);
                validateFinite(sheath, "sheath", frame);

                if (isAxisCoupledFrame(frame)) {
                    Vec3 axis = vector(sheath.tipDirection).normalize();
                    Vec3 delta = blade.grip.subtract(sheath.grip);
                    double error = delta.subtract(axis.scale(delta.dot(axis))).length();
                    maxAxisError = Math.max(maxAxisError, error);
                    if (frame >= 56.5F && frame <= 60.5F) {
                        maxInsertionError = Math.max(maxInsertionError, error);
                    }
                }

                if (previousBlade != null) {
                    double bladeAngle = quaternionAngle(previousBlade.orientation, blade.orientation);
                    double sheathAngle = quaternionAngle(previousSheath.orientation, sheath.orientation);
                    maxRotation = Math.max(maxRotation, Math.max(bladeAngle, sheathAngle));
                    double bladeMotion = previousBlade.grip.distanceTo(blade.grip) + bladeAngle * 0.01D;
                    double sheathMotion = previousSheath.grip.distanceTo(sheath.grip) + sheathAngle * 0.01D;
                    if (bladeMotion < 1.0E-8D || sheathMotion < 1.0E-8D) {
                        throw new IllegalArgumentException("Raizan track becomes stationary near frame " + frame);
                    }
                    if (bladeAngle > 75.0D || sheathAngle > 75.0D) {
                        throw new IllegalArgumentException("Raizan quaternion discontinuity near frame " + frame);
                    }
                }
                previousBlade = blade;
                previousSheath = sheath;
            }
            if (maxAxisError > 0.02D || maxInsertionError > 0.015D) {
                throw new IllegalArgumentException("Raizan sheath-axis coupling exceeds tolerance: "
                        + maxAxisError + "/" + maxInsertionError);
            }
            WeaponPose finalPose = sample("blade", 66.0F);
            if (finalPose.grip.distanceTo(new Vec3(-0.68D, 1.24D, -0.04D)) > 1.0E-4D) {
                throw new IllegalArgumentException("Raizan final blade grip no longer matches the approved pose");
            }
            return new Validation(maxAxisError, maxInsertionError, maxRotation);
        }

        private static boolean isAxisCoupledFrame(float frame) {
            return frame <= 8.6F || (frame >= 25.0F && frame <= 31.2F)
                    || frame >= 56.5F;
        }

        private static void validateFinite(WeaponPose pose, String track, float frame) {
            Quaternionf q = pose.orientation;
            if (!Double.isFinite(pose.grip.x) || !Double.isFinite(pose.grip.y)
                    || !Double.isFinite(pose.grip.z) || !Float.isFinite(q.x)
                    || !Float.isFinite(q.y) || !Float.isFinite(q.z) || !Float.isFinite(q.w)) {
                throw new IllegalArgumentException("Non-finite " + track + " transform at frame " + frame);
            }
        }

        private static double quaternionAngle(Quaternionf a, Quaternionf b) {
            double dot = Math.abs(a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w);
            return Math.toDegrees(2.0D * Math.acos(Mth.clamp(dot, 0.0D, 1.0D)));
        }

        private WeaponPose sampleInternal(String name, float frame, boolean secondary, int depth) {
            if (depth > 4) {
                throw new IllegalStateException("Circular Raizan track reference involving " + name);
            }
            Track track = tracks.get(name);
            if (track == null) {
                throw new IllegalArgumentException("Unknown Raizan animation track: " + name);
            }
            float clamped = Mth.clamp(frame, 0.0F, frameCount - 1.0F);
            if (secondary && !track.segments.isEmpty()) {
                Segment coupled = segmentAt(track, clamped);
                if ("axis_draw".equals(coupled.path.type)) {
                    float raw = (clamped - coupled.start)
                            / Math.max(coupled.end - coupled.start, 1.0E-4F);
                    float t = ease(raw, coupled.easing);
                    WeaponPose reference = sampleInternal(coupled.path.reference,
                            clamped, true, depth + 1);
                    double distance = Mth.lerp(t, coupled.path.distanceStart,
                            coupled.path.distanceEnd);
                    Vec3 position = reference.grip.subtract(
                            vector(reference.tipDirection).scale(distance));
                    return new WeaponPose(position, new Vector3f(reference.tipDirection),
                            reference.rollRadians, new Quaternionf(reference.orientation));
                }
            }
            WeaponPose pose = track.segments.isEmpty()
                    ? sampleLegacy(track.keys, clamped)
                    : sampleGeometric(name, track, clamped, depth);
            if (!secondary || track.secondary == SecondaryMotion.NONE) {
                return pose;
            }
            return applySecondary(pose, track.secondary, clamped);
        }

        private WeaponPose sampleGeometric(String trackName, Track track, float frame, int depth) {
            int index = segmentIndex(track, frame);
            Segment segment = track.segments.get(index);
            float raw = (frame - segment.start) / Math.max(segment.end - segment.start, 1.0E-4F);
            float t = ease(raw, segment.easing);
            Vec3 position = pathPosition(trackName, segment, frame, t, depth);
            Quaternionf orientation = segmentOrientation(trackName, track, index, frame, t, depth);
            Vector3f direction = new Quaternionf(orientation).transform(new Vector3f(MODEL_AXIS)).normalize();
            float roll = Mth.lerp(t, segment.orientation.startRoll, segment.orientation.endRoll);
            return new WeaponPose(position, direction, roll, orientation);
        }

        private Vec3 pathPosition(String trackName, Segment segment, float frame, float t, int depth) {
            PathSpec path = segment.path;
            return switch (path.type) {
                case "line" -> path.p0.lerp(path.p1, t);
                case "bezier" -> bezier(path.p0, path.p1, path.p2, path.p3, t);
                case "ellipse" -> {
                    double angle = path.startAngle + path.sweep * t;
                    yield path.center.add(path.axisU.scale(Math.cos(angle)))
                            .add(path.axisV.scale(Math.sin(angle))).add(path.drift.scale(t));
                }
                case "axis_draw" -> {
                    WeaponPose reference = sampleInternal(path.reference, frame, false, depth + 1);
                    double distance = Mth.lerp(t, path.distanceStart, path.distanceEnd);
                    Vec3 axis = vector(reference.tipDirection);
                    yield reference.grip.subtract(axis.scale(distance));
                }
                case "damped_settle" -> {
                    Vec3 base = path.p0.lerp(path.p1, smooth(t));
                    double endpoint = Math.sin(t * Math.PI);
                    double wave = endpoint * endpoint
                            * Math.sin(t * Mth.TWO_PI * path.cycles)
                            * Math.exp(-path.damping * t);
                    yield base.add(path.axisV.scale(wave));
                }
                default -> throw new IllegalStateException("Unhandled path type " + path.type
                        + " for " + trackName);
            };
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

        private Vec3 pathTangent(String trackName, Segment segment, float frame, float t, int depth) {
            float epsilon = 0.0015F;
            float a = Mth.clamp(t - epsilon, 0.0F, 1.0F);
            float b = Mth.clamp(t + epsilon, 0.0F, 1.0F);
            float frameA = Mth.lerp(a, segment.start, segment.end);
            float frameB = Mth.lerp(b, segment.start, segment.end);
            Vec3 tangent = pathPosition(trackName, segment, frameB, b, depth)
                    .subtract(pathPosition(trackName, segment, frameA, a, depth));
            return tangent.lengthSqr() < 1.0E-9D ? new Vec3(0.0D, -1.0D, 0.0D) : tangent.normalize();
        }

        private Quaternionf segmentOrientation(String trackName, Track track, int index,
                                                float frame, float t, int depth) {
            Segment segment = track.segments.get(index);
            OrientationSpec spec = segment.orientation;
            float roll = Mth.lerp(smooth(t), spec.startRoll, spec.endRoll);
            return switch (spec.mode) {
                case "reference" -> {
                    WeaponPose reference = sampleInternal(spec.reference, frame, false, depth + 1);
                    yield new Quaternionf(reference.orientation);
                }
                case "path_tangent", "forward_tangent" -> {
                    Vec3 tangent = pathTangent(trackName, segment, frame, t, depth)
                            .scale(spec.tangentSign);
                    Vector3f direction;
                    if ("forward_tangent".equals(spec.mode)) {
                        Vector3f tangentVector = vec3f(tangent);
                        direction = new Vector3f(spec.forward).mul(1.0F - spec.tangentWeight)
                                .add(tangentVector.mul(spec.tangentWeight)).normalize();
                        if (spec.hasStartTip) {
                            float entrance = smooth(Mth.clamp(t / 0.20F, 0.0F, 1.0F));
                            direction = new Vector3f(spec.startTip).lerp(direction, entrance).normalize();
                        }
                        if (spec.hasEndTip) {
                            float exit = smooth(Mth.clamp((t - 0.80F) / 0.20F, 0.0F, 1.0F));
                            direction.lerp(spec.endTip, exit).normalize();
                        }
                    } else {
                        direction = vec3f(tangent).normalize();
                    }
                    yield orientation(direction, roll);
                }
                // A hard blade-plane constraint for committed cuts and their pose
                // holds. Unlike squad, neighbouring recovery rotations cannot pull
                // this segment away from its declared edge direction.
                case "locked" -> orientation(spec.startTip, spec.startRoll);
                case "explicit_squad" -> squadOrientation(track, index, t);
                default -> throw new IllegalStateException("Unknown orientation mode " + spec.mode);
            };
        }

        private static Quaternionf squadOrientation(Track track, int index, float t) {
            Segment current = track.segments.get(index);
            Quaternionf q0 = orientation(current.orientation.startTip, current.orientation.startRoll);
            Quaternionf q1 = sameHemisphere(q0,
                    orientation(current.orientation.endTip, current.orientation.endRoll));
            Quaternionf previous = q0;
            Quaternionf next = q1;
            if (index > 0 && "explicit_squad".equals(
                    track.segments.get(index - 1).orientation.mode)) {
                OrientationSpec spec = track.segments.get(index - 1).orientation;
                previous = sameHemisphere(q0, orientation(spec.startTip, spec.startRoll));
            }
            if (index + 1 < track.segments.size() && "explicit_squad".equals(
                    track.segments.get(index + 1).orientation.mode)) {
                OrientationSpec spec = track.segments.get(index + 1).orientation;
                next = sameHemisphere(q1, orientation(spec.endTip, spec.endRoll));
            }
            Quaternionf a = squadControl(previous, q0, q1);
            Quaternionf b = squadControl(q0, q1, next);
            // Raw squad controls can overshoot almost a half-turn when a short segment sits
            // between two very different blade rolls. Pulling them toward their endpoint
            // rotations retains C1-like angular flow without allowing the shortest-arc branch
            // to switch mid-segment (the visible 180-degree roll pop this format avoids).
            a = new Quaternionf(q0).slerp(sameHemisphere(q0, a), 0.35F).normalize();
            b = new Quaternionf(q1).slerp(sameHemisphere(q1, b), 0.35F).normalize();
            Quaternionf direct = new Quaternionf(q0).slerp(q1, t);
            Quaternionf control = new Quaternionf(a).slerp(b, t);
            return direct.slerp(control, 2.0F * t * (1.0F - t)).normalize();
        }

        private static WeaponPose applySecondary(WeaponPose pose, SecondaryMotion secondary, float frame) {
            float endpointWindow = Mth.sin(Mth.clamp(frame / 66.0F, 0.0F, 1.0F) * Mth.PI);
            double phase = frame * Mth.TWO_PI / secondary.period + secondary.phase;
            Vec3 offset = new Vec3(
                    secondary.amplitude.x * Math.sin(phase),
                    secondary.amplitude.y * Math.sin(phase * 1.31D + 1.1D),
                    secondary.amplitude.z * Math.cos(phase * 0.83D + 0.4D)).scale(endpointWindow);
            float rollOffset = secondary.rollAmplitude * Mth.sin((float) phase * 1.07F)
                    * endpointWindow;
            Quaternionf orientation = new Quaternionf(pose.orientation)
                    .rotateAxis(rollOffset, 0.0F, -1.0F, 0.0F).normalize();
            Vector3f direction = orientation.transform(new Vector3f(MODEL_AXIS)).normalize();
            return new WeaponPose(pose.grip.add(offset), direction,
                    pose.rollRadians + rollOffset, orientation);
        }

        private static WeaponPose sampleLegacy(List<Keyframe> keys, float frame) {
            if (frame <= keys.get(0).frame) return legacyPose(keys.get(0));
            for (int i = 1; i < keys.size(); i++) {
                Keyframe b = keys.get(i);
                if (frame <= b.frame) {
                    Keyframe a = keys.get(i - 1);
                    float raw = (frame - a.frame) / Math.max(b.frame - a.frame, 1.0E-4F);
                    float t = ease(raw, a.easing);
                    Vec3 position = "catmull".equals(a.curve)
                            ? catmull(keys.get(Math.max(0, i - 2)).grip, a.grip, b.grip,
                            keys.get(Math.min(keys.size() - 1, i + 1)).grip, t)
                            : a.grip.lerp(b.grip, t);
                    Quaternionf q0 = orientation(a.direction, a.rollRadians);
                    Quaternionf q1 = sameHemisphere(q0, orientation(b.direction, b.rollRadians));
                    Quaternionf orientation = q0.slerp(q1, t).normalize();
                    Vector3f direction = orientation.transform(new Vector3f(MODEL_AXIS)).normalize();
                    return new WeaponPose(position, direction,
                            Mth.lerp(t, a.rollRadians, b.rollRadians), orientation);
                }
            }
            return legacyPose(keys.get(keys.size() - 1));
        }

        private static WeaponPose legacyPose(Keyframe key) {
            return new WeaponPose(key.grip, new Vector3f(key.direction), key.rollRadians,
                    orientation(key.direction, key.rollRadians));
        }
    }

    private static Quaternionf orientation(Vector3f direction, float roll) {
        return new Quaternionf().rotationTo(new Vector3f(MODEL_AXIS),
                        new Vector3f(direction).normalize())
                .rotateAxis(roll, 0.0F, -1.0F, 0.0F).normalize();
    }

    private static Quaternionf sameHemisphere(Quaternionf reference, Quaternionf value) {
        float dot = reference.x * value.x + reference.y * value.y
                + reference.z * value.z + reference.w * value.w;
        return dot < 0.0F
                ? new Quaternionf(-value.x, -value.y, -value.z, -value.w) : value;
    }

    private static Quaternionf squadControl(Quaternionf previous, Quaternionf current,
                                            Quaternionf next) {
        Quaternionf inverse = new Quaternionf(current).conjugate().normalize();
        Quaternionf before = quaternionLog(new Quaternionf(inverse).mul(previous).normalize());
        Quaternionf after = quaternionLog(new Quaternionf(inverse).mul(next).normalize());
        Quaternionf average = new Quaternionf(
                -(before.x + after.x) * 0.25F,
                -(before.y + after.y) * 0.25F,
                -(before.z + after.z) * 0.25F, 0.0F);
        return new Quaternionf(current).mul(quaternionExp(average)).normalize();
    }

    private static Quaternionf quaternionLog(Quaternionf quaternion) {
        float w = Mth.clamp(quaternion.w, -1.0F, 1.0F);
        float angle = (float) Math.acos(w);
        float sin = (float) Math.sin(angle);
        float scale = Math.abs(sin) < 1.0E-6F ? 1.0F : angle / sin;
        return new Quaternionf(quaternion.x * scale, quaternion.y * scale,
                quaternion.z * scale, 0.0F);
    }

    private static Quaternionf quaternionExp(Quaternionf quaternion) {
        float angle = (float) Math.sqrt(quaternion.x * quaternion.x
                + quaternion.y * quaternion.y + quaternion.z * quaternion.z);
        float scale = angle < 1.0E-6F ? 1.0F : (float) Math.sin(angle) / angle;
        return new Quaternionf(quaternion.x * scale, quaternion.y * scale,
                quaternion.z * scale, (float) Math.cos(angle));
    }

    private static Vec3 bezier(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
        double u = 1.0D - t;
        return p0.scale(u * u * u)
                .add(p1.scale(3.0D * u * u * t))
                .add(p2.scale(3.0D * u * t * t))
                .add(p3.scale(t * t * t));
    }

    /** Centripetal Catmull-Rom retained for backwards-compatible resource packs. */
    private static Vec3 catmull(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
        double t0 = 0.0D;
        double t1 = t0 + knotDelta(p0, p1);
        double t2 = t1 + knotDelta(p1, p2);
        double t3 = t2 + knotDelta(p2, p3);
        double value = Mth.lerp(Mth.clamp(t, 0.0F, 1.0F), t1, t2);
        Vec3 a1 = timedLerp(p0, p1, t0, t1, value);
        Vec3 a2 = timedLerp(p1, p2, t1, t2, value);
        Vec3 a3 = timedLerp(p2, p3, t2, t3, value);
        Vec3 b1 = timedLerp(a1, a2, t0, t2, value);
        Vec3 b2 = timedLerp(a2, a3, t1, t3, value);
        return timedLerp(b1, b2, t1, t2, value);
    }

    private static double knotDelta(Vec3 a, Vec3 b) {
        return Math.max(1.0E-4D, Math.sqrt(a.distanceTo(b)));
    }

    private static Vec3 timedLerp(Vec3 a, Vec3 b, double ta, double tb, double value) {
        double factor = Mth.clamp((value - ta) / Math.max(tb - ta, 1.0E-7D), 0.0D, 1.0D);
        return a.lerp(b, factor);
    }

    private static Vec3 vector(Vector3f value) {
        return new Vec3(value.x(), value.y(), value.z());
    }

    private static Vector3f vec3f(Vec3 value) {
        return new Vector3f((float) value.x, (float) value.y, (float) value.z);
    }

    private static float ease(float value, String easing) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return switch (easing) {
            case "linear" -> t;
            case "accelerate" -> (float) Math.pow(t, 1.35D);
            case "decelerate" -> 1.0F - (float) Math.pow(1.0F - t, 1.45D);
            case "hermite", "smooth" -> smooth(t);
            case "smoother" -> t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
            default -> throw new IllegalArgumentException("Unknown Raizan easing: " + easing);
        };
    }

    private static float smooth(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}

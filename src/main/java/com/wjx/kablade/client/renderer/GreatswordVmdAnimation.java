package com.wjx.kablade.client.renderer;

import com.wjx.kablade.Entity.EntityNuclearShock;
import com.wjx.kablade.Entity.EntityValkyrieImpact;
import com.wjx.kablade.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.20 大剑 VMD 的轻量 Java 8 播放器。
 *
 * <p>这四份动作是逐帧烘焙数据，因此这里只实现骨骼位置、四元数插值和父子矩阵，
 * 不引入原版 MMD 播放器的材质、IK 与物理部分。</p>
 */
public final class GreatswordVmdAnimation implements IResourceManagerReloadListener {
    public static final GreatswordVmdAnimation INSTANCE = new GreatswordVmdAnimation();
    public static final float FPS = 30.0F;
    public static final float FRAMES_PER_TICK = FPS / 20.0F;
    private static final Charset SHIFT_JIS = Charset.forName("Shift_JIS");

    private volatile SkillMotion nuclear = SkillMotion.EMPTY;
    private volatile SkillMotion valkyrie = SkillMotion.EMPTY;
    private volatile Skill debugSkill;
    private volatile float debugFrame;

    private GreatswordVmdAnimation() {}

    @Override
    public void onResourceManagerReload(IResourceManager manager) {
        try {
            nuclear = load(manager, "fusion_nuclear_shock");
            valkyrie = load(manager, "valkyrie_impact");
            if (Main.logger != null) Main.logger.info("Loaded 1.20 greatsword VMD actions for 1.12");
        } catch (Throwable error) {
            if (Main.logger != null) Main.logger.warn(
                    "Could not load greatsword VMD actions; keeping the last valid motions", error);
        }
    }

    private static SkillMotion load(IResourceManager manager, String name) throws Exception {
        Animation player = read(manager, new ResourceLocation(Main.MODID,
                "combostate/" + name + "_player.vmd"));
        Animation blade = read(manager, new ResourceLocation(Main.MODID,
                "combostate/" + name + "_blade.vmd"));
        return new SkillMotion(player, blade);
    }

    private static Animation read(IResourceManager manager, ResourceLocation location) throws Exception {
        byte[] bytes;
        try (IResource resource = manager.getResource(location);
             InputStream input = resource.getInputStream();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = input.read(buffer)) >= 0) output.write(buffer, 0, length);
            bytes = output.toByteArray();
        }
        if (bytes.length < 54) throw new IllegalArgumentException("Truncated VMD: " + location);
        ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        data.position(50);
        long countLong = Integer.toUnsignedLong(data.getInt());
        if (countLong > 100000L || 54L + countLong * 111L > bytes.length) {
            throw new IllegalArgumentException("Invalid VMD bone frame count: " + countLong);
        }
        int count = (int) countLong;
        Map<String, List<Keyframe>> tracks = new HashMap<String, List<Keyframe>>();
        byte[] nameBytes = new byte[15];
        byte[] interpolation = new byte[64];
        for (int i = 0; i < count; i++) {
            data.get(nameBytes);
            String bone = decodeName(nameBytes);
            long frameLong = Integer.toUnsignedLong(data.getInt());
            if (frameLong > Integer.MAX_VALUE) throw new IllegalArgumentException("VMD frame overflow");
            Vec position = new Vec(data.getFloat(), data.getFloat(), data.getFloat());
            Quat rotation = new Quat(data.getFloat(), data.getFloat(), data.getFloat(), data.getFloat()).normal();
            data.get(interpolation);
            List<Keyframe> keys = tracks.get(bone);
            if (keys == null) {
                keys = new ArrayList<Keyframe>();
                tracks.put(bone, keys);
            }
            keys.add(new Keyframe((int) frameLong, position, rotation));
        }
        for (List<Keyframe> keys : tracks.values()) {
            Collections.sort(keys, Comparator.comparingInt(value -> value.frame));
        }
        return new Animation(Collections.unmodifiableMap(tracks));
    }

    private static String decodeName(byte[] bytes) {
        int length = 0;
        while (length < bytes.length && bytes[length] != 0) length++;
        return new String(bytes, 0, length, SHIFT_JIS).trim();
    }

    /** 当前玩家所拥有的 SA 实体就是动作时钟，服务器与所有客户端共用一条时间轴。 */
    public ActiveMotion findActive(EntityLivingBase owner, float partialTicks) {
        if (owner == null || owner.world == null) return null;
        Skill fixedSkill = debugSkill;
        if (fixedSkill != null && owner == Minecraft.getMinecraft().player) {
            return new ActiveMotion(fixedSkill,
                    fixedSkill == Skill.NUCLEAR ? nuclear : valkyrie, debugFrame);
        }
        for (Entity entity : owner.world.loadedEntityList) {
            if (entity instanceof EntityNuclearShock) {
                EntityNuclearShock shock = (EntityNuclearShock) entity;
                if (!shock.isDead && shock.getOwnerId() == owner.getEntityId()) {
                    return new ActiveMotion(Skill.NUCLEAR, nuclear,
                            shock.getAnimationFrame(partialTicks));
                }
            } else if (entity instanceof EntityValkyrieImpact) {
                EntityValkyrieImpact impact = (EntityValkyrieImpact) entity;
                if (!impact.isDead && impact.getOwnerId() == owner.getEntityId()) {
                    return new ActiveMotion(Skill.VALKYRIE, valkyrie,
                            impact.getAnimationFrame(partialTicks));
                }
            }
        }
        return null;
    }

    /** 开发环境逐帧验收入口；不会创建技能实体，也不会改服务器状态。 */
    public void setDebugFrame(Skill skill, float frame) {
        debugFrame = MathHelper.clamp(frame, 0.0F, 67.0F);
        debugSkill = skill;
    }

    public void clearDebugFrame() {
        debugSkill = null;
    }

    public boolean applyPlayerPose(ModelBiped model, EntityLivingBase owner, float partialTicks) {
        ActiveMotion active = findActive(owner, partialTicks);
        if (active == null || active.motion == SkillMotion.EMPTY) return false;

        Animation animation = active.motion.player;
        Pose torso = animation.sample("torso", active.frame);

        // body 是整副骨架的父节点，由 RenderLivingBase Mixin 统一施加；
        // 这里仅写局部骨骼，否则每个部位会绕自己的轴重复吃一次父旋转。
        apply(model.bipedBody, torso, false);
        apply(model.bipedHead, animation.sample("head", active.frame), false);
        apply(model.bipedHeadwear, animation.sample("head", active.frame), false);
        // Player Animator 与 1.12 ModelBiped 的肢体俯仰轴方向相反；
        // 翻转 X 后，手脚才会由人物后方越过身体运动到前方。
        apply(model.bipedRightArm, animation.sample("right arm", active.frame), true);
        apply(model.bipedLeftArm, animation.sample("left arm", active.frame), true);
        apply(model.bipedRightLeg, animation.sample("right leg", active.frame), true);
        apply(model.bipedLeftLeg, animation.sample("left leg", active.frame), true);
        return true;
    }

    /** 将 Player Animator 的 body 父节点施加到整个 1.12 人物模型。 */
    public boolean applyPlayerRootPose(EntityLivingBase owner, float partialTicks) {
        ActiveMotion active = findActive(owner, partialTicks);
        if (active == null || active.motion == SkillMotion.EMPTY) return false;
        active.applyBladeUserPose();
        return true;
    }

    /** ModelBiped 原逻辑不会主动清零头部和躯干的 Z 轴，动作结束时补一次复位。 */
    public void resetPlayerPose(ModelBiped model) {
        model.bipedBody.rotateAngleZ = 0.0F;
        model.bipedHead.rotateAngleZ = 0.0F;
        model.bipedHeadwear.rotateAngleZ = 0.0F;
    }

    private static void apply(ModelRenderer part, Pose pose, boolean invertPitch) {
        Euler local = pose.rotation.eulerZYX(false);
        // 1.20 VmdAnimation 在 blendArms/blendLegs 开启时会把 VMD 旋转
        // 加到 Player Animator 传入的原姿态，而不是覆盖原姿态。
        part.rotateAngleX += invertPitch ? -local.x : local.x;
        part.rotateAngleY += local.y;
        part.rotateAngleZ += local.z;
    }

    public enum Skill { NUCLEAR, VALKYRIE }

    public static final class ActiveMotion {
        public final Skill skill;
        public final float frame;
        private final SkillMotion motion;

        private ActiveMotion(Skill skill, SkillMotion motion, float frame) {
            this.skill = skill;
            this.motion = motion;
            this.frame = MathHelper.clamp(frame, 0.0F, 67.0F);
        }

        /** 写入与 1.20 bladeholder hardpointA 等价的 OpenGL 列主序矩阵。 */
        public void writeBladeMatrix(FloatBuffer output) {
            Mat4 matrix = Mat4.IDENTITY;
            String[] chain = {"センター", "JointA1", "JointA2", "JointA3", "hardpointA"};
            for (String bone : chain) matrix = matrix.multiply(Mat4.of(motion.blade.sample(bone, frame)));
            matrix.write(output);
        }

        /** LayerMainBlade#setUserPose 的 1.12 等价变换，使武器和玩家 body 父骨骼同步。 */
        public void applyBladeUserPose() {
            Pose body = motion.player.sample("body", frame);
            Euler rotation = body.rotation.eulerZYX(true);
            // 1.12 渲染空间的前后轴与女武神 VMD 导出方向相反；只为该动作
            // 翻转根节点 Z 位移，避免人物在出招时向后缩。
            float bodyZ = skill == Skill.VALKYRIE
                    ? body.position.z / 8.0F : -body.position.z / 8.0F;
            GlStateManager.translate(-body.position.x / 8.0F,
                    body.position.y / 8.0F + 0.7F, bodyZ);
            GlStateManager.rotate((float) Math.toDegrees(rotation.z), 0, 0, 1);
            GlStateManager.rotate((float) Math.toDegrees(rotation.y), 0, 1, 0);
            GlStateManager.rotate((float) Math.toDegrees(rotation.x), 1, 0, 0);
            GlStateManager.translate(0, -0.7F, 0);
        }
    }

    private static final class SkillMotion {
        private static final SkillMotion EMPTY = new SkillMotion(Animation.EMPTY, Animation.EMPTY);
        private final Animation player;
        private final Animation blade;

        private SkillMotion(Animation player, Animation blade) {
            this.player = player;
            this.blade = blade;
        }
    }

    private static final class Animation {
        private static final Animation EMPTY = new Animation(Collections.<String, List<Keyframe>>emptyMap());
        private final Map<String, List<Keyframe>> tracks;

        private Animation(Map<String, List<Keyframe>> tracks) { this.tracks = tracks; }

        private Pose sample(String bone, float frame) {
            List<Keyframe> keys = tracks.get(bone);
            if (keys == null || keys.isEmpty()) return Pose.IDENTITY;
            if (frame <= keys.get(0).frame) return keys.get(0).pose();
            Keyframe last = keys.get(keys.size() - 1);
            if (frame >= last.frame) return last.pose();
            int low = 0, high = keys.size() - 1;
            while (low + 1 < high) {
                int middle = (low + high) >>> 1;
                if (keys.get(middle).frame <= frame) low = middle; else high = middle;
            }
            Keyframe first = keys.get(low), second = keys.get(high);
            float t = (frame - first.frame) / Math.max(1.0F, second.frame - first.frame);
            return new Pose(Vec.lerp(first.position, second.position, t),
                    Quat.slerp(first.rotation, second.rotation, t));
        }
    }

    private static final class Keyframe {
        private final int frame;
        private final Vec position;
        private final Quat rotation;

        private Keyframe(int frame, Vec position, Quat rotation) {
            this.frame = frame;
            this.position = position;
            this.rotation = rotation;
        }

        private Pose pose() { return new Pose(position, rotation); }
    }

    private static final class Pose {
        private static final Pose IDENTITY = new Pose(Vec.ZERO, Quat.IDENTITY);
        private final Vec position;
        private final Quat rotation;

        private Pose(Vec position, Quat rotation) {
            this.position = position;
            this.rotation = rotation;
        }
    }

    private static final class Vec {
        private static final Vec ZERO = new Vec(0, 0, 0);
        private final float x, y, z;

        private Vec(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }

        private static Vec lerp(Vec a, Vec b, float t) {
            return new Vec(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t,
                    a.z + (b.z - a.z) * t);
        }
    }

    private static final class Euler {
        private final float x, y, z;
        private Euler(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
    }

    private static final class Quat {
        private static final Quat IDENTITY = new Quat(0, 0, 0, 1);
        private final float x, y, z, w;

        private Quat(float x, float y, float z, float w) {
            this.x = x; this.y = y; this.z = z; this.w = w;
        }

        private Quat normal() {
            float length = MathHelper.sqrt(x * x + y * y + z * z + w * w);
            return length < 1.0E-7F ? IDENTITY : new Quat(x / length, y / length, z / length, w / length);
        }

        private Euler eulerZYX(boolean body) {
            float rx = (float) Math.atan2(2.0F * (w * x + y * z),
                    1.0F - 2.0F * (x * x + y * y));
            float sinY = MathHelper.clamp(2.0F * (w * y - z * x), -1.0F, 1.0F);
            float ry = (float) Math.asin(sinY);
            float rz = (float) Math.atan2(2.0F * (w * z + x * y),
                    1.0F - 2.0F * (y * y + z * z));
            // 精确对应 1.20 VmdAnimation：body (1,-1,-1)，其余 (-1,1,-1)。
            return body ? new Euler(rx, -ry, -rz) : new Euler(-rx, ry, -rz);
        }

        private static Quat slerp(Quat a, Quat value, float t) {
            Quat b = value;
            float dot = a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w;
            if (dot < 0.0F) {
                dot = -dot;
                b = new Quat(-b.x, -b.y, -b.z, -b.w);
            }
            dot = MathHelper.clamp(dot, -1.0F, 1.0F);
            if (dot > 0.9995F) return new Quat(a.x + (b.x - a.x) * t,
                    a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t,
                    a.w + (b.w - a.w) * t).normal();
            double theta = Math.acos(dot);
            double sine = Math.sin(theta);
            float first = (float) (Math.sin((1.0F - t) * theta) / sine);
            float second = (float) (Math.sin(t * theta) / sine);
            return new Quat(a.x * first + b.x * second, a.y * first + b.y * second,
                    a.z * first + b.z * second, a.w * first + b.w * second).normal();
        }
    }

    private static final class Mat4 {
        private static final Mat4 IDENTITY = new Mat4(new float[]{
                1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1});
        private final float[] m;

        private Mat4(float[] m) { this.m = m; }

        private static Mat4 of(Pose pose) {
            Quat q = pose.rotation;
            float xx = q.x * q.x, yy = q.y * q.y, zz = q.z * q.z;
            float xy = q.x * q.y, xz = q.x * q.z, yz = q.y * q.z;
            float wx = q.w * q.x, wy = q.w * q.y, wz = q.w * q.z;
            return new Mat4(new float[]{
                    1 - 2 * (yy + zz), 2 * (xy + wz), 2 * (xz - wy), 0,
                    2 * (xy - wz), 1 - 2 * (xx + zz), 2 * (yz + wx), 0,
                    2 * (xz + wy), 2 * (yz - wx), 1 - 2 * (xx + yy), 0,
                    pose.position.x, pose.position.y, pose.position.z, 1});
        }

        private Mat4 multiply(Mat4 right) {
            float[] out = new float[16];
            for (int column = 0; column < 4; column++) {
                for (int row = 0; row < 4; row++) {
                    float value = 0;
                    for (int k = 0; k < 4; k++) value += m[k * 4 + row] * right.m[column * 4 + k];
                    out[column * 4 + row] = value;
                }
            }
            return new Mat4(out);
        }

        private void write(FloatBuffer output) {
            output.clear();
            output.put(m);
            output.flip();
        }
    }
}

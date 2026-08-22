package com.wjx.kablade.client.renderer;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 地藏御魂四刚体动画。这里自带最小四元数实现，避免把 1.20 的 JOML API 硬套进 1.12.2。
 */
@SideOnly(Side.CLIENT)
public final class JizoSoulAnimation {
    public static final Vec BODY_PIVOT = new Vec(0.0F, 1.28F, -0.16F);
    public static final Vec HAND_L_PIVOT = new Vec(0.82F, 1.04F, 0.28F);
    public static final Vec HAND_R_PIVOT = new Vec(-0.82F, 1.04F, 0.28F);
    public static final Vec BLADE_PIVOT = new Vec(-0.51F, -1.0F, 0.54F);

    private static final Vec BLADE_TIP = new Vec(-1.335146F, 1.755704F, 5.585857F);
    private static final Vec BLADE_AXIS = new Vec(-0.000112F, 0.363538F, 4.827546F).normalize();
    private static final Vec FINAL_BLADE_DIRECTION = new Vec(0.0F, -0.249F, 0.968F).normalize();
    private static final float BLADE_LENGTH_SCALE = 1.50F;

    private JizoSoulAnimation() {
    }

    public static Pose sample(float seconds) {
        float summon = easeOutCubic((seconds - 0.2F) / 0.85F);
        float stabilize = smooth((seconds - 0.78F) / 0.72F);
        float rise = smooth((seconds - 1.82F) / 0.48F);
        float strikeProgress = smooth((seconds - 2.28F) / 0.56F);
        float strike = easeInCubic((seconds - 2.28F) / 0.56F);
        float idle = seconds > 1.1F && seconds < 1.9F
                ? MathHelper.sin(seconds * 3.2F) : 0.0F;
        float summonPose = summon * (1.0F - stabilize);

        float rootX = lerp(stabilize, 0.25F, 0.0F);
        float rootY = lerp(summon, -0.72F, 0.2F)
                + (seconds > 1.2F && seconds < 1.95F
                ? MathHelper.sin(seconds * 4.1F) * 0.08F : 0.0F)
                + rise * 2.15F - strike * 2.05F + 0.22F;
        float rootZ = lerp(stabilize, -0.1F, 0.2F) + strike * 0.78F;
        float rootPitch = -strike * 0.24F;
        float rootRoll = (1.0F - summon) * -0.08F + rise * 0.09F - strike * 0.21F;
        Quat rootRotation = Quat.eulerXYZ(rootPitch, 0.0F, rootRoll);
        Quat rigRoot = Quat.eulerYXZ(0.0F, idle * 0.018F, 0.0F);

        float bodyWorldPitch = lerp(strikeProgress,
                -0.1F * summonPose - 0.22F * rise, 0.22F);
        Quat body = Quat.eulerYXZ(bodyWorldPitch - rootPitch + idle * 0.018F,
                idle * 0.012F, lerp(strikeProgress, 0.035F * rise, -0.08F));
        Quat handL = Quat.eulerYXZ(
                -0.44F * rise + 0.07F * strikeProgress + idle * 0.028F,
                -0.2F * rise,
                0.3F * summonPose - 1.12F * rise + 0.08F * strikeProgress);

        float referenceHandRX = -0.58F * rise + 0.74F * strikeProgress - idle * 0.025F;
        float forwardHandRX = referenceHandRX - 0.65F * strikeProgress;
        float handRY = 0.2F * rise;
        float referenceHandRZ = -0.3F * summonPose + 0.82F * rise
                - 0.08F * strikeProgress;
        Quat handR = Quat.eulerYXZ(forwardHandRX, handRY, referenceHandRZ);
        Quat referenceHandR = Quat.eulerYXZ(referenceHandRX, handRY, referenceHandRZ);
        Quat referenceBlade = Quat.eulerYXZ(
                lerp(strikeProgress, -0.62F * rise, -0.195F),
                lerp(strikeProgress, 0.08F * rise, -0.015F),
                lerp(strikeProgress, 0.12F * rise, -0.1F));

        Quat blade = handR.inverse().mul(referenceHandR).mul(referenceBlade).normalize();
        blade = correctBlade(rootRotation, rigRoot, body, handR, blade, strikeProgress);

        float scale = (0.58F + summon * 0.42F)
                * (1.0F + (1.0F - stabilize) * 0.12F);
        float alpha = smooth((seconds - 0.2F) / 0.7F);
        float groundContact = smooth((strikeProgress - 0.72F) / 0.28F);
        float correctedY = correctTipHeight(rootX, rootY, rootZ, scale,
                rootRotation, rigRoot, body, handR, blade, groundContact);
        return new Pose(rootX, correctedY, rootZ, scale, alpha,
                rootRotation, rigRoot, body, handL, handR, blade);
    }

    public static void applyRotation(Quat rotation) {
        Quat value = rotation.normalize();
        float w = MathHelper.clamp(value.w, -1.0F, 1.0F);
        float angle = 2.0F * (float) Math.acos(w);
        float sinHalf = (float) Math.sqrt(Math.max(0.0F, 1.0F - w * w));
        if (angle < 1.0E-6F || sinHalf < 1.0E-6F) {
            return;
        }
        GlStateManager.rotate(angle * 57.2957795F,
                value.x / sinHalf, value.y / sinHalf, value.z / sinHalf);
    }

    private static Quat correctBlade(Quat rootRotation, Quat rigRoot,
                                     Quat body, Quat hand, Quat blade, float progress) {
        Quat parent = rootRotation.mul(rigRoot).mul(body).mul(hand).normalize();
        Quat world = parent.mul(blade).normalize();
        Vec currentDirection = world.transform(BLADE_AXIS).normalize();
        Vec targetDirection = currentDirection.lerp(FINAL_BLADE_DIRECTION, progress).normalize();
        world = Quat.rotationTo(currentDirection, targetDirection).mul(world).normalize();

        Vec currentWidth = world.transform(new Vec(0.0F, 1.0F, 0.0F));
        currentWidth = currentWidth.subtract(targetDirection.scale(
                currentWidth.dot(targetDirection))).normalize();
        Vec targetWidth = new Vec(0.0F, 1.0F, 0.0F)
                .subtract(targetDirection.scale(targetDirection.y)).normalize();
        Vec cross = currentWidth.cross(targetWidth);
        float roll = (float) Math.atan2(targetDirection.dot(cross),
                MathHelper.clamp(currentWidth.dot(targetWidth), -1.0F, 1.0F));
        world = Quat.axisAngle(targetDirection, roll * progress).mul(world).normalize();
        return parent.inverse().mul(world).normalize();
    }

    private static float correctTipHeight(float rootX, float rootY, float rootZ, float scale,
                                          Quat rootRotation, Quat rigRoot,
                                          Quat body, Quat hand, Quat blade,
                                          float groundContact) {
        if (groundContact <= 0.0F) {
            return rootY;
        }

        Vec pivotSum = BODY_PIVOT.add(HAND_R_PIVOT).add(BLADE_PIVOT);
        Vec tip = BLADE_TIP.subtract(pivotSum).scale(1.0F, 1.0F, BLADE_LENGTH_SCALE);
        tip = blade.transform(tip).add(BLADE_PIVOT);
        tip = hand.transform(tip).add(HAND_R_PIVOT);
        tip = body.transform(tip).add(BODY_PIVOT);
        tip = rigRoot.transform(tip).scale(scale);
        tip = rootRotation.transform(tip).add(rootX, rootY, rootZ);
        return rootY + (0.265F - tip.y) * groundContact;
    }

    private static float lerp(float progress, float start, float end) {
        return start + (end - start) * progress;
    }

    private static float clamp01(float value) {
        return MathHelper.clamp(value, 0.0F, 1.0F);
    }

    private static float smooth(float value) {
        float t = clamp01(value);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float easeOutCubic(float value) {
        float t = clamp01(value);
        float inverse = 1.0F - t;
        return 1.0F - inverse * inverse * inverse;
    }

    private static float easeInCubic(float value) {
        float t = clamp01(value);
        return t * t * t;
    }

    public static final class Pose {
        public final float rootX;
        public final float rootY;
        public final float rootZ;
        public final float scale;
        public final float alpha;
        public final Quat rootRotation;
        public final Quat rigRootRotation;
        public final Quat bodyRotation;
        public final Quat handLRotation;
        public final Quat handRRotation;
        public final Quat bladeRotation;

        private Pose(float rootX, float rootY, float rootZ, float scale, float alpha,
                     Quat rootRotation, Quat rigRootRotation, Quat bodyRotation,
                     Quat handLRotation, Quat handRRotation, Quat bladeRotation) {
            this.rootX = rootX;
            this.rootY = rootY;
            this.rootZ = rootZ;
            this.scale = scale;
            this.alpha = alpha;
            this.rootRotation = rootRotation;
            this.rigRootRotation = rigRootRotation;
            this.bodyRotation = bodyRotation;
            this.handLRotation = handLRotation;
            this.handRRotation = handRRotation;
            this.bladeRotation = bladeRotation;
        }
    }

    public static final class Vec {
        public final float x;
        public final float y;
        public final float z;

        public Vec(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Vec add(Vec value) {
            return add(value.x, value.y, value.z);
        }

        public Vec add(float dx, float dy, float dz) {
            return new Vec(this.x + dx, this.y + dy, this.z + dz);
        }

        public Vec subtract(Vec value) {
            return new Vec(this.x - value.x, this.y - value.y, this.z - value.z);
        }

        public Vec scale(float value) {
            return new Vec(this.x * value, this.y * value, this.z * value);
        }

        public Vec scale(float scaleX, float scaleY, float scaleZ) {
            return new Vec(this.x * scaleX, this.y * scaleY, this.z * scaleZ);
        }

        public float dot(Vec value) {
            return this.x * value.x + this.y * value.y + this.z * value.z;
        }

        public Vec cross(Vec value) {
            return new Vec(this.y * value.z - this.z * value.y,
                    this.z * value.x - this.x * value.z,
                    this.x * value.y - this.y * value.x);
        }

        public Vec normalize() {
            float length = (float) Math.sqrt(this.dot(this));
            return length < 1.0E-7F ? new Vec(0.0F, 0.0F, 0.0F) : scale(1.0F / length);
        }

        public Vec lerp(Vec target, float progress) {
            return new Vec(this.x + (target.x - this.x) * progress,
                    this.y + (target.y - this.y) * progress,
                    this.z + (target.z - this.z) * progress);
        }
    }

    public static final class Quat {
        public final float x;
        public final float y;
        public final float z;
        public final float w;

        private Quat(float x, float y, float z, float w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        private static Quat eulerXYZ(float x, float y, float z) {
            return axisAngle(new Vec(1, 0, 0), x)
                    .mul(axisAngle(new Vec(0, 1, 0), y))
                    .mul(axisAngle(new Vec(0, 0, 1), z)).normalize();
        }

        private static Quat eulerYXZ(float x, float y, float z) {
            return axisAngle(new Vec(0, 1, 0), y)
                    .mul(axisAngle(new Vec(1, 0, 0), x))
                    .mul(axisAngle(new Vec(0, 0, 1), z)).normalize();
        }

        private static Quat axisAngle(Vec axis, float angle) {
            Vec normal = axis.normalize();
            float half = angle * 0.5F;
            float sin = MathHelper.sin(half);
            return new Quat(normal.x * sin, normal.y * sin, normal.z * sin,
                    MathHelper.cos(half)).normalize();
        }

        private static Quat rotationTo(Vec from, Vec to) {
            Vec start = from.normalize();
            Vec end = to.normalize();
            float dot = MathHelper.clamp(start.dot(end), -1.0F, 1.0F);
            if (dot < -0.999999F) {
                Vec axis = Math.abs(start.x) < 0.8F
                        ? start.cross(new Vec(1, 0, 0)).normalize()
                        : start.cross(new Vec(0, 1, 0)).normalize();
                return axisAngle(axis, (float) Math.PI);
            }
            if (dot > 0.999999F) {
                return new Quat(0, 0, 0, 1);
            }
            Vec cross = start.cross(end);
            float scale = (float) Math.sqrt((1.0F + dot) * 2.0F);
            float inverse = 1.0F / scale;
            return new Quat(cross.x * inverse, cross.y * inverse,
                    cross.z * inverse, scale * 0.5F).normalize();
        }

        private Quat mul(Quat value) {
            return new Quat(
                    this.w * value.x + this.x * value.w
                            + this.y * value.z - this.z * value.y,
                    this.w * value.y - this.x * value.z
                            + this.y * value.w + this.z * value.x,
                    this.w * value.z + this.x * value.y
                            - this.y * value.x + this.z * value.w,
                    this.w * value.w - this.x * value.x
                            - this.y * value.y - this.z * value.z);
        }

        private Quat inverse() {
            float lengthSquared = this.x * this.x + this.y * this.y
                    + this.z * this.z + this.w * this.w;
            if (lengthSquared < 1.0E-8F) {
                return new Quat(0, 0, 0, 1);
            }
            return new Quat(-this.x / lengthSquared, -this.y / lengthSquared,
                    -this.z / lengthSquared, this.w / lengthSquared);
        }

        private Quat normalize() {
            float length = (float) Math.sqrt(this.x * this.x + this.y * this.y
                    + this.z * this.z + this.w * this.w);
            if (length < 1.0E-8F) {
                return new Quat(0, 0, 0, 1);
            }
            return new Quat(this.x / length, this.y / length,
                    this.z / length, this.w / length);
        }

        private Vec transform(Vec value) {
            Vec q = new Vec(this.x, this.y, this.z);
            Vec uv = q.cross(value);
            Vec uuv = q.cross(uv);
            return value.add(uv.scale(2.0F * this.w)).add(uuv.scale(2.0F));
        }
    }
}

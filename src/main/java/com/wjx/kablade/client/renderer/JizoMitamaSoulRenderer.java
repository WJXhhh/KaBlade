package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.KabladeRenderTypes;
import com.wjx.kablade.entity.JizoMitamaSoulEntity;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Renders the four rigid OBJ groups used by Soul Appearance's shortened 3.3-second animation. */
public final class JizoMitamaSoulRenderer extends EntityRenderer<JizoMitamaSoulEntity> {

    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            Main.MODID, "model/util/jizo_skill_boss/mdl.obj");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Main.MODID, "model/util/jizo_skill_boss/tex.png");
    private static final ResourceLocation EMPTY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Main.MODID, "textures/entity/empty.png");

    private static final Vector3f BODY_PIVOT = new Vector3f(0.0F, 1.28F, -0.16F);
    private static final Vector3f HAND_L_PIVOT = new Vector3f(0.82F, 1.04F, 0.28F);
    private static final Vector3f HAND_R_PIVOT = new Vector3f(-0.82F, 1.04F, 0.28F);
    private static final Vector3f BLADE_PIVOT = new Vector3f(-0.51F, -1.0F, 0.54F);
    private static final Vector3f BLADE_TIP = new Vector3f(-1.335146F, 1.755704F, 5.585857F);
    /** Twice the previous 0.75 length; only the blade's local forward axis is stretched. */
    private static final float BLADE_LENGTH_SCALE = 1.50F;
    private static final float SOUL_BODY_OPACITY = 0.48F;
    private static final Vector3f FINAL_BLADE_DIRECTION =
            new Vector3f(0.0F, -0.249F, 0.968F).normalize();

    public JizoMitamaSoulRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(JizoMitamaSoulEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.getRenderAge(partialTick);
        float seconds = age / 20.0F;
        AnimationPose animation = sample(seconds);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        renderGroundEffects(age, poseStack, buffer);
        if (seconds >= 0.2F) {
            if (JizoMitamaSoulOculusPipeline.enqueue(entity, partialTick, animation)) {
                renderJizoBlade(animation, poseStack, buffer, packedLight);
            } else {
                renderJizo(animation, poseStack, buffer, packedLight);
            }
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void renderJizo(AnimationPose animation, PoseStack poseStack,
                                   MultiBufferSource buffer, int packedLight) {
        WavefrontObject model = BladeModelManager.getInstance().getModel(MODEL);
        int solidAlpha = Mth.clamp((int) (255.0F * animation.alpha), 0, 255);
        int soulAlpha = Mth.clamp((int) (solidAlpha * SOUL_BODY_OPACITY), 0, 255);

        poseStack.pushPose();
        poseStack.translate(animation.rootX, animation.correctedRootY, animation.rootZ);
        poseStack.mulPose(animation.rootRotation);
        poseStack.scale(animation.scale, animation.scale, animation.scale);
        poseStack.mulPose(animation.rigRootRotation);

        // Resolve one continuous outer shell before applying alpha. This prevents the
        // model's internal and back-facing triangles from accumulating into a dark grid.
        renderBody(model, animation.bodyRotation, poseStack, buffer, packedLight, soulAlpha, SoulPass.DEPTH);
        renderHand(model, "Hand_L", HAND_L_PIVOT, animation.handLRotation,
                animation.bodyRotation, poseStack, buffer, packedLight, soulAlpha, SoulPass.DEPTH);
        renderHand(model, "Hand_R", HAND_R_PIVOT, animation.handRRotation,
                animation.bodyRotation, poseStack, buffer, packedLight, soulAlpha, SoulPass.DEPTH);

        renderBody(model, animation.bodyRotation, poseStack, buffer, packedLight, soulAlpha, SoulPass.SURFACE);
        renderHand(model, "Hand_L", HAND_L_PIVOT, animation.handLRotation,
                animation.bodyRotation, poseStack, buffer, packedLight, soulAlpha, SoulPass.SURFACE);
        renderHand(model, "Hand_R", HAND_R_PIVOT, animation.handRRotation,
                animation.bodyRotation, poseStack, buffer, packedLight, soulAlpha, SoulPass.SURFACE);

        renderBody(model, animation.bodyRotation, poseStack, buffer, packedLight, soulAlpha, SoulPass.GLOW);
        renderHand(model, "Hand_L", HAND_L_PIVOT, animation.handLRotation,
                animation.bodyRotation, poseStack, buffer, packedLight, soulAlpha, SoulPass.GLOW);
        renderHand(model, "Hand_R", HAND_R_PIVOT, animation.handRRotation,
                animation.bodyRotation, poseStack, buffer, packedLight, soulAlpha, SoulPass.GLOW);
        renderBlade(model, animation, poseStack, buffer, packedLight, solidAlpha);

        poseStack.popPose();
        BladeRenderState.resetCol();
    }

    /** Oculus renders the translucent soul later through its private HDR pipeline. */
    private static void renderJizoBlade(AnimationPose animation, PoseStack poseStack,
                                        MultiBufferSource buffer, int packedLight) {
        WavefrontObject model = BladeModelManager.getInstance().getModel(MODEL);
        int solidAlpha = Mth.clamp((int) (255.0F * animation.alpha), 0, 255);
        poseStack.pushPose();
        poseStack.translate(animation.rootX, animation.correctedRootY, animation.rootZ);
        poseStack.mulPose(animation.rootRotation);
        poseStack.scale(animation.scale, animation.scale, animation.scale);
        poseStack.mulPose(animation.rigRootRotation);
        renderBlade(model, animation, poseStack, buffer, packedLight, solidAlpha);
        poseStack.popPose();
        BladeRenderState.resetCol();
    }

    static void renderOculusFallbackSoul(AnimationPose animation, PoseStack poseStack,
                                         MultiBufferSource buffer, int packedLight) {
        WavefrontObject model = BladeModelManager.getInstance().getModel(MODEL);
        int solidAlpha = Mth.clamp((int) (255.0F * animation.alpha), 0, 255);
        int soulAlpha = Mth.clamp((int) (solidAlpha * 0.62F), 0, 255);
        poseStack.pushPose();
        poseStack.translate(animation.rootX, animation.correctedRootY, animation.rootZ);
        poseStack.mulPose(animation.rootRotation);
        poseStack.scale(animation.scale, animation.scale, animation.scale);
        poseStack.mulPose(animation.rigRootRotation);
        renderBody(model, animation.bodyRotation, poseStack, buffer, packedLight, soulAlpha, SoulPass.DEPTH);
        renderHand(model, "Hand_L", HAND_L_PIVOT, animation.handLRotation,
                animation.bodyRotation, poseStack, buffer, packedLight, soulAlpha, SoulPass.DEPTH);
        renderHand(model, "Hand_R", HAND_R_PIVOT, animation.handRRotation,
                animation.bodyRotation, poseStack, buffer, packedLight, soulAlpha, SoulPass.DEPTH);
        renderBody(model, animation.bodyRotation, poseStack, buffer, packedLight, soulAlpha, SoulPass.SURFACE);
        renderHand(model, "Hand_L", HAND_L_PIVOT, animation.handLRotation,
                animation.bodyRotation, poseStack, buffer, packedLight, soulAlpha, SoulPass.SURFACE);
        renderHand(model, "Hand_R", HAND_R_PIVOT, animation.handRRotation,
                animation.bodyRotation, poseStack, buffer, packedLight, soulAlpha, SoulPass.SURFACE);
        poseStack.popPose();
        BladeRenderState.resetCol();
    }

    private static void renderBody(WavefrontObject model, Quaternionf body,
                                   PoseStack poseStack, MultiBufferSource buffer,
                                   int packedLight, int alpha, SoulPass pass) {
        poseStack.pushPose();
        translate(poseStack, BODY_PIVOT);
        poseStack.mulPose(body);
        translate(poseStack, new Vector3f(BODY_PIVOT).negate());
        renderSoulPart(model, "Body", poseStack, buffer, packedLight, alpha, pass);
        poseStack.popPose();
    }

    private static void renderHand(WavefrontObject model, String part, Vector3f handPivot,
                                   Quaternionf hand, Quaternionf body, PoseStack poseStack,
                                   MultiBufferSource buffer, int packedLight, int alpha,
                                   SoulPass pass) {
        poseStack.pushPose();
        translate(poseStack, BODY_PIVOT);
        poseStack.mulPose(body);
        translate(poseStack, handPivot);
        poseStack.mulPose(hand);
        translate(poseStack, new Vector3f(BODY_PIVOT).add(handPivot).negate());
        renderSoulPart(model, part, poseStack, buffer, packedLight, alpha, pass);
        poseStack.popPose();
    }

    private static void renderBlade(WavefrontObject model, AnimationPose animation,
                                    PoseStack poseStack, MultiBufferSource buffer,
                                    int packedLight, int alpha) {
        poseStack.pushPose();
        translate(poseStack, BODY_PIVOT);
        poseStack.mulPose(animation.bodyRotation);
        translate(poseStack, HAND_R_PIVOT);
        poseStack.mulPose(animation.handRRotation);
        translate(poseStack, BLADE_PIVOT);
        poseStack.mulPose(animation.bladeRotation);
        poseStack.scale(1.0F, 1.0F, BLADE_LENGTH_SCALE);
        translate(poseStack, new Vector3f(BODY_PIVOT).add(HAND_R_PIVOT).add(BLADE_PIVOT).negate());
        renderSolidBlade(model, poseStack, buffer, packedLight, alpha);
        poseStack.popPose();
    }

    private static void renderSoulPart(WavefrontObject model, String part, PoseStack poseStack,
                                       MultiBufferSource buffer, int packedLight, int alpha,
                                       SoulPass pass) {
        if (pass == SoulPass.DEPTH) {
            BladeRenderState.setCol(0xFFFFFFFF);
            BladeRenderState.renderOverrided(ItemStack.EMPTY, model, part, TEXTURE, poseStack,
                    buffer, packedLight, KabladeRenderTypes::jizoSoulDepth, false);
            return;
        }

        if (pass == SoulPass.GLOW) {
            int glowAlpha = Mth.clamp((int) (alpha * 0.78F), 0, 255);
            BladeRenderState.setCol(0xFF1606 | (glowAlpha << 24));
            BladeRenderState.renderOverrided(ItemStack.EMPTY, model, part, TEXTURE, poseStack,
                    buffer, BladeRenderState.MAX_LIGHT, KabladeRenderTypes::jizoSoulGlow, false);
            return;
        }

        BladeRenderState.setCol(0xFFFFFF | (alpha << 24));
        BladeRenderState.renderOverrided(ItemStack.EMPTY, model, part, TEXTURE, poseStack,
                buffer, packedLight, KabladeRenderTypes::jizoSoulSurface, false);
    }

    private static void renderSolidBlade(WavefrontObject model, PoseStack poseStack,
                                         MultiBufferSource buffer, int packedLight, int alpha) {
        BladeRenderState.setCol(0xFFFFFF | (alpha << 24));
        BladeRenderState.renderOverrided(ItemStack.EMPTY, model, "Blade_Big", TEXTURE,
                poseStack, buffer, packedLight);
    }

    private enum SoulPass {
        DEPTH,
        SURFACE,
        GLOW
    }

    static AnimationPose sample(float seconds) {
        float summon = easeOutCubic((seconds - 0.2F) / 0.85F);
        float stabilize = smooth((seconds - 0.78F) / 0.72F);
        float rise = smooth((seconds - 1.82F) / 0.48F);
        float strikeProgress = smooth((seconds - 2.28F) / 0.56F);
        float strike = easeInCubic((seconds - 2.28F) / 0.56F);
        float idle = seconds > 1.1F && seconds < 1.9F ? Mth.sin(seconds * 3.2F) : 0.0F;
        float summonPose = summon * (1.0F - stabilize);

        float rootX = Mth.lerp(stabilize, 0.25F, 0.0F);
        float rootY = Mth.lerp(summon, -0.72F, 0.2F)
                + (seconds > 1.2F && seconds < 1.95F ? Mth.sin(seconds * 4.1F) * 0.08F : 0.0F)
                + rise * 2.15F - strike * 2.05F + 0.22F;
        float rootZ = Mth.lerp(stabilize, -0.1F, 0.2F) + strike * 0.78F;
        float rootPitch = -strike * 0.24F;
        float rootRoll = (1.0F - summon) * -0.08F + rise * 0.09F - strike * 0.21F;
        Quaternionf rootRotation = eulerXYZ(rootPitch, 0.0F, rootRoll);
        Quaternionf rigRoot = eulerYXZ(0.0F, idle * 0.018F, 0.0F);

        float bodyWorldPitch = Mth.lerp(strikeProgress,
                -0.1F * summonPose - 0.22F * rise, 0.22F);
        Quaternionf body = eulerYXZ(bodyWorldPitch - rootPitch + idle * 0.018F,
                idle * 0.012F, Mth.lerp(strikeProgress, 0.035F * rise, -0.08F));
        Quaternionf handL = eulerYXZ(-0.44F * rise + 0.07F * strikeProgress + idle * 0.028F,
                -0.2F * rise,
                0.3F * summonPose - 1.12F * rise + 0.08F * strikeProgress);

        float referenceHandRX = -0.58F * rise + 0.74F * strikeProgress - idle * 0.025F;
        float forwardHandRX = referenceHandRX - 0.65F * strikeProgress;
        float handRY = 0.2F * rise;
        float referenceHandRZ = -0.3F * summonPose + 0.82F * rise - 0.08F * strikeProgress;
        Quaternionf handR = eulerYXZ(forwardHandRX, handRY, referenceHandRZ);
        Quaternionf referenceHandR = eulerYXZ(referenceHandRX, handRY, referenceHandRZ);
        Quaternionf referenceBlade = eulerYXZ(
                Mth.lerp(strikeProgress, -0.62F * rise, -0.195F),
                Mth.lerp(strikeProgress, 0.08F * rise, -0.015F),
                Mth.lerp(strikeProgress, 0.12F * rise, -0.1F));

        Quaternionf blade = new Quaternionf(handR).invert()
                .mul(referenceHandR).mul(referenceBlade).normalize();
        blade = correctBlade(rootRotation, rigRoot, body, handR, blade, strikeProgress);

        float scale = (0.58F + summon * 0.42F) * (1.0F + (1.0F - stabilize) * 0.12F);
        float alpha = smooth((seconds - 0.2F) / 0.7F);
        float groundContact = smooth((strikeProgress - 0.72F) / 0.28F);
        float correctedY = correctTipHeight(rootX, rootY, rootZ, scale, rootRotation,
                rigRoot, body, handR, blade, groundContact);
        return new AnimationPose(rootX, correctedY, rootZ, scale, alpha,
                rootRotation, rigRoot, body, handL, handR, blade);
    }

    private static Quaternionf correctBlade(Quaternionf rootRotation, Quaternionf rigRoot,
                                            Quaternionf body, Quaternionf hand,
                                            Quaternionf blade, float progress) {
        Quaternionf parent = new Quaternionf(rootRotation).mul(rigRoot).mul(body).mul(hand);
        Quaternionf world = new Quaternionf(parent).mul(blade).normalize();
        Vector3f scaledBladeAxis = new Vector3f(-0.000112F, 0.363538F, 4.827546F).normalize();
        Vector3f currentDirection = world.transform(new Vector3f(scaledBladeAxis)).normalize();
        Vector3f targetDirection = new Vector3f(currentDirection)
                .lerp(FINAL_BLADE_DIRECTION, progress).normalize();
        Quaternionf directionCorrection = new Quaternionf().rotationTo(currentDirection, targetDirection);
        world.premul(directionCorrection).normalize();

        Vector3f currentWidth = world.transform(new Vector3f(0.0F, 1.0F, 0.0F));
        currentWidth.add(new Vector3f(targetDirection).mul(-currentWidth.dot(targetDirection))).normalize();
        Vector3f targetWidth = new Vector3f(0.0F, 1.0F, 0.0F)
                .add(new Vector3f(targetDirection).mul(targetDirection.y * -1.0F)).normalize();
        Vector3f cross = new Vector3f(currentWidth).cross(targetWidth);
        float roll = (float) Mth.atan2(targetDirection.dot(cross),
                Mth.clamp(currentWidth.dot(targetWidth), -1.0F, 1.0F));
        world.premul(new Quaternionf().fromAxisAngleRad(targetDirection, roll * progress)).normalize();
        return parent.invert().mul(world).normalize();
    }

    private static float correctTipHeight(float rootX, float rootY, float rootZ, float scale,
                                          Quaternionf rootRotation, Quaternionf rigRoot,
                                          Quaternionf body, Quaternionf hand, Quaternionf blade,
                                          float groundContact) {
        if (groundContact <= 0.0F) {
            return rootY;
        }
        Matrix4f matrix = new Matrix4f().translation(rootX, rootY, rootZ)
                .rotate(rootRotation).scale(scale).rotate(rigRoot)
                .translate(BODY_PIVOT).rotate(body)
                .translate(HAND_R_PIVOT).rotate(hand)
                .translate(BLADE_PIVOT).rotate(blade)
                .scale(1.0F, 1.0F, BLADE_LENGTH_SCALE)
                .translate(new Vector3f(BODY_PIVOT).add(HAND_R_PIVOT).add(BLADE_PIVOT).negate());
        Vector3f tip = matrix.transformPosition(new Vector3f(BLADE_TIP));
        return rootY + (0.265F - tip.y) * groundContact;
    }

    private static void renderGroundEffects(float age, PoseStack poseStack, MultiBufferSource buffer) {
        float seconds = age / 20.0F;
        float summon = Mth.sin(clamp01(seconds / 1.15F) * Mth.PI);
        float impact = smooth((seconds - 2.82F) / 0.16F)
                * (1.0F - smooth((seconds - 3.18F) / 0.12F));
        float impactHold = smooth((seconds - 2.85F) / 0.16F);
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();

        if (summon > 0.002F) {
            ring(consumer, matrix, 0.0F, 0.05F, 0.0F,
                    0.8F + summon * 1.35F, 0.055F, 0.72F, 0.03F, 0.02F, summon * 0.62F);
            ring(consumer, matrix, 0.0F, 0.06F, 0.0F,
                    1.25F + summon * 1.8F, 0.035F, 1.0F, 0.22F, 0.06F, summon * 0.38F);
        }
        if (impactHold > 0.002F) {
            float radius = 0.9F + impactHold * 10.4F;
            ring(consumer, matrix, 0.0F, 0.07F, 10.0F,
                    radius, 0.14F, 1.0F, 0.08F, 0.025F, impact * 0.92F);
            ring(consumer, matrix, 0.0F, 0.075F, 10.0F,
                    radius * 0.68F, 0.07F, 1.0F, 0.58F, 0.12F, impact * 0.78F);
        }
    }

    private static void ring(VertexConsumer consumer, Matrix4f matrix,
                             float cx, float y, float cz, float radius, float width,
                             float red, float green, float blue, float alpha) {
        if (alpha <= 0.002F) {
            return;
        }
        int segments = 64;
        for (int i = 0; i < segments; i++) {
            float a0 = i * Mth.TWO_PI / segments;
            float a1 = (i + 1) * Mth.TWO_PI / segments;
            float inner = radius - width;
            float outer = radius + width;
            vertex(consumer, matrix, cx + Mth.cos(a0) * outer, y, cz + Mth.sin(a0) * outer,
                    red, green, blue, alpha);
            vertex(consumer, matrix, cx + Mth.cos(a1) * outer, y, cz + Mth.sin(a1) * outer,
                    red, green, blue, alpha);
            vertex(consumer, matrix, cx + Mth.cos(a1) * inner, y, cz + Mth.sin(a1) * inner,
                    red, green, blue, alpha);
            vertex(consumer, matrix, cx + Mth.cos(a0) * inner, y, cz + Mth.sin(a0) * inner,
                    red, green, blue, alpha);
        }
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
                               float x, float y, float z,
                               float red, float green, float blue, float alpha) {
        consumer.vertex(matrix, x, y, z).color(red, green, blue, alpha).endVertex();
    }

    private static Quaternionf eulerXYZ(float x, float y, float z) {
        return new Quaternionf().rotateX(x).rotateY(y).rotateZ(z);
    }

    private static Quaternionf eulerYXZ(float x, float y, float z) {
        return new Quaternionf().rotateY(y).rotateX(x).rotateZ(z);
    }

    private static void translate(PoseStack poseStack, Vector3f value) {
        poseStack.translate(value.x, value.y, value.z);
    }

    private static float clamp01(float value) {
        return Mth.clamp(value, 0.0F, 1.0F);
    }

    private static float smooth(float value) {
        float t = clamp01(value);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float easeOutCubic(float value) {
        float t = clamp01(value);
        float inv = 1.0F - t;
        return 1.0F - inv * inv * inv;
    }

    private static float easeInCubic(float value) {
        float t = clamp01(value);
        return t * t * t;
    }

    record AnimationPose(float rootX, float correctedRootY, float rootZ,
                         float scale, float alpha,
                         Quaternionf rootRotation, Quaternionf rigRootRotation,
                         Quaternionf bodyRotation, Quaternionf handLRotation,
                         Quaternionf handRRotation, Quaternionf bladeRotation) {
    }

    @Override
    public ResourceLocation getTextureLocation(JizoMitamaSoulEntity entity) {
        return EMPTY_TEXTURE;
    }
}

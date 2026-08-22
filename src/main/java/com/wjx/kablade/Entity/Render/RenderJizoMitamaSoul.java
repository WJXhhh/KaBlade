package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.EntityJizoMitamaSoul;
import com.wjx.kablade.client.renderer.JizoSoulAnimation;
import com.wjx.kablade.client.renderer.JizoSoulAnimation.Pose;
import com.wjx.kablade.client.renderer.JizoSoulAnimation.Quat;
import com.wjx.kablade.client.renderer.JizoSoulAnimation.Vec;
import mods.flammpfeil.slashblade.client.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.util.ResourceLocationRaw;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;

/** 地藏御魂的深度外壳、本体、Fresnel 辉光与四刚体下劈动画。 */
@SideOnly(Side.CLIENT)
public class RenderJizoMitamaSoul extends Render<EntityJizoMitamaSoul> {
    private static final ResourceLocationRaw MODEL = new ResourceLocationRaw(
            "kablade", "model/util/jizo_skill_boss/mdl.obj");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "kablade", "model/util/jizo_skill_boss/tex.png");
    private static final float BODY_OPACITY = 0.48F;
    private static final float BLADE_LENGTH_SCALE = 1.50F;

    public RenderJizoMitamaSoul(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0.0F;
        this.shadowOpaque = 0.0F;
    }

    @Override
    public void doRender(EntityJizoMitamaSoul entity, double x, double y, double z,
                         float entityYaw, float partialTicks) {
        float age = entity.getRenderAge(partialTicks);
        float seconds = age / 20.0F;
        Pose animation = JizoSoulAnimation.sample(seconds);
        float oldLightX = OpenGlHelper.lastBrightnessX;
        float oldLightY = OpenGlHelper.lastBrightnessY;
        boolean pushed = false;
        try {
            GlStateManager.pushMatrix();
            pushed = true;
            GlStateManager.translate(x, y, z);
            GlStateManager.rotate(-entity.rotationYaw, 0.0F, 1.0F, 0.0F);
            renderGroundEffects(age);
            if (seconds >= 0.2F) {
                renderJizo(animation);
            }
        } finally {
            JizoSoulShader.INSTANCE.restore(0);
            GL11.glColorMask(true, true, true, true);
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
            GlStateManager.enableCull();
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            OpenGlHelper.setLightmapTextureCoords(
                    OpenGlHelper.lightmapTexUnit, oldLightX, oldLightY);
            if (pushed) {
                GlStateManager.popMatrix();
            }
        }
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    private void renderJizo(Pose animation) {
        WavefrontObject model = BladeModelManager.getInstance().getModel(MODEL);
        float soulAlpha = animation.alpha * BODY_OPACITY;

        bindTexture(TEXTURE);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.10F);
        GlStateManager.disableCull();

        // 和 1.20 一样先解析单一外壳，防止 OBJ 内部三角形叠成暗色网格。
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GL11.glColorMask(false, false, false, false);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        renderSoulGroups(model, animation);

        GL11.glColorMask(true, true, true, true);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(false);
        GlStateManager.depthFunc(GL11.GL_EQUAL);
        GlStateManager.disableLighting();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, soulAlpha);
        renderSoulGroups(model, animation);

        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.enableCull();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ZERO,
                GlStateManager.DestFactor.ONE);
        GlStateManager.color(1.0F, 0.086F, 0.024F, soulAlpha * 0.78F);
        int oldProgram = JizoSoulShader.INSTANCE.bind();
        renderSoulGroups(model, animation);
        JizoSoulShader.INSTANCE.restore(oldProgram);

        GlStateManager.depthMask(true);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.enableLighting();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, animation.alpha);
        renderBlade(model, animation);
    }

    private static void renderSoulGroups(WavefrontObject model, Pose animation) {
        GlStateManager.pushMatrix();
        applyRoot(animation);
        renderBody(model, animation.bodyRotation);
        renderHand(model, "Hand_L", JizoSoulAnimation.HAND_L_PIVOT,
                animation.handLRotation, animation.bodyRotation);
        renderHand(model, "Hand_R", JizoSoulAnimation.HAND_R_PIVOT,
                animation.handRRotation, animation.bodyRotation);
        GlStateManager.popMatrix();
    }

    private static void renderBody(WavefrontObject model, Quat body) {
        GlStateManager.pushMatrix();
        translate(JizoSoulAnimation.BODY_PIVOT);
        JizoSoulAnimation.applyRotation(body);
        translate(JizoSoulAnimation.BODY_PIVOT.scale(-1.0F));
        model.renderPart("Body");
        GlStateManager.popMatrix();
    }

    private static void renderHand(WavefrontObject model, String part, Vec handPivot,
                                   Quat hand, Quat body) {
        GlStateManager.pushMatrix();
        translate(JizoSoulAnimation.BODY_PIVOT);
        JizoSoulAnimation.applyRotation(body);
        translate(handPivot);
        JizoSoulAnimation.applyRotation(hand);
        translate(JizoSoulAnimation.BODY_PIVOT.add(handPivot).scale(-1.0F));
        model.renderPart(part);
        GlStateManager.popMatrix();
    }

    private static void renderBlade(WavefrontObject model, Pose animation) {
        GlStateManager.pushMatrix();
        applyRoot(animation);
        translate(JizoSoulAnimation.BODY_PIVOT);
        JizoSoulAnimation.applyRotation(animation.bodyRotation);
        translate(JizoSoulAnimation.HAND_R_PIVOT);
        JizoSoulAnimation.applyRotation(animation.handRRotation);
        translate(JizoSoulAnimation.BLADE_PIVOT);
        JizoSoulAnimation.applyRotation(animation.bladeRotation);
        GlStateManager.scale(1.0F, 1.0F, BLADE_LENGTH_SCALE);
        translate(JizoSoulAnimation.BODY_PIVOT
                .add(JizoSoulAnimation.HAND_R_PIVOT)
                .add(JizoSoulAnimation.BLADE_PIVOT).scale(-1.0F));
        model.renderPart("Blade_Big");
        GlStateManager.popMatrix();
    }

    private static void applyRoot(Pose animation) {
        GlStateManager.translate(animation.rootX, animation.rootY, animation.rootZ);
        JizoSoulAnimation.applyRotation(animation.rootRotation);
        GlStateManager.scale(animation.scale, animation.scale, animation.scale);
        JizoSoulAnimation.applyRotation(animation.rigRootRotation);
    }

    private static void translate(Vec value) {
        GlStateManager.translate(value.x, value.y, value.z);
    }

    private static void renderGroundEffects(float age) {
        float seconds = age / 20.0F;
        float summon = MathHelper.sin(clamp01(seconds / 1.15F) * (float) Math.PI);
        float impact = smooth((seconds - 2.82F) / 0.16F)
                * (1.0F - smooth((seconds - 3.18F) / 0.12F));
        float impactHold = smooth((seconds - 2.85F) / 0.16F);

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.depthMask(false);
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        if (summon > 0.002F) {
            ring(buffer, 0.0F, 0.05F, 0.0F,
                    0.8F + summon * 1.35F, 0.055F,
                    0.72F, 0.03F, 0.02F, summon * 0.62F);
            ring(buffer, 0.0F, 0.06F, 0.0F,
                    1.25F + summon * 1.8F, 0.035F,
                    1.0F, 0.22F, 0.06F, summon * 0.38F);
        }
        if (impactHold > 0.002F) {
            float radius = 0.9F + impactHold * 10.4F;
            ring(buffer, 0.0F, 0.07F, 10.0F,
                    radius, 0.14F,
                    1.0F, 0.08F, 0.025F, impact * 0.92F);
            ring(buffer, 0.0F, 0.075F, 10.0F,
                    radius * 0.68F, 0.07F,
                    1.0F, 0.58F, 0.12F, impact * 0.78F);
        }
        Tessellator.getInstance().draw();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
    }

    private static void ring(BufferBuilder buffer, float centerX, float y, float centerZ,
                             float radius, float width,
                             float red, float green, float blue, float alpha) {
        if (alpha <= 0.002F) {
            return;
        }
        int segments = 64;
        for (int i = 0; i < segments; i++) {
            float angle0 = i * (float) Math.PI * 2.0F / segments;
            float angle1 = (i + 1) * (float) Math.PI * 2.0F / segments;
            float inner = radius - width;
            float outer = radius + width;
            vertex(buffer, centerX + MathHelper.cos(angle0) * outer, y,
                    centerZ + MathHelper.sin(angle0) * outer, red, green, blue, alpha);
            vertex(buffer, centerX + MathHelper.cos(angle1) * outer, y,
                    centerZ + MathHelper.sin(angle1) * outer, red, green, blue, alpha);
            vertex(buffer, centerX + MathHelper.cos(angle1) * inner, y,
                    centerZ + MathHelper.sin(angle1) * inner, red, green, blue, alpha);
            vertex(buffer, centerX + MathHelper.cos(angle0) * inner, y,
                    centerZ + MathHelper.sin(angle0) * inner, red, green, blue, alpha);
        }
    }

    private static void vertex(BufferBuilder buffer, float x, float y, float z,
                               float red, float green, float blue, float alpha) {
        buffer.pos(x, y, z).color(red, green, blue, alpha).endVertex();
    }

    private static float clamp01(float value) {
        return MathHelper.clamp(value, 0.0F, 1.0F);
    }

    private static float smooth(float value) {
        float t = clamp01(value);
        return t * t * (3.0F - 2.0F * t);
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityJizoMitamaSoul entity) {
        return TEXTURE;
    }
}

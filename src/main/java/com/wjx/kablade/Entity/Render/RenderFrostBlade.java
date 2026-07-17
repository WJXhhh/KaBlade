package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.SummonBladeOfFrostBlade;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Additive, textureless renderer for the Frost Blade SA. */
public class RenderFrostBlade extends Render<SummonBladeOfFrostBlade> {
    private static final float CYAN_R = 0.08F;
    private static final float CYAN_G = 0.82F;
    private static final float CYAN_B = 1.0F;

    public RenderFrostBlade(RenderManager renderManager) {
        super(renderManager);
        shadowSize = 0.0F;
    }

    @Override
    public void doRender(SummonBladeOfFrostBlade entity, double x, double y, double z,
                         float entityYaw, float partialTicks) {
        if (entity.getPhase() == SummonBladeOfFrostBlade.PHASE_WAITING) {
            return;
        }

        boolean texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int blendSrc = GL11.glGetInteger(GL11.GL_BLEND_SRC);
        int blendDst = GL11.glGetInteger(GL11.GL_BLEND_DST);
        int shadeModel = GL11.glGetInteger(GL11.GL_SHADE_MODEL);

        GL11.glPushMatrix();
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glDepthMask(false);
            GL11.glShadeModel(GL11.GL_SMOOTH);
            GL11.glTranslatef((float) x, (float) y + 0.12F, (float) z);

            if (entity.getPhase() == SummonBladeOfFrostBlade.PHASE_IMPACT) {
                renderImpact(entity, partialTicks);
            } else {
                renderFlyingBlade(entity, partialTicks);
            }
        } finally {
            GL11.glPopMatrix();
            GL11.glDepthMask(depthMask);
            GL11.glBlendFunc(blendSrc, blendDst);
            GL11.glShadeModel(shadeModel);
            restoreCapability(GL11.GL_TEXTURE_2D, texture);
            restoreCapability(GL11.GL_LIGHTING, lighting);
            restoreCapability(GL11.GL_BLEND, blend);
            restoreCapability(GL11.GL_CULL_FACE, cull);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private void renderFlyingBlade(SummonBladeOfFrostBlade entity, float partialTicks) {
        float age = entity.ticksExisted + partialTicks;
        float phaseAge = entity.getClientPhaseTicks() + partialTicks;
        float appear = entity.getPhase() == SummonBladeOfFrostBlade.PHASE_ARMING
                ? clamp(phaseAge / 2.0F) : 1.0F;
        float pulse = 0.96F + 0.04F * (float) Math.sin(age * 1.4F);
        float finisherScale = entity.isFinisher() ? 1.28F : 1.0F;

        // 飞行速度是实际朝向的最终来源，可避免无锁定发射时旋转同步慢一帧而横置。
        double horizontalMotion = Math.sqrt(entity.motionX * entity.motionX + entity.motionZ * entity.motionZ);
        if (horizontalMotion * horizontalMotion + entity.motionY * entity.motionY > 1.0E-8D) {
            GL11.glRotatef((float) (Math.atan2(entity.motionX, entity.motionZ) * 180.0D / Math.PI),
                    0.0F, 1.0F, 0.0F);
            GL11.glRotatef((float) (Math.atan2(-entity.motionY, horizontalMotion) * 180.0D / Math.PI),
                    1.0F, 0.0F, 0.0F);
        } else {
            GL11.glRotatef(-lerpDegrees(entity.prevRotationYaw, entity.rotationYaw, partialTicks),
                    0.0F, 1.0F, 0.0F);
            GL11.glRotatef(lerp(entity.prevRotationPitch, entity.rotationPitch, partialTicks),
                    1.0F, 0.0F, 0.0F);
        }
        float scale = appear * pulse * finisherScale;
        GL11.glScalef(scale, scale, scale);

        if (entity.getPhase() == SummonBladeOfFrostBlade.PHASE_FLYING) {
            float trail = entity.isFinisher() ? 5.1F : 3.8F;
            drawTrail(trail, 0.34F, CYAN_R, CYAN_G, CYAN_B, 0.30F);
            drawTrail(trail * 0.72F, 0.105F, 0.92F, 1.0F, 1.0F, 0.88F);
        } else {
            drawTrail(1.15F, 0.28F, CYAN_R, CYAN_G, CYAN_B, 0.24F);
        }

        drawBlade(1.0F, 0.33F, CYAN_R, CYAN_G, CYAN_B, 0.38F);
        drawBlade(0.88F, 0.205F, 0.32F, 0.94F, 1.0F, 0.92F);
        drawBlade(0.74F, 0.078F, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderImpact(SummonBladeOfFrostBlade entity, float partialTicks) {
        float impactAge = entity.getClientPhaseTicks() + partialTicks;
        float duration = entity.isFinisher() ? 11.0F : 8.0F;
        float t = clamp(impactAge / duration);
        float alpha = clamp(impactAge / 1.25F) * clamp((duration - impactAge) / 4.5F);
        if (alpha <= 0.01F) {
            return;
        }

        float size = (entity.isFinisher() ? 1.55F : 1.0F) * (0.62F + 0.58F * easeOut(t));
        GL11.glRotatef((entity.getEntityId() * 37) % 360 + impactAge * 7.0F, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(size, size, size);

        drawBlade(0.78F, 0.30F, 0.72F, 0.98F, 1.0F, alpha * 0.88F);
        GL11.glPushMatrix();
        GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
        drawBlade(0.78F, 0.30F, 0.72F, 0.98F, 1.0F, alpha * 0.76F);
        GL11.glPopMatrix();

        int spikes = entity.isFinisher() ? 10 : 8;
        for (int i = 0; i < spikes; i++) {
            GL11.glPushMatrix();
            GL11.glRotatef(i * (360.0F / spikes), 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-38.0F + (i % 3) * 36.0F, 1.0F, 0.0F, 0.0F);
            float length = (1.25F + (i % 4) * 0.18F) * (0.72F + 0.42F * easeOut(t));
            drawSpike(length, 0.17F + (i & 1) * 0.045F,
                    CYAN_R, CYAN_G, CYAN_B, alpha * 0.80F);
            GL11.glPopMatrix();
        }

        float ringRadius = 0.38F + easeOut(t) * (entity.isFinisher() ? 1.55F : 1.15F);
        GL11.glPushMatrix();
        GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
        drawRing(ringRadius, 0.095F, 0.70F, 0.98F, 1.0F, alpha * (1.0F - t));
        GL11.glPopMatrix();
        GL11.glPushMatrix();
        GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
        drawRing(ringRadius * 0.82F, 0.065F, 1.0F, 1.0F, 1.0F,
                alpha * 0.72F * (1.0F - t));
        GL11.glPopMatrix();
    }

    private void drawBlade(float scale, float width, float r, float g, float b, float alpha) {
        float tip = 1.72F * scale;
        float shoulder = 0.18F * scale;
        float base = -0.62F * scale;
        float guardZ = -0.34F * scale;
        float guardWidth = width * 1.65F;

        BufferBuilder buffer = beginQuads();
        quad(buffer,
                0.0F, 0.0F, tip, r, g, b, alpha,
                width, 0.0F, shoulder, r, g, b, alpha * 0.82F,
                0.0F, 0.0F, base, r, g, b, alpha * 0.45F,
                -width, 0.0F, shoulder, r, g, b, alpha * 0.82F);
        quad(buffer,
                0.0F, 0.0F, tip, r, g, b, alpha,
                0.0F, width, shoulder, r, g, b, alpha * 0.82F,
                0.0F, 0.0F, base, r, g, b, alpha * 0.45F,
                0.0F, -width, shoulder, r, g, b, alpha * 0.82F);
        quad(buffer,
                0.0F, 0.0F, guardZ + width * 0.55F, r, g, b, alpha * 0.82F,
                guardWidth, 0.0F, guardZ, r, g, b, alpha * 0.66F,
                0.0F, 0.0F, guardZ - width * 0.55F, r, g, b, alpha * 0.45F,
                -guardWidth, 0.0F, guardZ, r, g, b, alpha * 0.66F);
        Tessellator.getInstance().draw();
    }

    private void drawTrail(float length, float width, float r, float g, float b, float alpha) {
        float front = -0.18F;
        float tail = -length;
        BufferBuilder buffer = beginQuads();
        quad(buffer,
                -width, 0.0F, front, r, g, b, alpha,
                width, 0.0F, front, r, g, b, alpha,
                0.0F, 0.0F, tail, r, g, b, 0.0F,
                0.0F, 0.0F, tail, r, g, b, 0.0F);
        quad(buffer,
                0.0F, -width, front, r, g, b, alpha,
                0.0F, width, front, r, g, b, alpha,
                0.0F, 0.0F, tail, r, g, b, 0.0F,
                0.0F, 0.0F, tail, r, g, b, 0.0F);
        Tessellator.getInstance().draw();
    }

    private void drawSpike(float length, float width, float r, float g, float b, float alpha) {
        BufferBuilder buffer = beginQuads();
        quad(buffer,
                0.0F, 0.0F, length, 1.0F, 1.0F, 1.0F, alpha,
                width, 0.0F, 0.0F, r, g, b, alpha * 0.72F,
                0.0F, 0.0F, -0.18F, r, g, b, 0.0F,
                -width, 0.0F, 0.0F, r, g, b, alpha * 0.72F);
        quad(buffer,
                0.0F, 0.0F, length, 1.0F, 1.0F, 1.0F, alpha,
                0.0F, width, 0.0F, r, g, b, alpha * 0.72F,
                0.0F, 0.0F, -0.18F, r, g, b, 0.0F,
                0.0F, -width, 0.0F, r, g, b, alpha * 0.72F);
        Tessellator.getInstance().draw();
    }

    private void drawRing(float radius, float width, float r, float g, float b, float alpha) {
        BufferBuilder buffer = beginQuads();
        int segments = 40;
        for (int i = 0; i < segments; i++) {
            float a0 = (float) (Math.PI * 2.0D * i / segments);
            float a1 = (float) (Math.PI * 2.0D * (i + 1) / segments);
            float outer = radius + width * 0.5F;
            float inner = radius - width * 0.5F;
            quad(buffer,
                    (float) Math.cos(a0) * outer, (float) Math.sin(a0) * outer, 0.0F, r, g, b, alpha,
                    (float) Math.cos(a1) * outer, (float) Math.sin(a1) * outer, 0.0F, r, g, b, alpha,
                    (float) Math.cos(a1) * inner, (float) Math.sin(a1) * inner, 0.0F,
                    r, g, b, alpha * 0.45F,
                    (float) Math.cos(a0) * inner, (float) Math.sin(a0) * inner, 0.0F,
                    r, g, b, alpha * 0.45F);
        }
        Tessellator.getInstance().draw();
    }

    private BufferBuilder beginQuads() {
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        return buffer;
    }

    private void quad(BufferBuilder buffer,
                      float x0, float y0, float z0, float r0, float g0, float b0, float a0,
                      float x1, float y1, float z1, float r1, float g1, float b1, float a1,
                      float x2, float y2, float z2, float r2, float g2, float b2, float a2,
                      float x3, float y3, float z3, float r3, float g3, float b3, float a3) {
        vertex(buffer, x0, y0, z0, r0, g0, b0, a0);
        vertex(buffer, x1, y1, z1, r1, g1, b1, a1);
        vertex(buffer, x2, y2, z2, r2, g2, b2, a2);
        vertex(buffer, x3, y3, z3, r3, g3, b3, a3);
    }

    private void vertex(BufferBuilder buffer, float x, float y, float z,
                        float r, float g, float b, float alpha) {
        buffer.pos(x, y, z).color(toColor(r), toColor(g), toColor(b), toColor(alpha)).endVertex();
    }

    private int toColor(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0F)));
    }

    private void restoreCapability(int capability, boolean enabled) {
        if (enabled) {
            GL11.glEnable(capability);
        } else {
            GL11.glDisable(capability);
        }
    }

    private float easeOut(float t) {
        return 1.0F - (1.0F - t) * (1.0F - t);
    }

    private float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private float lerp(float start, float end, float percent) {
        return start + percent * (end - start);
    }

    private float lerpDegrees(float start, float end, float percent) {
        float difference = end - start;
        while (difference < -180.0F) {
            difference += 360.0F;
        }
        while (difference >= 180.0F) {
            difference -= 360.0F;
        }
        return start + percent * difference;
    }

    @Override
    protected ResourceLocation getEntityTexture(SummonBladeOfFrostBlade entity) {
        return null;
    }
}

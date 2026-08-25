package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.EntityValkyrieImpact;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;

/** 1.20 女武神冲击：落地爆发、五路地裂、实体岩板、碎屑和灼痕。 */
public class RenderValkyrieImpact extends Render<EntityValkyrieImpact> {
    private static final ResourceLocation ROCK = new ResourceLocation("minecraft", "textures/blocks/stone.png");
    private static final float TWO_PI = (float) Math.PI * 2.0F;
    private static final int DEBRIS_COUNT = 240;
    private static final Lane[] LANES = {
            new Lane(0, 0, 7.25F, 1.20F, 1.10F, 0, 16),
            new Lane(-14, -.45F, 6.40F, .95F, .95F, .025F, 14),
            new Lane(14, .45F, 6.40F, .95F, .95F, .025F, 14),
            new Lane(-28, -.80F, 5.20F, .75F, .85F, .050F, 12),
            new Lane(28, .80F, 5.20F, .75F, .85F, .050F, 12)
    };

    public RenderValkyrieImpact(RenderManager manager) { super(manager); shadowSize = 0.0F; }
    @Nullable @Override protected ResourceLocation getEntityTexture(EntityValkyrieImpact entity) { return ROCK; }

    @Override
    public void doRender(EntityValkyrieImpact entity, double x, double y, double z,
                         float yaw, float partialTicks) {
        float seconds = entity.getAnimationFrame(partialTicks) / 30.0F;
        if (seconds < 0 || seconds > 2.24F) return;
        float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            GlStateManager.translate(x, y + .025D, z);
            GlStateManager.rotate(-entity.getEffectYaw(), 0, 1, 0);
            GlStateManager.disableCull();
            GlStateManager.enableBlend();
            GlStateManager.disableLighting();

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder b = tessellator.getBuffer();

            // 先写深度的实体岩板，地裂不再是悬在地面上的一排扁三角。
            if (seconds >= .56F) {
                GlStateManager.enableTexture2D();
                GlStateManager.depthMask(true);
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                        GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                Minecraft.getMinecraft().getTextureManager().bindTexture(ROCK);
                b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
                fissureRocks(b, seconds);
                tessellator.draw();
            }

            GlStateManager.disableTexture2D();
            GlStateManager.depthMask(false);
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ZERO);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            if (seconds >= .56F && seconds < 1.0F) impactBurst(b, seconds - .56F);
            if (seconds >= .56F) {
                fissureEnergy(b, seconds);
                scorchTrails(b, seconds - .56F);
            }
            if (seconds >= .58F && seconds < 2.20F) debris(b, seconds - .58F);
            tessellator.draw();
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GlStateManager.color(1, 1, 1, 1);
            GL11.glPopAttrib();
            GL11.glPopMatrix();
            GlStateManager.enableTexture2D();
            GlStateManager.enableCull();
            GlStateManager.enableLighting();
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
        }
        super.doRender(entity, x, y, z, yaw, partialTicks);
    }

    private static void impactBurst(BufferBuilder b, float t) {
        float progress = t / .42F;
        float alpha = Math.max(0, 1.0F - progress);
        float radius = .3F + progress * 2.9F;
        for (int i = 0; i < 8; i++) {
            double p1 = i / 8.0D * Math.PI * .5D, p2 = (i + 1) / 8.0D * Math.PI * .5D;
            for (int j = 0; j < 16; j++) {
                double a = j / 16.0D * TWO_PI, n = (j + 1) / 16.0D * TWO_PI;
                domeVertex(b, radius, p1, a, 1, .98F, .85F, alpha * .9F);
                domeVertex(b, radius, p1, n, 1, .98F, .85F, alpha * .9F);
                domeVertex(b, radius, p2, n, 1, .85F, .40F, alpha * .9F);
                domeVertex(b, radius, p2, a, 1, .85F, .40F, alpha * .9F);
            }
        }
        float inner = .15F + progress * 3.2F, outer = .45F + progress * 5.5F;
        for (int j = 0; j < 32; j++) {
            double a = j / 32.0D * TWO_PI, n = (j + 1) / 32.0D * TWO_PI;
            vertex(b, Math.cos(a) * inner, .02, Math.sin(a) * inner, 1, .95F, .60F, alpha * .85F);
            vertex(b, Math.cos(n) * inner, .02, Math.sin(n) * inner, 1, .95F, .60F, alpha * .85F);
            vertex(b, Math.cos(n) * outer, .02, Math.sin(n) * outer, .95F, .60F, .10F, 0);
            vertex(b, Math.cos(a) * outer, .02, Math.sin(a) * outer, .95F, .60F, .10F, 0);
        }
        for (int i = 0; i < 24; i++) {
            double angle = i / 24.0D * TWO_PI;
            boolean longSpike = (i & 1) == 0;
            float length = longSpike ? .5F + progress * 2.8F : .8F + progress * 4.2F;
            float width = longSpike ? .09F : .06F;
            double px = -Math.sin(angle) * width, pz = Math.cos(angle) * width;
            double tx = Math.cos(angle) * length, tz = Math.sin(angle) * length;
            double ty = (longSpike ? .12F : .08F) + progress * .45F;
            vertex(b, -px, .05, -pz, 1, .80F, .20F, alpha * .85F);
            vertex(b, px, .05, pz, 1, .80F, .20F, alpha * .85F);
            vertex(b, tx, ty, tz, 1, .98F, .90F, 0);
            vertex(b, tx, ty, tz, 1, .98F, .90F, 0);
        }
    }

    private static void fissureRocks(BufferBuilder b, float seconds) {
        for (Lane lane : LANES) {
            Basis basis = lane.basis();
            for (int i = 0; i < lane.segments; i++) {
                float progress = i / (float) (lane.segments - 1);
                Segment segment = segment(seconds, lane, progress);
                if (segment == null) continue;
                float dist = progress * lane.length;
                float x = lane.offsetX + basis.fx * dist, z = basis.fz * dist;
                float offset = .20F * lane.widthScale;
                float width = .22F * lane.widthScale, length = .32F * lane.widthScale;
                float height = .16F * lane.heightScale * segment.eruption;
                rock(b, x - basis.px * offset, z - basis.pz * offset, width, length,
                        height, .38F, basis);
                rock(b, x + basis.px * offset, z + basis.pz * offset, width, length,
                        height, -.38F, basis);
            }
        }
    }

    private static void fissureEnergy(BufferBuilder b, float seconds) {
        for (Lane lane : LANES) {
            Basis basis = lane.basis();
            for (int i = 0; i < lane.segments; i++) {
                float progress = i / (float) (lane.segments - 1);
                Segment segment = segment(seconds, lane, progress);
                if (segment == null) continue;
                float dist = progress * lane.length;
                float x = lane.offsetX + basis.fx * dist, z = basis.fz * dist;
                float height = (.55F + (float) Math.sin(progress * Math.PI) * .65F)
                        * lane.heightScale * segment.eruption;
                float halfW = .14F * lane.widthScale, halfL = .18F * lane.widthScale;
                float nx = x - basis.fx * halfL, nz = z - basis.fz * halfL;
                float sx = x + basis.fx * halfL, sz = z + basis.fz * halfL;
                float wx = x - basis.px * halfW, wz = z - basis.pz * halfW;
                float ex = x + basis.px * halfW, ez = z + basis.pz * halfW;
                float tx = x + basis.fx * .04F, tz = z + basis.fz * .04F;
                crestFace(b, wx, wz, ex, ez, tx, tz, height, segment.alpha, .65F);
                crestFace(b, ex, ez, wx, wz, tx, tz, height, segment.alpha, .65F);
                crestFace(b, ex, ez, sx, sz, tx, tz, height, segment.alpha, .55F);
                crestFace(b, wx, wz, nx, nz, tx, tz, height, segment.alpha, .55F);
            }
        }
    }

    private static Segment segment(float seconds, Lane lane, float progress) {
        float delay = .56F + lane.delayOffset + progress * .36F;
        if (seconds < delay) return null;
        float age = seconds - delay;
        float eruption, alpha;
        if (age < .12F) {
            float p = age / .12F;
            eruption = 1.0F - (1.0F - p) * (1.0F - p);
            alpha = eruption * .95F;
        } else if (age < .67F) {
            eruption = 1;
            alpha = .85F + (float) Math.sin((age + lane.delayOffset) * 18.0F) * .15F;
        } else if (age < 1.37F) {
            float p = (age - .67F) / .70F;
            eruption = 1.0F - p * .5F;
            alpha = Math.max(0, (1.0F - p) * .85F);
        } else return null;
        return new Segment(eruption, alpha);
    }

    private static void rock(BufferBuilder b, float cx, float cz, float width, float length,
                             float height, float tilt, Basis d) {
        if (height <= .005F) return;
        float hw = width * .5F, hl = length * .5F;
        float cos = (float) Math.cos(tilt), sin = (float) Math.sin(tilt);
        float x1 = cx - d.px * hw * cos, z1 = cz - d.pz * hw * cos, y1 = sin * hw;
        float x2 = cx + d.px * hw * cos, z2 = cz + d.pz * hw * cos, y2 = -sin * hw;
        float fx1 = x1 - d.fx * hl, fz1 = z1 - d.fz * hl;
        float fx2 = x2 - d.fx * hl, fz2 = z2 - d.fz * hl;
        float bx1 = x1 + d.fx * hl, bz1 = z1 + d.fz * hl;
        float bx2 = x2 + d.fx * hl, bz2 = z2 + d.fz * hl;
        rockQuad(b, fx1, y1 + height, fz1, 0, 0, fx2, y2 + height, fz2, 1, 0,
                bx2, y2 + height, bz2, 1, 1, bx1, y1 + height, bz1, 0, 1, .58F);
        rockQuad(b, bx1, 0, bz1, 0, 1, fx1, 0, fz1, 1, 1,
                fx1, y1 + height, fz1, 1, 0, bx1, y1 + height, bz1, 0, 0, .35F);
        rockQuad(b, fx2, y2 + height, fz2, 0, 0, fx2, 0, fz2, 0, 1,
                bx2, 0, bz2, 1, 1, bx2, y2 + height, bz2, 1, 0, .38F);
        rockQuad(b, fx1, 0, fz1, 0, 1, fx2, 0, fz2, 1, 1,
                fx2, y2 + height, fz2, 1, 0, fx1, y1 + height, fz1, 0, 0, .46F);
        rockQuad(b, bx2, 0, bz2, 0, 1, bx1, 0, bz1, 1, 1,
                bx1, y1 + height, bz1, 1, 0, bx2, y2 + height, bz2, 0, 0, .42F);
    }

    private static void rockQuad(BufferBuilder b,
                                 float x1,float y1,float z1,float u1,float v1,
                                 float x2,float y2,float z2,float u2,float v2,
                                 float x3,float y3,float z3,float u3,float v3,
                                 float x4,float y4,float z4,float u4,float v4,float shade) {
        rockVertex(b,x1,y1,z1,u1,v1,shade); rockVertex(b,x2,y2,z2,u2,v2,shade);
        rockVertex(b,x3,y3,z3,u3,v3,shade); rockVertex(b,x4,y4,z4,u4,v4,shade);
    }

    private static void rockVertex(BufferBuilder b, float x,float y,float z,float u,float v,float shade) {
        b.pos(x,y,z).tex(u,v).color(shade, shade * .97F, shade * .91F, 1).endVertex();
    }

    private static void crestFace(BufferBuilder b, float ax,float az,float bx,float bz,
                                  float tx,float tz,float height,float alpha,float green) {
        vertex(b, ax, .02, az, 1, green, .08F, alpha * .85F);
        vertex(b, bx, .02, bz, 1, green, .08F, alpha * .85F);
        vertex(b, tx, height, tz, 1, .98F, .85F, alpha);
        vertex(b, tx, height, tz, 1, .98F, .85F, alpha);
    }

    private static void debris(BufferBuilder b, float age) {
        float alpha = Math.max(0, 1.0F - age / 1.38F);
        for (int i = 0; i < DEBRIS_COUNT; i++) {
            float seed = i * 13.371F;
            float distance = ((float) Math.sin(seed * 2.1F) * .5F + .5F) * 7.25F;
            float angle = (float) Math.sin(seed * 5.7F) * .55F;
            float x0 = (float) Math.sin(angle) * distance + (float) Math.cos(seed * 3.3F) * .4F;
            float z0 = (float) Math.cos(angle) * distance;
            float vx = (float) Math.cos(seed * 4.3F) * 2.4F;
            float vy = 2.2F + ((float) Math.sin(seed * 8.9F) * .5F + .5F) * 4.2F;
            float vz = ((float) Math.sin(seed * 3.1F) * .5F + .5F) * 2.2F;
            float x = x0 + vx * age;
            float y = Math.max(.02F, .05F + vy * age - 3.75F * age * age);
            float z = z0 + vz * age * .6F;
            float size = i % 3 == 0 ? .065F : .04F;
            boolean glow = (i & 1) == 0;
            float red = glow ? 1 : .85F, green = glow ? .88F : .55F, blue = glow ? .35F : .15F;
            // 两张交叉碎片，侧视也不会消失。
            particleQuad(b, x, y, z, size, red, green, blue, alpha * .9F, false);
            particleQuad(b, x, y, z, size, red, green, blue, alpha * .72F, true);
        }
    }

    private static void particleQuad(BufferBuilder b,float x,float y,float z,float s,
                                     float r,float g,float blue,float a,boolean crossed) {
        if (crossed) {
            vertex(b,x,y-s,z-s,r,g,blue,a); vertex(b,x,y-s,z+s,r,g,blue,a);
            vertex(b,x,y+s,z+s,r,g,blue,a); vertex(b,x,y+s,z-s,r,g,blue,a);
        } else {
            vertex(b,x-s,y-s,z,r,g,blue,a); vertex(b,x+s,y-s,z,r,g,blue,a);
            vertex(b,x+s,y+s,z,r,g,blue,a); vertex(b,x-s,y+s,z,r,g,blue,a);
        }
    }

    private static void scorchTrails(BufferBuilder b, float age) {
        float alpha = age < .35F ? age / .35F * .75F
                : Math.max(0, (1.0F - (age - .35F) / 1.33F) * .75F);
        if (alpha <= .005F) return;
        for (Lane lane : LANES) {
            Basis d = lane.basis();
            float half = .28F * lane.widthScale;
            float sx = lane.offsetX, sz = 0;
            float ex = sx + d.fx * lane.length, ez = d.fz * lane.length;
            vertex(b,sx-d.px*half,.006,sz-d.pz*half,.95F,.60F,.15F,alpha*.70F);
            vertex(b,sx+d.px*half,.006,sz+d.pz*half,.95F,.60F,.15F,alpha*.70F);
            vertex(b,ex+d.px*half,.006,ez+d.pz*half,.95F,.30F,.06F,alpha*.18F);
            vertex(b,ex-d.px*half,.006,ez-d.pz*half,.95F,.30F,.06F,alpha*.18F);
        }
    }

    private static void domeVertex(BufferBuilder b,float radius,double phi,double theta,
                                   float r,float g,float blue,float alpha) {
        vertex(b,radius*Math.cos(phi)*Math.cos(theta),radius*Math.sin(phi)+.05,
                radius*Math.cos(phi)*Math.sin(theta),r,g,blue,alpha);
    }

    private static void vertex(BufferBuilder b,double x,double y,double z,
                               float r,float g,float blue,float alpha) {
        b.pos(x,y,z).color(r,g,blue,MathHelper.clamp(alpha,0,1)).endVertex();
    }

    private static final class Lane {
        final float angle, offsetX, length, heightScale, widthScale, delayOffset;
        final int segments;
        Lane(float angle,float offsetX,float length,float heightScale,float widthScale,
             float delayOffset,int segments) {
            this.angle=angle; this.offsetX=offsetX; this.length=length;
            this.heightScale=heightScale; this.widthScale=widthScale;
            this.delayOffset=delayOffset; this.segments=segments;
        }
        Basis basis() {
            double rad=Math.toRadians(angle);
            return new Basis((float)Math.sin(rad),(float)Math.cos(rad),
                    (float)Math.cos(rad),(float)-Math.sin(rad));
        }
    }
    private static final class Basis {
        final float fx,fz,px,pz;
        Basis(float fx,float fz,float px,float pz){this.fx=fx;this.fz=fz;this.px=px;this.pz=pz;}
    }
    private static final class Segment {
        final float eruption,alpha;
        Segment(float eruption,float alpha){this.eruption=eruption;this.alpha=alpha;}
    }
}

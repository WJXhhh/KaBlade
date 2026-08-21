package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.EntityBloodfyreFrenzy;
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

/**
 * 浴血狂樱的 1.12.2 动态几何渲染器。保留环形刀光、血焰爆裂、烟雾、
 * 地面灼痕、余烬和残樱，并由 BloodfyreShader 提供噪声侵蚀与白热内芯。
 */
@SideOnly(Side.CLIENT)
public class RenderBloodfyreFrenzy extends Render<EntityBloodfyreFrenzy> {
    private static final int SEGMENTS = 128;
    private static final float TAU = (float) Math.PI * 2.0F;
    private static final float SPAN = 240.0F * 0.017453292F;
    private static final float START = -40.0F * 0.017453292F + (float) Math.PI - SPAN * 0.5F;

    public RenderBloodfyreFrenzy(RenderManager manager) {
        super(manager);
        this.shadowSize = 0.0F;
        this.shadowOpaque = 0.0F;
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityBloodfyreFrenzy entity) {
        return null;
    }

    @Override
    public void doRender(EntityBloodfyreFrenzy entity, double x, double y, double z,
                         float entityYaw, float partialTicks) {
        float age = MathHelper.clamp(entity.ticksExisted + partialTicks, 0.0F,
                EntityBloodfyreFrenzy.LIFETIME_TICKS);
        if (age >= EntityBloodfyreFrenzy.LIFETIME_TICKS) return;
        float oldX = OpenGlHelper.lastBrightnessX;
        float oldY = OpenGlHelper.lastBrightnessY;
        int oldProgram = BloodfyreShader.bind(age);
        boolean pushed = false;
        try {
            GlStateManager.disableLighting();
            GlStateManager.disableCull();
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.depthMask(false);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
            GlStateManager.pushMatrix();
            pushed = true;
            GlStateManager.translate(x, y + 0.025D, z);
            GlStateManager.rotate(180.0F - entity.rotationYaw, 0.0F, 1.0F, 0.0F);

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buffer = tess.getBuffer();
            // 暗色刀身、烟雾和焦痕必须使用普通透明混合。
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            renderMainSlash(buffer, age, false);
            renderSmoke(buffer, age, entity.getEntityId());
            renderScar(buffer, age, false);
            tess.draw();

            // 白热刀锋和粒子层使用加色叠加，模拟原版 bloom。
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            renderGuide(buffer, age);
            renderMainSlash(buffer, age, true);
            renderRupture(buffer, age, entity.getEntityId());
            renderScar(buffer, age, true);
            renderEmbers(buffer, age, entity.getEntityId());
            renderPetals(buffer, age, entity.getEntityId());
            tess.draw();
        } finally {
            if (pushed) GlStateManager.popMatrix();
            BloodfyreShader.restore(oldProgram);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, oldX, oldY);
            GlStateManager.enableTexture2D();
            GlStateManager.depthMask(true);
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.disableBlend();
            GlStateManager.enableCull();
            GlStateManager.enableLighting();
        }
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    private static void renderGuide(BufferBuilder b, float age) {
        float alpha = window(age, 5.0F, 9.6F, 0.65F, 0.8F);
        float p = smooth((age - 5.0F) / 3.0F);
        float radius = lerp(p, 1.72F, 4.04F);
        float span = lerp(p, 0.20F, 1.0F);
        arcBand(b, (1.0F - span) * 0.5F, (1.0F + span) * 0.5F,
                radius - 0.03F, radius + 0.03F, 0.06F,
                1.0F, 0.82F, 0.26F, alpha, 3.0F);
    }

    private static void renderMainSlash(BufferBuilder b, float age, boolean glow) {
        float alpha = mainAlpha(age), head = slashProgress(age);
        if (alpha <= 0.002F || head <= 0.001F) return;
        float erosion = smooth((age - 14.6F) / 19.4F);
        float bodyTail = 0.16F * smooth((age - 23.0F) / 8.0F);
        float darkTail = 0.12F * smooth((age - 24.5F) / 8.0F);
        float darkFade = 1.0F - 0.22F * smooth((age - 19.0F) / 12.0F);
        if (!glow) {
            erodedArcBand(b, darkTail, head, 3.02F, 4.94F, -0.015F,
                    0.30F, 0.008F, 0.018F, alpha * darkFade * 0.72F, 0.0F,
                    101, MathHelper.clamp(erosion * 1.08F, 0.0F, 1.0F));
            erodedArcBand(b, bodyTail, head, 3.34F, 4.78F, 0.028F,
                    0.94F, 0.012F, 0.022F, alpha * darkFade * 0.90F, 1.0F,
                    131, erosion);
        } else {
            brightArcBand(b, age, 0.0F, head, 3.42F, 4.84F, 0.072F, alpha, 163);
            for (int i = 0; i < 4; i++) {
                float ft = head < 0.995F ? Math.max(0.0F, head - 0.30F - i * 0.045F)
                        : 0.055F * smooth((age - 18.0F - i * 0.35F) / 8.0F);
                float filamentHead = head < 0.995F ? Math.max(0.0F, head - i * 0.018F)
                        : 1.0F - 0.035F * smooth((age - 18.0F - i * 0.25F) / 7.0F);
                float fade = 1.0F - smooth((age - 24.0F - i * 0.6F) / 5.5F);
                float radius = 4.20F + i * 0.135F;
                arcBand(b, ft, filamentHead, radius - 0.012F, radius + 0.012F,
                        0.092F + i * 0.01F, 1.0F, 0.58F, 0.12F,
                        alpha * fade * 0.78F, 3.0F);
            }
        }
    }

    private static void renderRupture(BufferBuilder b, float age, int seed) {
        float layerAlpha = window(age, 10.8F, 28.0F, 0.55F, 5.5F);
        if (layerAlpha <= 0.002F) return;
        for (int i = 0; i < 13; i++) {
            float u = 0.05F + i * 0.077F;
            float birth = slashBirth(u) + 0.32F + (i % 3) * 0.08F;
            float localAge = age - birth;
            if (localAge <= 0.0F || localAge >= 14.0F) continue;
            float grain = random(seed + i * 47, 3.8F);
            float grow = fastOut(localAge / 1.2F);
            float erode = smooth((localAge - 4.2F) / 7.0F);
            float fade = 1.0F - smooth((localAge - 7.0F) / 7.0F);
            float distance = localAge * (0.16F + grain * 0.10F)
                    + (float) Math.pow(localAge, 1.28D) * (0.045F + grain * 0.025F);
            float width = (0.16F + grain * 0.24F) * grow * (1.0F - erode * 0.48F);
            Point anchor = arc(u, 4.56F, 0.02F);
            Point direction = tangent(u, 4.56F).scale(0.44F + grain * 0.26F)
                    .add(radial(u).scale(0.70F + smooth((u - 0.78F) / 0.20F) * 0.58F))
                    .add(0.0F, 0.30F + grain * 0.42F, 0.0F).normalize();
            float strength = layerAlpha * fade;
            jet(b, anchor, direction, distance * 1.08F + 0.18F,
                    width * 0.72F + 0.018F, 1.0F, 0.30F, 0.028F,
                    strength * 0.82F, 1.0F);
            jet(b, anchor.add(direction.scale(0.035F)), direction, distance * 1.14F + 0.12F,
                    width * 0.20F + 0.010F, 1.0F, 0.78F, 0.30F,
                    strength * (1.0F - erode * 0.62F) * 0.70F, 2.0F);
        }
        float flash = window(age, 11.2F, 15.8F, 0.22F, 2.6F);
        if (flash > 0.002F) {
            Point endpoint = arc(0.975F, 4.62F, 0.55F);
            cross(b, endpoint, 0.24F + flash * 0.42F, 0.18F + flash * 0.36F,
                    1.0F, 0.36F, 0.05F, flash * 0.72F, 3.0F);
        }
    }

    private static void renderSmoke(BufferBuilder b, float age, int seed) {
        float layerAlpha = window(age, 11.4F, 35.0F, 1.0F, 7.0F);
        if (layerAlpha <= 0.002F) return;
        for (int i = 0; i < 18; i++) {
            float u = 0.035F + i / 17.0F * 0.94F;
            float birth = slashBirth(u) + 0.72F + (i % 4) * 0.11F;
            float localAge = age - birth;
            if (localAge <= 0.0F || localAge >= 19.0F) continue;
            float grain = random(seed + i * 59, 8.1F);
            float grow = fastOut(localAge / 2.1F);
            float fade = 1.0F - smooth((localAge - 9.0F) / 10.0F);
            Point anchor = arc(u, 4.45F, 0.22F);
            Point tangent = tangent(u, 4.45F);
            Point center = anchor.add(tangent.scale(localAge * (0.055F + grain * 0.035F)))
                    .add(radial(u).scale(localAge * (0.065F + grain * 0.045F)))
                    .add(0.0F, localAge * (0.08F + grain * 0.045F), 0.0F);
            float size = 0.18F + grow * (0.34F + grain * 0.34F) + localAge * 0.045F;
            cross(b, center, size * (1.20F + (i % 3) * 0.10F), size,
                    0.10F, 0.018F, 0.024F,
                    layerAlpha * fade * (0.34F + grain * 0.18F), 0.0F);
        }
    }

    private static void renderScar(BufferBuilder b, float age, boolean glow) {
        float alpha = scarAlpha(age);
        if (alpha <= 0.002F) return;
        float width = glow ? 0.055F : 0.22F;
        groundBand(b, slashProgress(age), 4.34F - width, 4.34F + width,
                glow ? 0.025F : 0.012F, glow ? 1.0F : 0.07F,
                glow ? 0.18F : 0.001F, 0.005F, alpha * (glow ? 0.82F : 0.88F),
                glow ? 3.0F : 4.0F);
        if (age <= 11.0F) return;
        float reveal = smooth((age - 11.0F) / 6.0F);
        for (int i = 0; i < 10; i++) {
            Point root = groundArc(0.09F + i * 0.095F, 4.34F).withY(glow ? 0.026F : 0.013F);
            Point end = root.add(new Point(root.x, 0.0F, root.z).normalize()
                    .scale((0.45F + random(i, 4.2F) * 0.70F) * reveal));
            strip(b, root, end, glow ? 0.018F : 0.055F,
                    glow ? 1.0F : 0.05F, glow ? 0.14F : 0.001F, 0.003F,
                    alpha * (glow ? 0.62F : 0.72F), glow ? 3.0F : 4.0F);
        }
    }

    private static void renderEmbers(BufferBuilder b, float age, int seed) {
        float alpha = window(age, 10.8F, 43.0F, 1.2F, 9.0F);
        for (int i = 0; i < 34 && alpha > 0.002F; i++) {
            float phase = random(seed + i, 12.1F);
            float local = Math.max(0.0F, age - 10.8F - phase * 5.0F);
            float angle = random(seed + i, 13.7F) * TAU + local * 0.035F;
            float radius = 1.2F + random(seed + i, 15.4F) * 4.8F + local * 0.025F;
            Point center = new Point(MathHelper.sin(angle) * radius,
                    0.35F + random(seed + i, 17.6F) * 2.0F + local * 0.055F,
                    MathHelper.cos(angle) * radius);
            float size = 0.025F + random(seed + i, 19.1F) * 0.065F;
            cross(b, center, size, size * 2.2F, 1.0F, 0.34F, 0.025F,
                    alpha * (0.32F + phase * 0.55F), 3.0F);
        }
    }

    private static void renderPetals(BufferBuilder b, float age, int seed) {
        float alpha = window(age, 8.0F, 48.0F, 3.0F, 9.0F);
        for (int i = 0; i < 30 && alpha > 0.002F; i++) {
            float phase = random(seed + i, 21.4F);
            float angle = random(seed + i, 23.8F) * TAU + age * (0.018F + phase * 0.020F);
            float radius = 1.0F + random(seed + i, 25.2F) * 5.3F;
            Point center = new Point(MathHelper.sin(angle) * radius,
                    Math.max(0.08F, 0.45F + random(seed + i, 27.7F) * 3.0F - age * 0.015F),
                    MathHelper.cos(angle) * radius);
            diamond(b, center, 0.035F + phase * 0.07F, angle + age * 0.05F,
                    0.72F, 0.006F, 0.035F, alpha * 0.58F, 1.0F);
        }
    }

    private static void arcBand(BufferBuilder b, float from, float to, float inner, float outer,
                                float y, float r, float g, float blue, float alpha, float kind) {
        if (alpha <= 0.002F || to <= from) return;
        int start = Math.max(0, MathHelper.floor(from * SEGMENTS));
        int end = Math.min(SEGMENTS, MathHelper.ceil(to * SEGMENTS));
        for (int i = start; i < end; i++) {
            float u0 = Math.max(from, i / (float) SEGMENTS), u1 = Math.min(to, (i + 1) / (float) SEGMENTS);
            quad(b, arc(u0, outer, y), u0, 1, arc(u1, outer, y), u1, 1,
                    arc(u1, inner, y), u1, 0, arc(u0, inner, y), u0, 0,
                    r, g, blue, alpha, kind);
        }
    }

    /**
     * 原版刀身并不是一整张半透明扇面，而是按半径分片后由内向外侵蚀。
     * 这里保留 1.12.2 的 BufferBuilder 写法，只移植其网格与时间规律。
     */
    private static void erodedArcBand(BufferBuilder b, float from, float to,
                                      float inner, float outer, float y,
                                      float r, float g, float blue, float alpha, float kind,
                                      int seed, float erosion) {
        if (alpha <= 0.002F || to <= from + 0.001F) return;
        int start = Math.max(0, MathHelper.floor(from * SEGMENTS));
        int end = Math.min(SEGMENTS, MathHelper.ceil(to * SEGMENTS));
        final int radialSlices = 32;
        for (int i = start; i < end; i++) {
            float u0 = Math.max(from, i / (float) SEGMENTS);
            float u1 = Math.min(to, (i + 1) / (float) SEGMENTS);
            float edgeNoise0 = (random(seed + i * 17, 2.8F) - 0.5F) * 0.10F;
            float edgeNoise1 = (random(seed + (i + 1) * 17, 2.8F) - 0.5F) * 0.10F;
            for (int slice = 0; slice < radialSlices; slice++) {
                float v0 = slice / (float) radialSlices;
                float v1 = (slice + 1) / (float) radialSlices;
                float vCenter = (v0 + v1) * 0.5F;
                float clippedV0 = v0;
                float cellGrain = random(seed + i * 97 + slice * 29, 9.4F);

                if (erosion > 0.08F) {
                    float cellU = (u0 + u1) * 0.5F;
                    float edgeWave = MathHelper.sin(vCenter * 18.0F + seed * 0.006F) * 0.5F
                            + MathHelper.sin(vCenter * 41.0F - seed * 0.003F) * 0.25F;
                    float headBite = erosion * (0.010F + (1.0F - vCenter) * 0.032F
                            + Math.max(0.0F, edgeWave) * 0.012F);
                    float tailBite = erosion * (0.016F + (1.0F - vCenter) * 0.054F
                            + Math.max(0.0F, -edgeWave) * 0.022F);
                    if (cellU > to - headBite || cellU < from + tailBite) continue;
                }

                if (erosion > 0.03F) {
                    float dissolve = smooth((erosion - 0.03F) / 0.95F);
                    float boundaryWave = MathHelper.sin(i * 0.21F + seed * 0.004F) * 0.055F
                            + MathHelper.sin(i * 0.47F - seed * 0.002F) * 0.025F;
                    float innerBite = dissolve * (kind > 0.5F ? 0.42F : 0.56F)
                            + boundaryWave * dissolve;
                    float threshold = dissolve * (kind > 0.5F ? 0.70F : 0.58F)
                            * (0.70F + (1.0F - vCenter) * 0.50F);
                    if (v1 <= innerBite || cellGrain < threshold) continue;
                    clippedV0 = Math.max(v0, MathHelper.clamp(innerBite, 0.0F, v1));
                }

                float radius00 = lerp(clippedV0, inner, outer)
                        + lerp(clippedV0, -edgeNoise0 * 0.22F, edgeNoise0);
                float radius01 = lerp(v1, inner, outer)
                        + lerp(v1, -edgeNoise0 * 0.22F, edgeNoise0);
                float radius10 = lerp(clippedV0, inner, outer)
                        + lerp(clippedV0, -edgeNoise1 * 0.22F, edgeNoise1);
                float radius11 = lerp(v1, inner, outer)
                        + lerp(v1, -edgeNoise1 * 0.22F, edgeNoise1);
                quad(b, arc(u0, radius01, y), u0, v1,
                        arc(u1, radius11, y), u1, v1,
                        arc(u1, radius10, y), u1, clippedV0,
                        arc(u0, radius00, y), u0, clippedV0,
                        r, g, blue, alpha, kind);
            }
        }
    }

    private static void brightArcBand(BufferBuilder b, float age, float from, float to,
                                      float inner, float outer, float y, float alpha, int seed) {
        int start = Math.max(0, MathHelper.floor(from * SEGMENTS));
        int end = Math.min(SEGMENTS, MathHelper.ceil(to * SEGMENTS));
        for (int i = start; i < end; i++) {
            float u0 = Math.max(from, i / (float) SEGMENTS);
            float u1 = Math.min(to, (i + 1) / (float) SEGMENTS);
            float localAge0 = age - slashBirth(u0);
            float localAge1 = age - slashBirth(u1);
            float visible0 = brightVisibility(localAge0);
            float visible1 = brightVisibility(localAge1);
            if (Math.max(visible0, visible1) <= 0.002F) continue;

            float boundary0 = brightBoundary(age, u0, localAge0, seed);
            float boundary1 = brightBoundary(age, u1, localAge1, seed);
            float noise0 = brightOuterNoise(age, u0, seed);
            float noise1 = brightOuterNoise(age, u1, seed);
            float inner0 = lerp(boundary0, inner, outer) - noise0 * 0.12F;
            float inner1 = lerp(boundary1, inner, outer) - noise1 * 0.12F;
            quad(b, arc(u0, outer + noise0, y), u0, 1.0F,
                    arc(u1, outer + noise1, y), u1, 1.0F,
                    arc(u1, inner1, y), u1, 0.0F,
                    arc(u0, inner0, y), u0, 0.0F,
                    1.0F, 0.76F, 0.20F, alpha * Math.min(visible0, visible1), 2.0F);
        }
    }

    private static void groundBand(BufferBuilder b, float head, float inner, float outer, float y,
                                   float r, float g, float blue, float alpha, float kind) {
        for (int i = 0, end = Math.min(SEGMENTS, MathHelper.ceil(head * SEGMENTS)); i < end; i++) {
            float u0 = i / (float) SEGMENTS, u1 = (i + 1) / (float) SEGMENTS;
            quad(b, groundArc(u0, outer).withY(y), u0, 1, groundArc(u1, outer).withY(y), u1, 1,
                    groundArc(u1, inner).withY(y), u1, 0, groundArc(u0, inner).withY(y), u0, 0,
                    r, g, blue, alpha, kind);
        }
    }

    private static void ring(BufferBuilder b, float y, float radius, float width,
                             float r, float g, float blue, float alpha, float kind) {
        for (int i = 0; i < 72; i++) {
            float a0 = i * TAU / 72.0F, a1 = (i + 1) * TAU / 72.0F;
            quad(b, polar(a0, radius + width, y), 0, 1, polar(a1, radius + width, y), 1, 1,
                    polar(a1, radius - width, y), 1, 0, polar(a0, radius - width, y), 0, 0,
                    r, g, blue, alpha, kind);
        }
    }

    private static void jet(BufferBuilder b, Point root, Point direction, float length, float width,
                            float r, float g, float blue, float alpha, float kind) {
        if (length <= 0.01F || width <= 0.001F || alpha <= 0.002F) return;
        Point dir = direction.normalize(), side = new Point(-dir.z, 0, dir.x).normalize();
        Point up = new Point(0, 1, 0);
        Point tip = root.add(dir.scale(length));
        float rootWidth = width * 0.22F;
        float tipWidth = width * 0.08F;
        quad(b, root.add(side.scale(-rootWidth)), 0, 0, tip.add(side.scale(-tipWidth)), 1, 0.20F,
                tip.add(side.scale(tipWidth)), 1, 0.80F, root.add(side.scale(rootWidth)), 0, 1,
                r, g, blue, alpha, kind);
        quad(b, root.add(up.scale(-rootWidth)), 0, 0, tip.add(up.scale(-tipWidth)), 1, 0.20F,
                tip.add(up.scale(tipWidth)), 1, 0.80F, root.add(up.scale(rootWidth)), 0, 1,
                r, g, blue, alpha * 0.86F, kind);
    }

    private static void strip(BufferBuilder b, Point a, Point end, float width,
                              float r, float g, float blue, float alpha, float kind) {
        Point dir = end.subtract(a).normalize(), side = new Point(-dir.z, 0, dir.x).scale(width);
        quad(b, a.add(side), 0, 0, end.add(side.scale(0.15F)), 1, 0,
                end.add(side.scale(-0.15F)), 1, 1, a.add(side.scale(-1)), 0, 1,
                r, g, blue, alpha, kind);
    }

    private static void cross(BufferBuilder b, Point c, float w, float h,
                              float r, float g, float blue, float alpha, float kind) {
        quad(b, c.add(-w, -h, 0), 0, 0, c.add(w, -h, 0), 1, 0,
                c.add(w, h, 0), 1, 1, c.add(-w, h, 0), 0, 1, r, g, blue, alpha, kind);
        quad(b, c.add(0, -h, -w), 0, 0, c.add(0, -h, w), 1, 0,
                c.add(0, h, w), 1, 1, c.add(0, h, -w), 0, 1, r, g, blue, alpha * 0.86F, kind);
    }

    private static void diamond(BufferBuilder b, Point c, float size, float rot,
                                float r, float g, float blue, float alpha, float kind) {
        float cx = MathHelper.cos(rot) * size, cz = MathHelper.sin(rot) * size;
        float sx = -MathHelper.sin(rot) * size * 0.45F, sz = MathHelper.cos(rot) * size * 0.45F;
        quad(b, c.add(-cx, 0, -cz), 0, 0.5F, c.add(sx, size * 0.52F, sz), 0.5F, 0,
                c.add(cx, 0, cz), 1, 0.5F, c.add(-sx, -size * 0.52F, -sz), 0.5F, 1,
                r, g, blue, alpha, kind);
    }

    private static void quad(BufferBuilder b, Point a, float au, float av, Point c, float cu, float cv,
                             Point d, float du, float dv, Point e, float eu, float ev,
                             float r, float g, float blue, float alpha, float kind) {
        vertex(b, a, au, av, r, g, blue, alpha, kind); vertex(b, c, cu, cv, r, g, blue, alpha, kind);
        vertex(b, d, du, dv, r, g, blue, alpha, kind); vertex(b, e, eu, ev, r, g, blue, alpha, kind);
    }

    private static void vertex(BufferBuilder b, Point p, float u, float v,
                               float r, float g, float blue, float alpha, float kind) {
        b.pos(p.x, p.y, p.z).tex(kind * 2.0F + MathHelper.clamp(u, 0, 1), MathHelper.clamp(v, 0, 1))
                .color(r, g, blue, MathHelper.clamp(alpha, 0, 1)).endVertex();
    }

    private static Point arc(float u, float radius, float y) {
        float angle = START + u * SPAN;
        float x = -MathHelper.sin(angle) * radius, z = MathHelper.cos(angle) * radius;
        return new Point(x, 1.12F + x * (float) Math.tan(6.0F * 0.017453292F) + y, z);
    }

    private static Point groundArc(float u, float radius) {
        float angle = START + u * SPAN;
        float torn = MathHelper.sin(u * 37.0F + 0.8F) * 0.075F + MathHelper.sin(u * 91.0F + 2.1F) * 0.034F;
        return new Point(-MathHelper.sin(angle) * (radius + torn), 0, MathHelper.cos(angle) * (radius + torn));
    }

    private static Point tangent(float u, float radius) {
        float before = Math.max(0.0F, u - 0.0025F);
        float after = Math.min(1.0F, u + 0.0025F);
        return arc(after, radius, 0.0F).subtract(arc(before, radius, 0.0F)).normalize();
    }

    private static Point radial(float u) {
        Point point = groundArc(u, 1.0F);
        return new Point(point.x, 0.0F, point.z).normalize();
    }

    private static Point polar(float angle, float radius, float y) {
        return new Point(MathHelper.cos(angle) * radius, y, MathHelper.sin(angle) * radius);
    }

    private static float slashProgress(float age) {
        float raw = MathHelper.clamp((age - 8.4F) / 5.4F, 0, 1);
        if (raw < 0.55F) { float f = raw / 0.55F; return 0.72F * (1.0F - (float) Math.pow(1.0F - f, 3)); }
        return 0.72F + 0.28F * smooth((raw - 0.55F) / 0.45F);
    }

    private static float slashBirth(float u) {
        float low = 8.4F, high = 13.8F;
        for (int i = 0; i < 16; i++) {
            float middle = (low + high) * 0.5F;
            if (slashProgress(middle) < u) low = middle;
            else high = middle;
        }
        return (low + high) * 0.5F;
    }

    private static float brightVisibility(float localAge) {
        if (localAge <= 0.0F) return 0.0F;
        return fastOut(localAge / 0.22F)
                * (1.0F - smooth((localAge - 7.0F) / 5.5F));
    }

    private static float brightBoundary(float age, float u, float localAge, int seed) {
        float erosion = smooth((localAge - 1.30F) / 5.20F);
        float finish = smooth((age - 13.8F) / 4.0F);
        float reach = lerp(finish, 0.60F, 0.90F);
        float wave = MathHelper.sin(u * 31.0F + seed * 0.006F + age * 0.31F) * 0.064F
                + MathHelper.sin(u * 79.0F - seed * 0.003F - age * 0.23F) * 0.030F;
        return MathHelper.clamp(erosion * reach + wave * erosion * (1.0F - erosion * 0.35F),
                0.0F, 0.92F);
    }

    private static float brightOuterNoise(float age, float u, int seed) {
        return MathHelper.sin(u * 37.0F + seed * 0.004F + age * 0.22F) * 0.028F
                + MathHelper.sin(u * 103.0F - seed * 0.002F - age * 0.16F) * 0.012F;
    }

    private static float mainAlpha(float age) { return age < 8.4F ? 0 : age <= 29.5F ? 1 : 1 - smooth((age - 29.5F) / 4.5F); }
    private static float scarAlpha(float age) { return age < 8.7F ? 0 : age < 13.5F ? smooth((age - 8.7F) / 4.8F) : age <= 44 ? 1 : 1 - smooth((age - 44) / 8); }
    private static float window(float age, float start, float end, float in, float out) { return age <= start || age >= end ? 0 : smooth((age - start) / in) * (1 - smooth((age - end + out) / out)); }
    private static float smooth(float v) { v = MathHelper.clamp(v, 0, 1); return v * v * v * (v * (v * 6 - 15) + 10); }
    private static float fastOut(float v) { float i = 1 - MathHelper.clamp(v, 0, 1); return 1 - i * i * i * i; }
    private static float lerp(float t, float a, float c) { return a + (c - a) * MathHelper.clamp(t, 0, 1); }
    private static float random(int i, float salt) { float v = MathHelper.sin(i * 12.9898F + salt * 78.233F) * 43758.547F; return v - (float) Math.floor(v); }

    private static final class Point {
        final float x, y, z;
        Point(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
        Point add(float dx, float dy, float dz) { return new Point(x + dx, y + dy, z + dz); }
        Point add(Point p) { return add(p.x, p.y, p.z); }
        Point subtract(Point p) { return new Point(x - p.x, y - p.y, z - p.z); }
        Point scale(float value) { return new Point(x * value, y * value, z * value); }
        Point withY(float value) { return new Point(x, value, z); }
        Point normalize() { float l = MathHelper.sqrt(x * x + y * y + z * z); return l < 1.0E-5F ? new Point(0, 0, 0) : scale(1.0F / l); }
    }
}

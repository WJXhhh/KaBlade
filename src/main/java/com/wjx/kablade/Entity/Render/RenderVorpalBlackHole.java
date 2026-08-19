package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.EntityVorpalBlackHole;
import com.wjx.kablade.client.renderer.VorpalBlackHoleGeometry;
import com.wjx.kablade.client.renderer.VorpalBlackHoleTimeline;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;

/**
 * 完整贯穿 56-tick 生命周期、全程高能连续演出的时空黑洞渲染器。
 */
@SideOnly(Side.CLIENT)
public class RenderVorpalBlackHole extends Render<EntityVorpalBlackHole> {

    private static final float SCALE = 0.78F;
    private static final float VORTEX_SPIN = 1.85F;
    private static final float OWNER_ANCHOR_FADE_START = 6.0F;
    private static final float OWNER_ANCHOR_FADE_END = 10.0F;
    private static final float CHEST_Y = -1.12F;
    private static final float CHEST_Z = -0.14F;

    private static final float[][] ARC_SPECS = {
            {1.38F, .052F, 1.18F, -.52F, .18F, .22F},
            {1.52F, .044F, 1.42F, 2.32F, -.22F, -.18F},
            {1.70F, .030F, .92F, -.18F, .36F, -.28F},
            {1.76F, .034F, 1.06F, 2.65F, -.34F, .26F},
            {1.31F, .026F, .74F, .96F, .42F, .12F},
            {1.88F, .025F, .86F, 3.74F, -.42F, -.10F}
    };
    private static final int[] ARC_COLORS = {0xF24B6B, 0xC9244C, 0xFF98AA, 0xFF5B7D, 0x8E285B, 0xE62F62};
    private static final float[][] CUT_SPECS = {
            {-3.2F, .7F, -.9F, 3.9F, 4.8F, .8F, .025F, 34F},
            {-4F, 2.2F, -.3F, 4.7F, 2.5F, .4F, .022F, 34.6F},
            {-2.5F, 4.8F, .8F, 2.3F, .4F, -.9F, .024F, 35.2F},
            {-1F, .3F, -1.2F, 5.6F, 3.8F, .4F, .018F, 36F},
            {-4.8F, 3.6F, .6F, 1.5F, 1.4F, -.8F, .020F, 37.2F},
            {-3.4F, 1F, -.6F, 4F, 1.9F, .2F, .015F, 38F},
            {-.4F, 5.2F, 1F, 1.2F, .2F, -.8F, .018F, 39.1F},
            {-5.2F, 2.8F, .2F, 3.1F, 3.5F, .4F, .014F, 40F},
            {-2.1F, .5F, -.8F, 4.8F, 4.5F, .7F, .014F, 41F},
            {-4.6F, 1.6F, -.2F, 2.9F, .9F, -.7F, .013F, 42.2F}
    };

    private final float[] ribbonPoints = new float[128 * 3];

    public RenderVorpalBlackHole(RenderManager renderManagerIn) {
        super(renderManagerIn);
        this.shadowSize = 0.0F;
        this.shadowOpaque = 0.0F;
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityVorpalBlackHole entity) {
        return null;
    }

    @Override
    public void doRender(EntityVorpalBlackHole entity, double x, double y, double z, float entityYaw, float partialTicks) {
        float age = entity.ticksExisted + partialTicks;
        float frame = VorpalBlackHoleTimeline.frame(age);
        if (frame > VorpalBlackHoleTimeline.LAST_FRAME) {
            return;
        }

        long seed = entity.getUniqueID().getMostSignificantBits() ^ entity.getUniqueID().getLeastSignificantBits();
        float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;

        // 计算摄像机在实体局部坐标系下的位置
        double camX = this.renderManager.viewerPosX;
        double camY = this.renderManager.viewerPosY;
        double camZ = this.renderManager.viewerPosZ;
        double entityPosX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double entityPosY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double entityPosZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        double deltaX = camX - entityPosX;
        double deltaY = camY - entityPosY;
        double deltaZ = camZ - entityPosZ;

        float yawRad = (float) Math.toRadians(yaw);
        float cos = MathHelper.cos(yawRad);
        float sin = MathHelper.sin(yawRad);
        float cameraLocalX = (float) (deltaX * cos + deltaZ * sin) / SCALE;
        float cameraLocalY = (float) deltaY / SCALE;
        float cameraLocalZ = (float) (-deltaX * sin + deltaZ * cos) / SCALE;

        // 计算玩家手持刀剑锚点插值
        float bladeAnchorX = 0.90F;
        float bladeAnchorY = -1.48F;
        float bladeAnchorZ = -2.15F;
        EntityLivingBase owner = entity.getOwnerEntity();
        if (owner != null) {
            double ownerX = owner.lastTickPosX + (owner.posX - owner.lastTickPosX) * partialTicks;
            double ownerY = owner.lastTickPosY + (owner.posY - owner.lastTickPosY) * partialTicks + owner.getEyeHeight() * 0.72;
            double ownerZ = owner.lastTickPosZ + (owner.posZ - owner.lastTickPosZ) * partialTicks;
            double fromHoleX = ownerX - entityPosX;
            double fromHoleY = ownerY - entityPosY;
            double fromHoleZ = ownerZ - entityPosZ;
            float dist = (float) Math.sqrt(fromHoleX * fromHoleX + fromHoleY * fromHoleY + fromHoleZ * fromHoleZ);
            float anchorWeight = 1.0F - VorpalBlackHoleTimeline.smooth(OWNER_ANCHOR_FADE_START, OWNER_ANCHOR_FADE_END, dist);
            float ownerAnchorX = (float) (fromHoleX * cos + fromHoleZ * sin) / SCALE;
            float ownerAnchorY = (float) fromHoleY / SCALE;
            float ownerAnchorZ = (float) (-fromHoleX * sin + fromHoleZ * cos) / SCALE;
            bladeAnchorX = VorpalBlackHoleTimeline.lerp(anchorWeight, bladeAnchorX, ownerAnchorX);
            bladeAnchorY = VorpalBlackHoleTimeline.lerp(anchorWeight, bladeAnchorY, ownerAnchorY);
            bladeAnchorZ = VorpalBlackHoleTimeline.lerp(anchorWeight, bladeAnchorZ, ownerAnchorZ);
        }

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        float lastBrightnessX = OpenGlHelper.lastBrightnessX;
        float lastBrightnessY = OpenGlHelper.lastBrightnessY;
        int previousProgram = VorpalBlackHoleShader.bind(frame, 1.0F, 0);

        try {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.disableCull();

            // ================= PASS 1: CORE (纯黑事件视界，写入深度) =================
            VorpalBlackHoleShader.bind(frame, 1.0F, 0);
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            renderFacingVoidCore(tessellator, buffer, frame);

            // ================= PASS 2: DARK (暗黑引力体积与黑雾，Alpha 混合) =================
            VorpalBlackHoleShader.bind(frame, 1.0F, 1);
            GlStateManager.depthMask(false);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            renderFacingVoidDark(tessellator, buffer, frame);
            renderWorldVolumeDark(tessellator, buffer, frame, seed, yaw);
            renderFacingRuptureDark(tessellator, buffer, frame, seed);
            renderWorldSlashesDark(tessellator, buffer, frame, seed, yaw, cameraLocalX, cameraLocalY, cameraLocalZ, bladeAnchorX, bladeAnchorY, bladeAnchorZ);

            // ================= PASS 3: GLOW (能量高光与斩击光刃，Additive 加色混合) =================
            VorpalBlackHoleShader.bind(frame, 1.0F, 2);
            GlStateManager.depthMask(false);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);

            renderFacingVoidGlow(tessellator, buffer, frame, seed);
            renderWorldVolumeGlow(tessellator, buffer, frame, seed, yaw);
            renderFacingRuptureGlow(tessellator, buffer, frame, seed);
            renderChestImpactGlow(tessellator, buffer, frame, seed, yaw);
            renderWorldSlashesGlow(tessellator, buffer, frame, seed, yaw, cameraLocalX, cameraLocalY, cameraLocalZ, bladeAnchorX, bladeAnchorY, bladeAnchorZ);

            GlStateManager.popMatrix();
        } finally {
            VorpalBlackHoleShader.restore(previousProgram);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastBrightnessX, lastBrightnessY);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableTexture2D();
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.disableBlend();
            GlStateManager.enableCull();
            GlStateManager.enableLighting();
        }

        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    private void applyFacingTransform(float localTime, float birth, float rupture, float collapse) {
        GlStateManager.rotate(180.0F - this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate((float) Math.toDegrees(localTime * -0.0035F * VORTEX_SPIN), 0.0F, 0.0F, 1.0F);
        float collapseScale = 1.0F - collapse * 0.85F;
        GlStateManager.scale(
                SCALE * birth * (1.0F + rupture * 0.15F) * collapseScale,
                SCALE * birth / (1.0F + rupture * 0.15F) * collapseScale,
                SCALE * birth * collapseScale
        );
    }

    private void renderFacingVoidCore(Tessellator tessellator, BufferBuilder buffer, float frame) {
        float coreAlpha = VorpalBlackHoleTimeline.vortexCore(frame);
        if (coreAlpha <= 0.002F) return;

        float birth = Math.max(0.001F, VorpalBlackHoleTimeline.lerp(
                VorpalBlackHoleTimeline.easeOutBack(VorpalBlackHoleTimeline.smooth(0.0F, 19.6F, frame)),
                0.42F, 1.0F));
        float rupture = VorpalBlackHoleTimeline.gaussian(frame, 28.7F, 0.72F)
                + VorpalBlackHoleTimeline.gaussian(frame, 30.5F, 1.08F) * 0.82F;
        float collapse = VorpalBlackHoleTimeline.smooth(78.0F, 84.0F, frame);
        float compression = VorpalBlackHoleTimeline.smooth(28.6F, 32.8F, frame);
        float localTime = frame - 20.0F;

        GlStateManager.pushMatrix();
        applyFacingTransform(localTime, birth, rupture, collapse);

        float cx = 0.76F * (1.0F + compression * 0.48F);
        float cy = 0.76F * 0.82F * (1.0F - compression * 0.25F);
        float cz = 0.76F * 0.48F * (1.0F - compression * 0.05F + rupture * 0.035F);

        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        VorpalBlackHoleGeometry.ellipsoid(buffer, 0, 0, 0, cx, cy, cz, 18, 32, 0x010003, 1.0F);
        VorpalBlackHoleGeometry.ellipsoid(buffer, 0, 0, -0.015F, cx * 0.82F, cy * 0.84F, cz * 0.88F, 14, 28, 0x000001, 1.0F);
        tessellator.draw();

        GlStateManager.popMatrix();
    }

    private void renderFacingVoidDark(Tessellator tessellator, BufferBuilder buffer, float frame) {
        float coreAlpha = VorpalBlackHoleTimeline.vortexCore(frame);
        if (coreAlpha <= 0.002F) return;

        float birth = Math.max(0.001F, VorpalBlackHoleTimeline.lerp(
                VorpalBlackHoleTimeline.easeOutBack(VorpalBlackHoleTimeline.smooth(0.0F, 19.6F, frame)),
                0.42F, 1.0F));
        float rupture = VorpalBlackHoleTimeline.gaussian(frame, 28.7F, 0.72F)
                + VorpalBlackHoleTimeline.gaussian(frame, 30.5F, 1.08F) * 0.82F;
        float collapse = VorpalBlackHoleTimeline.smooth(78.0F, 84.0F, frame);
        float localTime = frame - 20.0F;

        GlStateManager.pushMatrix();
        applyFacingTransform(localTime, birth, rupture, collapse);

        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        VorpalBlackHoleGeometry.funnel(buffer, 0.56F, 1.78F, 0.72F,
                56, 14, 1.08F, 0x08040C, 0x5E102E, coreAlpha * 0.58F,
                localTime * -0.012F * VORTEX_SPIN);
        tessellator.draw();

        GlStateManager.popMatrix();
    }

    private void renderFacingVoidGlow(Tessellator tessellator, BufferBuilder buffer, float frame, long seed) {
        float coreAlpha = VorpalBlackHoleTimeline.vortexCore(frame);
        float seedAlpha = VorpalBlackHoleTimeline.vortexSeed(frame);
        float spiralAlpha = VorpalBlackHoleTimeline.vortexSpiral(frame);
        float arcAlpha = VorpalBlackHoleTimeline.vortexArcs(frame);
        float needleAlpha = VorpalBlackHoleTimeline.vortexNeedles(frame);
        if (Math.max(Math.max(coreAlpha, spiralAlpha), Math.max(arcAlpha, needleAlpha)) < 0.002F) return;

        float birth = Math.max(0.001F, VorpalBlackHoleTimeline.lerp(
                VorpalBlackHoleTimeline.easeOutBack(VorpalBlackHoleTimeline.smooth(0.0F, 19.6F, frame)),
                0.42F, 1.0F));
        float rupture = VorpalBlackHoleTimeline.gaussian(frame, 28.7F, 0.72F)
                + VorpalBlackHoleTimeline.gaussian(frame, 30.5F, 1.08F) * 0.82F;
        float collapse = VorpalBlackHoleTimeline.smooth(78.0F, 84.0F, frame);
        float localTime = frame - 20.0F;

        GlStateManager.pushMatrix();
        applyFacingTransform(localTime, birth, rupture, collapse);

        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);

        if (coreAlpha > 0.002F) {
            VorpalBlackHoleGeometry.funnel(buffer, 0.60F, 1.67F, 0.66F,
                    56, 13, -1.24F, 0x351022, 0xF02A4C, coreAlpha * 0.38F,
                    localTime * 0.018F * VORTEX_SPIN);

            if (seedAlpha > 0.002F) {
                float seedPulse = 0.92F + MathHelper.sin(frame * 0.42F) * 0.12F;
                VorpalBlackHoleGeometry.glowDisc(buffer, 1.04F * seedPulse, -0.34F,
                        36, 0x7D1737, 0x22030F, seedAlpha * 0.22F);
                VorpalBlackHoleGeometry.torus(buffer, 0, 0, 0,
                        0.91F * seedPulse, 0.034F, frame * -0.035F, VorpalBlackHoleTimeline.TWO_PI,
                        44, 5, 0.12F, -0.16F, 0.0F,
                        0xB72A4C, seedAlpha * 0.62F);
            }

            for (int i = 0; i < 3; i++) {
                float shell = 0.98F + i * 0.18F + rupture * (0.06F + i * 0.03F);
                int color = i == 0 ? 0x8E1940 : i == 1 ? 0xE12C50 : 0x5D1B55;
                VorpalBlackHoleGeometry.ellipsoid(buffer, 0, 0, 0,
                        shell * (1.20F + i * 0.06F), shell * (0.98F + i * 0.035F),
                        shell * (0.62F + i * 0.08F), 10, 24, color,
                        coreAlpha * (0.065F + i * 0.018F));
            }
            for (int i = 0; i < 5; i++) {
                float spin = localTime * (i % 2 == 0 ? 0.021F : -0.025F) * VORTEX_SPIN;
                VorpalBlackHoleGeometry.torus(buffer, 0, 0, 0,
                        0.76F + i * 0.065F, 0.026F + (i % 2) * 0.012F,
                        spin, VorpalBlackHoleTimeline.TWO_PI, 44, 5,
                        (i - 2) * 0.15F, (i % 3 - 1) * 0.20F, i * 0.61F,
                        i % 2 == 0 ? 0x8D1536 : 0xFF3657, coreAlpha * 0.62F);
            }

            // 6 次能量脉冲共振光环（Frame 36 ~ 72）
            for (int p = 0; p < 6; p++) {
                float pulse = VorpalBlackHoleTimeline.pulseWave(frame, p);
                if (pulse > 0.005F) {
                    float pRadius = 0.85F + (1.0F - pulse) * 1.65F;
                    VorpalBlackHoleGeometry.torus(buffer, 0, 0, 0,
                            pRadius, 0.032F + p * 0.006F, localTime * 0.035F, VorpalBlackHoleTimeline.TWO_PI,
                            40, 5, (p - 2) * 0.12F, (p % 2 == 0 ? 0.2F : -0.2F), p * 0.8F,
                            p % 2 == 0 ? 0xFF3E6C : 0xFFFFFF, pulse * 0.82F);
                }
            }
        }

        if (spiralAlpha > 0.002F) {
            for (int s = 0; s < 10; s++) {
                float side = s < 6 ? -1.0F : 1.0F;
                float phase = noise(seed, s, 11.7F) * VorpalBlackHoleTimeline.TWO_PI;
                float turns = 1.18F + (s % 5) * 0.13F;
                float px = 0, py = 0, pz = 0;
                for (int j = 0; j <= 32; j++) {
                    float u = j / 32.0F;
                    float z = side * VorpalBlackHoleTimeline.lerp(VorpalBlackHoleTimeline.smooth(0, 1, u), 0.96F, 0.055F);
                    float radius = VorpalBlackHoleTimeline.lerp(VorpalBlackHoleTimeline.smooth(0, 1, u),
                            1.74F - (s % 3) * 0.05F, 0.57F + (s % 2) * 0.045F);
                    float angle = phase + u * VorpalBlackHoleTimeline.TWO_PI * turns + side * u * u * 0.76F
                            + localTime * side * (0.014F + (s % 4) * 0.0035F) * VORTEX_SPIN;
                    float flutter = MathHelper.sin(u * 17.0F + phase) * 0.025F * (1.0F - u * 0.58F);
                    float x = MathHelper.cos(angle) * (radius + flutter);
                    float y = MathHelper.sin(angle) * (radius + flutter) * 0.88F;
                    z += MathHelper.sin(u * 9.0F + s) * 0.024F;
                    if (j > 0) VorpalBlackHoleGeometry.tube(buffer, px, py, pz, x, y, z,
                            0.018F + (s % 4) * 0.0065F, 5,
                            s % 4 == 0 ? 0xE52B50 : s % 2 == 0 ? 0x47142B : 0x7E1733,
                            spiralAlpha * (0.48F + (s % 3) * 0.05F));
                    px = x; py = y; pz = z;
                }
            }
            for (int i = 0; i < 7; i++) {
                VorpalBlackHoleGeometry.torus(buffer, 0, 0, 0,
                        0.71F + i * 0.105F, 0.018F + (i % 3) * 0.007F,
                        localTime * (i % 2 == 0 ? 0.015F : -0.018F) * VORTEX_SPIN,
                        VorpalBlackHoleTimeline.TWO_PI, 48, 5,
                        (i - 3) * 0.105F, (i % 3 - 1) * 0.18F, i * 0.43F,
                        i % 3 == 0 ? 0xFF4161 : i % 2 == 0 ? 0x55142E : 0x8E1C3C,
                        spiralAlpha * 0.52F);
            }
        }

        if (arcAlpha > 0.002F) {
            for (int i = 0; i < ARC_SPECS.length; i++) {
                float[] s = ARC_SPECS[i];
                VorpalBlackHoleGeometry.torus(buffer, 0, 0, 0, s[0], s[1], s[3], s[2], 32, 5,
                        s[4] + localTime * (i % 2 == 0 ? .006F : -.008F) * VORTEX_SPIN, s[5],
                        localTime * (i % 2 == 0 ? .017F : -.020F) * VORTEX_SPIN,
                        ARC_COLORS[i], arcAlpha * (0.44F + 0.18F * MathHelper.sin(frame * .43F + i) * MathHelper.sin(frame * .43F + i)));
            }
        }

        if (needleAlpha > 0.002F) {
            for (int i = 0; i < 32; i++) {
                float angle = noise(seed, i, 31.2F) * VorpalBlackHoleTimeline.TWO_PI + localTime * -0.008F;
                float delay = noise(seed, i, 33.8F) * 1.2F;
                float impulse = VorpalBlackHoleTimeline.smooth(27.6F + delay * .12F, 30.0F + delay * .25F, frame);
                float length = (.56F + noise(seed, i, 36.1F) * 1.16F) * (0.76F + MathHelper.sin(frame * .58F + i) * .16F) * (1 + impulse * .66F);
                float inner = .67F + noise(seed, i, 39.7F) * .30F + impulse * .18F;
                float depth = (noise(seed, i, 41.4F) - .5F) * .68F;
                VorpalBlackHoleGeometry.wedge(buffer, angle, inner, length, .12F + noise(seed, i, 43.2F) * .19F,
                        depth, .018F, i % 5 == 0 ? 0x120710 : i % 4 == 0 ? 0x7B1232 : 0x4B1028,
                        needleAlpha * (i % 5 == 0 ? .72F : .52F), noise(seed, i, 45.9F) - .5F);
            }
        }

        tessellator.draw();
        GlStateManager.popMatrix();
    }

    private void renderWorldVolumeDark(Tessellator tessellator, BufferBuilder buffer,
                                       float frame, long seed, float yaw) {
        float smokeAlpha = VorpalBlackHoleTimeline.vortexSmoke(frame);
        if (smokeAlpha <= 0.002F) return;

        float localTime = frame - 20.0F;
        float birth = Math.max(.001F, VorpalBlackHoleTimeline.lerp(VorpalBlackHoleTimeline.easeOutBack(
                VorpalBlackHoleTimeline.smooth(0.0F, 19.6F, frame)), .25F, 1F));
        float blast = VorpalBlackHoleTimeline.smooth(28.7F, 32.7F, frame);

        GlStateManager.pushMatrix();
        GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(SCALE * birth, SCALE * birth, SCALE * birth);

        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < 48; i++) {
            float depth = (noise(seed, i, 51.2F) - .5F) * 1.86F;
            float depth01 = Math.abs(depth) / .93F;
            float angle = noise(seed, i, 53.8F) * VorpalBlackHoleTimeline.TWO_PI - localTime * (.015F + (noise(seed, i, 55.4F) - .5F) * .034F);
            float radius = .84F + depth01 * .66F + noise(seed, i, 57.9F) * .38F;
            float base = .18F + noise(seed, i, 60.1F) * .34F;
            float breathe = .88F + MathHelper.sin(localTime * .17F + noise(seed, i, 62.7F) * VorpalBlackHoleTimeline.TWO_PI) * .10F;
            float x = MathHelper.cos(angle) * radius * (1 + blast * .18F);
            float y = MathHelper.sin(angle) * radius * .88F * (1 + blast * .18F);
            float z = depth + MathHelper.sin(localTime * .11F + i) * .10F;
            int color = i % 7 == 0 ? 0x703046 : i % 3 == 0 ? 0x403047 : 0x261D31;
            VorpalBlackHoleGeometry.icosahedron(buffer, x, y, z,
                    base * breathe * (.82F + noise(seed, i, 65.3F) * .62F),
                    base * breathe * (.70F + noise(seed, i, 67.1F) * .52F),
                    base * breathe * (.72F + noise(seed, i, 69.4F) * .68F),
                    localTime * (noise(seed, i, 71.2F) - .5F) * .08F + i,
                    localTime * (noise(seed, i, 73.5F) - .5F) * .09F,
                    localTime * (noise(seed, i, 75.8F) - .5F) * .075F,
                    color, smokeAlpha * (.14F + (i % 5) * .012F));
        }
        for (int i = 0; i < 12; i++) {
            float side = i % 2 == 0 ? -1 : 1;
            float phase = noise(seed, i, 78.2F) * VorpalBlackHoleTimeline.TWO_PI + localTime * side * (.011F + (i % 4) * .0025F);
            float px = 0, py = 0, pz = 0;
            for (int j = 0; j <= 18; j++) {
                float u = j / 18F;
                float z = side * VorpalBlackHoleTimeline.lerp(VorpalBlackHoleTimeline.smooth(0, 1, u), .92F, .10F);
                float radius = VorpalBlackHoleTimeline.lerp(VorpalBlackHoleTimeline.smooth(0, 1, u), 1.72F, .66F);
                float angle = phase + u * VorpalBlackHoleTimeline.TWO_PI * (1.04F + (i % 3) * .14F) + side * u * .52F;
                float x = MathHelper.cos(angle) * radius;
                float y = MathHelper.sin(angle) * radius * .88F;
                if (j > 0) VorpalBlackHoleGeometry.tube(buffer, px, py, pz, x, y, z, .035F + (i % 4) * .009F, 5,
                        i % 3 == 0 ? 0x5E1832 : 0x241426, smokeAlpha * .14F);
                px = x; py = y; pz = z;
            }
        }
        tessellator.draw();
        GlStateManager.popMatrix();
    }

    private void renderWorldVolumeGlow(Tessellator tessellator, BufferBuilder buffer,
                                       float frame, long seed, float yaw) {
        float emberAlpha = VorpalBlackHoleTimeline.vortexEmbers(frame);
        if (emberAlpha <= 0.002F) return;

        float localTime = frame - 20.0F;
        float birth = Math.max(.001F, VorpalBlackHoleTimeline.lerp(VorpalBlackHoleTimeline.easeOutBack(
                VorpalBlackHoleTimeline.smooth(0.0F, 19.6F, frame)), .25F, 1F));
        float blast = VorpalBlackHoleTimeline.smooth(28.7F, 32.7F, frame);

        GlStateManager.pushMatrix();
        GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(SCALE * birth, SCALE * birth, SCALE * birth);

        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < 46; i++) {
            float a = noise(seed, i, 81.4F) * VorpalBlackHoleTimeline.TWO_PI + localTime * (.011F + noise(seed, i, 83.7F) * .032F);
            float radius = .42F + noise(seed, i, 86.1F) * 1.72F;
            float tilt = (noise(seed, i, 88.4F) - .5F) * 1.7F;
            float out = blast * (.18F + noise(seed, i, 91.0F) * .76F) * 2.45F;
            float x = MathHelper.cos(a) * (radius + out);
            float y = MathHelper.sin(a) * (radius + out) * MathHelper.cos(tilt);
            float z = MathHelper.sin(a) * (radius + out) * MathHelper.sin(tilt) + (noise(seed, i, 93.6F) - .5F) * .62F;
            VorpalBlackHoleGeometry.tetrahedron(buffer, x, y, z, .045F * (.68F + MathHelper.sin(frame * .5F + i) * .20F),
                    frame * .08F + i, frame * .12F + i * .3F, frame * .06F, i % 6 == 0 ? 0xFFA086 : 0xFF284F, emberAlpha * .78F);
        }
        tessellator.draw();
        GlStateManager.popMatrix();
    }

    private void renderFacingRuptureDark(Tessellator tessellator, BufferBuilder buffer, float frame, long seed) {
        float black = VorpalBlackHoleTimeline.blackSpikes(frame);
        float speed = VorpalBlackHoleTimeline.speedStreaks(frame);
        if (Math.max(black, speed) < .002F) return;

        GlStateManager.pushMatrix();
        GlStateManager.rotate(180.0F - this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(SCALE, SCALE, SCALE);

        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        if (black > .002F) {
            for (int i = 0; i < 22; i++) {
                float delay = noise(seed, i, 101.2F) * .8F, age = frame - 28.55F - delay;
                if (age < -.02F) continue;
                float grow = VorpalBlackHoleTimeline.easeOutBack(VorpalBlackHoleTimeline.smooth(0, 1.45F, age));
                float fade = 1 - VorpalBlackHoleTimeline.smooth(33F + noise(seed, i, 103.9F), 36.5F + noise(seed, i, 103.9F) * .38F, frame);
                VorpalBlackHoleGeometry.wedge(buffer, i / 22F * VorpalBlackHoleTimeline.TWO_PI + (noise(seed, i, 106.4F) - .5F) * .36F,
                        .06F * grow, (2.4F + noise(seed, i, 109.1F) * 3.5F) * grow,
                        (.48F + noise(seed, i, 111.8F) * .82F) * (.75F + noise(seed, i, 114.3F) * .5F),
                        (noise(seed, i, 116.7F) - .5F) * .82F + .12F, .035F, i % 5 == 0 ? 0x2B0A1A : 0x08030A,
                        black * fade * .86F, noise(seed, i, 119.2F) - .5F);
            }
        }
        if (speed > .002F) {
            for (int i = 0; i < 46; i++) {
                float delay = noise(seed, i, 143.2F) * 1.8F, age = frame - 33.1F - delay;
                if (age <= 0) continue;
                float grow = VorpalBlackHoleTimeline.smooth(0, 1.3F, age), fade = 1 - VorpalBlackHoleTimeline.smooth(2.2F, 5.2F, age);
                float radius = .30F + age * .16F;
                int color = i % 6 == 0 ? 0x2A4D7C : i % 3 == 0 ? 0x13223C : 0x080D18;
                VorpalBlackHoleGeometry.wedge(buffer, i / 46F * VorpalBlackHoleTimeline.TWO_PI + (noise(seed, i, 145.9F) - .5F) * .24F,
                        radius, (2F + noise(seed, i, 148.5F) * 4.2F) * grow * fade, .13F + noise(seed, i, 151.2F) * .24F,
                        (noise(seed, i, 153.8F) - .5F) * 1.3F, .018F, color, speed * fade * .70F, noise(seed, i, 156.4F) - .5F);
            }
        }
        tessellator.draw();
        GlStateManager.popMatrix();
    }

    private void renderFacingRuptureGlow(Tessellator tessellator, BufferBuilder buffer, float frame, long seed) {
        float red = VorpalBlackHoleTimeline.redSpikes(frame);
        float pre = VorpalBlackHoleTimeline.preFlash(frame);
        if (Math.max(red, pre) < .002F) return;

        GlStateManager.pushMatrix();
        GlStateManager.rotate(180.0F - this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(SCALE, SCALE, SCALE);

        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        if (pre > .002F) {
            float pulse = VorpalBlackHoleTimeline.gaussian(frame, 27.55F, .28F) * .70F
                    + VorpalBlackHoleTimeline.gaussian(frame, 28.62F, .25F)
                    + VorpalBlackHoleTimeline.gaussian(frame, 29.25F, .38F) * .72F;
            VorpalBlackHoleGeometry.glowDisc(buffer, (.54F + pulse * .62F), -.42F, 36, 0xFFF1F7, 0xFF3D6E, pre * .62F);
        }
        if (red > .002F) {
            for (int i = 0; i < 58; i++) {
                float delay = noise(seed, i, 122.1F) * 1.25F, age = frame - 28.75F - delay;
                if (age < -.02F) continue;
                float grow = VorpalBlackHoleTimeline.easeOutBack(VorpalBlackHoleTimeline.smooth(0, 1.8F, age));
                float off = noise(seed, i, 124.7F) * .8F;
                float fade = 1 - VorpalBlackHoleTimeline.smooth(32.5F + off, 36.0F + off * .35F, frame);
                float overshoot = 1 + VorpalBlackHoleTimeline.gaussian(age, 1.85F, .72F) * .18F;
                int color = i % 7 == 0 ? 0xFF516D : i % 3 == 0 ? 0xB81232 : 0xE22240;
                VorpalBlackHoleGeometry.wedge(buffer, i / 58F * VorpalBlackHoleTimeline.TWO_PI + (noise(seed, i, 127.3F) - .5F) * .18F,
                        .10F * grow, (2F + (float) Math.pow(noise(seed, i, 129.8F), .52F) * 3.8F) * grow * overshoot,
                        (.22F + noise(seed, i, 132.4F) * .56F) * (.72F + noise(seed, i, 135.1F) * .68F),
                        (noise(seed, i, 137.8F) - .5F) * .72F, .024F, color, red * fade * .78F, noise(seed, i, 140.4F) - .5F);
            }
        }
        tessellator.draw();
        GlStateManager.popMatrix();
    }

    private void renderChestImpactGlow(Tessellator tessellator, BufferBuilder buffer,
                                       float frame, long seed, float yaw) {
        float core = VorpalBlackHoleTimeline.impactCore(frame), wave = VorpalBlackHoleTimeline.shockwave(frame);
        if (Math.max(core, wave) < .002F) return;

        GlStateManager.pushMatrix();
        GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(0.0F, CHEST_Y * SCALE, CHEST_Z * SCALE);
        GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(180.0F - this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(SCALE, SCALE, SCALE);

        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        if (core > .002F) {
            float pulse = VorpalBlackHoleTimeline.gaussian(frame, 29.4F, .55F) * .9F
                    + VorpalBlackHoleTimeline.gaussian(frame, 31.15F, .95F)
                    + VorpalBlackHoleTimeline.gaussian(frame, 33F, 1.35F) * .42F;
            VorpalBlackHoleGeometry.glowDisc(buffer, 1.05F + pulse * 1.5F, .015F, 40, 0xFFFFFF, 0xFF285F, core * .26F);
            VorpalBlackHoleGeometry.glowDisc(buffer, .54F + pulse * .74F, .025F, 36, 0xFFFFFF, 0xFF91C7, core * .74F);
            VorpalBlackHoleGeometry.glowDisc(buffer, .22F + pulse * .31F, .035F, 30, 0xFFFFFF, 0xFFFFFF, core * .92F);
        }
        if (wave > .002F) {
            for (int i = 0; i < 5; i++) {
                float age = frame - 29.05F - i * .24F;
                if (age <= 0) continue;
                float grow = VorpalBlackHoleTimeline.smooth(0, 3F, age), fade = 1 - VorpalBlackHoleTimeline.smooth(1.2F, 4.4F, age);
                float radius = .18F + grow * (2F + i * .44F);
                VorpalBlackHoleGeometry.torus(buffer, 0, 0, 0, radius, .022F + i * .006F, frame * (i % 2 == 0 ? .014F : -.018F),
                        VorpalBlackHoleTimeline.TWO_PI, 48, 5, (i - 2) * .10F, (i - 2) * .16F, i,
                        i == 0 ? 0xFFFFFF : i % 2 == 0 ? 0xFFA3C5 : 0xFF4C82,
                        wave * fade * (.74F - i * .08F));
            }
        }
        tessellator.draw();
        GlStateManager.popMatrix();
    }

    private void renderWorldSlashesDark(Tessellator tessellator, BufferBuilder buffer, float frame, long seed,
                                        float yaw, float cameraX, float cameraY, float cameraZ,
                                        float bladeX, float bladeY, float bladeZ) {
        float entry = VorpalBlackHoleTimeline.entryRibbon(frame), sever = VorpalBlackHoleTimeline.horizontalSever(frame);
        float weapon = VorpalBlackHoleTimeline.weaponTrail(frame);
        float beam = VorpalBlackHoleTimeline.magentaBeam(frame), crescent = VorpalBlackHoleTimeline.followupCrescent(frame);
        float debris = VorpalBlackHoleTimeline.debris(frame);
        if (Math.max(Math.max(Math.max(Math.max(entry, weapon), sever), Math.max(beam, crescent)), debris) < .002F) return;

        GlStateManager.pushMatrix();
        GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(SCALE, SCALE, SCALE);

        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        if (entry > .002F) {
            int count = sampleEntry(bladeX, bladeY, bladeZ);
            float head = VorpalBlackHoleTimeline.smooth(14.0F, 22.2F, frame);
            float tail = .88F * VorpalBlackHoleTimeline.smooth(24.0F, 28.4F, frame);
            if (head > tail + .002F) {
                VorpalBlackHoleGeometry.ribbon(buffer, ribbonPoints, count, cameraX, cameraY, cameraZ, .34F, entry * .56F, 0x6F0C35, tail, head);
            }
        }
        if (weapon > .002F) {
            int count = sampleWeaponTrail(bladeX, bladeY, bladeZ);
            float head = VorpalBlackHoleTimeline.smooth(12.0F, 19.4F, frame);
            float tail = .82F * VorpalBlackHoleTimeline.smooth(25.0F, 78.0F, frame);
            if (head > tail + .002F) {
                VorpalBlackHoleGeometry.ribbon(buffer, ribbonPoints, count, cameraX, cameraY, cameraZ, .20F, weapon * .56F, 0x6C1236, tail, head);
            }
        }
        if (sever > .002F) {
            int count = sampleSever();
            float head = VorpalBlackHoleTimeline.smooth(27.5F, 31.35F, frame);
            float tail = .83F * VorpalBlackHoleTimeline.smooth(33.0F, 38.0F, frame);
            if (head > tail + .002F) {
                VorpalBlackHoleGeometry.ribbon(buffer, ribbonPoints, count, cameraX, cameraY, cameraZ, .52F, sever * .56F, 0x5E082B, tail, head);
            }
        }
        if (beam > .002F) {
            int count = sampleBeam();
            float head = VorpalBlackHoleTimeline.smooth(29.5F, 32.8F, frame);
            float tail = .78F * VorpalBlackHoleTimeline.smooth(34.0F, 39.0F, frame);
            if (head > tail + .002F) {
                VorpalBlackHoleGeometry.ribbon(buffer, ribbonPoints, count, cameraX, cameraY, cameraZ, .48F, beam * .56F, 0x89124F, tail, head);
            }
        }
        if (crescent > .002F) {
            int count = sampleCrescent();
            float head = VorpalBlackHoleTimeline.smooth(32.0F, 37.5F, frame);
            float tail = .88F * VorpalBlackHoleTimeline.smooth(40.0F, 48.0F, frame);
            if (head > tail + .002F) {
                VorpalBlackHoleGeometry.ribbon(buffer, ribbonPoints, count, cameraX, cameraY, cameraZ, .28F, crescent * .56F, 0x7E123B, tail, head);
            }
        }
        if (debris > .002F) {
            renderDebrisDark(buffer, frame, debris, seed);
        }
        tessellator.draw();
        GlStateManager.popMatrix();
    }

    private void renderWorldSlashesGlow(Tessellator tessellator, BufferBuilder buffer, float frame, long seed,
                                        float yaw, float cameraX, float cameraY, float cameraZ,
                                        float bladeX, float bladeY, float bladeZ) {
        float entry = VorpalBlackHoleTimeline.entryRibbon(frame), sever = VorpalBlackHoleTimeline.horizontalSever(frame);
        float weapon = VorpalBlackHoleTimeline.weaponTrail(frame);
        float beam = VorpalBlackHoleTimeline.magentaBeam(frame), crescent = VorpalBlackHoleTimeline.followupCrescent(frame);
        float cuts = VorpalBlackHoleTimeline.thinCuts(frame), debris = VorpalBlackHoleTimeline.debris(frame);
        if (Math.max(Math.max(Math.max(Math.max(entry, weapon), sever), Math.max(beam, crescent)), Math.max(cuts, debris)) < .002F) return;

        GlStateManager.pushMatrix();
        GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(SCALE, SCALE, SCALE);

        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        if (entry > .002F) {
            int count = sampleEntry(bladeX, bladeY, bladeZ);
            float head = VorpalBlackHoleTimeline.smooth(14.0F, 22.2F, frame);
            float tail = .88F * VorpalBlackHoleTimeline.smooth(24.0F, 28.4F, frame);
            if (head > tail + .002F) {
                VorpalBlackHoleGeometry.ribbon(buffer, ribbonPoints, count, cameraX, cameraY, cameraZ, .22F, entry * .82F, 0xFF4F9F, tail * .96F, head);
                VorpalBlackHoleGeometry.ribbon(buffer, ribbonPoints, count, cameraX, cameraY, cameraZ, .075F, entry * .96F, 0xFFE3F0, tail * .90F, head);
            }
        }
        if (weapon > .002F) {
            int count = sampleWeaponTrail(bladeX, bladeY, bladeZ);
            float head = VorpalBlackHoleTimeline.smooth(12.0F, 19.4F, frame);
            float tail = .82F * VorpalBlackHoleTimeline.smooth(25.0F, 78.0F, frame);
            if (head > tail + .002F) {
                VorpalBlackHoleGeometry.ribbon(buffer, ribbonPoints, count, cameraX, cameraY, cameraZ, .115F, weapon * .82F, 0xFF367E, tail * .96F, head);
                VorpalBlackHoleGeometry.ribbon(buffer, ribbonPoints, count, cameraX, cameraY, cameraZ, .038F, weapon * .96F, 0xFFE5F1, tail * .90F, head);
            }
        }
        if (sever > .002F) {
            int count = sampleSever();
            float head = VorpalBlackHoleTimeline.smooth(27.5F, 31.35F, frame);
            float tail = .83F * VorpalBlackHoleTimeline.smooth(33.0F, 38.0F, frame);
            if (head > tail + .002F) {
                VorpalBlackHoleGeometry.ribbon(buffer, ribbonPoints, count, cameraX, cameraY, cameraZ, .34F, sever * .82F, 0xFF2E81, tail * .96F, head);
                VorpalBlackHoleGeometry.ribbon(buffer, ribbonPoints, count, cameraX, cameraY, cameraZ, .095F, sever * .96F, 0xFFFFFF, tail * .90F, head);
            }
        }
        if (beam > .002F) {
            int count = sampleBeam();
            float head = VorpalBlackHoleTimeline.smooth(29.5F, 32.8F, frame);
            float tail = .78F * VorpalBlackHoleTimeline.smooth(34.0F, 39.0F, frame);
            if (head > tail + .002F) {
                VorpalBlackHoleGeometry.ribbon(buffer, ribbonPoints, count, cameraX, cameraY, cameraZ, .31F, beam * .82F, 0xFF2D9B, tail * .96F, head);
                VorpalBlackHoleGeometry.ribbon(buffer, ribbonPoints, count, cameraX, cameraY, cameraZ, .082F, beam * .96F, 0xFFFFFF, tail * .90F, head);
                int tip = Math.min(count - 1, Math.max(1, (int) (head * (count - 1))));
                int p = tip * 3, prev = (tip - 1) * 3;
                VorpalBlackHoleGeometry.tube(buffer, ribbonPoints[prev], ribbonPoints[prev + 1], ribbonPoints[prev + 2],
                        ribbonPoints[p] + (ribbonPoints[p] - ribbonPoints[prev]) * .42F,
                        ribbonPoints[p + 1] + (ribbonPoints[p + 1] - ribbonPoints[prev + 1]) * .42F,
                        ribbonPoints[p + 2] + (ribbonPoints[p + 2] - ribbonPoints[prev + 2]) * .42F, .16F, 5, 0xFF4DB1, beam * .86F);
            }
        }
        if (crescent > .002F) {
            int count = sampleCrescent();
            float head = VorpalBlackHoleTimeline.smooth(32.0F, 37.5F, frame);
            float tail = .88F * VorpalBlackHoleTimeline.smooth(40.0F, 48.0F, frame);
            if (head > tail + .002F) {
                VorpalBlackHoleGeometry.ribbon(buffer, ribbonPoints, count, cameraX, cameraY, cameraZ, .18F, crescent * .82F, 0xFF3B82, tail * .96F, head);
                VorpalBlackHoleGeometry.ribbon(buffer, ribbonPoints, count, cameraX, cameraY, cameraZ, .052F, crescent * .96F, 0xFFFFFF, tail * .90F, head);
            }
        }
        if (cuts > .002F) {
            renderThinCuts(buffer, frame, cuts);
        }
        if (debris > .002F) {
            renderDebrisGlow(buffer, frame, debris, seed);
        }
        tessellator.draw();
        GlStateManager.popMatrix();
    }

    private int sampleEntry(float startX, float startY, float startZ) {
        for (int i = 0; i < 48; i++) {
            float u = i / 47F;
            float inv = 1 - u;
            setPoint(i,
                    VorpalBlackHoleTimeline.lerp(u, startX, -1.35F) + MathHelper.sin(u * (float) Math.PI) * .30F,
                    VorpalBlackHoleTimeline.lerp(u, startY, .42F) + MathHelper.sin(u * (float) Math.PI) * .32F,
                    VorpalBlackHoleTimeline.lerp(u, startZ, .18F) + inv * u * .42F);
        }
        return 48;
    }

    private int sampleWeaponTrail(float anchorX, float anchorY, float anchorZ) {
        for (int i = 0; i < 48; i++) {
            float u = i / 47F, angle = VorpalBlackHoleTimeline.lerp(u, -2.65F, .55F);
            setPoint(i, anchorX + MathHelper.cos(angle) * 1.34F - .24F,
                    anchorY + .32F + MathHelper.sin(angle) * .72F, anchorZ - .22F + u * .46F);
        }
        return 48;
    }

    private int sampleSever() {
        for (int i = 0; i < 64; i++) {
            float u = i / 63F;
            setPoint(i, VorpalBlackHoleTimeline.lerp(u, -4.8F, 5.65F),
                    CHEST_Y + VorpalBlackHoleTimeline.lerp(u, -.08F, .28F) + MathHelper.sin(u * (float) Math.PI) * .12F,
                    CHEST_Z + VorpalBlackHoleTimeline.lerp(u, -.10F, .34F));
        }
        return 64;
    }

    private int sampleBeam() {
        for (int i = 0; i < 56; i++) {
            float u = i / 55F;
            setPoint(i, .10F + u * 7.45F,
                    CHEST_Y + MathHelper.sin(u * (float) Math.PI) * -.18F, CHEST_Z + u * u * .86F);
        }
        return 56;
    }

    private int sampleCrescent() {
        for (int i = 0; i < 72; i++) {
            float u = i / 71F, angle = VorpalBlackHoleTimeline.lerp(u, -2.92F, 1.02F), radius = 2.05F + MathHelper.sin(u * (float) Math.PI) * .35F;
            setPoint(i, MathHelper.cos(angle) * radius - .35F, CHEST_Y - .08F + MathHelper.sin(angle * .72F) * .58F + u * .18F,
                    CHEST_Z + MathHelper.sin(angle * .78F) - .48F);
        }
        return 72;
    }

    private void setPoint(int i, float x, float y, float z) {
        int p = i * 3;
        ribbonPoints[p] = x;
        ribbonPoints[p + 1] = y;
        ribbonPoints[p + 2] = z;
    }

    private static void renderThinCuts(BufferBuilder buffer, float frame, float alpha) {
        for (int i = 0; i < CUT_SPECS.length; i++) {
            float[] s = CUT_SPECS[i];
            float age = (frame - s[7]) % 18.0F; // 循环脉动生成
            if (age <= 0) continue;
            float duration = 2.1F + (i % 3) * .55F, grow = VorpalBlackHoleTimeline.smooth(0, .55F, age), fade = 1 - VorpalBlackHoleTimeline.smooth(.65F, duration, age);
            float x1 = VorpalBlackHoleTimeline.lerp(grow, s[0], s[3]);
            float y1 = VorpalBlackHoleTimeline.lerp(grow, s[1] - 3.55F, s[4] - 3.55F);
            float z1 = VorpalBlackHoleTimeline.lerp(grow, s[2], s[5]);
            VorpalBlackHoleGeometry.tube(buffer, s[0], s[1] - 3.55F, s[2], x1, y1, z1, s[6], 4,
                    i % 4 == 0 ? 0xFFD0DF : i % 3 == 0 ? 0xE82554 : 0xFF5E8F, alpha * fade * .76F);
        }
    }

    private static void renderDebrisDark(BufferBuilder buffer, float frame, float alpha, long seed) {
        for (int i = 0; i < 46; i++) {
            if (i % 5 != 0) continue;
            float birth = 28.0F + noise(seed, i, 171.2F) * 6.0F;
            float age = (frame - birth) % 24.0F;
            if (age <= 0) continue;
            float duration = 7F + noise(seed, i, 173.8F) * 8F, fade = 1 - VorpalBlackHoleTimeline.smooth(duration * .62F, duration, age);
            if (fade <= .002F) continue;
            boolean fromVoid = i < 29;
            float baseX = (noise(seed, i, 176.5F) - .5F) * .9F;
            float baseY = fromVoid ? -.45F + noise(seed, i, 179.1F) * .85F : CHEST_Y + noise(seed, i, 181.7F) * 1.15F;
            float baseZ = -.25F + (noise(seed, i, 184.3F) - .5F) * .75F, angle = noise(seed, i, 187F) * VorpalBlackHoleTimeline.TWO_PI;
            float speed = .16F + noise(seed, i, 189.6F) * .42F;
            float x = baseX + MathHelper.cos(angle) * speed * age, y = baseY + (noise(seed, i, 192.2F) - .10F) * .34F * age - age * age * .011F;
            float z = baseZ + MathHelper.sin(angle) * speed * .42F * age, size = .035F + VorpalBlackHoleTimeline.smooth(0, .38F, age) * .055F;
            VorpalBlackHoleGeometry.tetrahedron(buffer, x, y, z, size, age * .9F, age * 1.2F, age * .8F, 0x210B18, alpha * fade * .78F);
        }
    }

    private static void renderDebrisGlow(BufferBuilder buffer, float frame, float alpha, long seed) {
        for (int i = 0; i < 46; i++) {
            if (i % 5 == 0) continue;
            float birth = 28.0F + noise(seed, i, 171.2F) * 6.0F;
            float age = (frame - birth) % 24.0F;
            if (age <= 0) continue;
            float duration = 7F + noise(seed, i, 173.8F) * 8F, fade = 1 - VorpalBlackHoleTimeline.smooth(duration * .62F, duration, age);
            if (fade <= .002F) continue;
            boolean fromVoid = i < 29;
            float baseX = (noise(seed, i, 176.5F) - .5F) * .9F;
            float baseY = fromVoid ? -.45F + noise(seed, i, 179.1F) * .85F : CHEST_Y + noise(seed, i, 181.7F) * 1.15F;
            float baseZ = -.25F + (noise(seed, i, 184.3F) - .5F) * .75F, angle = noise(seed, i, 187F) * VorpalBlackHoleTimeline.TWO_PI;
            float speed = .16F + noise(seed, i, 189.6F) * .42F;
            float x = baseX + MathHelper.cos(angle) * speed * age, y = baseY + (noise(seed, i, 192.2F) - .10F) * .34F * age - age * age * .011F;
            float z = baseZ + MathHelper.sin(angle) * speed * .42F * age, size = .035F + VorpalBlackHoleTimeline.smooth(0, .38F, age) * .055F;
            int color = i % 3 == 0 ? 0xFF5577 : 0xC5183D;
            VorpalBlackHoleGeometry.tetrahedron(buffer, x, y, z, size, age * .9F, age * 1.2F, age * .8F, color, alpha * fade * .78F);
        }
    }

    private static float noise(long seed, int index, float salt) {
        long x = seed + index * 0x9E3779B97F4A7C15L + Float.floatToIntBits(salt) * 0xC2B2AE3D27D4EB4FL;
        x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 27; x *= 0x94D049BB133111EBL;
        x ^= x >>> 31;
        return (x >>> 40) / (float) (1L << 24);
    }
}

package com.wjx.kablade.client.renderer;

import com.wjx.kablade.Entity.EntityVorpalBlackHole;
import com.wjx.kablade.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 客户端本地视听反馈：镜头震颤、FOV 冲击拉伸、全屏高光爆发。
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = Main.MODID, value = Side.CLIENT)
public final class VorpalBlackHoleFeedback {

    private static final double MAX_DISTANCE = 48.0;

    private VorpalBlackHoleFeedback() {
    }

    @SubscribeEvent
    public static void cameraAngles(EntityViewRenderEvent.CameraSetup event) {
        Entity entity = event.getEntity();
        if (entity == null || entity.world == null) return;
        Vec3d cameraPos = new Vec3d(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
        Feedback feedback = strongest(entity.world, cameraPos, (float) event.getRenderPartialTicks());
        if (feedback == null || feedback.impact < 0.002F) return;

        float t = feedback.frame;
        float strength = feedback.impact * feedback.distanceFade;
        float noiseA = MathHelper.sin(t * 4.731F + feedback.seedPhase);
        float noiseB = MathHelper.sin(t * 7.913F + feedback.seedPhase * 1.73F);
        float rebound = MathHelper.sin(MathHelper.clamp((t - 29.0F) / 7.0F, 0.0F, 1.0F) * (float) Math.PI);
        event.setYaw(event.getYaw() + noiseA * 0.72F * strength);
        event.setPitch(event.getPitch() + noiseB * 0.58F * strength);
        event.setRoll(event.getRoll() + (noiseA * 0.42F - rebound * 0.28F) * strength);
    }

    @SubscribeEvent
    public static void fov(EntityViewRenderEvent.FOVModifier event) {
        Entity entity = event.getEntity();
        if (entity == null || entity.world == null) return;
        Vec3d cameraPos = new Vec3d(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
        Feedback feedback = strongest(entity.world, cameraPos, (float) event.getRenderPartialTicks());
        if (feedback == null) return;
        float strength = feedback.impact * feedback.distanceFade;
        float compression = VorpalBlackHoleTimeline.gaussian(feedback.frame, 28.72F, 0.34F);
        float punch = VorpalBlackHoleTimeline.gaussian(feedback.frame, 30.15F, 1.18F);
        event.setFOV(event.getFOV() - compression * 1.8F * strength + punch * 4.6F * strength);
    }

    @SubscribeEvent
    public static void guiFlash(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null) return;
        Vec3d cameraPos = new Vec3d(mc.player.posX, mc.player.posY + mc.player.getEyeHeight(), mc.player.posZ);
        Feedback feedback = strongest(mc.world, cameraPos, event.getPartialTicks());
        if (feedback == null) return;

        float flash = VorpalBlackHoleTimeline.gaussian(feedback.frame, 29.12F, 0.38F)
                + VorpalBlackHoleTimeline.gaussian(feedback.frame, 30.55F, 0.72F) * 0.34F;
        int alpha = (int) (MathHelper.clamp(flash * feedback.distanceFade * 0.24F, 0.0F, 0.24F) * 255.0F);
        if (alpha <= 0) return;
        int color = (alpha << 24) | 0xFFF2F8;
        ScaledResolution res = event.getResolution();
        Gui.drawRect(0, 0, res.getScaledWidth(), res.getScaledHeight(), color);
    }

    private static Feedback strongest(World world, Vec3d camera, float partialTick) {
        if (world == null) return null;
        AxisAlignedBB bounds = new AxisAlignedBB(
                camera.x - MAX_DISTANCE, camera.y - MAX_DISTANCE, camera.z - MAX_DISTANCE,
                camera.x + MAX_DISTANCE, camera.y + MAX_DISTANCE, camera.z + MAX_DISTANCE);
        List<EntityVorpalBlackHole> holes = world.getEntitiesWithinAABB(EntityVorpalBlackHole.class, bounds);
        if (holes.isEmpty()) return null;

        Feedback best = null;
        float bestScore = 0.0F;
        for (EntityVorpalBlackHole hole : holes) {
            float frame = VorpalBlackHoleTimeline.frame(hole.ticksExisted + partialTick);
            if (frame < 27.0F || frame > 40.0F) continue;
            double distance = camera.distanceTo(new Vec3d(hole.posX, hole.posY, hole.posZ));
            float fade = 1.0F - VorpalBlackHoleTimeline.smooth(8.0F, (float) MAX_DISTANCE, (float) distance);
            float impact = VorpalBlackHoleTimeline.cameraImpact(frame);
            float score = fade * impact;
            if (score > bestScore) {
                long seed = hole.getUniqueID().getMostSignificantBits() ^ hole.getUniqueID().getLeastSignificantBits();
                bestScore = score;
                best = new Feedback(frame, impact, fade, (seed & 0xFFFF) / 65535.0F * VorpalBlackHoleTimeline.TWO_PI);
            }
        }
        return best;
    }

    private static class Feedback {
        final float frame;
        final float impact;
        final float distanceFade;
        final float seedPhase;

        Feedback(float frame, float impact, float distanceFade, float seedPhase) {
            this.frame = frame;
            this.impact = impact;
            this.distanceFade = distanceFade;
            this.seedPhase = seedPhase;
        }
    }
}

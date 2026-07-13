package com.wjx.kablade.client.renderer;

import com.wjx.kablade.Main;
import com.wjx.kablade.entity.VorpalBlackHoleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Local-only hit feedback for the Vorpal Hole main rupture. */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class VorpalBlackHoleFeedback {

    private static final double MAX_DISTANCE = 48.0;

    private VorpalBlackHoleFeedback() {
    }

    @SubscribeEvent
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Feedback feedback = strongest(event.getCamera().getPosition(), (float) event.getPartialTick());
        if (feedback == null || feedback.impact < 0.002F) return;

        float accessibility = damageTiltScale();
        if (accessibility <= 0.001F) return;
        float t = feedback.frame;
        float strength = feedback.impact * feedback.distanceFade * accessibility;
        float noiseA = Mth.sin(t * 4.731F + feedback.seedPhase);
        float noiseB = Mth.sin(t * 7.913F + feedback.seedPhase * 1.73F);
        float rebound = Mth.sin(Mth.clamp((t - 29.0F) / 7.0F, 0.0F, 1.0F) * Mth.PI);
        event.setYaw(event.getYaw() + noiseA * 0.72F * strength);
        event.setPitch(event.getPitch() + noiseB * 0.58F * strength);
        event.setRoll(event.getRoll() + (noiseA * 0.42F - rebound * 0.28F) * strength);
    }

    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov event) {
        Feedback feedback = strongest(event.getCamera().getPosition(), (float) event.getPartialTick());
        if (feedback == null) return;
        float strength = feedback.impact * feedback.distanceFade;
        float compression = VorpalBlackHoleTimeline.gaussian(feedback.frame, 28.72F, 0.34F);
        float punch = VorpalBlackHoleTimeline.gaussian(feedback.frame, 30.15F, 1.18F);
        event.setFOV(event.getFOV() - compression * 1.8F * strength + punch * 4.6F * strength);
    }

    @SubscribeEvent
    public static void guiFlash(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameRenderer == null) return;
        Feedback feedback = strongest(minecraft.gameRenderer.getMainCamera().getPosition(), event.getPartialTick());
        if (feedback == null) return;
        float screenScale = screenEffectScale();
        if (screenScale <= 0.001F) return;

        float flash = VorpalBlackHoleTimeline.gaussian(feedback.frame, 29.12F, 0.38F)
                + VorpalBlackHoleTimeline.gaussian(feedback.frame, 30.55F, 0.72F) * 0.34F;
        int alpha = (int) (Mth.clamp(flash * feedback.distanceFade * screenScale * 0.24F, 0.0F, 0.24F) * 255.0F);
        if (alpha <= 0) return;
        int color = alpha << 24 | 0xFFF2F8;
        event.getGuiGraphics().fill(0, 0, event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight(), color);
    }

    private static Feedback strongest(Vec3 camera, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;
        AABB bounds = AABB.ofSize(camera, MAX_DISTANCE * 2.0, MAX_DISTANCE * 2.0, MAX_DISTANCE * 2.0);
        Feedback best = null;
        float bestScore = 0.0F;
        for (VorpalBlackHoleEntity hole : minecraft.level.getEntitiesOfClass(VorpalBlackHoleEntity.class, bounds)) {
            float frame = VorpalBlackHoleTimeline.frame(hole.tickCount + partialTick);
            if (frame < 27.0F || frame > 40.0F) continue;
            double distance = camera.distanceTo(hole.getPosition(partialTick));
            float fade = 1.0F - VorpalBlackHoleTimeline.smooth(8.0F, (float) MAX_DISTANCE, (float) distance);
            float impact = VorpalBlackHoleTimeline.cameraImpact(frame);
            float score = fade * impact;
            if (score > bestScore) {
                long seed = hole.getUUID().getMostSignificantBits() ^ hole.getUUID().getLeastSignificantBits();
                bestScore = score;
                best = new Feedback(frame, impact, fade, (seed & 0xFFFF) / 65535.0F * Mth.TWO_PI);
            }
        }
        return best;
    }

    private static float damageTiltScale() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.options.damageTiltStrength().get().floatValue();
    }

    private static float screenEffectScale() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.options.screenEffectScale().get().floatValue();
    }

    private record Feedback(float frame, float impact, float distanceFade, float seedPhase) {
    }
}

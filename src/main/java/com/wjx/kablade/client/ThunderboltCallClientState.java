package com.wjx.kablade.client;

import com.wjx.kablade.Main;
import com.wjx.kablade.config.KabladeClientConfig;
import com.wjx.kablade.entity.ThunderboltCallEntity;
import com.wjx.kablade.slasharts.ThunderboltCallTimeline;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Package-defined camera, FOV and full-screen feedback for Thunderbolt Call. */
@Mod.EventBusSubscriber(modid = Main.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ThunderboltCallClientState {

    private ThunderboltCallClientState() {
    }

    @SubscribeEvent
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!KabladeClientConfig.THUNDERBOLT_CALL_CAMERA_SHAKE.get()) {
            return;
        }
        Feedback feedback = strongest(event.getCamera().getPosition(), (float) event.getPartialTick());
        if (feedback == null || feedback.impact < 0.002F) {
            return;
        }
        float accessibility = Minecraft.getInstance().options.damageTiltStrength().get().floatValue();
        float strength = feedback.impact * feedback.distanceFade * accessibility;
        event.setYaw(event.getYaw() + Mth.sin(feedback.frame * 2.31F + feedback.seedPhase)
                * 0.72F * strength);
        event.setPitch(event.getPitch() + Mth.sin(feedback.frame * 2.83F + feedback.seedPhase * 1.7F)
                * 0.64F * strength);
        event.setRoll(event.getRoll() + Mth.sin(feedback.frame * 1.97F + feedback.seedPhase * 0.7F)
                * 0.44F * strength);
    }

    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov event) {
        if (!KabladeClientConfig.THUNDERBOLT_CALL_CAMERA_SHAKE.get()) {
            return;
        }
        Feedback feedback = strongest(event.getCamera().getPosition(), (float) event.getPartialTick());
        if (feedback != null) {
            event.setFOV(event.getFOV() + feedback.fovImpulse * feedback.distanceFade);
        }
    }

    @SubscribeEvent
    public static void guiFlash(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.gameRenderer == null) {
            return;
        }
        Feedback feedback = strongest(minecraft.gameRenderer.getMainCamera().getPosition(),
                event.getPartialTick());
        if (feedback == null) {
            return;
        }
        float scale = minecraft.options.screenEffectScale().get().floatValue();
        if (KabladeClientConfig.THUNDERBOLT_CALL_REDUCED_FLASH.get()) {
            scale *= 0.28F;
        }
        int alpha = (int) (Mth.clamp(feedback.impact * feedback.distanceFade * scale * 0.20F,
                0.0F, 0.20F) * 255.0F);
        if (alpha > 0) {
            event.getGuiGraphics().fill(0, 0, event.getWindow().getGuiScaledWidth(),
                    event.getWindow().getGuiScaledHeight(), alpha << 24 | 0xEEE7FF);
        }
    }

    private static Feedback strongest(Vec3 camera, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        Feedback best = null;
        float bestScore = 0.0F;
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof ThunderboltCallEntity cast) || !cast.isAlive()) {
                continue;
            }
            float frame = cast.getReferenceFrame(partialTick);
            float impact = ThunderboltCallTimeline.impactStrength(frame);
            float fovImpulse = ThunderboltCallTimeline.fovImpulse(frame);
            double targetDistance = camera.distanceTo(cast.getTargetAnchor(partialTick));
            double ownerDistance = camera.distanceTo(cast.getOwnerAnchor(partialTick));
            double distance = Math.min(targetDistance, ownerDistance);
            float fade = 1.0F - ThunderboltCallTimeline.smooth((float) distance, 4.0F, 32.0F);
            float score = Math.max(impact, Math.abs(fovImpulse) / 3.4F) * fade;
            if (score > bestScore) {
                bestScore = score;
                best = new Feedback(frame, impact, fade, fovImpulse,
                        (cast.getSeed() & 0xFFFFL) / 65535.0F * Mth.TWO_PI);
            }
        }
        return best;
    }

    private record Feedback(float frame, float impact, float distanceFade,
                            float fovImpulse, float seedPhase) {
    }
}

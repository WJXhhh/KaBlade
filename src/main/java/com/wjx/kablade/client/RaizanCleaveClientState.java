package com.wjx.kablade.client;

import com.wjx.kablade.Main;
import com.wjx.kablade.config.KabladeClientConfig;
import com.wjx.kablade.entity.RaizanCleaveEntity;
import com.wjx.kablade.slasharts.RaizanCleaveTimeline;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

/** Client cache, held-blade suppression and impact feedback for active Raizan casts. */
@Mod.EventBusSubscriber(modid = Main.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RaizanCleaveClientState {

    private static final Set<Integer> ACTIVE_OWNERS = new HashSet<>();

    private RaizanCleaveClientState() {
    }

    public static boolean isActive(int entityId) {
        return ACTIVE_OWNERS.contains(entityId);
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ACTIVE_OWNERS.clear();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof RaizanCleaveEntity cast && cast.isAlive()) {
                ACTIVE_OWNERS.add(cast.getOwnerId());
            }
        }
    }

    @SubscribeEvent
    public static void hideFirstPersonBlade(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getHand() == InteractionHand.MAIN_HAND && minecraft.player != null
                && isActive(minecraft.player.getId())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!KabladeClientConfig.RAIZAN_CLEAVE_CAMERA_SHAKE.get()) {
            return;
        }
        Feedback feedback = strongest(event.getCamera().getPosition(), (float) event.getPartialTick());
        if (feedback == null || feedback.impact < 0.002F) {
            return;
        }
        float accessibility = Minecraft.getInstance().options.damageTiltStrength().get().floatValue();
        float strength = feedback.impact * feedback.distanceFade * accessibility;
        event.setYaw(event.getYaw() + Mth.sin(feedback.time * 0.91F + feedback.seedPhase)
                * 0.60F * strength);
        event.setPitch(event.getPitch() + Mth.sin(feedback.time * 1.27F + feedback.seedPhase * 1.7F)
                * 0.52F * strength);
        event.setRoll(event.getRoll() + Mth.sin(feedback.time * 0.73F + feedback.seedPhase * 0.7F)
                * 0.38F * strength);
    }

    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov event) {
        if (!KabladeClientConfig.RAIZAN_CLEAVE_CAMERA_SHAKE.get()) {
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
        if (KabladeClientConfig.RAIZAN_CLEAVE_REDUCED_FLASH.get()) {
            scale *= 0.28F;
        }
        int alpha = (int) (Mth.clamp(feedback.impact * feedback.distanceFade * scale * 0.18F,
                0.0F, 0.18F) * 255.0F);
        if (alpha > 0) {
            event.getGuiGraphics().fill(0, 0, event.getWindow().getGuiScaledWidth(),
                    event.getWindow().getGuiScaledHeight(), alpha << 24 | 0xEDEBFF);
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
            if (!(entity instanceof RaizanCleaveEntity cast) || !cast.isAlive()) {
                continue;
            }
            float age = cast.getRenderAge(partialTick);
            float impact = 0.0F;
            for (int i = 0; i < RaizanCleaveTimeline.HIT_TICKS.length; i++) {
                int hit = RaizanCleaveTimeline.HIT_TICKS[i];
                float width = i == 0 || i == 5 || i == 8 ? 0.72F : 0.52F;
                impact = Math.max(impact, gaussian(age, hit, width));
            }
            float chopFov = cinematicFov(age, RaizanCleaveTimeline.HIT_TICKS[0], 2.8F);
            float finalFov = cinematicFov(age,
                    RaizanCleaveTimeline.HIT_TICKS[RaizanCleaveTimeline.HIT_TICKS.length - 1], 4.2F);
            float fovImpulse = Math.abs(chopFov) > Math.abs(finalFov) ? chopFov : finalFov;
            double distance = camera.distanceTo(cast.getTargetAnchor());
            float fade = 1.0F - smooth((float) ((distance - 4.0D) / 28.0D));
            float score = impact * fade;
            if (score > bestScore) {
                bestScore = score;
                best = new Feedback(age, impact, fade, fovImpulse,
                        (cast.getSeed() & 0xFFFFL) / 65535.0F * Mth.TWO_PI);
            }
        }
        return best;
    }

    private static float gaussian(float value, float center, float width) {
        float x = (value - center) / Math.max(width, 1.0E-4F);
        return (float) Math.exp(-0.5F * x * x);
    }

    private static float cinematicFov(float age, float hit, float amplitude) {
        float compression = gaussian(age, hit - 0.62F, 0.42F);
        float release = gaussian(age, hit + 0.58F, 0.78F);
        return -amplitude * 0.54F * compression + amplitude * 0.72F * release;
    }

    private static float smooth(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private record Feedback(float time, float impact, float distanceFade,
                            float fovImpulse, float seedPhase) {
    }
}

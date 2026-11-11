package com.wjx.kablade.client;

import com.wjx.kablade.Main;
import com.wjx.kablade.config.KabladeClientConfig;
import com.wjx.kablade.entity.SunfireImpactEntity;
import com.wjx.kablade.slasharts.SunfireImpactTimeline;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Local movement prediction and accessibility-aware, contact-synchronized feedback. */
@Mod.EventBusSubscriber(modid = Main.MODID, value = Dist.CLIENT)
public final class SunfireImpactClientState {
    private static boolean predicting;
    private static boolean previousNoGravity;
    private SunfireImpactClientState() { }

    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        predicting = false;
    }

    private static SunfireImpactEntity localCast() {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof SunfireImpactEntity cast && cast.getOwnerId() == mc.player.getId()
                    && cast.isControlling() && cast.isAlive()) return cast;
        }
        return null;
    }

    @SubscribeEvent public static void input(MovementInputUpdateEvent event) {
        if (localCast() == null) return;
        var input = event.getInput();
        input.forwardImpulse = input.leftImpulse = 0;
        input.up = input.down = input.left = input.right = input.jumping = input.shiftKeyDown = false;
    }

    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        var mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.START || event.player != mc.player) return;
        var cast = localCast();
        if (cast != null) {
            if (!predicting) previousNoGravity = cast.wasNoGravity();
            predicting = true;
            event.player.setNoGravity(true);
            Vec3 next = cast.plannedPosition(cast.getAge(0) + 1);
            Vec3 delta = next.subtract(event.player.position());
            if (delta.lengthSqr() < 16) event.player.setDeltaMovement(delta);
            event.player.fallDistance = 0;
        } else if (predicting) {
            event.player.setNoGravity(previousNoGravity);
            event.player.setDeltaMovement(Vec3.ZERO);
            predicting = false;
        }
    }

    private static Feedback feedback(float partial) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return new Feedback(0, 0);
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        float best = 0, bestTime = 0;
        for (var entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof SunfireImpactEntity cast) || cast.getImpactAge() < 0) continue;
            float time = cast.getAge(partial) - cast.getImpactAge();
            if (time < 0 || time > 10) continue;
            float fade = 1 - SunfireImpactTimeline.smooth(4, 26, (float) camera.distanceTo(cast.getImpactCenter()));
            float strength = (float) Math.exp(-time * 0.42) * fade;
            if (strength > best) { best = strength; bestTime = time; }
        }
        return new Feedback(best, bestTime);
    }

    @SubscribeEvent public static void camera(ViewportEvent.ComputeCameraAngles event) {
        if (!KabladeClientConfig.SUNFIRE_IMPACT_CAMERA_SHAKE.get()) return;
        Feedback fx = feedback((float) event.getPartialTick());
        float scale = Minecraft.getInstance().options.damageTiltStrength().get().floatValue() * fx.strength;
        event.setPitch(event.getPitch() + Mth.cos(fx.time * 2.7F) * 1.6F * scale);
        event.setYaw(event.getYaw() + Mth.sin(fx.time * 3.5F) * 0.7F * scale);
        event.setRoll(event.getRoll() + Mth.sin(fx.time * 2.1F) * 0.75F * scale);
    }

    @SubscribeEvent public static void fov(ViewportEvent.ComputeFov event) {
        if (!KabladeClientConfig.SUNFIRE_IMPACT_CAMERA_SHAKE.get()) return;
        Feedback fx = feedback((float) event.getPartialTick());
        float scale = Minecraft.getInstance().options.fovEffectScale().get().floatValue();
        event.setFOV(event.getFOV() + Mth.cos(fx.time * 0.65F) * fx.strength * 3 * scale);
    }

    @SubscribeEvent public static void flash(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        Feedback fx = feedback(event.getPartialTick());
        float scale = mc.options.screenEffectScale().get().floatValue();
        if (KabladeClientConfig.SUNFIRE_IMPACT_REDUCED_FLASH.get()) scale *= 0.2F;
        int alpha = (int) (Math.max(0, 1 - fx.time / 4) * fx.strength * scale * 48);
        if (alpha > 0) event.getGuiGraphics().fill(0, 0, event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight(), alpha << 24 | 0xFFE8A8);
    }
    private record Feedback(float strength, float time) { }
}

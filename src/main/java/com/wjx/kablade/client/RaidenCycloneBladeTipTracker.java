package com.wjx.kablade.client;

import com.wjx.kablade.Main;
import mods.flammpfeil.slashblade.event.client.RenderOverrideEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Captures MAG-Typhoon's actual OBJ tip after SlashBlade has applied its render pose. */
@Mod.EventBusSubscriber(modid = Main.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RaidenCycloneBladeTipTracker {

    // Farthest blade vertex in model/honkai/mag_typhoon/mdl.obj.
    private static final Vector4f MODEL_TIP = new Vector4f(-280.934692F, 23.079803F, 0.400531F, 1.0F);
    private static final ThreadLocal<LivingEntity> RENDERING_OWNER = new ThreadLocal<>();

    private RaidenCycloneBladeTipTracker() {
    }

    public static void beginBladeLayer(LivingEntity owner) {
        RENDERING_OWNER.set(owner);
    }

    public static void endBladeLayer() {
        RENDERING_OWNER.remove();
    }

    @SubscribeEvent
    public static void capture(RenderOverrideEvent event) {
        LivingEntity owner = RENDERING_OWNER.get();
        if (owner == null || !RaidenCycloneRenderer.isActive(owner.getId())) return;
        String target = event.getTarget();
        if (target == null || !(target.equals("blade") || target.equals("blade_luminous")
                || target.equals("blade_damaged"))) return;

        Matrix4f transform = new Matrix4f(event.getPoseStack().last().pose());
        Vector4f tip = new Vector4f(MODEL_TIP);
        transform.transform(tip);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameRenderer == null) return;
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        RaidenCycloneRenderer.recordBladeTip(owner.getId(),
                new Vec3(tip.x + camera.x, tip.y + camera.y, tip.z + camera.z));
    }
}

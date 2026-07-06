package com.wjx.kablade.event;

import com.wjx.kablade.Main;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.entity.IShootable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SaFriendlyFireHandler {

    private SaFriendlyFireHandler() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        Entity direct = source.getDirectEntity();
        if (!isSlashArtEntity(direct)) {
            return;
        }

        Entity owner = source.getEntity();
        if (owner == null && direct instanceof IShootable shootable) {
            owner = shootable.getShooter();
        }
        LivingEntity target = event.getEntity();
        if (owner != null && !SaTargeting.canDamage(owner, target)) {
            event.setCanceled(true);
        }
    }

    private static boolean isSlashArtEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        String className = entity.getClass().getName();
        return className.startsWith("com.wjx.kablade.entity.")
                || className.startsWith("mods.flammpfeil.slashblade.entity.");
    }
}

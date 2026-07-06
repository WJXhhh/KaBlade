package com.wjx.kablade.specialeffect;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModSpecialEffects;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Thunder Blitz, Key of Castigation's mark detonation special effect. */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ThunderBlitz extends SpecialEffect {

    public static final String THUNDER_MARK_TAG = Main.MODID + ":thunder_blitz_time";

    private static final double RADIUS = 10.0D;
    private static final double VERTICAL_RADIUS = 5.0D;
    private static final float TICK_DAMAGE = 5.0F;

    public ThunderBlitz() {
        super(-1, true, true);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }

        tickThunderMark(entity);

        if (!(entity instanceof Player player) || !hasEffect(player)) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        AABB area = player.getBoundingBox()
                .inflate(RADIUS, VERTICAL_RADIUS, RADIUS)
                .move(player.getDeltaMovement());
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                target -> SaTargeting.canDamage(player, target))) {
            if (target.getPersistentData().getInt(THUNDER_MARK_TAG) > 0 && level.getGameTime() % 10L == 0L) {
                target.hurt(level.damageSources().playerAttack(player), TICK_DAMAGE);
            }
        }
    }

    private static void tickThunderMark(LivingEntity entity) {
        int remaining = entity.getPersistentData().getInt(THUNDER_MARK_TAG);
        if (remaining <= 0) {
            return;
        }
        remaining--;
        if (remaining <= 0) {
            entity.getPersistentData().remove(THUNDER_MARK_TAG);
        } else {
            entity.getPersistentData().putInt(THUNDER_MARK_TAG, remaining);
        }
    }

    private static boolean hasEffect(Player player) {
        ItemStack blade = player.getMainHandItem();
        if (!(blade.getItem() instanceof ItemSlashBlade)) {
            return false;
        }
        return blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.THUNDER_BLITZ.getId()))
                .orElse(false);
    }
}

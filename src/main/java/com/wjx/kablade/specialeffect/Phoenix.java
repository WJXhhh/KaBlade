package com.wjx.kablade.specialeffect;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModSpecialEffects;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 凰 —— 凰剑专属特殊效果。
 * 从 1.12.2 {@code SEPhoenix} 移植而来。
 * <p>
 * 攻击时若目标已在燃烧，伤害 ×1.2；否则点燃目标 5 秒。
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Phoenix extends mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect {

    private static final float DAMAGE_BOOST = 1.2F;
    private static final int FIRE_SECONDS = 5;

    public Phoenix() {
        super(-1, true, true);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        LivingEntity target = event.getEntity();
        if (!(event.getSource().getEntity() instanceof Player player)) return;

        ItemStack blade = player.getMainHandItem();
        if (!(blade.getItem() instanceof ItemSlashBlade)) return;

        boolean hasPhoenix = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.PHOENIX.getId()))
                .orElse(false);
        if (!hasPhoenix) return;
        if (!SaTargeting.canDamage(player, target)) return;

        if (target.isOnFire()) {
            event.setAmount(event.getAmount() * DAMAGE_BOOST);
        } else {
            target.setSecondsOnFire(FIRE_SECONDS);
        }
    }
}

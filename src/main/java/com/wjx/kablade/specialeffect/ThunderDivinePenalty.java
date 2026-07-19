package com.wjx.kablade.specialeffect;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModSpecialEffects;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 罪雷天罚（Divine Penalty）—— 天殛之境的鸣神处决效果。
 * <p>
 * 主手持有此效果的拔刀剑攻击带有鸣神标记的目标时，该次伤害提高 20%。
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ThunderDivinePenalty extends SpecialEffect {

    private static final float DAMAGE_MULTIPLIER = 1.20F;

    public ThunderDivinePenalty() {
        super(-1, true, true);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player player) || !hasEffect(player)) {
            return;
        }

        LivingEntity target = event.getEntity();
        if (!SaTargeting.canDamage(player, target)
                || target.getPersistentData().getInt(ThunderBlitz.NARUKAMI_TAG) <= 0) {
            return;
        }

        event.setAmount(event.getAmount() * DAMAGE_MULTIPLIER);
    }

    private static boolean hasEffect(Player player) {
        ItemStack blade = player.getMainHandItem();
        if (!(blade.getItem() instanceof ItemSlashBlade)) {
            return false;
        }
        return blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.THUNDER_DIVINE_PENALTY.getId()))
                .orElse(false);
    }
}

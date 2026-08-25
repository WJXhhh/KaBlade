package com.wjx.kablade.specialeffect;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModSpecialEffects;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 血气唤醒 (Blood Awakening) —— 「绯红皇后」专属特殊效果。
 * <p>
 * 玩家持有带有此 SE 的拔刀剑时，生命值越低造成伤害越高；
 * 每损失 1% 的生命值，造成伤害提高 0.6%。
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BloodAwakening extends SpecialEffect {

    public BloodAwakening() {
        super(-1, true, true);
    }

    @Override
    public Component getDescription() {
        return Component.translatable(getDescriptionId()).withStyle(ChatFormatting.RED);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof Player player)) {
            return;
        }
        if (!isBladeWithEffect(player.getMainHandItem())) {
            return;
        }
        if (!SaTargeting.canDamage(player, event.getEntity())) {
            return;
        }

        float maxHealth = player.getMaxHealth();
        if (maxHealth <= 0.0F) {
            return;
        }

        float currentHealth = Math.max(0.0F, player.getHealth());
        float lostPercent = Math.max(0.0F, Math.min(100.0F, (1.0F - (currentHealth / maxHealth)) * 100.0F));
        if (lostPercent > 0.0F) {
            float boost = 1.0F + (lostPercent * 0.006F);
            event.setAmount(event.getAmount() * boost);
        }
    }

    public static boolean isHeldBy(Player player) {
        return isBladeWithEffect(player.getMainHandItem());
    }

    public static int getLostHealthPercentInt(Player player) {
        if (!isHeldBy(player)) {
            return 0;
        }
        float maxHealth = player.getMaxHealth();
        if (maxHealth <= 0.0F) {
            return 0;
        }
        float currentHealth = Math.max(0.0F, player.getHealth());
        float lostPercent = (1.0F - (currentHealth / maxHealth)) * 100.0F;
        return Math.max(0, Math.min(100, Math.round(lostPercent)));
    }

    private static boolean isBladeWithEffect(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemSlashBlade)) {
            return false;
        }
        return stack.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.BLOOD_AWAKENING.getId()))
                .orElse(false);
    }
}

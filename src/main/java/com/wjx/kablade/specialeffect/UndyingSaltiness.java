package com.wjx.kablade.specialeffect;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModSpecialEffects;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Undying Saltiness, One Salty Tuna's 1.12.2 special effect. */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class UndyingSaltiness extends SpecialEffect {

    public UndyingSaltiness() {
        super(-1, false, false);
    }

    @Override
    public Component getDescription() {
        return Component.translatable(getDescriptionId()).withStyle(ChatFormatting.DARK_GREEN);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity)) {
            return;
        }

        ItemStack blade = player.getMainHandItem();
        if (!(blade.getItem() instanceof ItemSlashBlade)) {
            return;
        }

        boolean active = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.UNDYING_SALTINESS.getId()))
                .orElse(false);
        if (!active) {
            return;
        }

        float healthRatio = player.getHealth() / Math.max(1.0F, player.getMaxHealth());
        event.setAmount(event.getAmount() * (1.0F + (1.0F - healthRatio) * 4.0F));
    }
}

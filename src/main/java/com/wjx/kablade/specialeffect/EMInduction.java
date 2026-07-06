package com.wjx.kablade.specialeffect;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModMobEffects;
import com.wjx.kablade.init.ModSpecialEffects;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** EM Induction, Mag Storm's blade attack special effect from 1.12.2. */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EMInduction extends SpecialEffect {

    private static final int PARALYSIS_DURATION = 60;
    private static final int PARALYSIS_AMPLIFIER = 1;

    public EMInduction() {
        super(-1, true, true);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        Entity source = event.getSource().getEntity();
        if (!(source instanceof Player player) || !hasEffect(player)) {
            return;
        }
        if (!SaTargeting.canDamage(player, event.getEntity())) {
            return;
        }

        event.getEntity().addEffect(new MobEffectInstance(ModMobEffects.PARALYSIS.get(),
                PARALYSIS_DURATION, PARALYSIS_AMPLIFIER));
    }

    private static boolean hasEffect(Player player) {
        ItemStack blade = player.getMainHandItem();
        if (!(blade.getItem() instanceof ItemSlashBlade)) {
            return false;
        }
        return blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.EM_INDUCTION.getId()))
                .orElse(false);
    }
}

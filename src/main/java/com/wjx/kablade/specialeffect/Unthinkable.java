package com.wjx.kablade.specialeffect;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.KabladeCapabilities;
import com.wjx.kablade.init.ModSpecialEffects;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 心行处灭 —— 澄凝之境：自性纯一专属特殊效果。
 * <p>
 * 命中时有 50% 概率叠加预知（上限 3），每层提升 20% 伤害，
 * 并且每 100 tick 自动消退一层。
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Unthinkable extends SpecialEffect {

    /** 与明心见性共用同一个预知层数，只由当前所持刀的 SE 决定实际倍率。 */
    public static final String PROP_KEY = TrueSelf.PROP_KEY;

    private static final int MAX_STACKS = 3;
    private static final float STACK_CHANCE = 0.50F;

    public Unthinkable() {
        super(-1, true, true);
    }

    @SubscribeEvent
    public static void onHit(mods.flammpfeil.slashblade.event.SlashBladeEvent.HitEvent event) {
        LivingEntity user = event.getUser();
        if (!(user instanceof Player player)) return;
        if (user.level().isClientSide()) return;

        ItemStack blade = event.getBlade();
        if (!hasUnthinkable(blade)) return;
        if (!SaTargeting.canDamage(player, event.getTarget())) return;
        if (player.getRandom().nextFloat() >= STACK_CHANCE) return;

        player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                .ifPresent(cap -> {
                    int current = cap.get(PROP_KEY);
                    if (current < MAX_STACKS) {
                        cap.set(PROP_KEY, current + 1);
                    }
                });
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        if (!hasUnthinkableOnHand(player)) return;
        if (!SaTargeting.canDamage(player, event.getEntity())) return;

        player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                .ifPresent(cap -> {
                    int stacks = cap.get(PROP_KEY);
                    if (stacks > 0) {
                        event.setAmount(event.getAmount() * (1.0F + stacks * 0.2F));
                    }
                });
    }

    private static boolean hasUnthinkable(ItemStack stack) {
        return stack.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.UNTHINKABLE.getId()))
                .orElse(false);
    }

    private static boolean hasUnthinkableOnHand(Player player) {
        return hasUnthinkable(player.getMainHandItem());
    }
}

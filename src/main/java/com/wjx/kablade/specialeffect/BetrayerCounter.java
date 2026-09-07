package com.wjx.kablade.specialeffect;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModSpecialEffects;
import com.wjx.kablade.util.SaDamage;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 背叛者反击 —— 背叛者巨剑专属特殊效果。
 *
 * <p>持有者受到来自生物的伤害时，有 49% 的概率以当前攻击力的一半反击攻击者。
 * 玩家当前的 {@link Attributes#ATTACK_DAMAGE} 已经包含主手拔刀剑的攻击力，
 * 因此这里不再重复叠加刀的基础攻击力。</p>
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BetrayerCounter extends SpecialEffect {

    private static final float COUNTER_CHANCE = 0.49F;
    private static final double COUNTER_DAMAGE_RATIO = 0.5D;
    private static final float MIN_COUNTER_DAMAGE = 1.0F;

    /** Prevents two players with this effect from recursively countering one another. */
    private static final Set<CounterDamageKey> ACTIVE_COUNTER_DAMAGE = new HashSet<>();

    public BetrayerCounter() {
        super(-1, true, true);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0.0F
                || event.getEntity().level().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof Player player) || !hasEffect(player)) {
            return;
        }

        LivingEntity attacker = resolveAttacker(event.getSource());
        if (attacker == null || !SaTargeting.canDamage(player, attacker)) {
            return;
        }

        Level level = player.level();
        if (level.random.nextFloat() >= COUNTER_CHANCE) {
            return;
        }

        CounterDamageKey counterDamage = new CounterDamageKey(player.getUUID(), attacker.getUUID());
        if (!ACTIVE_COUNTER_DAMAGE.add(counterDamage)) {
            return;
        }

        try {
            SaDamage.hurt(attacker, level.damageSources().playerAttack(player), counterDamage(player));
        } finally {
            ACTIVE_COUNTER_DAMAGE.remove(counterDamage);
        }
    }

    private static LivingEntity resolveAttacker(DamageSource source) {
        Entity sourceEntity = source.getEntity();
        if (sourceEntity instanceof LivingEntity livingSource) {
            return livingSource;
        }

        Entity directEntity = source.getDirectEntity();
        return directEntity instanceof LivingEntity livingDirect ? livingDirect : null;
    }

    private static float counterDamage(Player player) {
        double currentAttackDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        return (float) Math.max(MIN_COUNTER_DAMAGE, currentAttackDamage * COUNTER_DAMAGE_RATIO);
    }

    private static boolean hasEffect(Player player) {
        ItemStack blade = player.getMainHandItem();
        if (!(blade.getItem() instanceof ItemSlashBlade)) {
            return false;
        }
        return blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.BETRAYER_COUNTER.getId()))
                .orElse(false);
    }

    private record CounterDamageKey(UUID ownerUUID, UUID targetUUID) {
    }
}

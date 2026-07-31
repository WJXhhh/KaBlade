package com.wjx.kablade.specialeffect;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModMobEffects;
import com.wjx.kablade.init.ModSpecialEffects;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaDamage;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 天罚 —— 「开天剑」专属特殊效果。
 * <p>
 * 从 1.12.2 {@code SEDivinePenalty} 移植而来：
 * 持有者被攻击时反击攻击者（雷电 + 2 点伤害 + 麻痹 60 tick）；
 * 持有者攻击处于麻痹状态的目标时，伤害 ×1.4。
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DivinePenalty extends SpecialEffect {

    private static final float COUNTER_DAMAGE_BASE = 2.0F;
    private static final float COUNTER_DAMAGE_FACTOR = 0.35F;
    private static final int PARALYSIS_DURATION = 60;
    private static final int PARALYSIS_AMPLIFIER = 1;
    private static final float DAMAGE_BOOST = 1.4F;
    /** A multi-hit attack must not turn a reflected hit into a lightning storm. */
    private static final long COUNTER_COOLDOWN_TICKS = 10L;

    /** 仅屏蔽当前反击造成的同一条伤害边，避免影响伤害调用链中的其他玩家。 */
    private static final Set<CounterDamageKey> ACTIVE_COUNTER_DAMAGE = new HashSet<>();
    /** Last successful counter by directed attacker-victim edge. */
    private static final Map<CounterDamageKey, Long> LAST_COUNTER_TICK = new HashMap<>();

    public DivinePenalty() {
        super(-1, true, true);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        LivingEntity victim = event.getEntity();
        Level level = victim.level();

        // 持有者被攻击时反击（带重入守卫，防止互相反击递归）
        if (victim instanceof Player player && hasEffect(player)) {
            if (event.getSource().getEntity() instanceof LivingEntity attacker
                    && SaTargeting.canDamage(player, attacker)) {
                CounterDamageKey counterDamage = new CounterDamageKey(attacker.getUUID(), victim.getUUID());
                if (!ACTIVE_COUNTER_DAMAGE.contains(counterDamage)
                        && !isCounterOnCooldown(counterDamage, level.getGameTime())) {
                    LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
                    bolt.setPos(attacker.getX(), attacker.getY(), attacker.getZ());
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                    ACTIVE_COUNTER_DAMAGE.add(counterDamage);
                    try {
                        SaDamage.hurt(attacker, level.damageSources().playerAttack(player),
                                counterDamage(player.getMainHandItem()));
                    } finally {
                        ACTIVE_COUNTER_DAMAGE.remove(counterDamage);
                    }
                    attacker.addEffect(new MobEffectInstance(ModMobEffects.PARALYSIS.get(),
                            PARALYSIS_DURATION, PARALYSIS_AMPLIFIER));
                }
            }
        }

        // 持有者攻击麻痹目标时增伤
        if (event.getSource().getEntity() instanceof Player player && hasEffect(player)) {
            LivingEntity target = event.getEntity();
            if (!SaTargeting.canDamage(player, target)) {
                return;
            }
            if (target.hasEffect(ModMobEffects.PARALYSIS.get())) {
                event.setAmount(event.getAmount() * DAMAGE_BOOST);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        LAST_COUNTER_TICK.keySet().removeIf(key -> key.attackerUUID().equals(playerId)
                || key.victimUUID().equals(playerId));
    }

    private static boolean hasEffect(Player player) {
        ItemStack blade = player.getMainHandItem();
        if (!(blade.getItem() instanceof ItemSlashBlade)) {
            return false;
        }
        return blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.DIVINE_PENALTY.getId()))
                .orElse(false);
    }

    private static float counterDamage(ItemStack blade) {
        float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        return (COUNTER_DAMAGE_BASE + MathFunc.amplifierCalc(bladeAttack, COUNTER_DAMAGE_FACTOR)) * 2.0F;
    }

    private static boolean isCounterOnCooldown(CounterDamageKey key, long gameTime) {
        LAST_COUNTER_TICK.entrySet().removeIf(entry -> gameTime - entry.getValue() >= COUNTER_COOLDOWN_TICKS);
        Long lastCounterTick = LAST_COUNTER_TICK.get(key);
        if (lastCounterTick != null && gameTime - lastCounterTick < COUNTER_COOLDOWN_TICKS) {
            return true;
        }
        LAST_COUNTER_TICK.put(key, gameTime);
        return false;
    }

    private record CounterDamageKey(UUID attackerUUID, UUID victimUUID) {
    }
}

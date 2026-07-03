package com.wjx.kablade.specialeffect;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModMobEffects;
import com.wjx.kablade.init.ModSpecialEffects;
import com.wjx.kablade.util.MathFunc;
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
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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

    /** 反击守卫：反击本身会再次触发 LivingHurtEvent，避免双方都持开天剑时无限递归。 */
    private static boolean countering = false;

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
        if (!countering && victim instanceof Player player && hasEffect(player)) {
            if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
                bolt.setPos(attacker.getX(), attacker.getY(), attacker.getZ());
                level.addFreshEntity(bolt);
                countering = true;
                try {
                    attacker.hurt(level.damageSources().playerAttack(player),
                            counterDamage(player.getMainHandItem()));
                } finally {
                    countering = false;
                }
                attacker.addEffect(new MobEffectInstance(ModMobEffects.PARALYSIS.get(),
                        PARALYSIS_DURATION, PARALYSIS_AMPLIFIER));
            }
        }

        // 持有者攻击麻痹目标时增伤
        if (event.getSource().getEntity() instanceof Player player && hasEffect(player)) {
            LivingEntity target = event.getEntity();
            if (target.hasEffect(ModMobEffects.PARALYSIS.get())) {
                event.setAmount(event.getAmount() * DAMAGE_BOOST);
            }
        }
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
}

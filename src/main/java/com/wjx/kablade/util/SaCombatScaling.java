package com.wjx.kablade.util;

import com.wjx.kablade.compat.SstBladeCompat;
import com.wjx.kablade.compat.SridrCompat;
import mods.flammpfeil.slashblade.SlashBladeConfig;
import mods.flammpfeil.slashblade.capability.concentrationrank.CapabilityConcentrationRank;
import mods.flammpfeil.slashblade.capability.concentrationrank.IConcentrationRank.ConcentrationRanks;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Capture the same sustained offensive growth as SlashBlade melee, before SA timing splits. */
public final class SaCombatScaling {
    private SaCombatScaling() {
    }

    /** Complete multiplier for fixed/direct/custom projectile damage. */
    public static float damageMultiplier(LivingEntity owner) {
        return attackMultiplier(owner) * globalMultiplier(owner);
    }

    private static float attackMultiplier(LivingEntity owner) {
        ItemStack blade = owner.getMainHandItem();
        if (!(blade.getItem() instanceof ItemSlashBlade)) return 1.0F;
        return blade.getCapability(ItemSlashBlade.BLADESTATE).map(state -> {
            float base = state.getBaseAttackModifier();
            if (state.isBroken() || !Float.isFinite(base) || base <= 0.0F) return 1.0F;
            double attack = currentAttack(owner, blade);
            double rankBonus = SstBladeCompat.isLoaded() && owner instanceof Player
                    ? SstBladeCompat.rankBonus(rank(owner).level, Math.max(0, state.getRefine()))
                    : nativeRankBonus(owner);
            double enchantment = EnchantmentHelper.getDamageBonus(blade, MobType.UNDEFINED);
            if (SstBladeCompat.isLoaded() && owner instanceof Player) {
                attack += 5.0D * EnchantmentHelper.getSweepingDamageRatio(owner);
            }
            double ratio = (attack + rankBonus + enchantment) / base;
            return Double.isFinite(ratio) && ratio >= 0.0D ? (float) ratio : 1.0F;
        }).orElse(1.0F);
    }

    /** Account for a blade equipped/refined during this tick without mutating the owner's attributes. */
    public static double currentAttack(LivingEntity owner, ItemStack blade) {
        AttributeInstance current = owner.getAttribute(Attributes.ATTACK_DAMAGE);
        if (current == null) return 0.0D;
        AttributeInstance snapshot = new AttributeInstance(Attributes.ATTACK_DAMAGE, ignored -> {});
        snapshot.replaceFrom(current);
        snapshot.removeModifier(SridrCompat.weaponDamageUuid());
        for (var modifier : blade.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
            snapshot.removeModifier(modifier.getId());
            snapshot.addTransientModifier(modifier);
        }
        return snapshot.getValue();
    }

    private static float globalMultiplier(LivingEntity owner) {
        double multiplier = AttackManager.getSlashBladeDamageScale(owner)
                * SlashBladeConfig.SLASHBLADE_DAMAGE_MULTIPLIER.get();
        return Double.isFinite(multiplier) && multiplier >= 0.0D ? (float) multiplier : 1.0F;
    }

    /** Standard SlashBlade/CR projectile bonus; available even in forks without AttackHelper. */
    public static float nativeRankBonus(LivingEntity owner) {
        ConcentrationRanks rank = rank(owner);
        double bonus = rank.level / 2.0D;
        if (rank.level >= ConcentrationRanks.S.level && owner instanceof Player player) {
            int refine = owner.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                    .map(state -> Math.max(0, state.getRefine())).orElse(0);
            bonus = Math.max(bonus, Math.min(Math.max(0, player.experienceLevel), refine)
                    * SlashBladeConfig.REFINE_DAMAGE_MULTIPLIER.get());
        }
        return (float) bonus;
    }

    private static ConcentrationRanks rank(LivingEntity owner) {
        return owner.getCapability(CapabilityConcentrationRank.RANK_POINT)
                .map(value -> value.getRank(owner.level().getGameTime())).orElse(ConcentrationRanks.NONE);
    }

    public static float scaleDamage(LivingEntity owner, float damage) {
        return damage * damageMultiplier(owner);
    }

    public static double scaleDamage(LivingEntity owner, double damage) {
        return damage * damageMultiplier(owner);
    }

    /** Native summoned swords already apply the global SlashBlade attribute/config themselves. */
    public static float scaleSummonedSwordDamage(LivingEntity owner, float damage) {
        return damage * attackMultiplier(owner);
    }
}

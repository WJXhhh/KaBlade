package com.wjx.kablade.util;

import com.wjx.kablade.config.KabladeConfig;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;

/**
 * Shared harmful-SA target rules.
 */
public final class SaTargeting {

    private static final TargetSelector.AttackablePredicate SLASHBLADE_ATTACKABLE =
            new TargetSelector.AttackablePredicate();

    private SaTargeting() {
    }

    public static boolean canDamage(Entity owner, LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (owner != null && target == owner) {
            return false;
        }
        if (target instanceof Player && filtersPlayers()) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        return scoreboardAllowsDamage(owner, target);
    }

    public static boolean canDamageAttackable(Entity owner, LivingEntity target) {
        if (!canDamage(owner, target)) {
            return false;
        }
        return target instanceof Mob || target instanceof Player || SLASHBLADE_ATTACKABLE.test(target);
    }

    private static boolean filtersPlayers() {
        return !KabladeConfig.SPEC.isLoaded() || KabladeConfig.FILTER_PLAYERS_IN_SA_TARGETING.get();
    }

    private static boolean scoreboardAllowsDamage(Entity owner, LivingEntity target) {
        if (owner == null) {
            return true;
        }
        if (owner instanceof Player attacker && target instanceof Player playerTarget
                && !attacker.canHarmPlayer(playerTarget)) {
            return false;
        }

        Team ownerTeam = owner.getTeam();
        Team targetTeam = target.getTeam();
        return ownerTeam == null
                || targetTeam == null
                || !ownerTeam.isAlliedTo(targetTeam)
                || ownerTeam.isAllowFriendlyFire();
    }
}

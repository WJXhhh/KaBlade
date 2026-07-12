package com.wjx.kablade.specialeffect;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.KabladeCapabilities;
import com.wjx.kablade.init.ModSpecialEffects;
import com.wjx.kablade.util.SaDamage;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fuel the Ruin, Ruinous Sakura's special effect.
 *
 * <p>After Bloodfyre Frenzy is cast, the user bleeds for two seconds. Every
 * actual point of health lost in that window is retained, then converted into
 * a five-second attack bonus and an immediate fire burst.</p>
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FuelTheRuin extends SpecialEffect {

    public static final String HUD_BLEEDING_KEY = "fuel_the_ruin_bleeding";
    public static final String HUD_BUFF_KEY = "fuel_the_ruin";

    private static final int BLEED_DURATION_TICKS = 40;
    private static final int BLEED_INTERVAL_TICKS = 10;
    private static final int BUFF_DURATION_TICKS = 100;
    private static final float BLEED_HEALTH_FLOOR = 0.30F;
    private static final double RANGE = 13.0D;
    private static final double VERTICAL_BELOW = 1.25D;
    private static final double VERTICAL_ABOVE = 4.5D;
    private static final UUID ATTACK_MODIFIER_UUID =
            UUID.fromString("c6c4e511-9db1-4a8f-9f09-65d33e7d3738");

    private static final Map<UUID, BleedSession> BLEEDING_PLAYERS = new HashMap<>();

    public FuelTheRuin() {
        super(-1, true, true);
    }

    /** Starts (or restarts) the two-second health-loss window after the blade's SA. */
    public static void trigger(Player player) {
        if (player.level().isClientSide() || !hasEffect(player)) {
            return;
        }

        BLEEDING_PLAYERS.put(player.getUUID(), new BleedSession());
        removeAttackModifier(player);
        player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA).ifPresent(data -> {
            data.remove(HUD_BUFF_KEY);
            data.set(HUD_BLEEDING_KEY, 1);
        });
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        BleedSession session = BLEEDING_PLAYERS.get(player.getUUID());
        if (session != null && event.getAmount() > 0.0F) {
            // LivingDamageEvent contains the final health damage, so this also
            // includes damage not caused by this special effect during the window.
            session.lostHealth += event.getAmount();
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }

        Player player = event.player;
        tickBuff(player);

        BleedSession session = BLEEDING_PLAYERS.get(player.getUUID());
        if (session == null) {
            return;
        }
        if (!player.isAlive()) {
            endBleed(player);
            return;
        }

        session.age++;
        if (session.age % BLEED_INTERVAL_TICKS == 0) {
            applyBleedDamage(player);
        }
        if (session.age >= BLEED_DURATION_TICKS) {
            resolveBleed((ServerLevel) player.level(), player, session.lostHealth);
            endBleed(player);
        }
    }

    /**
     * Bleeding can only take health while the player is above 30% max health.
     * Clamping the final pulse to that floor also makes this self-inflicted
     * damage unable to kill the player.
     */
    private static void applyBleedDamage(Player player) {
        float healthFloor = player.getMaxHealth() * BLEED_HEALTH_FLOOR;
        float availableHealth = player.getHealth() - healthFloor;
        if (availableHealth <= 0.0F) {
            return;
        }

        float bleedDamage = Math.min((float) (player.getMaxHealth() * 0.01D), availableHealth);
        player.hurt(player.level().damageSources().magic(), bleedDamage);
    }

    private static void resolveBleed(ServerLevel level, Player player, float lostHealth) {
        if (lostHealth <= 0.0F) {
            return;
        }

        AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null) {
            return;
        }

        attack.removeModifier(ATTACK_MODIFIER_UUID);
        attack.addTransientModifier(new AttributeModifier(
                ATTACK_MODIFIER_UUID,
                "kablade.fuel_the_ruin",
                lostHealth * 2.0D,
                AttributeModifier.Operation.ADDITION));
        player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                .ifPresent(data -> data.set(HUD_BUFF_KEY, BUFF_DURATION_TICKS));

        // Read the attribute only after adding the modifier: this strike must
        // receive the same attack bonus that is displayed to the player.
        float damage = (float) (lostHealth * 0.8D * attack.getValue());
        damageNearby(level, player, damage);
        spawnFireParticles(level, player);
    }

    private static void damageNearby(ServerLevel level, Player player, float damage) {
        Vec3 origin = player.position().add(0.0D, 1.0D, 0.0D);
        AABB area = new AABB(
                player.getX() - RANGE, player.getY() - VERTICAL_BELOW, player.getZ() - RANGE,
                player.getX() + RANGE, player.getY() + VERTICAL_ABOVE, player.getZ() + RANGE);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> SaTargeting.canDamageAttackable(player, entity))) {
            Vec3 offset = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).subtract(origin);
            if (offset.horizontalDistanceSqr() <= RANGE * RANGE) {
                SaDamage.hurtNoIFrame(target, level.damageSources().playerAttack(player), damage);
            }
        }
    }

    private static void spawnFireParticles(ServerLevel level, Player player) {
        // Individual particles keep the field sparse and uneven rather than a dense, uniform cloud.
        for (int i = 0; i < 42; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double radius = Math.sqrt(level.random.nextDouble()) * RANGE;
            double x = player.getX() + Math.cos(angle) * radius;
            double y = player.getY() + 0.10D + level.random.nextDouble() * 2.6D;
            double z = player.getZ() + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.FLAME, x, y, z, 1,
                    0.0D, 0.015D + level.random.nextDouble() * 0.025D, 0.0D, 0.002D);
        }
    }

    private static void tickBuff(Player player) {
        player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA).ifPresent(data -> {
            int remaining = data.get(HUD_BUFF_KEY);
            if (remaining > 0) {
                data.set(HUD_BUFF_KEY, remaining - 1);
                if (remaining == 1) {
                    removeAttackModifier(player);
                }
            }
        });
    }

    private static void endBleed(Player player) {
        BLEEDING_PLAYERS.remove(player.getUUID());
        player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                .ifPresent(data -> data.remove(HUD_BLEEDING_KEY));
    }

    private static void removeAttackModifier(Player player) {
        AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.removeModifier(ATTACK_MODIFIER_UUID);
        }
    }

    private static boolean hasEffect(Player player) {
        ItemStack blade = player.getMainHandItem();
        if (!(blade.getItem() instanceof ItemSlashBlade)) {
            return false;
        }
        return blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.FUEL_THE_RUIN.getId()))
                .orElse(false);
    }

    private static final class BleedSession {
        private int age;
        private float lostHealth;
    }
}

package com.wjx.kablade.slasharts;

import com.wjx.kablade.Main;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Molten Blade - Thermal Cutter / Phoenix slash art. */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MoltenBladeArts extends SlashArts {

    private static final Map<UUID, LungeState> ACTIVE_LUNGES = new ConcurrentHashMap<>();

    private static final float BASE_DAMAGE = 10.0F;
    private static final float ATTACK_FACTOR = 15.0F;
    private static final float DAMAGE_MULTIPLIER = 0.5F;
    private static final int FIRE_SECONDS = 5;
    private static final double RANGE_XZ = 4.5D;
    private static final double RANGE_Y = 1.5D;

    public MoltenBladeArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide()) {
            return super.doArts(type, user);
        }
        if (type == ArtsType.Fail) {
            SaFx.cancelPiercingCharge(user);
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        ResourceLocation combo = SaFx.startPiercingCombo(user, type);
        float bladeAttack = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float damage = (BASE_DAMAGE + MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR)) * DAMAGE_MULTIPLIER;

        startLunge(level, user, damage);

        Vec3 pos = user.position();
        level.sendParticles(ParticleTypes.LAVA, pos.x, pos.y + 1.0D, pos.z,
                28, RANGE_XZ / 2.0D, RANGE_Y, RANGE_XZ / 2.0D, 0.0D);
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.LAVA_POP, SoundSource.PLAYERS, 1.0F, 1.0F);

        return combo;
    }

    private static void startLunge(ServerLevel level, LivingEntity user, float damage) {
        if (!SaFx.isPiercingLungeWindow(user)) {
            return;
        }
        long now = level.getServer().getTickCount();
        LungeState state = new LungeState(level.dimension(), user.getUUID(), damage,
                now + SaFx.PIERCING_LUNGE_TICKS, now + 1L, user.getBoundingBox());
        ACTIVE_LUNGES.put(user.getUUID(), state);
        SaFx.applyPiercingBoost(user);
        damageSweep(level, user, state);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE_LUNGES.isEmpty()) {
            return;
        }

        long now = event.getServer().getTickCount();
        ACTIVE_LUNGES.entrySet().removeIf(entry -> {
            LungeState state = entry.getValue();
            ServerLevel level = event.getServer().getLevel(state.dimension);
            if (level == null || now >= state.expiresAt) {
                return true;
            }

            Entity ownerEntity = level.getEntity(state.ownerUUID);
            if (!(ownerEntity instanceof LivingEntity user) || !user.isAlive()) {
                return true;
            }
            if (!SaFx.isPiercingLungeWindow(user)) {
                return true;
            }

            if (now < state.nextTickAt) {
                return false;
            }

            state.nextTickAt = now + 1L;
            SaFx.applyPiercingBoost(user);
            damageSweep(level, user, state);
            return false;
        });
    }

    private static void damageSweep(ServerLevel level, LivingEntity user, LungeState state) {
        AABB current = user.getBoundingBox();
        AABB projected = current.move(SaFx.flatLook(user).scale(SaFx.piercingProjection(user)));
        AABB sweep = state.lastBox.minmax(current).minmax(projected).inflate(RANGE_XZ, RANGE_Y, RANGE_XZ);
        state.lastBox = current;

        DamageSource src = user instanceof Player player
                ? level.damageSources().playerAttack(player)
                : level.damageSources().mobAttack(user);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, sweep,
                target -> isAttackable(user, target) && !state.hitTargets.contains(target.getUUID()));

        for (LivingEntity target : targets) {
            state.hitTargets.add(target.getUUID());
            target.hurt(src, state.damage);
            target.setSecondsOnFire(FIRE_SECONDS);
            if (!state.bladeDamaged) {
                user.getMainHandItem().hurtAndBreak(1, user,
                        e -> e.broadcastBreakEvent(user.getUsedItemHand()));
                state.bladeDamaged = true;
            }
        }
    }

    private static boolean isAttackable(LivingEntity user, LivingEntity target) {
        return SaTargeting.canDamageAttackable(user, target);
    }

    private static final class LungeState {
        private final ResourceKey<Level> dimension;
        private final UUID ownerUUID;
        private final float damage;
        private final long expiresAt;
        private long nextTickAt;
        private AABB lastBox;
        private boolean bladeDamaged;
        private final Set<UUID> hitTargets = new HashSet<>();

        private LungeState(ResourceKey<Level> dimension, UUID ownerUUID,
                           float damage, long expiresAt, long nextTickAt, AABB lastBox) {
            this.dimension = dimension;
            this.ownerUUID = ownerUUID;
            this.damage = damage;
            this.expiresAt = expiresAt;
            this.nextTickAt = nextTickAt;
            this.lastBox = lastBox;
        }
    }
}

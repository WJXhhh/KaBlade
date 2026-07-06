package com.wjx.kablade.slasharts;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModMobEffects;
import com.wjx.kablade.network.KabladeNetwork;
import com.wjx.kablade.network.MagChaosBladeFxPacket;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Mag Chaos Blade, the 1.12.2 SA 298 used by Mag Storm. */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MagChaosBladeArts extends SlashArts {

    private static final Map<PendingKey, PendingAttack> PENDING_ATTACKS = new ConcurrentHashMap<>();
    private static final double RANGE = 6.0D;
    private static final float IMMEDIATE_DAMAGE = 40.0F;
    private static final float DELAYED_DAMAGE = 20.0F;
    private static final float ATTACK_FACTOR = 10.0F;
    private static final int DELAY_TICKS = 6;
    private static final int PARALYSIS_DURATION = 100;
    private static final int PARALYSIS_AMPLIFIER = 3;
    private static final Vector3f ELECTRIC = new Vector3f(0.08F, 0.95F, 1.0F);

    public MagChaosBladeArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        ItemStack blade = user.getMainHandItem();
        float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float extraDamage = MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR);

        broadcastFx(user);
        spawnReleaseParticles(level, user);
        boolean hit = sweepVolume(level, user, IMMEDIATE_DAMAGE + extraDamage);
        if (hit) {
            blade.hurtAndBreak(1, user, e -> e.broadcastBreakEvent(user.getUsedItemHand()));
        }

        long dueTick = level.getServer().getTickCount() + DELAY_TICKS;
        PendingAttack attack = new PendingAttack(level.dimension(), user.getUUID(), dueTick, extraDamage);
        PENDING_ATTACKS.put(new PendingKey(user.getUUID(), dueTick), attack);

        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.9F, 1.65F);
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.55F, 1.9F);

        return super.doArts(type, user);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING_ATTACKS.isEmpty()) {
            return;
        }

        long now = event.getServer().getTickCount();
        PENDING_ATTACKS.entrySet().removeIf(entry -> {
            PendingAttack attack = entry.getValue();
            if (now < attack.dueTick()) {
                return false;
            }

            ServerLevel level = event.getServer().getLevel(attack.dimension());
            if (level == null) {
                return true;
            }
            Entity entity = level.getEntity(attack.ownerUuid());
            if (!(entity instanceof LivingEntity user) || !user.isAlive()) {
                return true;
            }

            broadcastFx(user);
            spawnReleaseParticles(level, user);
            boolean hit = rayStrike(level, user, DELAYED_DAMAGE + attack.extraDamage());
            if (hit) {
                user.getMainHandItem().hurtAndBreak(1, user,
                        e -> e.broadcastBreakEvent(user.getUsedItemHand()));
            }
            return true;
        });
    }

    private static boolean sweepVolume(ServerLevel level, LivingEntity user, float damage) {
        Vec3 look = user.getLookAngle();
        AABB box = user.getBoundingBox()
                .expandTowards(look.scale(RANGE))
                .inflate(3.0D, 1.0D, 3.0D);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, box, target -> isAttackable(user, target));
        boolean hit = false;
        for (LivingEntity target : targets) {
            hitTarget(level, user, target, damage);
            hit = true;
        }
        return hit;
    }

    private static boolean rayStrike(ServerLevel level, LivingEntity user, float damage) {
        Vec3 eye = user.getEyePosition();
        Vec3 look = user.getLookAngle();
        Vec3 end = eye.add(look.scale(RANGE));
        AABB scanBox = user.getBoundingBox()
                .expandTowards(look.scale(RANGE))
                .inflate(1.0D, 1.0D, 1.0D);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, scanBox, target -> isAttackable(user, target));

        boolean hit = false;
        for (LivingEntity target : targets) {
            AABB targetBox = target.getBoundingBox().inflate(target.getPickRadius());
            if (!targetBox.contains(eye) && targetBox.clip(eye, end).isEmpty()) {
                continue;
            }
            hitTarget(level, user, target, damage);
            hit = true;
        }
        return hit;
    }

    private static void hitTarget(ServerLevel level, LivingEntity user, LivingEntity target, float damage) {
        DamageSource source = user instanceof Player player
                ? level.damageSources().playerAttack(player)
                : level.damageSources().mobAttack(user);
        if (user instanceof Player player) {
            player.crit(target);
        }
        target.invulnerableTime = 0;
        target.hurt(source, damage);
        target.addEffect(new MobEffectInstance(ModMobEffects.PARALYSIS.get(),
                PARALYSIS_DURATION, PARALYSIS_AMPLIFIER));
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                10, target.getBbWidth() * 0.4D, target.getBbHeight() * 0.35D,
                target.getBbWidth() * 0.4D, 0.08D);
    }

    private static boolean isAttackable(LivingEntity user, LivingEntity target) {
        if (!target.isPickable() || !SaTargeting.canDamage(user, target)) {
            return false;
        }
        try {
            return SaTargeting.canDamageAttackable(user, target);
        } catch (NullPointerException ignored) {
            return false;
        }
    }

    private static void broadcastFx(LivingEntity user) {
        KabladeNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                new MagChaosBladeFxPacket(user.getId(), 4,
                        user.getX(), user.getY(), user.getZ(), user.getYRot()));
    }

    private static void spawnReleaseParticles(ServerLevel level, LivingEntity user) {
        Vec3 eye = user.getEyePosition();
        Vec3 look = user.getLookAngle();
        for (int i = 0; i < 16; i++) {
            double d = 0.35D + i * 0.36D;
            level.sendParticles(new DustParticleOptions(ELECTRIC, 1.05F),
                    eye.x + look.x * d,
                    eye.y + look.y * d,
                    eye.z + look.z * d,
                    1, 0.18D, 0.10D, 0.18D, 0.0D);
        }
        level.sendParticles(ParticleTypes.FLASH,
                user.getX() + look.x, user.getY() + user.getBbHeight() * 0.55D, user.getZ() + look.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private record PendingKey(UUID ownerUuid, long dueTick) {
    }

    private record PendingAttack(ResourceKey<Level> dimension, UUID ownerUuid, long dueTick,
                                 float extraDamage) {
    }
}

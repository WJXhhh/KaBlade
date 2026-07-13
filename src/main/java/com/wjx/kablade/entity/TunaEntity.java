package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.util.SaTargeting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.UUID;

/** Visual and damage entity for One Salty Tuna's Lethal Thrust SA. */
public class TunaEntity extends Entity {

    private static final int FIRST_HIT_TICK = 5;
    private static final int FINAL_HIT_TICK = 40;
    private static final double HIT_RANGE_XZ = 5.0D;
    private static final double HIT_RANGE_Y = 2.0D;
    private static final float FIRST_HIT_DAMAGE = 10.0F;
    private static final float FINAL_HIT_DAMAGE = 14.0F;

    private UUID ownerUUID;
    private LivingEntity owner;
    private float extraDamage;

    public TunaEntity(EntityType<? extends TunaEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public TunaEntity(Level level, LivingEntity owner, float extraDamage) {
        this(ModEntities.TUNA.get(), level);
        this.owner = owner;
        this.ownerUUID = owner.getUUID();
        this.extraDamage = extraDamage;
        this.setYRot(owner.getYRot());
    }

    public static TunaEntity spawn(ServerLevel level, LivingEntity owner, double x, double y, double z,
                                   float extraDamage) {
        TunaEntity entity = new TunaEntity(level, owner, extraDamage);
        entity.setPos(x, y, z);
        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            return;
        }

        resolveOwner();

        if (this.tickCount == FIRST_HIT_TICK) {
            damageNearby(FIRST_HIT_DAMAGE + this.extraDamage);
        }

        if (this.tickCount > FINAL_HIT_TICK) {
            ServerLevel level = (ServerLevel) this.level();
            damageNearby(FINAL_HIT_DAMAGE + this.extraDamage);
            spawnFinalParticles(level);
            level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 1.0F, 1.0F);
            this.discard();
        }
    }

    private void resolveOwner() {
        if (this.owner == null && this.ownerUUID != null && this.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.ownerUUID);
            if (entity instanceof LivingEntity living) {
                this.owner = living;
            }
        }
    }

    private void damageNearby(float damage) {
        ServerLevel level = (ServerLevel) this.level();
        AABB area = this.getBoundingBox().inflate(HIT_RANGE_XZ, HIT_RANGE_Y, HIT_RANGE_XZ);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                this::canHit);

        for (LivingEntity target : targets) {
            target.invulnerableTime = 0;
            if (this.owner instanceof Player player) {
                player.crit(target);
            }
            com.wjx.kablade.util.SaDamage.hurtSlashArtNoIFrame(target, level, this, this.owner, damage);
            hurtBladeOnce();
        }
    }

    private boolean canHit(LivingEntity target) {
        try {
            return SaTargeting.canDamageAttackable(this.owner, target);
        } catch (NullPointerException ignored) {
            return false;
        }
    }

    private DamageSource damageSource(ServerLevel level) {
        if (this.owner instanceof Player player) {
            return level.damageSources().playerAttack(player);
        }
        if (this.owner != null) {
            return level.damageSources().mobAttack(this.owner);
        }
        return level.damageSources().magic();
    }

    private void hurtBladeOnce() {
        if (this.owner == null) {
            return;
        }
        ItemStack blade = this.owner.getMainHandItem();
        if (blade.getItem() instanceof ItemSlashBlade) {
            blade.hurtAndBreak(1, this.owner,
                    entity -> entity.broadcastBreakEvent(this.owner.getUsedItemHand()));
        }
    }

    private void spawnFinalParticles(ServerLevel level) {
        for (int i = 0; i < 10; i++) {
            double x = this.getX() + (level.random.nextDouble() - 0.5D) * 4.0D;
            double y = this.getY() + 1.0D + (level.random.nextDouble() - 0.5D);
            double z = this.getZ() + (level.random.nextDouble() - 0.5D) * 4.0D;
            double dx = (level.random.nextInt(5) - 3) / 10.0D;
            double dy = (level.random.nextInt(5) - 3) / 10.0D;
            double dz = (level.random.nextInt(5) - 3) / 10.0D;
            level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, dx, dy, dz, 0.0D);
            level.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, dx, dy, dz, 0.0D);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
        if (tag.contains("ExtraDamage")) {
            this.extraDamage = tag.getFloat("ExtraDamage");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUUID != null) {
            tag.putUUID("OwnerUUID", this.ownerUUID);
        }
        tag.putFloat("ExtraDamage", this.extraDamage);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}

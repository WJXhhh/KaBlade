package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 通用可变速驱动实体 —— 1.12.2 {@code ExSaEntityDrive / EntityDriveAdd} 的移植。
 * <p>
 * 特性：初速/变速时间/末速三段速度曲线、粒子样式、颜色、多段命中、自定义音效。
 * 子类重写 {@link #onImpact(LivingEntity)} 实现不同命中效果。
 */
public class ExSlashDriveEntity extends Entity {

    // ── 同步数据 ──────────────────────────────────────────────
    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(ExSlashDriveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_ROLL =
            SynchedEntityData.defineId(ExSlashDriveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_MULTI_HIT =
            SynchedEntityData.defineId(ExSlashDriveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_CHANGE_TIME =
            SynchedEntityData.defineId(ExSlashDriveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_INITIAL_SPEED =
            SynchedEntityData.defineId(ExSlashDriveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_NEXT_SPEED =
            SynchedEntityData.defineId(ExSlashDriveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(ExSlashDriveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SCALE_X =
            SynchedEntityData.defineId(ExSlashDriveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SCALE_Y =
            SynchedEntityData.defineId(ExSlashDriveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SCALE_Z =
            SynchedEntityData.defineId(ExSlashDriveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_PARTICLE_ENABLED =
            SynchedEntityData.defineId(ExSlashDriveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_PARTICLE_STYLE =
            SynchedEntityData.defineId(ExSlashDriveEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_SOUND_NAME =
            SynchedEntityData.defineId(ExSlashDriveEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_SOUND_PLAYED =
            SynchedEntityData.defineId(ExSlashDriveEntity.class, EntityDataSerializers.BOOLEAN);

    // ── 实例字段 ──────────────────────────────────────────────
    protected LivingEntity thrower;
    protected ItemStack blade = ItemStack.EMPTY;
    protected final List<UUID> alreadyHit = new ArrayList<>();
    protected float attackDamage;

    public ExSlashDriveEntity(EntityType<? extends ExSlashDriveEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** 工厂：用默认参数创建后加入世界。 */
    public static ExSlashDriveEntity spawn(Level level, LivingEntity thrower, Vec3 pos,
                                           Vec3 direction, float damage,
                                           int color, int lifetime, float roll,
                                           boolean multiHit) {
        ExSlashDriveEntity e = new ExSlashDriveEntity(ModEntities.EX_SLASH_DRIVE.get(), level);
        e.setPos(pos.x, pos.y, pos.z);
        e.setDeltaMovement(direction);
        e.thrower = thrower;
        e.attackDamage = damage;
        e.setColor(color);
        e.setLifetime(lifetime);
        e.setRoll(roll);
        e.setMultiHit(multiHit);
        e.blade = thrower.getMainHandItem();
        if (thrower != null) {
            e.alreadyHit.add(thrower.getUUID());
        }
        level.addFreshEntity(e);
        return e;
    }

    // ── Getters/setters ──────────────────────────────────────

    public int getLifetime()               { return entityData.get(DATA_LIFETIME); }
    public void setLifetime(int v)         { entityData.set(DATA_LIFETIME, v); }
    public float getRoll()                 { return entityData.get(DATA_ROLL); }
    public void setRoll(float v)           { entityData.set(DATA_ROLL, v); }
    public boolean isMultiHit()            { return entityData.get(DATA_MULTI_HIT); }
    public void setMultiHit(boolean v)     { entityData.set(DATA_MULTI_HIT, v); }
    public int getChangeTime()             { return entityData.get(DATA_CHANGE_TIME); }
    public void setChangeTime(int v)       { entityData.set(DATA_CHANGE_TIME, v); }
    public float getInitialSpeed()         { return entityData.get(DATA_INITIAL_SPEED); }
    public void setInitialSpeed(float v)   { entityData.set(DATA_INITIAL_SPEED, v); }
    public float getNextSpeed()            { return entityData.get(DATA_NEXT_SPEED); }
    public void setNextSpeed(float v)      { entityData.set(DATA_NEXT_SPEED, v); }
    public int getColor()                  { return entityData.get(DATA_COLOR); }
    public void setColor(int v)            { entityData.set(DATA_COLOR, v); }
    public float getScaleX()               { return entityData.get(DATA_SCALE_X); }
    public void setScaleX(float v)         { entityData.set(DATA_SCALE_X, v); }
    public float getScaleY()               { return entityData.get(DATA_SCALE_Y); }
    public void setScaleY(float v)         { entityData.set(DATA_SCALE_Y, v); }
    public float getScaleZ()               { return entityData.get(DATA_SCALE_Z); }
    public void setScaleZ(float v)         { entityData.set(DATA_SCALE_Z, v); }
    public boolean isParticleEnabled()     { return entityData.get(DATA_PARTICLE_ENABLED); }
    public void setParticleEnabled(boolean v) { entityData.set(DATA_PARTICLE_ENABLED, v); }
    public String getParticleStyle()       { return entityData.get(DATA_PARTICLE_STYLE); }
    public void setParticleStyle(String v) { entityData.set(DATA_PARTICLE_STYLE, v); }
    public String getSoundName()           { return entityData.get(DATA_SOUND_NAME); }
    public void setSoundName(String v)     { entityData.set(DATA_SOUND_NAME, v); }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_LIFETIME, 20);
        entityData.define(DATA_ROLL, 0.0F);
        entityData.define(DATA_MULTI_HIT, false);
        entityData.define(DATA_CHANGE_TIME, 0);
        entityData.define(DATA_INITIAL_SPEED, 1.05F);
        entityData.define(DATA_NEXT_SPEED, 0.0F);
        entityData.define(DATA_COLOR, 0xFFFFFF);
        entityData.define(DATA_SCALE_X, 0.25F);
        entityData.define(DATA_SCALE_Y, 1.0F);
        entityData.define(DATA_SCALE_Z, 1.0F);
        entityData.define(DATA_PARTICLE_ENABLED, false);
        entityData.define(DATA_PARTICLE_STYLE, "");
        entityData.define(DATA_SOUND_NAME, "");
        entityData.define(DATA_SOUND_PLAYED, false);
    }

    @Override
    public void tick() {
        super.tick();

        if (tickCount >= getLifetime()) {
            discard();
            return;
        }

        // 播放音效（到达 changeTime 时触发）
        if (!level().isClientSide() && getChangeTime() > 0 && tickCount == getChangeTime()) {
            playSoundEffect();
        }

        // 速度控制
        Vec3 motion = getDeltaMovement();
        if (getChangeTime() > 0 && tickCount >= getChangeTime()) {
            float nextSpd = getNextSpeed();
            if (nextSpd > 0.01F) {
                motion = motion.scale(nextSpd);
            }
        } else {
            float initSpd = getInitialSpeed();
            if (initSpd >= 1.05F) {
                motion = motion.scale(initSpd);
            } else {
                motion = motion.scale(0.1F);
            }
        }
        setDeltaMovement(motion);

        // 移动
        Vec3 pos = position().add(motion);
        setPos(pos.x, pos.y, pos.z);

        // 碰撞方块检测
        Iterable<VoxelShape> collisions = level().getBlockCollisions(this, getBoundingBox());
        if (collisions.iterator().hasNext()) {
            discard();
            return;
        }

        // 命中检测（服务端）
        if (!level().isClientSide()) {
            doHitDetection();
        }

        // 粒子（双端：服务端通过 sendParticles，客户端直接 addParticle）
        if (isParticleEnabled()) {
            spawnTrailParticles();
        }
    }

    // ── 命中检测 ──────────────────────────────────────────────
    protected void doHitDetection() {
        double ambit = 1.5;
        AABB bb = getBoundingBox().inflate(ambit);
        List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class, bb,
                e -> e.isAlive() && e != thrower && !alreadyHit.contains(e.getUUID()));

        for (LivingEntity target : targets) {
            if (isMultiHit() || !alreadyHit.contains(target.getUUID())) {
                if (!isMultiHit()) {
                    alreadyHit.add(target.getUUID());
                }
                onImpact(target);
            }
        }
    }

    /**
     * 命中回调 —— 子类重写以实现火焰/水流等特殊效果。
     */
    protected void onImpact(LivingEntity target) {
        if (!level().isClientSide()) {
            target.invulnerableTime = 0;
            target.hurt(damageSource(), attackDamage);
            hitBlade(target);
        }
    }

    protected DamageSource damageSource() {
        return level().damageSources().indirectMagic(this, thrower);
    }

    /** 触发拔刀剑的命中效果（如 SA 判定、特殊效果触发等）。 */
    protected void hitBlade(LivingEntity target) {
        // 1.20.1 SlashBlade 的 ItemSlashBlade.hurtEnemy 由 mixin 处理，
        // 直接触发标准伤害即可。
    }

    // ── 音效 ──────────────────────────────────────────────────
    protected void playSoundEffect() {
        if (entityData.get(DATA_SOUND_PLAYED)) return;
        String name = getSoundName();
        if (name.isEmpty()) return;
        SoundEvent ev = ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.tryParse(name));
        if (ev != null && thrower != null) {
            level().playSound(null, thrower.getX(), thrower.getY(), thrower.getZ(),
                    ev, SoundSource.PLAYERS, 1.0F, 1.5F);
        }
        entityData.set(DATA_SOUND_PLAYED, true);
    }

    // ── 粒子 ──────────────────────────────────────────────────
    protected void spawnTrailParticles() {
        if (!(level() instanceof ServerLevel server)) return;
        String style = getParticleStyle();
        if (style.isEmpty()) return;
        ParticleOptions particle = parseParticle(style);
        if (particle == null) return;
        for (int i = 0; i < 3; i++) {
            double rx = (random.nextDouble() - 0.5) * getBbWidth();
            double ry = (random.nextDouble() - 0.5) * getBbHeight();
            double rz = (random.nextDouble() - 0.5) * getBbWidth();
            server.sendParticles(particle, getX() + rx, getY() + ry, getZ() + rz,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    @Nullable
    private static ParticleOptions parseParticle(String name) {
        return switch (name) {
            case "LAVA" -> ParticleTypes.LAVA;
            case "WATER_SPLASH" -> ParticleTypes.SPLASH;
            case "FLAME" -> ParticleTypes.FLAME;
            case "EXPLOSION_NORMAL" -> ParticleTypes.EXPLOSION;
            case "SPELL_WITCH" -> ParticleTypes.WITCH;
            case "DRIP_LAVA" -> ParticleTypes.DRIPPING_LAVA;
            case "DRIP_WATER" -> ParticleTypes.DRIPPING_WATER;
            default -> ParticleTypes.EXPLOSION;
        };
    }

    // ── NBT ──────────────────────────────────────────────────
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

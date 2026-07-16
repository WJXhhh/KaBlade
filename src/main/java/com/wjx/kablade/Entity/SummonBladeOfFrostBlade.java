package com.wjx.kablade.Entity;

import com.wjx.kablade.util.TargetingUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.registry.IThrowableEntity;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Six-wave ice blade used by the Frost Blade SA. */
public class SummonBladeOfFrostBlade extends Entity implements IThrowableEntity {
    public static final int PHASE_WAITING = 0;
    public static final int PHASE_ARMING = 1;
    public static final int PHASE_FLYING = 2;
    public static final int PHASE_IMPACT = 3;

    private static final int[] WAVE_DELAYS = {0, 2, 4, 6, 8, 11};
    private static final double[][] SPAWN_OFFSETS = {
            {-0.95D, -0.65D, 0.18D},
            {0.85D, -0.30D, 0.28D},
            {-1.55D, 0.10D, 0.16D},
            {1.45D, 0.45D, 0.24D},
            {-0.55D, 0.85D, 0.34D},
            {0.55D, 1.10D, 0.45D}
    };

    private static final int ACTIVE_LIFETIME = 36;
    private static final int NORMAL_IMPACT_LIFETIME = 8;
    private static final int FINISHER_IMPACT_LIFETIME = 11;
    private static final double FLIGHT_SPEED = 1.75D;

    private static final DataParameter<Integer> OWNER_ID = EntityDataManager.createKey(
            SummonBladeOfFrostBlade.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> TARGET_ID = EntityDataManager.createKey(
            SummonBladeOfFrostBlade.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> WAVE = EntityDataManager.createKey(
            SummonBladeOfFrostBlade.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> PHASE = EntityDataManager.createKey(
            SummonBladeOfFrostBlade.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> COLOR = EntityDataManager.createKey(
            SummonBladeOfFrostBlade.class, DataSerializers.VARINT);

    private EntityLivingBase thrower;
    private UUID throwerUuid;
    private UUID targetUuid;
    private float attackDamage;
    private int activeTicks;
    private int phaseTicks;
    private Vec3d fallbackDirection = new Vec3d(0.0D, 0.0D, 1.0D);

    private int clientPhase = -1;
    private int clientPhaseTicks;

    public SummonBladeOfFrostBlade(World world) {
        super(world);
        setSize(0.35F, 0.35F);
        noClip = true;
        setNoGravity(true);
        isImmuneToFire = true;
    }

    public SummonBladeOfFrostBlade(World world, EntityLivingBase thrower, @Nullable EntityLivingBase target,
                                   float attackDamage, int wave, Vec3d fallbackDirection) {
        this(world);
        setThrower(thrower);
        setTarget(target);
        this.attackDamage = attackDamage;
        setWave(wave);
        if (fallbackDirection != null && fallbackDirection.lengthSquared() > 1.0E-8D) {
            this.fallbackDirection = fallbackDirection.normalize();
        }
        updateFormationPosition(thrower);
        if (getWaveDelay() == 0) {
            activate(thrower, target);
        }
    }

    @Override
    protected void entityInit() {
        dataManager.register(OWNER_ID, -1);
        dataManager.register(TARGET_ID, -1);
        dataManager.register(WAVE, 0);
        dataManager.register(PHASE, PHASE_WAITING);
        dataManager.register(COLOR, 0x20DDF4);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (world.isRemote) {
            updateClientEffects();
            return;
        }

        EntityLivingBase owner = getOwnerEntity();
        if (owner == null || !owner.isEntityAlive() || owner.world != world) {
            setDead();
            return;
        }

        switch (getPhase()) {
            case PHASE_WAITING:
                EntityLivingBase waitingTarget = getTargetEntity();
                if (getTargetId() > 0 && (waitingTarget == null || !waitingTarget.isEntityAlive())) {
                    setDead();
                    return;
                }
                updateFormationPosition(owner);
                if (ticksExisted >= getWaveDelay()) {
                    activate(owner, waitingTarget);
                }
                break;
            case PHASE_ARMING:
                tickArming();
                break;
            case PHASE_FLYING:
                tickFlying();
                break;
            case PHASE_IMPACT:
                phaseTicks++;
                if (phaseTicks >= (isFinisher() ? FINISHER_IMPACT_LIFETIME : NORMAL_IMPACT_LIFETIME)) {
                    setDead();
                }
                break;
            default:
                setDead();
        }
    }

    private void activate(EntityLivingBase owner, @Nullable EntityLivingBase target) {
        updateFormationPosition(owner);
        Vec3d direction = target == null
                ? fallbackDirection
                : target.getPositionVector().add(new Vec3d(0.0D, target.height * 0.52D, 0.0D))
                .subtract(getPositionVector());
        setDirection(direction);
        activeTicks = 0;
        phaseTicks = 0;
        setPhase(target == null ? PHASE_FLYING : PHASE_ARMING);

        world.playSound(null, posX, posY, posZ, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                SoundCategory.PLAYERS, isFinisher() ? 0.78F : 0.48F,
                isFinisher() ? 1.25F : 1.55F + getWave() * 0.025F);
    }

    private void tickArming() {
        activeTicks++;
        phaseTicks++;
        if (activeTicks >= ACTIVE_LIFETIME) {
            setDead();
            return;
        }

        EntityLivingBase target = getTargetEntity();
        if (target == null || !target.isEntityAlive()) {
            setDead();
            return;
        }
        turnToward(target, phaseTicks);
        if (phaseTicks >= 2) {
            phaseTicks = 0;
            setPhase(PHASE_FLYING);
        }
    }

    private void tickFlying() {
        activeTicks++;
        if (activeTicks >= ACTIVE_LIFETIME) {
            setDead();
            return;
        }

        Vec3d start = getPositionVector();
        Vec3d end = start.add(new Vec3d(motionX, motionY, motionZ));
        RayTraceResult blockHit = world.rayTraceBlocks(start, end, false, true, false);
        Vec3d collisionEnd = blockHit != null && blockHit.hitVec != null ? blockHit.hitVec : end;
        EntityLivingBase hit = findEntityHit(start, collisionEnd);
        if (hit != null) {
            impact(hit);
            return;
        }
        if (blockHit != null) {
            setDead();
            return;
        }

        move(MoverType.SELF, motionX, motionY, motionZ);
    }

    @Nullable
    private EntityLivingBase findEntityHit(Vec3d start, Vec3d end) {
        EntityLivingBase owner = getOwnerEntity();
        if (owner == null) {
            return null;
        }

        double inflate = isFinisher() ? 1.25D : 0.95D;
        EntityLivingBase lockedTarget = getTargetEntity();
        if (getTargetId() > 0) {
            return intersectsPath(lockedTarget, owner, start, end, inflate) ? lockedTarget : null;
        }

        AxisAlignedBB sweep = getEntityBoundingBox().union(getEntityBoundingBox().offset(
                end.x - start.x, end.y - start.y, end.z - start.z)).grow(inflate);
        List<EntityLivingBase> candidates = world.getEntitiesWithinAABB(EntityLivingBase.class, sweep,
                entity -> TargetingUtil.canDamage(owner, entity));
        return candidates.stream()
                .filter(entity -> intersectsPath(entity, owner, start, end, inflate))
                .min(Comparator.comparingDouble(entity -> pathDistanceSq(entity, start, end, inflate)))
                .orElse(null);
    }

    private boolean intersectsPath(@Nullable EntityLivingBase target, EntityLivingBase owner,
                                   Vec3d start, Vec3d end, double inflate) {
        return target != null && TargetingUtil.canDamage(owner, target)
                && pathDistanceSq(target, start, end, inflate) < Double.MAX_VALUE;
    }

    private double pathDistanceSq(EntityLivingBase target, Vec3d start, Vec3d end, double inflate) {
        AxisAlignedBB box = target.getEntityBoundingBox().grow(inflate);
        if (box.contains(start)) {
            return 0.0D;
        }
        RayTraceResult hit = box.calculateIntercept(start, end);
        return hit != null && hit.hitVec != null ? start.squareDistanceTo(hit.hitVec) : Double.MAX_VALUE;
    }

    private void impact(EntityLivingBase target) {
        EntityLivingBase owner = getOwnerEntity();
        if (owner == null || !TargetingUtil.canDamage(owner, target)) {
            setDead();
            return;
        }

        target.hurtTime = 0;
        target.hurtResistantTime = 0;
        boolean damaged = target.attackEntityFrom(
                new EntityDamageSourceIndirect("indirectMagic", this, owner).setMagicDamage(), attackDamage);
        target.hurtResistantTime = 0;
        if (damaged) {
            target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 2));
            target.motionX *= 0.35D;
            target.motionY *= 0.35D;
            target.motionZ *= 0.35D;
            target.velocityChanged = true;
        }

        setPosition(target.posX, target.posY + target.height * 0.52D, target.posZ);
        motionX = 0.0D;
        motionY = 0.0D;
        motionZ = 0.0D;
        phaseTicks = 0;
        setPhase(PHASE_IMPACT);
        world.setEntityState(this, (byte) 3);

        world.playSound(null, posX, posY, posZ, SoundEvents.BLOCK_GLASS_BREAK,
                SoundCategory.PLAYERS, isFinisher() ? 1.0F : 0.62F,
                isFinisher() ? 0.88F : 1.32F);
        world.playSound(null, posX, posY, posZ, SoundEvents.BLOCK_NOTE_CHIME,
                SoundCategory.PLAYERS, isFinisher() ? 0.9F : 0.45F,
                isFinisher() ? 0.72F : 1.55F);
    }

    private void turnToward(EntityLivingBase target, float maxDegrees) {
        Vec3d direction = target.getPositionVector().add(
                        new Vec3d(0.0D, target.getEyeHeight() * 0.75D, 0.0D))
                .subtract(getPositionVector()).normalize();
        float desiredYaw = (float) (MathHelper.atan2(-direction.x, direction.z) * 180.0D / Math.PI);
        float horizontal = MathHelper.sqrt(direction.x * direction.x + direction.z * direction.z);
        float desiredPitch = (float) (MathHelper.atan2(-direction.y, horizontal) * 180.0D / Math.PI);
        rotationYaw += MathHelper.clamp(MathHelper.wrapDegrees(desiredYaw - rotationYaw), -maxDegrees, maxDegrees);
        rotationPitch += MathHelper.clamp(MathHelper.wrapDegrees(desiredPitch - rotationPitch), -maxDegrees, maxDegrees);
        setMotionFromRotation();
    }

    private void setDirection(Vec3d direction) {
        if (direction == null || direction.lengthSquared() < 1.0E-8D) {
            direction = new Vec3d(0.0D, 0.0D, 1.0D);
        }
        direction = direction.normalize();
        rotationYaw = (float) (MathHelper.atan2(-direction.x, direction.z) * 180.0D / Math.PI);
        double horizontal = MathHelper.sqrt(direction.x * direction.x + direction.z * direction.z);
        rotationPitch = (float) (MathHelper.atan2(-direction.y, horizontal) * 180.0D / Math.PI);
        prevRotationYaw = rotationYaw;
        prevRotationPitch = rotationPitch;
        setMotionFromRotation();
    }

    private void setMotionFromRotation() {
        double yaw = Math.toRadians(rotationYaw);
        double pitch = Math.toRadians(rotationPitch);
        double horizontal = Math.cos(pitch);
        motionX = -Math.sin(yaw) * horizontal * FLIGHT_SPEED;
        motionY = -Math.sin(pitch) * FLIGHT_SPEED;
        motionZ = Math.cos(yaw) * horizontal * FLIGHT_SPEED;
        velocityChanged = true;
    }

    private void updateFormationPosition(EntityLivingBase owner) {
        float yaw = (float) Math.toRadians(owner.rotationYaw);
        Vec3d forward = new Vec3d(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        Vec3d right = new Vec3d(-forward.z, 0.0D, forward.x);
        double[] offset = SPAWN_OFFSETS[getWave()];
        Vec3d position = owner.getPositionVector()
                .add(right.scale(offset[0]))
                .add(forward.scale(offset[1]))
                .add(new Vec3d(0.0D, offset[2], 0.0D));
        setPosition(position.x, position.y, position.z);
    }

    private void updateClientEffects() {
        int phase = getPhase();
        if (phase != clientPhase) {
            clientPhase = phase;
            clientPhaseTicks = 0;
        } else {
            clientPhaseTicks++;
        }

        if (phase == PHASE_FLYING && getMotionVector().lengthSquared() > 0.01D) {
            Vec3d tail = getMotionVector().normalize().scale(-0.38D);
            world.spawnParticle(EnumParticleTypes.SNOW_SHOVEL,
                    posX + tail.x, posY + 0.08D + tail.y, posZ + tail.z,
                    tail.x * 0.06D, 0.002D, tail.z * 0.06D);
            if ((ticksExisted & 1) == 0) {
                world.spawnParticle(EnumParticleTypes.END_ROD,
                        posX + tail.x * 1.5D, posY + 0.08D + tail.y * 1.5D,
                        posZ + tail.z * 1.5D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private Vec3d getMotionVector() {
        return new Vec3d(motionX, motionY, motionZ);
    }

    @Override
    public void handleStatusUpdate(byte id) {
        if (id != 3) {
            super.handleStatusUpdate(id);
            return;
        }

        int snowCount = isFinisher() ? 28 : 16;
        int rodCount = isFinisher() ? 16 : 8;
        double spread = isFinisher() ? 0.72D : 0.48D;
        for (int i = 0; i < snowCount; i++) {
            world.spawnParticle(EnumParticleTypes.SNOW_SHOVEL,
                    posX + (rand.nextDouble() - 0.5D) * spread * 2.0D,
                    posY + (rand.nextDouble() - 0.5D) * spread * 2.0D,
                    posZ + (rand.nextDouble() - 0.5D) * spread * 2.0D,
                    (rand.nextDouble() - 0.5D) * 0.18D,
                    (rand.nextDouble() - 0.5D) * 0.18D,
                    (rand.nextDouble() - 0.5D) * 0.18D);
        }
        for (int i = 0; i < rodCount; i++) {
            world.spawnParticle(EnumParticleTypes.END_ROD,
                    posX + (rand.nextDouble() - 0.5D) * spread,
                    posY + (rand.nextDouble() - 0.5D) * spread,
                    posZ + (rand.nextDouble() - 0.5D) * spread,
                    (rand.nextDouble() - 0.5D) * 0.12D,
                    (rand.nextDouble() - 0.5D) * 0.12D,
                    (rand.nextDouble() - 0.5D) * 0.12D);
        }
        world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, posX, posY, posZ, 0.0D, 0.0D, 0.0D);
    }

    @Nullable
    private EntityLivingBase getOwnerEntity() {
        if (thrower != null && thrower.isEntityAlive() && thrower.world == world) {
            return thrower;
        }
        Entity entity = world.getEntityByID(dataManager.get(OWNER_ID));
        if (!(entity instanceof EntityLivingBase) && throwerUuid != null && world instanceof WorldServer) {
            entity = ((WorldServer) world).getEntityFromUuid(throwerUuid);
        }
        thrower = entity instanceof EntityLivingBase ? (EntityLivingBase) entity : null;
        return thrower;
    }

    @Nullable
    private EntityLivingBase getTargetEntity() {
        Entity entity = world.getEntityByID(getTargetId());
        if (!(entity instanceof EntityLivingBase) && targetUuid != null && world instanceof WorldServer) {
            entity = ((WorldServer) world).getEntityFromUuid(targetUuid);
        }
        return entity instanceof EntityLivingBase ? (EntityLivingBase) entity : null;
    }

    private void setTarget(@Nullable EntityLivingBase target) {
        dataManager.set(TARGET_ID, target == null ? -1 : target.getEntityId());
        targetUuid = target == null ? null : target.getUniqueID();
    }

    private int getTargetId() {
        return dataManager.get(TARGET_ID);
    }

    public int getPhase() {
        return dataManager.get(PHASE);
    }

    private void setPhase(int phase) {
        dataManager.set(PHASE, phase);
    }

    public int getWave() {
        return MathHelper.clamp(dataManager.get(WAVE), 0, WAVE_DELAYS.length - 1);
    }

    private void setWave(int wave) {
        dataManager.set(WAVE, MathHelper.clamp(wave, 0, WAVE_DELAYS.length - 1));
    }

    private int getWaveDelay() {
        return WAVE_DELAYS[getWave()];
    }

    public boolean isFinisher() {
        return getWave() == WAVE_DELAYS.length - 1;
    }

    public int getColor() {
        return dataManager.get(COLOR);
    }

    public void setColor(int color) {
        dataManager.set(COLOR, color);
    }

    public int getClientPhaseTicks() {
        return clientPhaseTicks;
    }

    @Override
    public Entity getThrower() {
        return getOwnerEntity();
    }

    @Override
    public void setThrower(Entity entity) {
        thrower = entity instanceof EntityLivingBase ? (EntityLivingBase) entity : null;
        throwerUuid = thrower == null ? null : thrower.getUniqueID();
        dataManager.set(OWNER_ID, thrower == null ? -1 : thrower.getEntityId());
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        attackDamage = compound.getFloat("AttackDamage");
        activeTicks = compound.getInteger("ActiveTicks");
        phaseTicks = compound.getInteger("PhaseTicks");
        setWave(compound.getInteger("Wave"));
        setPhase(compound.getInteger("Phase"));
        setColor(compound.getInteger("Color"));
        fallbackDirection = new Vec3d(compound.getDouble("FallbackX"), compound.getDouble("FallbackY"),
                compound.getDouble("FallbackZ"));
        if (compound.hasUniqueId("Owner")) {
            throwerUuid = compound.getUniqueId("Owner");
        }
        if (compound.hasUniqueId("Target")) {
            targetUuid = compound.getUniqueId("Target");
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        compound.setFloat("AttackDamage", attackDamage);
        compound.setInteger("ActiveTicks", activeTicks);
        compound.setInteger("PhaseTicks", phaseTicks);
        compound.setInteger("Wave", getWave());
        compound.setInteger("Phase", getPhase());
        compound.setInteger("Color", getColor());
        compound.setDouble("FallbackX", fallbackDirection.x);
        compound.setDouble("FallbackY", fallbackDirection.y);
        compound.setDouble("FallbackZ", fallbackDirection.z);
        if (throwerUuid != null) {
            compound.setUniqueId("Owner", throwerUuid);
        }
        if (targetUuid != null) {
            compound.setUniqueId("Target", targetUuid);
        }
    }
}

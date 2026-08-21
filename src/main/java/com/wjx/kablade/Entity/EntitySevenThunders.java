package com.wjx.kablade.Entity;

import com.wjx.kablade.util.TargetingUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;

/** 唤霆霓/鸣雷神的同步锚点与 54 tick 服务端时间轴。 */
public class EntitySevenThunders extends Entity {
    public static final int LIFETIME = 54;
    public static final int MODE_THUNDERBOLT_CALL = 0;
    public static final int MODE_NARUKAMI_DIVINITY = 1;

    private static final DataParameter<Integer> OWNER_ID = EntityDataManager.createKey(
            EntitySevenThunders.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> TARGET_ID = EntityDataManager.createKey(
            EntitySevenThunders.class, DataSerializers.VARINT);
    private static final DataParameter<Float> DAMAGE = EntityDataManager.createKey(
            EntitySevenThunders.class, DataSerializers.FLOAT);
    private static final DataParameter<Integer> MODE = EntityDataManager.createKey(
            EntitySevenThunders.class, DataSerializers.VARINT);
    private static final DataParameter<Float> DIR_X = EntityDataManager.createKey(
            EntitySevenThunders.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> DIR_Y = EntityDataManager.createKey(
            EntitySevenThunders.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> DIR_Z = EntityDataManager.createKey(
            EntitySevenThunders.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> TARGET_X = EntityDataManager.createKey(
            EntitySevenThunders.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> TARGET_Y = EntityDataManager.createKey(
            EntitySevenThunders.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> TARGET_Z = EntityDataManager.createKey(
            EntitySevenThunders.class, DataSerializers.FLOAT);

    public EntitySevenThunders(World world) {
        super(world); setSize(0.2F, 0.2F); noClip = true; setNoGravity(true); ignoreFrustumCheck = true;
    }

    public static boolean isCasting(EntityLivingBase owner, int mode) {
        if (owner == null || owner.world == null) return false;
        for (Entity entity : owner.world.loadedEntityList) {
            if (entity instanceof EntitySevenThunders) {
                EntitySevenThunders cast = (EntitySevenThunders) entity;
                if (!cast.isDead && cast.getMode() == mode
                        && cast.dataManager.get(OWNER_ID) == owner.getEntityId()) return true;
            }
        }
        return false;
    }

    public static EntitySevenThunders spawn(World world, EntityPlayer owner, Entity target,
                                             Vec3d targetAnchor, Vec3d direction,
                                             float damage, int mode) {
        EntitySevenThunders entity = new EntitySevenThunders(world);
        entity.setPosition(owner.posX, owner.posY, owner.posZ);
        entity.dataManager.set(OWNER_ID, owner.getEntityId());
        entity.dataManager.set(TARGET_ID, target == null ? -1 : target.getEntityId());
        entity.dataManager.set(DAMAGE, damage); entity.dataManager.set(MODE, mode);
        Vec3d dir = direction.lengthSquared() < 1.0E-8D ? new Vec3d(0, 0, 1) : direction.normalize();
        entity.dataManager.set(DIR_X, (float) dir.x); entity.dataManager.set(DIR_Y, (float) dir.y);
        entity.dataManager.set(DIR_Z, (float) dir.z);
        Vec3d offset = targetAnchor.subtract(entity.getPositionVector());
        entity.dataManager.set(TARGET_X, (float) offset.x); entity.dataManager.set(TARGET_Y, (float) offset.y);
        entity.dataManager.set(TARGET_Z, (float) offset.z);
        world.spawnEntity(entity);
        return entity;
    }

    @Override protected void entityInit() {
        dataManager.register(OWNER_ID, -1); dataManager.register(TARGET_ID, -1);
        dataManager.register(DAMAGE, 1.0F); dataManager.register(MODE, MODE_THUNDERBOLT_CALL);
        dataManager.register(DIR_X, 0F); dataManager.register(DIR_Y, 0F); dataManager.register(DIR_Z, 1F);
        dataManager.register(TARGET_X, 0F); dataManager.register(TARGET_Y, 1.25F); dataManager.register(TARGET_Z, 5F);
    }

    public int getMode() { return dataManager.get(MODE); }
    public float getBaseDamage() { return dataManager.get(DAMAGE); }
    public long getSeed() { return (long) getEntityId() * 341873128712L; }
    public Vec3d getDirection() {
        Vec3d dir = new Vec3d(dataManager.get(DIR_X), dataManager.get(DIR_Y), dataManager.get(DIR_Z));
        return dir.lengthSquared() < 1.0E-8D ? new Vec3d(0, 0, 1) : dir.normalize();
    }
    public Vec3d getStoredTargetAnchor() { return getPositionVector().add(
            dataManager.get(TARGET_X), dataManager.get(TARGET_Y), dataManager.get(TARGET_Z)); }
    public Vec3d getTargetAnchor() {
        Entity target = world.getEntityByID(dataManager.get(TARGET_ID));
        return target != null && target.isEntityAlive()
                ? target.getEntityBoundingBox().getCenter() : getStoredTargetAnchor();
    }
    public Vec3d getTargetAnchor(float partialTick) {
        Entity target = world.getEntityByID(dataManager.get(TARGET_ID));
        if (target == null || !target.isEntityAlive()) return getStoredTargetAnchor();
        Vec3d center = target.getEntityBoundingBox().getCenter();
        return center.add((target.prevPosX - target.posX) * (1.0F - partialTick),
                (target.prevPosY - target.posY) * (1.0F - partialTick),
                (target.prevPosZ - target.posZ) * (1.0F - partialTick));
    }
    public Vec3d getOwnerAnchor(float partialTick) {
        EntityLivingBase owner = getOwner();
        if (owner == null || !owner.isEntityAlive()) return getPositionVector();
        return new Vec3d(owner.prevPosX + (owner.posX - owner.prevPosX) * partialTick,
                owner.prevPosY + (owner.posY - owner.prevPosY) * partialTick,
                owner.prevPosZ + (owner.posZ - owner.prevPosZ) * partialTick);
    }
    public EntityLivingBase getOwner() {
        Entity entity = world.getEntityByID(dataManager.get(OWNER_ID));
        return entity instanceof EntityLivingBase ? (EntityLivingBase) entity : null;
    }

    @Override public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) return;
        EntityLivingBase owner = getOwner();
        if (owner == null || !owner.isEntityAlive()) { setDead(); return; }
        WorldServer server = (WorldServer) world;
        playSounds(server);
        if (getMode() == MODE_THUNDERBOLT_CALL) spawnParticles(server);
        if (getMode() == MODE_THUNDERBOLT_CALL) {
            if (ticksExisted == 7) thunderHit(owner, getBaseDamage() * 0.18F, false);
            if (ticksExisted == 16) thunderHit(owner, getBaseDamage() * 0.82F, true);
        } else {
            int[] times = {5, 9, 10, 24, 25, 26, 28, 29, 31};
            float[] weights = {0.12F, 0.14F, 0.14F, 0.08F, 0.08F, 0.08F, 0.08F, 0.08F, 0.20F};
            for (int i = 0; i < times.length; i++) if (ticksExisted == times[i]) narukamiHit(owner, i, getBaseDamage() * weights[i]);
        }
        if (ticksExisted >= LIFETIME) setDead();
    }

    private void thunderHit(EntityLivingBase owner, float damage, boolean cross) {
        List<Entity> targets = cross ? crossTargets(owner) : radiusTargets(owner, getTargetAnchor(), 3.6D);
        Vec3d anchor = getTargetAnchor();
        for (Entity receiver : targets) {
            EntityLivingBase target = TargetingUtil.getSelectionTarget(receiver);
            if (target == null) continue;
            target.hurtResistantTime = 0;
            if (!receiver.attackEntityFrom(source(owner), damage)) continue;
            target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 72, 8, false, true));
            target.getEntityData().setInteger("kabladeNarukamiTicks", 300);
            Vec3d impulse = cross ? getDirection().scale(0.72D).add(0, 0.16D, 0)
                    : anchor.subtract(target.getPositionVector()).normalize().scale(0.12D).add(0, 0.46D, 0);
            target.motionX = target.motionX * 0.4D + impulse.x;
            target.motionY = target.motionY * 0.4D + impulse.y;
            target.motionZ = target.motionZ * 0.4D + impulse.z;
            target.velocityChanged = true;
        }
    }

    private void narukamiHit(EntityLivingBase owner, int index, float damage) {
        Vec3d center = index == 0 ? getTargetAnchor() : owner.getPositionVector().add(0, 1.15D, 0);
        double radius = index == 0 ? 3.25D : index < 3 ? 6.0D : 4.35D;
        List<Entity> receivers = radiusTargets(owner, center, radius);
        if (index > 0 && index < 3) {
            List<Entity> merged = new ArrayList<>(receivers);
            merged.addAll(radiusTargets(owner, getTargetAnchor(), 4.55D));
            Entity locked = world.getEntityByID(dataManager.get(TARGET_ID));
            if (locked != null && TargetingUtil.canSelectForDamage(owner, locked)) merged.add(locked);
            receivers = TargetingUtil.getDistinctDamageTargets(merged);
        }
        boolean damagedAny = false;
        for (Entity receiver : receivers) {
            EntityLivingBase target = TargetingUtil.getSelectionTarget(receiver);
            if (target == null) continue;
            target.hurtResistantTime = 0;
            if (!receiver.attackEntityFrom(source(owner), damage)) continue;
            damagedAny = true;
            Vec3d push = target.getPositionVector().subtract(owner.getPositionVector());
            if (push.lengthSquared() > 1.0E-6D) {
                double strength = index == 8 ? 0.72D : 0.18D;
                push = push.normalize().scale(strength).add(0, 0.10D, 0);
                target.motionX = target.motionX * 0.5D + push.x;
                target.motionY = target.motionY * 0.5D + push.y;
                target.motionZ = target.motionZ * 0.5D + push.z;
                target.velocityChanged = true;
            }
        }
        spawnNarukamiHitParticles((WorldServer) world, center, index, damagedAny);
    }

    private List<Entity> radiusTargets(EntityLivingBase owner, Vec3d center, double radius) {
        AxisAlignedBB box = new AxisAlignedBB(center.x - radius, center.y - radius * 0.78D, center.z - radius,
                center.x + radius, center.y + radius * 0.78D, center.z + radius);
        List<Entity> all = world.getEntitiesInAABBexcluding(this, box,
                entity -> TargetingUtil.canSelectForDamage(owner, entity));
        List<Entity> result = new ArrayList<>();
        for (Entity entity : TargetingUtil.getDistinctDamageTargets(all))
            if (entity.getEntityBoundingBox().getCenter().squareDistanceTo(center) <= radius * radius) result.add(entity);
        return result;
    }

    private List<Entity> crossTargets(EntityLivingBase owner) {
        Vec3d forward = getDirection(), right = new Vec3d(0, 1, 0).crossProduct(forward);
        if (right.lengthSquared() < 1.0E-8D) right = new Vec3d(1, 0, 0); else right = right.normalize();
        Vec3d up = forward.crossProduct(right).normalize();
        Vec3d origin = getStoredTargetAnchor().subtract(forward.scale(1.1D));
        AxisAlignedBB box = new AxisAlignedBB(origin, origin.add(forward.scale(8.3D))).grow(6.3D, 4.7D, 6.3D);
        List<Entity> all = world.getEntitiesInAABBexcluding(this, box,
                entity -> TargetingUtil.canSelectForDamage(owner, entity));
        List<Entity> result = new ArrayList<>();
        for (Entity entity : TargetingUtil.getDistinctDamageTargets(all)) {
            Vec3d offset = entity.getEntityBoundingBox().getCenter().subtract(origin);
            double ahead = offset.dotProduct(forward);
            if (ahead >= -entity.width * 0.5D && ahead <= 8.3D + entity.width * 0.5D
                    && Math.abs(offset.dotProduct(right)) <= 4.8D + entity.width * 0.5D
                    && Math.abs(offset.dotProduct(up)) <= 3.2D + entity.height * 0.5D) result.add(entity);
        }
        return result;
    }

    private void playSounds(WorldServer server) {
        Vec3d anchor = getTargetAnchor();
        if (getMode() == MODE_THUNDERBOLT_CALL) {
            if (ticksExisted == 3) server.playSound(null, posX, posY + 1, posZ, SoundEvents.ENTITY_ENDERDRAGON_FLAP,
                    SoundCategory.PLAYERS, 0.45F, 1.72F);
            if (ticksExisted == 7 || ticksExisted == 16 || ticksExisted == 24 || ticksExisted == 31)
                server.playSound(null, anchor.x, anchor.y, anchor.z,
                        ticksExisted == 31 ? SoundEvents.ENTITY_GENERIC_EXPLODE : SoundEvents.ENTITY_LIGHTNING_IMPACT,
                        SoundCategory.PLAYERS, 1.05F, 1.2F + ticksExisted * 0.012F);
            return;
        }
        EntityLivingBase owner = getOwner();
        Vec3d ownerAnchor = owner == null ? getPositionVector() : owner.getPositionVector();
        if (ticksExisted == 4) {
            server.playSound(null, anchor.x, anchor.y, anchor.z, SoundEvents.ENTITY_LIGHTNING_IMPACT,
                    SoundCategory.PLAYERS, 1.08F, 1.28F);
            server.playSound(null, anchor.x, anchor.y, anchor.z, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                    SoundCategory.PLAYERS, 0.72F, 0.72F);
        } else if (ticksExisted == 8 || ticksExisted == 10) {
            server.playSound(null, anchor.x, anchor.y, anchor.z, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                    SoundCategory.PLAYERS, 0.94F, ticksExisted == 8 ? 0.62F : 0.78F);
        } else if (ticksExisted == 20) {
            server.playSound(null, ownerAnchor.x, ownerAnchor.y + 1.1D, ownerAnchor.z,
                    SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.PLAYERS, 0.82F, 1.52F);
            server.playSound(null, ownerAnchor.x, ownerAnchor.y + 1.1D, ownerAnchor.z,
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.64F, 0.58F);
        } else if (ticksExisted == 24 || ticksExisted == 28) {
            server.playSound(null, ownerAnchor.x, ownerAnchor.y + 1.1D, ownerAnchor.z,
                    SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.PLAYERS, 0.92F,
                    ticksExisted == 24 ? 1.54F : 1.82F);
        } else if (ticksExisted == 31) {
            server.playSound(null, ownerAnchor.x, ownerAnchor.y + 0.5D, ownerAnchor.z,
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.88F, 1.64F);
        }
    }

    private void spawnNarukamiHitParticles(WorldServer server, Vec3d center, int hitIndex, boolean hit) {
        int count = hit ? 10 + hitIndex * 2 : 4;
        server.spawnParticle(EnumParticleTypes.REDSTONE, center.x, center.y, center.z,
                count, 0.72D, 0.28D, 1.0D, 1.0D);
        server.spawnParticle(EnumParticleTypes.END_ROD, center.x, center.y, center.z,
                Math.max(4, count / 2), 0.75D, 0.55D, 0.75D, 0.055D);
        server.spawnParticle(EnumParticleTypes.CRIT, center.x, center.y, center.z,
                Math.max(3, count / 3), 0.65D, 0.45D, 0.65D, 0.09D);
    }

    private void spawnParticles(WorldServer server) {
        if ((ticksExisted >= 5 && ticksExisted <= 32) && (ticksExisted % 2 == 0)) {
            Vec3d center = getMode() == MODE_THUNDERBOLT_CALL ? getTargetAnchor()
                    : getOwner().getPositionVector().add(0, 1.0D, 0);
            server.spawnParticle(EnumParticleTypes.SPELL_WITCH, center.x, center.y, center.z,
                    10, 1.1D, 0.8D, 1.1D, 0.08D);
            server.spawnParticle(EnumParticleTypes.END_ROD, center.x, center.y, center.z,
                    4, 0.8D, 0.6D, 0.8D, 0.06D);
        }
    }

    private static DamageSource source(EntityLivingBase owner) { return owner instanceof EntityPlayer
            ? DamageSource.causePlayerDamage((EntityPlayer) owner) : DamageSource.causeMobDamage(owner); }
    @Override public AxisAlignedBB getRenderBoundingBox() { return getEntityBoundingBox().union(
            new AxisAlignedBB(getPositionVector(), getStoredTargetAnchor().add(getDirection().scale(8)))).grow(7, 6, 7); }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override protected void readEntityFromNBT(NBTTagCompound tag) {
        dataManager.set(OWNER_ID, tag.getInteger("OwnerId")); dataManager.set(TARGET_ID, tag.getInteger("TargetId"));
        dataManager.set(DAMAGE, tag.getFloat("Damage")); dataManager.set(MODE, tag.getInteger("Mode"));
        dataManager.set(DIR_X, tag.getFloat("Dx")); dataManager.set(DIR_Y, tag.getFloat("Dy")); dataManager.set(DIR_Z, tag.getFloat("Dz"));
        dataManager.set(TARGET_X, tag.getFloat("Tx")); dataManager.set(TARGET_Y, tag.getFloat("Ty")); dataManager.set(TARGET_Z, tag.getFloat("Tz"));
    }
    @Override protected void writeEntityToNBT(NBTTagCompound tag) {
        tag.setInteger("OwnerId", dataManager.get(OWNER_ID)); tag.setInteger("TargetId", dataManager.get(TARGET_ID));
        tag.setFloat("Damage", getBaseDamage()); tag.setInteger("Mode", getMode());
        tag.setFloat("Dx", dataManager.get(DIR_X)); tag.setFloat("Dy", dataManager.get(DIR_Y)); tag.setFloat("Dz", dataManager.get(DIR_Z));
        tag.setFloat("Tx", dataManager.get(TARGET_X)); tag.setFloat("Ty", dataManager.get(TARGET_Y)); tag.setFloat("Tz", dataManager.get(TARGET_Z));
    }
}

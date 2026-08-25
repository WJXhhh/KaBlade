package com.wjx.kablade.Entity;

import com.wjx.kablade.util.TargetingUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** 女武神冲击的落地爆发与前进裂地波。 */
public class EntityValkyrieImpact extends Entity {
    public static final int MAX_FRAME = 68;
    public static final int HIT_FRAME = 17;
    public static final int FISSURE_END_FRAME = 42;
    public static final int MAX_LIFETIME = 46;
    public static final int HIT_TICK = 12;
    public static final int FISSURE_END_TICK = 28;
    private static final DataParameter<Integer> OWNER = EntityDataManager.createKey(EntityValkyrieImpact.class, DataSerializers.VARINT);
    private static final DataParameter<Float> DAMAGE = EntityDataManager.createKey(EntityValkyrieImpact.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> YAW = EntityDataManager.createKey(EntityValkyrieImpact.class, DataSerializers.FLOAT);
    private final Set<UUID> fissureHits = new HashSet<>();
    private UUID ownerUuid;

    public EntityValkyrieImpact(World world) {
        super(world);
        setSize(0.2F, 0.2F);
        noClip = true;
        setNoGravity(true);
        ignoreFrustumCheck = true;
    }

    public static EntityValkyrieImpact spawn(World world, EntityPlayer owner, Vec3d pos, float yaw, float damage) {
        EntityValkyrieImpact entity = new EntityValkyrieImpact(world);
        entity.setPosition(pos.x, pos.y, pos.z);
        entity.ownerUuid = owner.getUniqueID();
        entity.rotationYaw = yaw;
        entity.prevRotationYaw = yaw;
        entity.dataManager.set(OWNER, owner.getEntityId());
        entity.dataManager.set(DAMAGE, damage);
        entity.dataManager.set(YAW, yaw);
        world.spawnEntity(entity);
        return entity;
    }

    @Override
    protected void entityInit() {
        dataManager.register(OWNER, -1);
        dataManager.register(DAMAGE, 0.0F);
        dataManager.register(YAW, 0.0F);
    }

    public float getDamage() { return dataManager.get(DAMAGE); }
    public int getOwnerId() { return dataManager.get(OWNER); }
    public float getAnimationFrame(float partialTicks) {
        return Math.min(MAX_FRAME, (ticksExisted + partialTicks) * 1.5F);
    }
    public float getEffectYaw() { return dataManager.get(YAW); }
    public Vec3d getForward() {
        float radians = -getEffectYaw() * 0.017453292F;
        return new Vec3d(MathHelper.sin(radians), 0.0D, MathHelper.cos(radians)).normalize();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!world.isRemote) {
            EntityPlayer owner = getOwner();
            if (owner == null) { setDead(); return; }
            WorldServer server = (WorldServer) world;
            if (ticksExisted == HIT_TICK) impact(server, owner);
            if (ticksExisted >= HIT_TICK && ticksExisted <= FISSURE_END_TICK) fissure(server, owner);
            if (ticksExisted >= MAX_LIFETIME) setDead();
        }
    }

    private void impact(WorldServer server, EntityPlayer owner) {
        server.playSound(null, posX, posY, posZ, SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.PLAYERS, 1.4F, 0.95F);
        server.playSound(null, posX, posY, posZ, SoundEvents.BLOCK_ANVIL_LAND,
                SoundCategory.PLAYERS, 1.0F, 0.75F);
        server.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, posX, posY + 0.2D, posZ,
                8, 1.4D, 0.45D, 1.4D, 0.05D);
        hitBox(owner, getEntityBoundingBox().grow(3.2D, 1.25D, 3.2D), getDamage() * 1.25F, true);
    }

    private void fissure(WorldServer server, EntityPlayer owner) {
        float progress = (ticksExisted - HIT_TICK) / (float) (FISSURE_END_TICK - HIT_TICK);
        Vec3d center = getPositionVector().add(getForward().scale(progress * 7.25D));
        double halfWidth = 1.2D + progress * 2.2D;
        AxisAlignedBB box = new AxisAlignedBB(center.x - halfWidth, center.y - 0.6D, center.z - halfWidth,
                center.x + halfWidth, center.y + 2.8D, center.z + halfWidth);
        List<Entity> entities = world.getEntitiesInAABBexcluding(owner, box,
                input -> TargetingUtil.canSelectForDamage(owner, input));
        for (Entity hit : TargetingUtil.getDistinctDamageTargets(entities)) {
            EntityLivingBase target = TargetingUtil.getSelectionTarget(hit);
            if (target == null || !fissureHits.add(target.getUniqueID())) continue;
            target.hurtResistantTime = 0;
            hit.attackEntityFrom(DamageSource.causePlayerDamage(owner).setDamageBypassesArmor(), getDamage() * 0.95F);
            target.hurtResistantTime = 0;
            Vec3d forward = getForward();
            target.motionX = forward.x * 0.45D;
            target.motionY = 1.15D;
            target.motionZ = forward.z * 0.45D;
            target.velocityChanged = true;
        }
        if ((ticksExisted & 1) == 0) server.spawnParticle(EnumParticleTypes.CRIT,
                center.x, center.y + 0.15D, center.z, 10, halfWidth * 0.35D, 0.15D, halfWidth * 0.35D, 0.08D);
    }

    private void hitBox(EntityPlayer owner, AxisAlignedBB box, float damage, boolean radial) {
        List<Entity> entities = world.getEntitiesInAABBexcluding(owner, box,
                input -> TargetingUtil.canSelectForDamage(owner, input));
        for (Entity hit : TargetingUtil.getDistinctDamageTargets(entities)) {
            EntityLivingBase target = TargetingUtil.getSelectionTarget(hit);
            if (target == null) continue;
            target.hurtResistantTime = 0;
            hit.attackEntityFrom(DamageSource.causePlayerDamage(owner).setDamageBypassesArmor(), damage);
            target.hurtResistantTime = 0;
            Vec3d push = target.getPositionVector().subtract(getPositionVector());
            push = new Vec3d(push.x, 0.0D, push.z);
            if (push.lengthSquared() < 1.0E-6D) push = getForward();
            push = push.normalize().scale(radial ? 1.1D : 0.45D);
            target.motionX = push.x;
            target.motionY = 0.65D;
            target.motionZ = push.z;
            target.velocityChanged = true;
        }
    }

    private EntityPlayer getOwner() {
        Entity raw = world.getEntityByID(dataManager.get(OWNER));
        if (raw instanceof EntityPlayer && (ownerUuid == null || ownerUuid.equals(raw.getUniqueID()))) return (EntityPlayer) raw;
        if (ownerUuid != null && world instanceof WorldServer) {
            raw = ((WorldServer) world).getEntityFromUuid(ownerUuid);
            if (raw instanceof EntityPlayer) return (EntityPlayer) raw;
        }
        return null;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() { return getEntityBoundingBox().grow(14.0D); }
    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        if (tag.hasUniqueId("Owner")) ownerUuid = tag.getUniqueId("Owner");
        dataManager.set(DAMAGE, tag.getFloat("Damage"));
        dataManager.set(YAW, tag.getFloat("Yaw"));
    }
    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        if (ownerUuid != null) tag.setUniqueId("Owner", ownerUuid);
        tag.setFloat("Damage", getDamage());
        tag.setFloat("Yaw", getEffectYaw());
    }
    @Override
    public boolean canBeCollidedWith() { return false; }
}

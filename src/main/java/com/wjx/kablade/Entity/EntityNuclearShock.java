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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;
import java.util.UUID;

/** 核能震动的伤害时间轴与同步渲染锚点。 */
public class EntityNuclearShock extends Entity {
    public static final int MAX_FRAME = 68;
    public static final int HIT_FRAME = 26;
    public static final int MAX_LIFETIME = 46;
    public static final int HIT_TICK = 18;
    public static final double RADIUS = 6.8D;
    private static final DataParameter<Integer> OWNER = EntityDataManager.createKey(EntityNuclearShock.class, DataSerializers.VARINT);
    private static final DataParameter<Float> DAMAGE = EntityDataManager.createKey(EntityNuclearShock.class, DataSerializers.FLOAT);
    private UUID ownerUuid;

    public EntityNuclearShock(World world) {
        super(world);
        setSize(0.2F, 0.2F);
        noClip = true;
        setNoGravity(true);
        ignoreFrustumCheck = true;
    }

    public static EntityNuclearShock spawn(World world, EntityPlayer owner, Vec3d pos, float damage) {
        EntityNuclearShock entity = new EntityNuclearShock(world);
        entity.setPosition(pos.x, pos.y, pos.z);
        entity.ownerUuid = owner.getUniqueID();
        entity.dataManager.set(OWNER, owner.getEntityId());
        entity.dataManager.set(DAMAGE, damage);
        world.spawnEntity(entity);
        return entity;
    }

    @Override
    protected void entityInit() {
        dataManager.register(OWNER, -1);
        dataManager.register(DAMAGE, 0.0F);
    }

    public float getDamage() { return dataManager.get(DAMAGE); }
    public int getOwnerId() { return dataManager.get(OWNER); }
    public float getAnimationFrame(float partialTicks) {
        return Math.min(MAX_FRAME, (ticksExisted + partialTicks) * 1.5F);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!world.isRemote && ticksExisted == HIT_TICK) burst((WorldServer) world);
        if (!world.isRemote && ticksExisted >= MAX_LIFETIME) setDead();
    }

    private void burst(WorldServer server) {
        EntityPlayer owner = getOwner();
        if (owner == null) { setDead(); return; }
        server.playSound(null, posX, posY, posZ, SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.PLAYERS, 1.4F, 0.75F);
        server.playSound(null, posX, posY, posZ, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.PLAYERS, 1.2F, 1.8F);
        server.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, posX, posY + 0.3D, posZ,
                12, 2.5D, 0.8D, 2.5D, 0.08D);
        server.spawnParticle(EnumParticleTypes.FLAME, posX, posY + 0.2D, posZ,
                70, 3.0D, 1.2D, 3.0D, 0.12D);

        AxisAlignedBB box = getEntityBoundingBox().grow(RADIUS, 3.5D, RADIUS);
        List<Entity> entities = world.getEntitiesInAABBexcluding(owner, box,
                input -> TargetingUtil.canSelectForDamage(owner, input));
        for (Entity hit : TargetingUtil.getDistinctDamageTargets(entities)) {
            EntityLivingBase target = TargetingUtil.getSelectionTarget(hit);
            if (target == null) continue;
            target.hurtResistantTime = 0;
            hit.attackEntityFrom(DamageSource.causePlayerDamage(owner).setDamageBypassesArmor(), getDamage());
            target.hurtResistantTime = 0;
            Vec3d horizontal = target.getPositionVector().subtract(getPositionVector());
            horizontal = new Vec3d(horizontal.x, 0.0D, horizontal.z);
            if (horizontal.lengthSquared() < 1.0E-6D) horizontal = new Vec3d(0.0D, 0.0D, 1.0D);
            horizontal = horizontal.normalize().scale(1.85D);
            target.motionX = horizontal.x;
            target.motionY = 0.65D;
            target.motionZ = horizontal.z;
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
    public AxisAlignedBB getRenderBoundingBox() { return getEntityBoundingBox().grow(16.0D); }
    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        if (tag.hasUniqueId("Owner")) ownerUuid = tag.getUniqueId("Owner");
        dataManager.set(DAMAGE, tag.getFloat("Damage"));
    }
    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        if (ownerUuid != null) tag.setUniqueId("Owner", ownerUuid);
        tag.setFloat("Damage", getDamage());
    }
    @Override
    public boolean canBeCollidedWith() { return false; }
}

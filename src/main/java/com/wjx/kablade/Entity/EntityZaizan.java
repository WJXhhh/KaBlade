package com.wjx.kablade.Entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/** 罪斩的服务端同步视觉锚点；实际伤害由 SA 即时结算。 */
public class EntityZaizan extends Entity {
    private static final DataParameter<Integer> LIFETIME =
            EntityDataManager.createKey(EntityZaizan.class, DataSerializers.VARINT);
    private static final DataParameter<Float> SCALE =
            EntityDataManager.createKey(EntityZaizan.class, DataSerializers.FLOAT);
    private static final DataParameter<Integer> OWNER_ID =
            EntityDataManager.createKey(EntityZaizan.class, DataSerializers.VARINT);
    private static final DataParameter<Float> FORWARD =
            EntityDataManager.createKey(EntityZaizan.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> UP =
            EntityDataManager.createKey(EntityZaizan.class, DataSerializers.FLOAT);

    public EntityZaizan(World worldIn) {
        super(worldIn);
        this.setSize(0.2F, 0.2F);
        this.noClip = true;
        this.setNoGravity(true);
        this.isImmuneToFire = true;
        this.ignoreFrustumCheck = true;
    }

    public static EntityZaizan spawn(World world, EntityLivingBase owner, float forward,
                                     float up, int lifetime, float scale) {
        EntityZaizan effect = new EntityZaizan(world);
        effect.rotationYaw = owner.rotationYaw;
        effect.prevRotationYaw = effect.rotationYaw;
        effect.dataManager.set(OWNER_ID, owner.getEntityId());
        effect.dataManager.set(FORWARD, forward);
        effect.dataManager.set(UP, up);
        effect.dataManager.set(LIFETIME, Math.max(1, lifetime));
        effect.dataManager.set(SCALE, Math.max(0.1F, scale));
        effect.followOwner();
        world.spawnEntity(effect);
        return effect;
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(LIFETIME, 32);
        this.dataManager.register(SCALE, 1.0F);
        this.dataManager.register(OWNER_ID, -1);
        this.dataManager.register(FORWARD, 0.0F);
        this.dataManager.register(UP, 0.0F);
    }

    public int getLifetime() { return this.dataManager.get(LIFETIME); }
    public float getScale() { return this.dataManager.get(SCALE); }
    public float getForwardOffset() { return this.dataManager.get(FORWARD); }
    public float getUpOffset() { return this.dataManager.get(UP); }

    public Entity getOwner() {
        int id = this.dataManager.get(OWNER_ID);
        return id >= 0 ? this.world.getEntityByID(id) : null;
    }

    private void followOwner() {
        Entity owner = getOwner();
        if (owner == null) return;
        float yaw = this.rotationYaw * 0.017453292F;
        double forwardX = -MathHelper.sin(yaw);
        double forwardZ = MathHelper.cos(yaw);
        this.setPosition(owner.posX + forwardX * getForwardOffset(),
                owner.posY + getUpOffset(), owner.posZ + forwardZ * getForwardOffset());
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        followOwner();
        if (!this.world.isRemote && this.ticksExisted >= getLifetime()) this.setDead();
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return this.getEntityBoundingBox().grow(9.0D, 4.0D, 9.0D);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        this.dataManager.set(LIFETIME, Math.max(1, tag.getInteger("Lifetime")));
        this.dataManager.set(SCALE, Math.max(0.1F, tag.getFloat("Scale")));
        this.dataManager.set(OWNER_ID, tag.getInteger("OwnerId"));
        this.dataManager.set(FORWARD, tag.getFloat("Forward"));
        this.dataManager.set(UP, tag.getFloat("Up"));
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        tag.setInteger("Lifetime", getLifetime());
        tag.setFloat("Scale", getScale());
        tag.setInteger("OwnerId", this.dataManager.get(OWNER_ID));
        tag.setFloat("Forward", getForwardOffset());
        tag.setFloat("Up", getUpOffset());
    }

    @Override
    public boolean canBeCollidedWith() { return false; }
}

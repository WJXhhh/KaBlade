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

/** 震荡冲击的 1.12.2 同步视觉锚点；伤害仍由 SA 在服务端即时结算。 */
public class EntityShockImpact extends Entity {
    private static final DataParameter<Integer> LIFETIME =
            EntityDataManager.createKey(EntityShockImpact.class, DataSerializers.VARINT);
    private static final DataParameter<Float> SCALE =
            EntityDataManager.createKey(EntityShockImpact.class, DataSerializers.FLOAT);
    private static final DataParameter<Integer> OWNER_ID =
            EntityDataManager.createKey(EntityShockImpact.class, DataSerializers.VARINT);
    private static final DataParameter<Float> FORWARD =
            EntityDataManager.createKey(EntityShockImpact.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> UP =
            EntityDataManager.createKey(EntityShockImpact.class, DataSerializers.FLOAT);

    public EntityShockImpact(World worldIn) {
        super(worldIn);
        this.setSize(0.2F, 0.2F);
        this.noClip = true;
        this.setNoGravity(true);
        this.isImmuneToFire = true;
        this.ignoreFrustumCheck = true;
    }

    public static EntityShockImpact spawn(World world, EntityLivingBase owner, float forward,
                                          float up, int lifetime, float scale) {
        EntityShockImpact effect = new EntityShockImpact(world);
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
        this.dataManager.register(LIFETIME, 26);
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
        double fx = -MathHelper.sin(yaw);
        double fz = MathHelper.cos(yaw);
        this.setPosition(owner.posX + fx * getForwardOffset(),
                owner.posY + getUpOffset(), owner.posZ + fz * getForwardOffset());
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        followOwner();
        if (!this.world.isRemote && this.ticksExisted >= getLifetime()) this.setDead();
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return this.getEntityBoundingBox().grow(8.0D);
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

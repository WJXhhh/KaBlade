package com.wjx.kablade.Entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;


public class EntityRaikiriBlade extends AbsEntityShield {

    public EntityLivingBase thrower;

    public static final DataParameter<Integer> throwerID = EntityDataManager.createKey(EntityRaikiriBlade.class,DataSerializers.VARINT);

    private static final DataParameter<Float> shieldBlood = EntityDataManager.createKey(EntityRaikiriBlade.class,DataSerializers.FLOAT);

    public boolean shouldFollow = false;
    public double followX = 0d,followY = 0d,followZ = 0d;

    public EntityRaikiriBlade(World worldIn) {
        super(worldIn);
        this.ticksExisted = 0;
        this.setSize(0.5F, 0.5F);
        thrower = null;
    }

    public EntityRaikiriBlade(World worldIn, EntityLivingBase throwerIn){
        super(worldIn);
        thrower = throwerIn;
        this.setLocationAndAngles(thrower.posX,thrower.posY,thrower.posZ,0,0);
        this.setSize(0.5F, 0.5F);

        MinecraftForge.EVENT_BUS.register(this);

        this.ticksExisted = 0;

        //thrower.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION,200,3));
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.ticksExisted > 200){
            setDead();
        }
        if (this.thrower != null && this.thrower.isDead){
            this.setDead();
        }

        if (this.dataManager.get(shieldBlood) <= 0){
            this.setDead();
        }

        if (!world.isRemote){
            if (this.thrower!=null&&!(this.thrower.isDead)){
                if (this.dataManager.get(throwerID) != this.thrower.getEntityId()){
                    this.dataManager.set(throwerID,this.thrower.getEntityId());
                }
            }
            else if (this.dataManager.get(throwerID) != -1){
                this.dataManager.set(throwerID,-1);
            }
        }

        if (this.thrower!=null&&!(this.thrower.isDead)) {
            followX = thrower.posX;
            followY = thrower.posY;
            followZ = thrower.posZ;
            setPositionAndUpdate(followX,followY,followZ);
            shouldFollow = true;
            AxisAlignedBB bb = thrower.getEntityBoundingBox().grow(1,0,1).offset(this.motionX,this.motionY,this.motionZ);
            List<Entity> entitieee = world.getEntitiesInAABBexcluding(this,bb, input -> input !=thrower&&input instanceof EntityLivingBase);
            for(Entity e : entitieee){
                if(e instanceof EntityLivingBase){
                    if (!world.isRemote)
                    e.attackEntityFrom(DamageSource.causeMobDamage(thrower),2f);
                }
            }
        }
        else{
            followX = posX;
            followY = posY;
            followZ = posZ;
            shouldFollow = false;
        }
    }

    @Override
    protected void entityInit() {
        this.getDataManager().register(shieldBlood,10f);
        this.getDataManager().register(throwerID,-1);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {

    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {

    }

    @Override
    public Entity getThrower() {
        return thrower;
    }

    @Override
    public void setThrower(Entity entity) {
        this.thrower = (EntityLivingBase) entity;
    }

    @Override
    public float getShieldBlood() {
        return this.getDataManager().get(shieldBlood);
    }

    @Override
    public void setShieldBlood(float blood) {
        this.dataManager.set(shieldBlood,blood);
            if (this.thrower != null){
                if (this.thrower instanceof EntityPlayer){
                    if (blood <= 0){
                        if (!world.isRemote){
                            ((EntityPlayer) this.thrower).sendStatusMessage(new TextComponentString("§b" + I18n.translateToLocal("msg.raikiri_shield_crash")),true);
                        }
                    }
                    else {
                        if(!world.isRemote)
                        ((EntityPlayer) this.thrower).sendStatusMessage(new TextComponentString("§b" + I18n.translateToLocal("msg.raikiri_shield_blood") + blood),true);
                    }
                }
            }
    }

    public void setData(float blood){
        this.dataManager.set(shieldBlood,blood);
    }
}

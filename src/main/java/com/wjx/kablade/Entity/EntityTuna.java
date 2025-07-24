package com.wjx.kablade.Entity;

import com.google.common.base.Predicates;
import mods.flammpfeil.slashblade.entity.selector.EntitySelectorAttackable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntitySelectors;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IThrowableEntity;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class EntityTuna extends Entity implements IThrowableEntity {

    //public static DataParameter<Integer> renderTick = EntityDataManager.createKey(EntityThunderEdgeAttack.class, DataSerializers.VARINT);



    public EntityLivingBase owner = null;

    public float bladeDamage = 10.0F;

    public EntityTuna(World world){
        super(world);
        ticksExisted = 0;

    }

    public EntityTuna(World world, EntityLivingBase ownerIn){
        this(world);
        owner = ownerIn;
        this.setPositionAndUpdate(owner.posX,owner.posY,owner.posZ);
        this.rotationYaw = ownerIn.rotationYaw;
    }



    @Override
    protected void entityInit() {

    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {

    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {

    }

    @Override
    public Entity getThrower() {
        return owner;
    }

    @Override
    public void setThrower(Entity entity) {
        owner = (EntityLivingBase) entity;
    }

    @Override
    public void onEntityUpdate() {
        super.onEntityUpdate();
        if(this.ticksExisted==5){
            World world1 = this.world;
            if(!world1.isRemote){
                List<Entity> list = world.getEntitiesInAABBexcluding(this,this.getEntityBoundingBox().grow(5.0D,2.0D,5.0D), EntitySelectorAttackable.getInstance());
                for(Entity entity : list){
                    if(entity instanceof EntityLivingBase&&!(entity.equals( owner))){
                        EntityLivingBase living = (EntityLivingBase) entity;
                        if(owner instanceof EntityPlayer){
                            living.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer) owner),10.0F);
                        }else{
                            living.attackEntityFrom(DamageSource.causeMobDamage(owner),10.0F);
                        }


                    }
                }

            }
        }
        if(this.ticksExisted>60){
            this.setDead();
        }
    }
}

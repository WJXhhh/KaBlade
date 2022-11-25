package com.wjx.kablade.Entity;

import mods.flammpfeil.slashblade.util.ReflectionAccessHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IThrowableEntity;

import java.util.List;
import java.util.function.Predicate;

public class EntityVorpalBlackHole extends Entity implements IThrowableEntity {
    public static final double catchSpeed = 2.2d;
    public EntityLivingBase thrower = null;

    public EntityVorpalBlackHole(World worldIn) {
        super(worldIn);
        this.ticksExisted = 0;
    }

    private final Predicate<Entity> p2 = input1 -> {
        if (input1 == thrower){
            return false;
        }
        if (!(input1 instanceof EntityLivingBase)){
            if (input1 instanceof IThrowableEntity){
                if (((IThrowableEntity)input1).getThrower() == this.thrower){
                    return false;
                }
            }
        }
        if (input1 instanceof IThrowableEntity){
            if (((IThrowableEntity)input1).getThrower() == this.thrower){
                return false;
            }
        }
        if (input1 instanceof EntityPlayerMP){
            return !((EntityPlayerMP) input1).capabilities.isCreativeMode;
        }
        return true;
    };

    @Override
    protected void entityInit() {

    }

    public EntityVorpalBlackHole(World worldIn,EntityLivingBase throwerIn,double xIn,double yIn,double zIn){
        super(worldIn);
        this.thrower = throwerIn;
        this.setSize(0.5F, 0.5F);
        this.setLocationAndAngles(xIn,yIn,zIn,0f,0f);
        this.ticksExisted = 0;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.ticksExisted > 20){
            this.setDead();
        }
        AxisAlignedBB bb = this.getEntityBoundingBox().grow(10,5,10).offset(this.motionX,this.motionY,this.motionZ);
        List<Entity> entities;
        if (thrower!=null){
            entities = world.getEntitiesInAABBexcluding(this,bb, p2::test);
        }else {
            entities = world.getEntitiesInAABBexcluding(this,bb,input -> input !=this);
        }
        for (Entity e : entities){
            if (this.getDistance(e)>=0.75){
                if (e instanceof EntityPlayerMP){
                    EntityPlayerMP player = (EntityPlayerMP) e;
                    if (player.capabilities.isCreativeMode){
                        continue;
                    }
                }
                if (e instanceof IThrowableEntity || e instanceof EntityLivingBase){
                    double posX1 = this.posX - e.posX;
                    double posY1 = this.posY - e.posY;
                    double posZ1 = this.posZ - e.posZ;
                    double disX = Math.abs(posX1);
                    double disY = Math.abs(posY1);
                    double disZ = Math.abs(posZ1);
                    double disCount = 1/this.getDistance(e);
                    double per1 = Math.max(disX,Math.max(disY,disZ));
                    double perX1,perY1,perZ1;
                    perX1 = posX1/per1;
                    perY1 = posY1/per1;
                    perZ1 = posZ1/per1;
                    ReflectionAccessHelper.setVelocity(e,perX1 * catchSpeed * disCount,perY1 * catchSpeed * disCount,perZ1 * catchSpeed * disCount);
                }
            }
        }
        AxisAlignedBB bb2 = this.getEntityBoundingBox().grow(0.5,0.5,0.5).offset(this.motionX,this.motionY,this.motionZ);
        List<Entity> entities2;
        if (thrower!=null){
            entities2 = world.getEntitiesInAABBexcluding(this,bb2, p2::test);
        }else {
            entities2 = world.getEntitiesInAABBexcluding(this,bb2,input -> input !=this);
        }
        for (Entity e : entities2){
            if (e instanceof EntityPlayerMP){
                EntityPlayerMP player = (EntityPlayerMP) e;
                if (player.interactionManager.getGameType() == GameType.CREATIVE ||player.interactionManager.getGameType() == GameType.SPECTATOR){
                    continue;
                }
            }
            if (e instanceof IThrowableEntity || e instanceof EntityLivingBase){
                ReflectionAccessHelper.setVelocity(e,0,0,0);
                if (this.thrower !=null){
                    e.attackEntityFrom(DamageSource.causeMobDamage(thrower),3);
                }
                else {
                    e.attackEntityFrom(DamageSource.GENERIC,3);
                }
            }
        }

    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {

    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {

    }

    @Override
    public void setItemStackToSlot(EntityEquipmentSlot slotIn, ItemStack stack) {

    }

    @Override
    public Entity getThrower() {
        return thrower;
    }

    @Override
    public void setThrower(Entity entity) {
        this.thrower = (EntityLivingBase) entity;
    }

    public void Xi(){

    }
}

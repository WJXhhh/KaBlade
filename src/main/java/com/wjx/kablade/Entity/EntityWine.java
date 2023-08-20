package com.wjx.kablade.Entity;

import com.wjx.kablade.util.KaBladeEntityProperties;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.List;

public class EntityWine extends Entity {
    public double longNess = 1d;

    public static final DataParameter<Integer> targetID = EntityDataManager.createKey(EntityWine.class, DataSerializers.VARINT);

    public EntityWine(World worldIn) {
        super(worldIn);
        this.ticksExisted = 0;
    }

    public EntityWine(World worldIn,double xIn,double yIn,double zIn){
        super(worldIn);
        this.setPositionAndUpdate(xIn,yIn,zIn);
        this.ticksExisted = 0;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.ticksExisted >160){
            this.setDead();
        }

        Entity target = this.world.getEntityByID(dataManager.get(targetID));
        if (world.isRemote && target instanceof EntityLivingBase){
            double distance = target.getDistance(this);
            longNess = distance;
            double distanceZ = Math.abs(posZ - target.posZ);
            double distanceX = Math.abs(posX - target.posX);
            double lengthDistance = Math.abs(posY - target.posY);
            double placeDistance = Math.sqrt((distance * distance) - (lengthDistance * lengthDistance));
            double v = Math.toDegrees(Math.atan(lengthDistance / placeDistance));
            if(posY >= target.posY){
                rotationPitch = (float) v;
            }
            else rotationPitch = -(float) v;

            double n = Math.toDegrees(Math.atan(distanceX / distanceZ));
            double k = Math.toDegrees(Math.atan(distanceZ / distanceX));
            if (posZ < target.posZ){
                if (posX > target.posX){
                    rotationYaw = (float) n;
                }
                else {
                    rotationYaw = -(float) n;
                }
            }
            else {
                if (posX > target.posX){
                    rotationYaw = 90f + (float) k;
                }
                else {
                    rotationYaw = -90f -(float) k;
                }
            }
        }
        if (!world.isRemote){
            if (target!=null){
                if (target.isDead || target.getDistance(this) > 8 || !(KaBladeEntityProperties.getPropCompound(target).getInteger(KaBladeEntityProperties.PROP_WINE_BIND) > 0)){
                    dataManager.set(targetID,-1);
                    longNess = 1;
                }
            }
            if (target==null){
                this.longNess = 1;
                AxisAlignedBB bb = this.getEntityBoundingBox().grow(4,4,4);
                List<Entity> list = this.world.getEntitiesInAABBexcluding(this,bb, input -> {
                    if (input instanceof EntityLivingBase){
                        return KaBladeEntityProperties.getPropCompound(input).getInteger(KaBladeEntityProperties.PROP_WINE_BIND) > 0;
                    }
                    return false;
                });
                if (!list.isEmpty()){
                    EntityLivingBase t = null;
                    double d0 = -1.0D;
                    for (Entity e : list){
                        double d1 = e.getDistance(this);

                        if (d0 == -1.0D || d1 < d0)
                        {
                            d0 = d1;
                            t = (EntityLivingBase) e;
                        }
                    }
                    dataManager.set(targetID,t.getEntityId());
                }
            }else if (!(KaBladeEntityProperties.getPropCompound(target).getInteger(KaBladeEntityProperties.PROP_WINE_BIND) > 0)){
                dataManager.set(targetID,-1);
            }
        }
        else if (world.getEntityByID(dataManager.get(targetID)) != null){
            Entity e = world.getEntityByID(dataManager.get(targetID));
            if (!e.isDead && e.getDistance(this) <= 8 && e instanceof EntityLivingBase){
                target = (EntityLivingBase) e;
            }
        }
    }

    @Override
    protected void entityInit() {
        dataManager.register(targetID,0);
        if(!world.isRemote){
            int id = Block.getStateId(Blocks.DIRT.getDefaultState());
            int count = 40;
            float angle = (float) (Math.PI * 2f / (float) count);
            double _x = this.posX;
            double _y = this.posY;
            double _z = this.posZ;
            float speed = 1f/20f;
            for (int i = 0; i <= count; i++)
            {
                final double cos = Math.cos(angle * i);
                final double sin = Math.sin(angle * i);

                float x = (float) (_x + cos);
                float y = (float) (_y + world.rand.nextFloat());
                float z = (float) (_z + sin);

                float vx = (float) (speed * cos);
                float vz = (float) (speed * sin);

                world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, x, y, z, vx, 0, vz,id);
            }
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {

    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {

    }
}

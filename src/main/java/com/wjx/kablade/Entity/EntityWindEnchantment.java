package com.wjx.kablade.Entity;

import com.google.common.collect.Maps;
import com.wjx.kablade.util.KaBladePlayerProp;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IThrowableEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class EntityWindEnchantment extends Entity implements IThrowableEntity {
    public EntityWindEnchantment(World worldIn){
        super(worldIn);
        this.ticksExisted = 0;
    }

    public EntityLivingBase thrower;

    public HashMap<String,Float> getRate = Maps.newHashMap();

    public static DataParameter<Integer> renderTick = EntityDataManager.createKey(EntityWindEnchantment.class, DataSerializers.VARINT);

    public EntityWindEnchantment(World worldIn, EntityPlayer player){
        super(worldIn);
        this.ticksExisted = 0;
        this.setPositionAndUpdate(player.posX,player.posY,player.posZ);
        this.thrower = player;
    }

    {
        Random random = new Random();
        int effect1Rate;
        int effect2Rate;
        int effect3Rate;
        int effect4Rate;
        if (random.nextBoolean()){
            effect1Rate = -3 - random.nextInt(3);
        }
        else {
            effect1Rate = 3 + random.nextInt(3);
        }
        if (random.nextBoolean()){
            effect2Rate = -2 - random.nextInt(3);
        }
        else {
            effect2Rate = 2 + random.nextInt(3);
        }
        if (random.nextBoolean()){
            effect3Rate = -1 - random.nextInt(3);
        }
        else {
            effect3Rate = 1 + random.nextInt(3);
        }
        if (random.nextBoolean()){
            effect4Rate = -4 - random.nextInt(3);
        }
        else {
            effect4Rate = 4 + random.nextInt(3);
        }
        getRate.put("effect1",effect1Rate*3.6f);
        getRate.put("effect2",effect2Rate*3.6f);
        getRate.put("effect3",effect3Rate*3.6f);
        getRate.put("effect4",effect4Rate*3.6f);
    }
    @Override
    protected void entityInit() {
        dataManager.register(renderTick,0);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {

    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {

    }

    public int getRenderTick(){
        return dataManager.get(renderTick);
    }

    public void setRenderTick(int renderTickIn){
        dataManager.set(renderTick,renderTickIn);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!world.isRemote){
            AxisAlignedBB bb = this.getEntityBoundingBox().grow(10d,0,10d).expand(0d,4d,0d);
            List<Entity> entityList = this.world.getEntitiesInAABBexcluding(this,bb,input -> input instanceof EntityPlayer);
            for (Entity e : entityList){
                if (e instanceof EntityPlayer){
                    KaBladePlayerProp.getPropCompound((EntityPlayer) e).setInteger(KaBladePlayerProp.WIND_ENCHANTMENT_BOOST,5);
                    KaBladePlayerProp.updateNBTForClient((EntityPlayer) e);
                }
            }
            if (getRenderTick() >=100){
                setRenderTick(0);
            }
            if (getRenderTick() < 100){
                setRenderTick(getRenderTick() + 1);
            }
            if (this.ticksExisted > 100){
                this.setDead();
            }
        }
    }

    @Override
    public Entity getThrower() {
        return this.thrower;
    }

    @Override
    public void setThrower(Entity entity) {
        this.thrower = (EntityLivingBase) entity;
    }
}

package com.wjx.kablade.Entity;

import com.wjx.kablade.util.KaBladeEntityProperties;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.List;

public class EntityFreezeDomainEx extends Entity {
    public EntityFreezeDomainEx(World worldIn){
        super(worldIn);
        this.ticksExisted = 0;
    }

    public EntityFreezeDomainEx(World worldIn, EntityPlayer player){
        super(worldIn);
        this.ticksExisted = 0;
        this.setPositionAndUpdate(player.posX,player.posY,player.posZ);
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
    public void onUpdate() {

    }
}

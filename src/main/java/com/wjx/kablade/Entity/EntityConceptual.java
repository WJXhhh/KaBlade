package com.wjx.kablade.Entity;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.registry.IThrowableEntity;

import javax.annotation.Nonnull;

public class EntityConceptual extends Entity implements IThrowableEntity {
    public EntityConceptual(World world) {
        super(world);
        ticksExisted = 0;
    }

    public EntityLivingBase owner=null;
    public ItemSlashBlade blade = null;

    @Override
    protected void entityInit() {

    }

    @Override
    protected void readEntityFromNBT(@Nonnull NBTTagCompound nbtTagCompound) {

    }

    @Override
    protected void writeEntityToNBT(@Nonnull NBTTagCompound nbtTagCompound) {

    }

    @Override
    public Entity getThrower() {
        return null;
    }

    @Override
    public void setThrower(Entity entity) {

    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if(ticksExisted>60){
            this.setDead();
        }
    }
}

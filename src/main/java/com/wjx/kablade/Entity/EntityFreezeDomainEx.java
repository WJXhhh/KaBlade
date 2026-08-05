package com.wjx.kablade.Entity;

import com.wjx.kablade.util.KaBladeEntityProperties;
import com.wjx.kablade.util.TargetingUtil;
import mods.flammpfeil.slashblade.entity.selector.EntitySelectorAttackable;
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
import net.minecraftforge.fml.common.registry.IThrowableEntity;

import java.util.List;

public class EntityFreezeDomainEx extends Entity implements IThrowableEntity {
    public EntityLivingBase owner;

    public static DataParameter<Integer> renderTick = EntityDataManager.createKey(EntityFreezeDomainEx.class, DataSerializers.VARINT);

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
        dataManager.register(renderTick,0);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {

    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {

    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.owner != null){
            this.setPositionAndUpdate(owner.posX,owner.posY,owner.posZ);
        }
        if (!world.isRemote){
            AxisAlignedBB bb = this.getEntityBoundingBox().grow(8d,0,8d).expand(0d,4d,0d);
            List<Entity> entityList = owner == null ? new java.util.ArrayList<>() : this.world.getEntitiesInAABBexcluding(this, bb,
                    entity -> TargetingUtil.canSelectForDamage(owner, entity));
            for (EntityLivingBase e : TargetingUtil.getDistinctSelectionTargets(entityList)){
                if (e != owner){
                    KaBladeEntityProperties.getPropCompound(e).setInteger(KaBladeEntityProperties.FREEZE_DOMAIN_DAMAGE_BOOSTER,40);
                    e.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS,40,3));
                }
            }
            if (this.ticksExisted > 150){
                this.setDead();
            }
        }

    }

    @Override
    public Entity getThrower() {
        return owner;
    }

    @Override
    public void setThrower(Entity entity) {
        if (entity instanceof EntityLivingBase){
            this.owner = (EntityLivingBase) entity;
        }

    }
}

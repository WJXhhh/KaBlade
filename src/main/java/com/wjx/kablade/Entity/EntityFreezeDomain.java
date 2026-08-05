package com.wjx.kablade.Entity;

import com.wjx.kablade.util.KaBladeEntityProperties;
import com.wjx.kablade.util.TargetingUtil;
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

public class EntityFreezeDomain extends Entity {
    public EntityFreezeDomain(World worldIn){
        super(worldIn);
        this.ticksExisted = 0;
    }

    public static DataParameter<Integer> renderTick = EntityDataManager.createKey(EntityFreezeDomain.class, DataSerializers.VARINT);

    public EntityFreezeDomain(World worldIn, EntityPlayer player){
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
            AxisAlignedBB bb = this.getEntityBoundingBox().grow(8d,0,8d).expand(0d,4d,0d);
            List<Entity> entityList = this.world.getEntitiesInAABBexcluding(this, bb,
                    input -> TargetingUtil.getSelectionTarget(input) != null
                            && !(TargetingUtil.getSelectionTarget(input) instanceof EntityPlayer));
            for (EntityLivingBase e : TargetingUtil.getDistinctSelectionTargets(entityList)){
                if (!(e instanceof EntityPlayer)){
                    KaBladeEntityProperties.getPropCompound(e).setInteger(KaBladeEntityProperties.FREEZE_DOMAIN_DAMAGE_BOOSTER,40);
                    e.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS,40,3));
                }
            }
            if (getRenderTick() >=5){
                setRenderTick(0);
            }
            if (getRenderTick() < 5){
                setRenderTick(getRenderTick() + 1);
            }
            if (this.ticksExisted > 100){
                this.setDead();
            }
        }
    }
}

package com.wjx.kablade.Entity;

import com.wjx.kablade.util.TargetingUtil;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.world.World;

public class EntitySummonedSwordPotionEffectAdd extends EntitySummonedSwordBasePlus {
    public EntitySummonedSwordPotionEffectAdd(World par1World) {
        super(par1World);
    }

    public EntitySummonedSwordPotionEffectAdd(World par1World, EntityLivingBase entityLiving, float AttackLevel) {
        super(par1World, entityLiving, AttackLevel);
    }

    public EntitySummonedSwordPotionEffectAdd(World par1World, EntityLivingBase entityLiving, float AttackLevel, float roll) {
        super(par1World, entityLiving, AttackLevel, roll);
    }

    public static DataParameter<Boolean> IS_BURST = EntityDataManager.createKey(EntitySummonedSwordPotionEffectAdd.class, DataSerializers.BOOLEAN);

    @Override
    protected void entityInit() {
        super.entityInit();
        this.getDataManager().register(IS_BURST,false);
    }

    public boolean getBurst() {
        return this.getDataManager().get(IS_BURST);
    }


    public void setBurst(boolean b){
        this.getDataManager().set(IS_BURST,b);
    }

    protected void attackEntity(Entity target) {
        if (this.getBurst()) {
            this.world.newExplosion(this, this.posX, this.posY, this.posZ, 1.0f, false, false);
        }
        if (!this.world.isRemote) {
            EntityLivingBase effectTarget = TargetingUtil.getSelectionTarget(target);
            target.hurtResistantTime = 0;
            if (effectTarget != null) {
                effectTarget.hurtResistantTime = 0;
            }
            DamageSource ds = new EntityDamageSource("directMagic", this.getThrower()).setDamageBypassesArmor().setMagicDamage();
            target.attackEntityFrom(ds, AttackLevel);
            if (this.blade != null && effectTarget != null && this.thrower instanceof EntityLivingBase) {
                if (!target.isEntityAlive()) {
                    // empty if block

                }
                effectTarget.motionX = 0.0;
                effectTarget.motionY = 0.0;
                effectTarget.motionZ = 0.0;
                effectTarget.addVelocity(0.0, 0.1, 0.0);
                effectTarget.hurtTime = 0;
                if (!this.getBurst()) {
                    effectTarget.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 100, 2));
                }
                ((ItemSlashBlade) this.blade.getItem()).setDaunting(effectTarget);
            }
        }
        this.setDead();
    }
}

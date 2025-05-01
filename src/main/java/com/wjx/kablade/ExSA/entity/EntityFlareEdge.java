package com.wjx.kablade.ExSA.entity;

import com.wjx.kablade.ExSA.ability.EnderTeleportCanceller;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;

import java.lang.reflect.Field;

public class EntityFlareEdge extends ExSaEntityDrive{
    public EntityFlareEdge(World par1World) {
        super(par1World);
    }

    public EntityFlareEdge(World par1World, EntityLivingBase entityLiving, float AttackLevel, boolean multiHit, float roll) {
        super(par1World, entityLiving, AttackLevel, multiHit, roll);
    }

    public EntityFlareEdge(World par1World, EntityLivingBase entityLiving, float AttackLevel, boolean multiHit) {
        super(par1World, entityLiving, AttackLevel, multiHit);
    }

    public EntityFlareEdge(World par1World, EntityLivingBase entityLiving, float AttackLevel) {
        super(par1World, entityLiving, AttackLevel);
    }

    @Override
    public void onImpact(Entity curEntity, float damage) throws NoSuchFieldException, IllegalAccessException {
        EnderTeleportCanceller.setTeleportCancel(curEntity, 100);
        if (!curEntity.world.isRemote) {
            curEntity.hurtResistantTime = 0;
            DamageSource ds = new EntityDamageSource("directMagic", this.getThrower()).setDamageBypassesArmor().setMagicDamage();
            curEntity.attackEntityFrom(ds, Math.max(damage / 2.0f, 1.0f));
        }
        curEntity.setFire(5);
        if (!curEntity.world.isRemote && this.blade != null && curEntity instanceof EntityLivingBase) {
            this.blade.getItem().hitEntity(this.blade, (EntityLivingBase)curEntity, (EntityLivingBase)this.thrower);
        }
        if (curEntity instanceof EntityEnderman) {
            ((EntityEnderman) curEntity).setAttackTarget(null);
            Field f =  EntityEnderman.class.getDeclaredField("SCREAMING");
            f.setAccessible(true);
            curEntity.getDataManager().set((DataParameter<Boolean>)f.get(null),false);
        }
    }
}

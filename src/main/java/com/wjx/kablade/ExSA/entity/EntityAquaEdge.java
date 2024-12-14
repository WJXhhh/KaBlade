package com.wjx.kablade.ExSA.entity;

import com.wjx.kablade.ExSA.ability.EnderTeleportCanceller;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;

public class EntityAquaEdge extends ExSaEntityDrive{
    public EntityAquaEdge(World par1World) {
        super(par1World);
    }

    public EntityAquaEdge(World par1World, EntityLivingBase entityLiving, float AttackLevel, boolean multiHit, float roll) {
        super(par1World, entityLiving, AttackLevel, multiHit, roll);
    }

    public EntityAquaEdge(World par1World, EntityLivingBase entityLiving, float AttackLevel, boolean multiHit) {
        super(par1World, entityLiving, AttackLevel, multiHit);
    }

    public EntityAquaEdge(World par1World, EntityLivingBase entityLiving, float AttackLevel) {
        super(par1World, entityLiving, AttackLevel);
    }

    private void spawnParticle(Entity target) {
        target.world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, target.posX, target.posY + (double)target.height, target.posZ, 3.0, 3.0, 3.0);
        target.world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, target.posX + 1.0, target.posY + (double)target.height + 1.0, target.posZ, 3.0, 3.0, 3.0);
        target.world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, target.posX, target.posY + (double)target.height + 0.5, target.posZ + 1.0, 3.0, 3.0, 3.0);
    }

    @Override
    public void onImpact(Entity curEntity, float damage) throws NoSuchFieldException, IllegalAccessException {
        EnderTeleportCanceller.setTeleportCancel(curEntity, 100);
        this.spawnParticle(curEntity);
        if (!curEntity.world.isRemote) {
            curEntity.hurtResistantTime = 0;
            DamageSource ds = new EntityDamageSource("drown", this.getThrower()).setDamageBypassesArmor().setMagicDamage();
            curEntity.attackEntityFrom(ds, damage);
        }
        if (curEntity.isBurning()) {
            curEntity.playSound(SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.7f, 1.6f + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.4f);
            byte data = curEntity.getDataManager().get(Entity.FLAGS);
            boolean flag = false;
            int offset = 0;
            if (flag) {
                curEntity.getDataManager().set(Entity.FLAGS, (byte)(data | 1 << offset));
            } else {
                curEntity.getDataManager().set(Entity.FLAGS, (byte)(data & ~(1 << offset)));
            }
        }
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

package com.wjx.kablade.ExSA.entity;

import com.wjx.kablade.Entity.EntityDriveAdd;
import mods.flammpfeil.slashblade.ability.StylishRankManager;
import mods.flammpfeil.slashblade.entity.selector.EntitySelectorAttackable;
import mods.flammpfeil.slashblade.entity.selector.EntitySelectorDestructable;
import mods.flammpfeil.slashblade.util.ReflectionAccessHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IThrowableEntity;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class ExSaEntityDrive extends EntityDriveAdd {
    public static DataParameter<String> SOUND = EntityDataManager.createKey(ExSaEntityDrive.class,DataSerializers.STRING);
    public static DataParameter<Boolean> PLAYED_SOUND = EntityDataManager.createKey(ExSaEntityDrive.class,DataSerializers.BOOLEAN);

    public ExSaEntityDrive(World par1World) {
        super(par1World);
    }

    public ExSaEntityDrive(World par1World, EntityLivingBase entityLiving, float AttackLevel, boolean multiHit, float roll) {
        super(par1World, entityLiving, AttackLevel, multiHit, roll);
    }

    public ExSaEntityDrive(World par1World, EntityLivingBase entityLiving, float AttackLevel, boolean multiHit) {
        super(par1World, entityLiving, AttackLevel, multiHit);
    }

    public ExSaEntityDrive(World par1World, EntityLivingBase entityLiving, float AttackLevel) {
        super(par1World, entityLiving, AttackLevel);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.getDataManager().register(SOUND,"");
        this.getDataManager().register(PLAYED_SOUND,false);
    }

    public boolean getPlayed(){
        return this.getDataManager().get(ExSaEntityDrive.PLAYED_SOUND);
    }

    public void setPlayed(){
        this.getDataManager().set(ExSaEntityDrive.PLAYED_SOUND,true);
    }

    public String getSound(){
        return this.getDataManager().get(ExSaEntityDrive.SOUND);
    }

    public void playSound() {
        if (!this.getPlayed()) {
            String sound = this.getSound();
            if (sound != null && !sound.isEmpty() && this.thrower instanceof EntityPlayer) {
                this.thrower.playSound(Objects.requireNonNull(SoundEvent.REGISTRY.getObject(new ResourceLocation(sound))), 1.0f, 1.5f);
            }
            setPlayed();
        }
    }

    public void onImpact(Entity target, float damage) throws NoSuchFieldException, IllegalAccessException {
        if (target != null) {
            if (target.world.isRemote) {
                return;
            }
            target.hurtResistantTime = 0;
            DamageSource ds = new EntityDamageSource(DamageSource.MAGIC.getDamageType(), this.getThrower()).setDamageBypassesArmor().setMagicDamage();
            target.attackEntityFrom(ds, damage);
            if (this.blade != null && target instanceof EntityLivingBase) {
                this.blade.getItem().hitEntity(this.blade, (EntityLivingBase)target, (EntityLivingBase)this.thrower);
            }
        }
    }

    @Override
    public void onUpdate() {
        this.lastTickPosX = this.posX;
        this.lastTickPosY = this.posY;
        this.lastTickPosZ = this.posZ;
        if (!this.world.isRemote) {
            double dAmbit = 1.5;
            AxisAlignedBB bb = new AxisAlignedBB(this.posX - dAmbit, this.posY - dAmbit, this.posZ - dAmbit, this.posX + dAmbit, this.posY + dAmbit, this.posZ + dAmbit);
            Iterator var6;
            Entity curEntity;
            if (this.getThrower() instanceof EntityLivingBase) {
                EntityLivingBase entityLiving = (EntityLivingBase)this.getThrower();
                List<Entity> list = this.world.getEntitiesInAABBexcluding(this.getThrower(), bb, EntitySelectorDestructable.getInstance());
                StylishRankManager.setNextAttackType(this.thrower, StylishRankManager.AttackTypes.DestructObject);
                list.removeAll(this.alreadyHitEntity);
                this.alreadyHitEntity.addAll(list);
                var6 = list.iterator();

                label115:
                while(true) {
                    boolean isDestruction;
                    do {
                        if (!var6.hasNext()) {
                            break label115;
                        }

                        curEntity = (Entity)var6.next();
                        if (this.blade.isEmpty()) {
                            break label115;
                        }

                        isDestruction = true;
                        if (curEntity instanceof EntityFireball) {
                            if (((EntityFireball)curEntity).shootingEntity != null && ((EntityFireball)curEntity).shootingEntity.getEntityId() == entityLiving.getEntityId()) {
                                isDestruction = false;
                            } else {
                                isDestruction = !curEntity.attackEntityFrom(DamageSource.causeMobDamage(entityLiving), this.AttackLevel);
                            }
                        } else if (curEntity instanceof EntityArrow) {
                            if (((EntityArrow)curEntity).shootingEntity != null && ((EntityArrow)curEntity).shootingEntity.getEntityId() == entityLiving.getEntityId()) {
                                isDestruction = false;
                            }
                        } else if (curEntity instanceof IThrowableEntity) {
                            if (((IThrowableEntity)curEntity).getThrower() != null && ((IThrowableEntity)curEntity).getThrower().getEntityId() == entityLiving.getEntityId()) {
                                isDestruction = false;
                            }
                        } else if (curEntity instanceof EntityThrowable && ((EntityThrowable)curEntity).getThrower() != null && ((EntityThrowable)curEntity).getThrower().getEntityId() == entityLiving.getEntityId()) {
                            isDestruction = false;
                        }
                    } while(!isDestruction);

                    ReflectionAccessHelper.setVelocity(curEntity, 0.0, 0.0, 0.0);
                    curEntity.setDead();

                    for(int var1 = 0; var1 < 10; ++var1) {
                        Random rand = this.getRand();
                        double var2 = rand.nextGaussian() * 0.02;
                        double var4 = rand.nextGaussian() * 0.02;
                        double var6p = rand.nextGaussian() * 0.02;
                        double var8 = 10.0;
                        //this.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, curEntity.posX + (double)(rand.nextFloat() * curEntity.width * 2.0F) - (double)curEntity.width - var2 * var8, curEntity.posY + (double)(rand.nextFloat() * curEntity.height) - var4 * var8, curEntity.posZ + (double)(rand.nextFloat() * curEntity.width * 2.0F) - (double)curEntity.width - var6p * var8, var2, var4, var6p);
                    }

                    StylishRankManager.doAttack(this.thrower);
                }
            }

            if (!this.getIsMultiHit() || this.ticksExisted % 2 == 0) {
                List<Entity> list = this.world.getEntitiesInAABBexcluding(this.getThrower(), bb, EntitySelectorAttackable.getInstance());
                list.removeAll(this.alreadyHitEntity);
                if (!this.getIsMultiHit()) {
                    this.alreadyHitEntity.addAll(list);
                }

                float magicDamage = Math.max(1.0F, this.AttackLevel);
                if (this.getIsMultiHit()) {
                    StylishRankManager.setNextAttackType(this.thrower, StylishRankManager.AttackTypes.QuickDrive);
                } else {
                    StylishRankManager.setNextAttackType(this.thrower, StylishRankManager.AttackTypes.Drive);
                }
                for (Entity ccc : list) {
                    try {
                        this.onImpact(ccc, magicDamage);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            if (!this.world.getCollisionBoxes(this, this.getEntityBoundingBox()).isEmpty()) {
                this.setDead();
            }
        }

        this.motionX *= 1.0499999523162842;
        this.motionY *= 1.0499999523162842;
        this.motionZ *= 1.0499999523162842;
        this.posX += this.motionX;
        this.posY += this.motionY;
        this.posZ += this.motionZ;
        this.setPosition(this.posX, this.posY, this.posZ);
        if (this.ticksExisted >= this.getLifeTime()) {
            this.alreadyHitEntity.clear();
            this.alreadyHitEntity = null;
            this.setDead();
        }

        double m_x = this.motionX;
        double m_y = this.motionY;
        double m_z = this.motionZ;
        this.posX += this.motionX;
        this.posY += this.motionY;
        this.posZ += this.motionZ;

        int changeTime = this.getChangeTime();
        if (changeTime != 0 && this.ticksExisted >= changeTime) {
            float nextSpeed = this.getNextSpeed();
            playSound();
            this.motionX = m_x *= nextSpeed;
            this.motionY = m_y *= nextSpeed;
            this.motionZ = m_z *= nextSpeed;
            this.posX += m_x;
            this.posY += m_y;
            this.posZ += m_z;
        } else {
            float initSpeed = this.getInitSpeed();
            if (initSpeed >= 1.05f) {
                this.motionX = m_x *= initSpeed;
                this.motionY = m_y *= initSpeed;
                this.motionZ = m_z *= initSpeed;
                this.posX += m_x;
                this.posY += m_y;
                this.posZ += m_z;
            } else {
                this.posX += this.motionX * 0.1;
                this.posY += this.motionY * 0.1;
                this.posZ += this.motionZ * 0.1;
            }
        }

        if(this.getDataManager().get(PL_PARTICAL))
        {
            this.playParticle();
        }

    }
}

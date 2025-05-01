package com.wjx.kablade.ExSA.entity;

import mods.flammpfeil.slashblade.ability.StylishRankManager;
import mods.flammpfeil.slashblade.entity.selector.EntitySelectorAttackable;
import mods.flammpfeil.slashblade.entity.selector.EntitySelectorDestructable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IThrowableEntity;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class EntityLightningSword extends EntityPhantomSwordEx{
    public EntityLightningSword(World par1World) {
        super(par1World);
    }

    public EntityLightningSword(World par1World, EntityLivingBase entityLiving, float AttackLevel, float roll) {
        super(par1World, entityLiving, AttackLevel, roll);
    }
    /*public static String AttackType = StylishRankManager.AttackTypes.registerAttackType("LightningSword", -0.5F);
    double hitX;
    double hitY;
    double hitZ;
    float hitYaw;
    float hitPitch;
    Entity ridingEntity2 = null;
    float iniYaw;
    float iniPitch;
    boolean initIniYP = false;

    public EntityLightningSword(World par1World) {
        super(par1World);
    }

    public EntityLightningSword(World par1World, EntityLivingBase entityLiving, float AttackLevel, float roll) {
        super(par1World, entityLiving, AttackLevel, roll);
    }

    public void onUpdate() {
        if (this.thrower == null) {
            this.setDead();
        } else if (this.ridingEntity2 != null) {
            this.updateRidden();
        } else {
            this.lastTickPosX = this.posX;
            this.lastTickPosY = this.posY;
            this.lastTickPosZ = this.posZ;
            double dAmbit = 0.75D;
            AxisAlignedBB bb =new AxisAlignedBB(this.posX - dAmbit, this.posY - dAmbit, this.posZ - dAmbit, this.posX + dAmbit, this.posY + dAmbit, this.posZ + dAmbit);
            List<Entity> list;
            double var4;
            if (this.getThrower() instanceof EntityLivingBase) {
                EntityLivingBase entityLiving = (EntityLivingBase)this.getThrower();
                list = this.world.getEntitiesInAABBexcluding(this.getThrower(), bb, EntitySelectorDestructable.getInstance());
                StylishRankManager.setNextAttackType(this.thrower, StylishRankManager.AttackTypes.DestructObject);
                list.removeAll(this.alreadyHitEntity);
                this.alreadyHitEntity.addAll(list);
                Iterator i$ = list.iterator();

                while(i$.hasNext()) {
                    Entity curEntity = (Entity)i$.next();
                    boolean isDestruction = true;
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

                    if (isDestruction) {
                        curEntity.motionX = 0.0D;
                        curEntity.motionY = 0.0D;
                        curEntity.motionZ = 0.0D;
                        curEntity.setDead();

                        for(int var1 = 0; var1 < 10; ++var1) {
                            Random rand = this.getRand();
                            double var2 = rand.nextGaussian() * 0.02D;
                            var4 = rand.nextGaussian() * 0.02D;
                            double var6 = rand.nextGaussian() * 0.02D;
                            double var8 = 10.0D;
                            this.world.spawnParticle("explode", curEntity.posX + (double)(rand.nextFloat() * curEntity.width * 2.0F) - (double)curEntity.width - var2 * var8, curEntity.posY + (double)(rand.nextFloat() * curEntity.height) - var4 * var8, curEntity.posZ + (double)(rand.nextFloat() * curEntity.width * 2.0F) - (double)curEntity.width - var6 * var8, var2, var4, var6);
                        }

                        StylishRankManager.doAttack(this.thrower);
                        this.setDead();
                        return;
                    }
                }
            }

            list = this.world.getEntitiesInAABBexcluding(this.getThrower(), bb, EntitySelectorAttackable.getInstance());
            list.removeAll(this.alreadyHitEntity);
            if (this.getTargetEntityId() != 0) {
                Entity target = this.world.getEntityByID(this.getTargetEntityId());
                if (target != null && target.isEntityAlive() && target.getEntityBoundingBox().intersects(bb)) {
                    list.add(target);
                }
            }

            this.alreadyHitEntity.addAll(list);
            Vec3d vec31 = new Vec3d(this.posX, this.posY, this.posZ);
            Vec3d vec3 = new Vec3d(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
            double d0 = 10.0D;
            Entity hitEntity = null;
            Iterator i$ = list.iterator();

            while(true) {
                Entity curEntity;
                do {
                    do {
                        if (!i$.hasNext()) {
                            if (hitEntity != null && !hitEntity.isDead) {
                                if (!this.world.isRemote) {
                                    this.world.addWeatherEffect(new EntityLightningBolt(this.world, hitEntity.posX, hitEntity.posY, hitEntity.posZ, hitEntity, EntitySelectorAttackable.getInstance(), this.thrower));
                                }

                                StylishRankManager.doAttack(this.thrower);
                                this.setDead();
                            }

                            int nPosX = MathHelper.floor_double(this.posX);
                            int nPosY = MathHelper.floor_double(this.posY);
                            int nPosZ = MathHelper.floor_double(this.posZ);
                            if (this.ridingEntity2 == null) {
                                Block nBlock = this.worldObj.getBlock(nPosX, nPosY, nPosZ);
                                if (!nBlock.isAir(this.worldObj, nPosX, nPosY, nPosZ) && nBlock.getCollisionBoundingBoxFromPool(this.worldObj, nPosX, nPosY, nPosZ) != null) {
                                    this.setDead();
                                    return;
                                }
                            }

                            if (this.getInterval() < this.ticksExisted) {
                                this.posX += this.motionX;
                                this.posY += this.motionY;
                                this.posZ += this.motionZ;
                            } else {
                                this.doTargeting();
                            }

                            this.setPosition(this.posX, this.posY, this.posZ);
                            if (this.ticksExisted >= this.getLifeTime()) {
                                this.setDead();
                            }

                            return;
                        }

                        curEntity = (Entity)i$.next();
                    } while(!curEntity.canBeCollidedWith());

                    var4 = (double)curEntity.getDistanceToEntity(this);
                } while(!(var4 < d0) && d0 != 0.0D);

                hitEntity = curEntity;
                d0 = var4;
            }
        }
    }

    public void mountEntity(Entity par1Entity) {
        if (par1Entity != null) {
            this.hitYaw = this.rotationYaw - par1Entity.rotationYaw;
            this.hitPitch = this.rotationPitch - par1Entity.rotationPitch;
            this.hitX = this.posX - par1Entity.posX;
            this.hitY = this.posY - par1Entity.posY;
            this.hitZ = this.posZ - par1Entity.posZ;
            this.ridingEntity2 = par1Entity;
            this.ticksExisted = Math.max(0, this.getLifeTime() - 20);
        }

    }

    public void doTargeting() {
        int targetid = this.getTargetEntityId();
        if (targetid != 0) {
            Entity target = this.worldObj.getEntityByID(targetid);
            if (target != null && this.thrower != null && !this.thrower.isDead) {
                if (!this.initIniYP) {
                    this.initIniYP = true;
                    if (this.getIniPitch() < -700.0F) {
                        this.iniYaw = this.thrower.rotationYaw;
                        this.iniPitch = this.thrower.rotationPitch;
                    } else {
                        this.iniYaw = this.getIniYaw();
                        this.iniPitch = this.getIniPitch();
                    }
                }

                this.faceEntity(this, target, (float)this.ticksExisted * 1.0F, (float)this.ticksExisted * 1.0F);
                this.setDriveVector(1.75F, false);
            }
        }

    }

    public void faceEntity(Entity viewer, Entity target, float yawStep, float pitchStep) {
        double d0 = target.posX - viewer.posX;
        double d1 = target.posZ - viewer.posZ;
        double d2;
        if (target instanceof EntityLivingBase) {
            EntityLivingBase entitylivingbase = (EntityLivingBase)target;
            d2 = entitylivingbase.posY + (double)entitylivingbase.getEyeHeight() - (viewer.posY + (double)viewer.getEyeHeight());
        } else {
            d2 = (target.boundingBox.minY + target.boundingBox.maxY) / 2.0D - (viewer.posY + (double)viewer.getEyeHeight());
        }

        double d3 = (double)MathHelper.sqrt_double(d0 * d0 + d1 * d1);
        float f2 = (float)(Math.atan2(d1, d0) * 180.0D / 3.141592653589793D) - 90.0F;
        float f3 = (float)(-(Math.atan2(d2, d3) * 180.0D / 3.141592653589793D));
        this.iniPitch = this.updateRotation(this.iniPitch, f3, pitchStep);
        this.iniYaw = this.updateRotation(this.iniYaw, f2, yawStep);
    }

    private float updateRotation(float par1, float par2, float par3) {
        float f3 = MathHelper.wrapAngleTo180_float(par2 - par1);
        if (f3 > par3) {
            f3 = par3;
        }

        if (f3 < -par3) {
            f3 = -par3;
        }

        return par1 + f3;
    }

    public void setDriveVector(float fYVecOfst, boolean init) {
        super.setDriveVector(fYVecOfst, init);
        float fYawDtoR = this.iniYaw / 180.0F * 3.141593F;
        float fPitDtoR = this.iniPitch / 180.0F * 3.141593F;
        this.motionX = (double)(-MathHelper.sin(fYawDtoR) * MathHelper.cos(fPitDtoR) * fYVecOfst);
        this.motionY = (double)(-MathHelper.sin(fPitDtoR) * fYVecOfst);
        this.motionZ = (double)(MathHelper.cos(fYawDtoR) * MathHelper.cos(fPitDtoR) * fYVecOfst);
        float f3 = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
        this.rotationYaw = (float)(Math.atan2(this.motionX, this.motionZ) * 180.0D / 3.141592653589793D);
        this.rotationPitch = (float)(Math.atan2(this.motionY, (double)f3) * 180.0D / 3.141592653589793D);
        if (init) {
            this.prevRotationYaw = this.rotationYaw;
            this.prevRotationPitch = this.rotationPitch;
        }

    }*/
}

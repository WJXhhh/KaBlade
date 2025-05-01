package com.wjx.kablade.ExSA.entity;

import mods.flammpfeil.slashblade.ability.StylishRankManager;
import mods.flammpfeil.slashblade.entity.EntitySummonedSword;
import mods.flammpfeil.slashblade.entity.selector.EntitySelectorAttackable;
import mods.flammpfeil.slashblade.entity.selector.EntitySelectorDestructable;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Blocks;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IThrowableEntity;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class EntityPhantomSwordEx extends EntitySummonedSword {

    double hitX;
    double hitY;
    double hitZ;
    float hitYaw;
    float hitPitch;
    Entity ridingEntity2 = null;
    float iniYaw;
    float iniPitch;
    boolean initIniYP = false;

    public static DataParameter<Integer> INTERVAL = EntityDataManager.createKey(EntityPhantomSwordEx.class, DataSerializers.VARINT);
    public static DataParameter<Integer> COLOR = EntityDataManager.createKey(EntityPhantomSwordEx.class, DataSerializers.VARINT);
    public static DataParameter<Float> INIYAW = EntityDataManager.createKey(EntityPhantomSwordEx.class, DataSerializers.FLOAT);
    public static DataParameter<Float> INIPITCH = EntityDataManager.createKey(EntityPhantomSwordEx.class, DataSerializers.FLOAT);


    public EntityPhantomSwordEx(World par1World) {
        super(par1World);
    }

    public EntityPhantomSwordEx(World par1World, EntityLivingBase entityLiving, float AttackLevel, float roll) {
        super(par1World, entityLiving, AttackLevel, roll);
    }
    @Override
    protected void entityInit() {
        super.entityInit();

        this.getDataManager().register(INTERVAL,7);
        this.getDataManager().register(INIYAW,0f);
        this.getDataManager().register(INIPITCH,-720f);
        this.getDataManager().register(COLOR,3355647);
    }


    public int getInterval() {
        return this.getDataManager().get(INTERVAL);
    }

    public void setInterval(int value) {
        this.getDataManager().set(INTERVAL,value);
    }

    public float getIniYaw() {
        return this.getDataManager().get(INIYAW);
    }

    public void setIniYaw(float value) {
        this.getDataManager().set(INIYAW,value);
    }

    public float getIniPitch() {
        return this.getDataManager().get(INIPITCH);
    }

    public void setIniPitch(float value) {
        this.getDataManager().set(INIPITCH,value);
    }

    public int getColor() {
        return this.getDataManager().get(COLOR);
    }

    public void setColor(int value) {
        this.getDataManager().set(COLOR,value);
    }
    public void updateRidden() {
        Entity ridingEntity = this.ridingEntity2;
        if (ridingEntity.isDead) {
            this.setDead();
        } else {
            this.lastTickPosX = this.posX;
            this.lastTickPosY = this.posY;
            this.lastTickPosZ = this.posZ;
            this.posX = ridingEntity.posX + (this.hitX * Math.cos(Math.toRadians(ridingEntity.rotationYaw)) - this.hitZ * Math.sin(Math.toRadians(ridingEntity.rotationYaw)));
            this.posY = ridingEntity.posY + this.hitY;
            this.posZ = ridingEntity.posZ + this.hitX * Math.sin(Math.toRadians(ridingEntity.rotationYaw)) + this.hitZ * Math.cos(Math.toRadians(ridingEntity.rotationYaw));
            this.rotationPitch = ridingEntity.rotationPitch + this.hitPitch;
            this.rotationYaw = ridingEntity.rotationYaw + this.hitYaw;
            this.setPosition(this.posX, this.posY, this.posZ);
            this.setRotation(this.rotationYaw, this.rotationPitch);
            if (this.ticksExisted >= this.getLifeTime()) {
                if (!ridingEntity.isDead) {
                    float magicDamage = Math.max(1.0F, this.AttackLevel / 2.0F);
                    ridingEntity.hurtResistantTime = 0;
                    DamageSource ds = (new EntityDamageSource("directMagic", this.getThrower())).setDamageBypassesArmor().setMagicDamage();
                    ridingEntity.attackEntityFrom(ds, magicDamage);
                    if (this.blade != null && ridingEntity instanceof EntityLivingBase) {
                        StylishRankManager.setNextAttackType(this.thrower, StylishRankManager.AttackTypes.BreakPhantomSword);
                        this.blade.getItem().hitEntity(this.blade, (EntityLivingBase)ridingEntity, (EntityLivingBase)this.thrower);
                        ridingEntity.motionX = 0.0D;
                        ridingEntity.motionY = 0.0D;
                        ridingEntity.motionZ = 0.0D;
                        ridingEntity.addVelocity(0.0D, 0.1D, 0.0D);
                    }
                }

                this.setDead();
            }

        }
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
            AxisAlignedBB bb = new AxisAlignedBB(this.posX - dAmbit, this.posY - dAmbit, this.posZ - dAmbit, this.posX + dAmbit, this.posY + dAmbit, this.posZ + dAmbit);
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
                            this.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, curEntity.posX + (double)(rand.nextFloat() * curEntity.width * 2.0F) - (double)curEntity.width - var2 * var8, curEntity.posY + (double)(rand.nextFloat() * curEntity.height) - var4 * var8, curEntity.posZ + (double)(rand.nextFloat() * curEntity.width * 2.0F) - (double)curEntity.width - var6 * var8, var2, var4, var6);
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
                            if (hitEntity != null) {
                                float magicDamage = Math.max(1.0F, this.AttackLevel);
                                hitEntity.hurtResistantTime = 0;
                                DamageSource ds = (new EntityDamageSource("directMagic", this.getThrower())).setDamageBypassesArmor().setMagicDamage();
                                hitEntity.attackEntityFrom(ds, magicDamage);
                                if (this.blade != null && hitEntity instanceof EntityLivingBase) {
                                    StylishRankManager.setNextAttackType(this.thrower, StylishRankManager.AttackTypes.PhantomSword);
                                    this.blade.getItem().hitEntity(this.blade, (EntityLivingBase)hitEntity, (EntityLivingBase)this.thrower);
                                    hitEntity.motionX = 0.0D;
                                    hitEntity.motionY = 0.0D;
                                    hitEntity.motionZ = 0.0D;
                                    hitEntity.addVelocity(0.0D, 0.1D, 0.0D);
                                }

                                this.mountEntity(hitEntity);
                            }

                            int nPosX = (int) Math.floor(this.posX);
                            int nPosY = (int) Math.floor(this.posY);
                            int nPosZ = (int) Math.floor(this.posZ);
                            if (this.ridingEntity2 == null) {
                                Block nBlock = this.world.getBlockState(new BlockPos(nPosX, nPosY, nPosZ)).getBlock();
                                AxisAlignedBB axisalignedbb = this.world.getBlockState(new BlockPos(nPosX, nPosY, nPosZ)).getCollisionBoundingBox(this.world, new BlockPos(nPosX, nPosY, nPosZ));
                                if (!(nBlock.equals(Blocks.AIR)) && axisalignedbb != null) {
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

                    var4 = curEntity.getDistance(this);
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
            Entity target = this.world.getEntityByID(targetid);
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

                this.faceEntity(this, target, (float) this.ticksExisted, (float) this.ticksExisted);
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
            d2 = (target.getEntityBoundingBox().minY + target.getEntityBoundingBox().maxY) / 2.0D - (viewer.posY + (double)viewer.getEyeHeight());
        }

        double d3 = Math.sqrt(d0 * d0 + d1 * d1);
        float f2 = (float)(Math.atan2(d1, d0) * 180.0D / 3.141592653589793D) - 90.0F;
        float f3 = (float)(-(Math.atan2(d2, d3) * 180.0D / 3.141592653589793D));
        this.iniPitch = this.updateRotation(this.iniPitch, f3, pitchStep);
        this.iniYaw = this.updateRotation(this.iniYaw, f2, yawStep);
    }

    private float updateRotation(float par1, float par2, float par3) {
        float f3 = wrapAngleTo180_float(par2 - par1);
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
        this.motionX = -Math.sin(fYawDtoR) * Math.cos(fPitDtoR) * fYVecOfst;
        this.motionY = -Math.sin(fPitDtoR) * fYVecOfst;
        this.motionZ = Math.cos(fYawDtoR) * Math.cos(fPitDtoR) * fYVecOfst;
        float f3 = (float) Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        this.rotationYaw = (float)(Math.atan2(this.motionX, this.motionZ) * 180.0D / 3.141592653589793D);
        this.rotationPitch = (float)(Math.atan2(this.motionY, f3) * 180.0D / 3.141592653589793D);
        if (init) {
            this.prevRotationYaw = this.rotationYaw;
            this.prevRotationPitch = this.rotationPitch;
        }

    }

    public static float wrapAngleTo180_float(float p_76142_0_) {
        p_76142_0_ %= 360.0F;
        if (p_76142_0_ >= 180.0F) {
            p_76142_0_ -= 360.0F;
        }

        if (p_76142_0_ < -180.0F) {
            p_76142_0_ += 360.0F;
        }

        return p_76142_0_;
    }
}



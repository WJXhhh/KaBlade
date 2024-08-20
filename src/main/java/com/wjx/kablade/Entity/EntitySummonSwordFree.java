package com.wjx.kablade.Entity;

import com.google.common.base.Predicate;
import mods.flammpfeil.slashblade.ability.StylishRankManager;
import mods.flammpfeil.slashblade.entity.selector.EntitySelectorAttackable;
import mods.flammpfeil.slashblade.entity.selector.EntitySelectorDestructable;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.ReflectionAccessHelper;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IThrowableEntity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class EntitySummonSwordFree extends Entity implements IThrowableEntity {
    protected Entity thrower;
    protected ItemStack blade;
    protected ArrayList<Entity> alreadyHitEntity;
    protected float AttackLevel;
    private static final DataParameter<Integer> THROWER_ENTITY_ID;
    private static final DataParameter<Integer> LIFETIME;
    private static final DataParameter<Float> ROLL;
    private static final DataParameter<Integer> TARGET_ENTITY_ID;
    private static final DataParameter<Integer> INTERVAL;
    private static final DataParameter<Integer> COLOR;
    protected static final DataParameter<Float> LOCK_YAW;
    public static final DataParameter<Boolean> LIGHTING;
    float speed;
    public float iniYaw;
    public float iniPitch;
    double hitX;
    double hitY;
    double hitZ;
    float hitYaw;
    float hitPitch;
    public Entity ridingEntity2;

    public EntitySummonSwordFree(World par1World) {
        super(par1World);
        this.blade = ItemStack.EMPTY;
        this.alreadyHitEntity = new ArrayList<>();
        this.AttackLevel = 0.0F;
        this.speed = 0.0F;
        this.iniYaw = Float.NaN;
        this.iniPitch = Float.NaN;
        this.ridingEntity2 = null;
        this.noClip = true;
        this.ticksExisted = 0;
        this.setSize(0.5F, 0.5F);
    }

    public EntitySummonSwordFree(World par1World, EntityLivingBase entityLiving, float AttackLevel,double xIn, double yIn ,double zIn,float pitchIn,float yawIn) {
        this(par1World);
        this.AttackLevel = AttackLevel;
        this.setThrower(entityLiving);
        this.blade = entityLiving.getHeldItem(EnumHand.MAIN_HAND);
        if (!this.blade.isEmpty() && !(this.blade.getItem() instanceof ItemSlashBlade)) {
            this.blade = ItemStack.EMPTY;
        }
        this.setLockYaw(yawIn);
        this.alreadyHitEntity.clear();
        this.alreadyHitEntity.add(this.thrower);
        this.alreadyHitEntity.add(this.thrower.getRidingEntity());
        this.alreadyHitEntity.addAll(this.thrower.getPassengers());
        this.setLocationAndAngles(xIn, yIn, zIn, yawIn, pitchIn);
        this.iniYaw = yawIn;
        this.iniPitch = pitchIn;
        this.setDriveVector(1.75F);
    }

    protected void entityInit() {
        this.getDataManager().register(THROWER_ENTITY_ID, 0);
        this.getDataManager().register(LIFETIME, 20);
        this.getDataManager().register(ROLL, 0.0F);
        this.getDataManager().register(TARGET_ENTITY_ID, 0);
        this.getDataManager().register(INTERVAL, 7);
        this.getDataManager().register(COLOR, 3355647);
        this.getDataManager().register(LOCK_YAW,0f);
    }

    public int getThrowerEntityId() {
        return this.getDataManager().get(THROWER_ENTITY_ID);
    }

    public void setThrowerEntityId(int entityid) {
        this.getDataManager().set(THROWER_ENTITY_ID, entityid);
    }

    public int getTargetEntityId() {
        return this.getDataManager().get(TARGET_ENTITY_ID);
    }

    public void setTargetEntityId(int entityid) {
        this.getDataManager().set(TARGET_ENTITY_ID, entityid);
    }

    public float getRoll() {
        return this.getDataManager().get(ROLL);
    }

    public void setRoll(float roll) {
        this.getDataManager().set(ROLL, roll);
    }

    public int getLifeTime() {
        return this.getDataManager().get(LIFETIME);
    }

    public void setLifeTime(int lifetime) {
        this.getDataManager().set(LIFETIME, lifetime);
    }

    public int getInterval() {
        return this.getDataManager().get(INTERVAL);
    }

    public void setInterval(int value) {
        this.getDataManager().set(INTERVAL, value);
    }

    public int getColor() {
        return this.getDataManager().get(COLOR);
    }

    public void setColor(int value) {
        this.getDataManager().set(COLOR, value);
    }

    public void setLockYaw(float value){this.getDataManager().set(LOCK_YAW,value);}

    public float getLockYaw(){return this.getDataManager().get(LOCK_YAW);}

    public boolean doTargeting() {
        return this.ticksExisted <= this.getInterval();
    }

    public Entity getRayTrace(Entity owner, double reachMax) {
        return this.getRayTrace(owner, reachMax, 1.0F, 0.0F);
    }

    public Entity getRayTrace(Entity owner, double reachMax, float expandFactor, float expandBorder) {
        float par1 = 1.0F;
        RayTraceResult objectMouseOver = rayTrace(owner, reachMax, par1);
        double reachMin = reachMax;
        Vec3d entityPos = getPosition(owner);
        if (objectMouseOver != null) {
            reachMin = objectMouseOver.hitVec.distanceTo(entityPos);
        }

        Vec3d lookVec = getLook(owner, par1);
        Vec3d reachVec = entityPos.add(lookVec.x * reachMax, lookVec.y * reachMax, lookVec.z * reachMax);
        Entity pointedEntity = null;
        List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this, this.getEntityBoundingBox().offset(lookVec.x * reachMax, lookVec.y * reachMax, lookVec.z * reachMax).grow((double)expandFactor + reachMax, (double)expandFactor + reachMax, (double)expandFactor + reachMax));
        list.removeAll(this.alreadyHitEntity);
        double tmpDistance = reachMin;
        EntityLivingBase viewer = owner instanceof EntityLivingBase ? (EntityLivingBase)owner : null;
        Iterator var18 = list.iterator();

        while(true) {
            Entity entity;
            do {
                while(true) {
                    do {
                        do {
                            do {
                                do {
                                    if (!var18.hasNext()) {
                                        return pointedEntity;
                                    }

                                    entity = (Entity)var18.next();
                                } while(entity == null);
                            } while(!entity.canBeCollidedWith());
                        } while(!EntitySelectorAttackable.getInstance().apply(entity));
                    } while(viewer != null && !viewer.canEntityBeSeen(entity));

                    float borderSize = entity.getCollisionBorderSize() + expandBorder;
                    AxisAlignedBB axisalignedbb = entity.getEntityBoundingBox().grow((double)borderSize, (double)borderSize, (double)borderSize);
                    RayTraceResult movingobjectposition = axisalignedbb.calculateIntercept(entityPos, reachVec);
                    if (axisalignedbb.contains(entityPos)) {
                        break;
                    }

                    if (movingobjectposition != null) {
                        double d3 = entityPos.distanceTo(movingobjectposition.hitVec);
                        if (d3 < tmpDistance || tmpDistance == 0.0D) {
                            if (entity == this.getRidingEntity() && !entity.canRiderInteract()) {
                                if (tmpDistance == 0.0D) {
                                    pointedEntity = entity;
                                }
                            } else {
                                pointedEntity = entity;
                                tmpDistance = d3;
                            }
                        }
                    }
                }
            } while(!(0.0D < tmpDistance) && tmpDistance != 0.0D);

            pointedEntity = entity;
            tmpDistance = 0.0D;
        }
    }

    public static RayTraceResult rayTrace(Entity owner, double par1, float par3) {
        Vec3d Vec3d = getPosition(owner);
        Vec3d Vec3d1 = getLook(owner, par3);
        Vec3d Vec3d2 = Vec3d.add(Vec3d1.x * par1, Vec3d1.y * par1, Vec3d1.z * par1);
        return owner.world.rayTraceBlocks(Vec3d, Vec3d2, false, false, true);
    }

    public static Vec3d getPosition(Entity owner) {
        return new Vec3d(owner.posX, owner.posY + (double)owner.getEyeHeight(), owner.posZ);
    }

    public static Vec3d getLook(Entity owner, float rotMax) {
        float f1;
        float f2;
        float f3;
        float f4;
        if (rotMax == 1.0F) {
            f1 = MathHelper.cos(-owner.rotationYaw * 0.017453292F - 3.1415927F);
            f2 = MathHelper.sin(-owner.rotationYaw * 0.017453292F - 3.1415927F);
            f3 = -MathHelper.cos(-owner.rotationPitch * 0.017453292F);
            f4 = MathHelper.sin(-owner.rotationPitch * 0.017453292F);
            return new Vec3d((double)(f2 * f3), (double)f4, (double)(f1 * f3));
        } else {
            f1 = owner.prevRotationPitch + (owner.rotationPitch - owner.prevRotationPitch) * rotMax;
            f2 = owner.prevRotationYaw + (owner.rotationYaw - owner.prevRotationYaw) * rotMax;
            f3 = MathHelper.cos(-f2 * 0.017453292F - 3.1415927F);
            f4 = MathHelper.sin(-f2 * 0.017453292F - 3.1415927F);
            float f5 = -MathHelper.cos(-f1 * 0.017453292F);
            float f6 = MathHelper.sin(-f1 * 0.017453292F);
            return new Vec3d((double)(f4 * f5), (double)f6, (double)(f3 * f5));
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
            AxisAlignedBB boundingBox = target.getEntityBoundingBox();
            d2 = (boundingBox.minY + boundingBox.maxY) / 2.0D - (viewer.posY + (double)viewer.getEyeHeight());
        }

        double d3 = (double)MathHelper.sqrt(d0 * d0 + d1 * d1);
        float f2 = (float)(Math.atan2(d1, d0) * 180.0D / 3.141592653589793D) - 90.0F;
        float f3 = (float)(-(Math.atan2(d2, d3) * 180.0D / 3.141592653589793D));
        this.iniPitch = this.updateRotation(this.iniPitch, f3, pitchStep);
        this.iniYaw = this.updateRotation(this.iniYaw, f2, yawStep);
    }

    private float updateRotation(float par1, float par2, float par3) {
        float f3 = MathHelper.wrapDegrees(par2 - par1);
        if (f3 > par3) {
            f3 = par3;
        }

        if (f3 < -par3) {
            f3 = -par3;
        }

        return par1 + f3;
    }

    public void setDriveVector(float fYVecOfset) {
        this.setDriveVector(fYVecOfset, true);
    }

    public void setDriveVector(float fYVecOfst, boolean init) {
        this.iniYaw = this.getLockYaw();
        float fYawDtoR = this.iniYaw / 180.0F * 3.1415927F;
        float fPitDtoR = this.iniPitch / 180.0F * 3.1415927F;
        this.motionX = (double)(-MathHelper.sin(fYawDtoR) * MathHelper.cos(fPitDtoR) * fYVecOfst);
        this.motionY = (double)(-MathHelper.sin(fPitDtoR) * fYVecOfst);
        this.motionZ = (double)(MathHelper.cos(fYawDtoR) * MathHelper.cos(fPitDtoR) * fYVecOfst);
        float f3 = MathHelper.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        this.rotationYaw = (float)(Math.atan2(this.motionX, this.motionZ) * 180.0D / 3.141592653589793D);
        this.rotationPitch = (float)(Math.atan2(this.motionY, (double)f3) * 180.0D / 3.141592653589793D);
        this.rotationYaw = this.getLockYaw();
        if (init) {
            this.speed = fYVecOfst;
            this.prevRotationYaw = this.rotationYaw;
            this.prevRotationPitch = this.rotationPitch;
        }

    }

    public void updateRidden() {
        Entity ridingEntity = this.ridingEntity2;
        if (ridingEntity.isDead) {
            this.setDead();
        } else {
            this.posX = ridingEntity.posX + (this.hitX * Math.cos(Math.toRadians((double)ridingEntity.rotationYaw)) - this.hitZ * Math.sin(Math.toRadians((double)ridingEntity.rotationYaw)));
            this.posY = ridingEntity.posY + this.hitY;
            this.posZ = ridingEntity.posZ + this.hitX * Math.sin(Math.toRadians((double)ridingEntity.rotationYaw)) + this.hitZ * Math.cos(Math.toRadians((double)ridingEntity.rotationYaw));
            this.prevRotationPitch = this.rotationPitch;
            this.prevRotationYaw = this.rotationYaw;
            this.rotationPitch = ridingEntity.rotationPitch + this.hitPitch;
            this.rotationYaw = ridingEntity.rotationYaw + this.hitYaw;
            this.setPosition(this.posX, this.posY, this.posZ);
            this.setRotation(this.rotationYaw, this.rotationPitch);
            if (this.ticksExisted >= 200) {
                if (!ridingEntity.isDead && !this.world.isRemote) {
                    float magicDamage = Math.max(1.0F, this.AttackLevel / 2.0F);
                    ridingEntity.hurtResistantTime = 0;
                    DamageSource ds = (new EntityDamageSource("directMagic", this.getThrower())).setDamageBypassesArmor().setMagicDamage();
                    ridingEntity.attackEntityFrom(ds, magicDamage);
                    if (!this.blade.isEmpty() && ridingEntity instanceof EntityLivingBase) {
                        if (this.thrower != null) {
                            StylishRankManager.setNextAttackType(this.thrower, StylishRankManager.AttackTypes.BreakPhantomSword);
                            ((ItemSlashBlade)this.blade.getItem()).hitEntity(this.blade, (EntityLivingBase)ridingEntity, (EntityLivingBase)this.thrower);
                        }

                        ReflectionAccessHelper.setVelocity(ridingEntity, 0.0D, 0.0D, 0.0D);
                        ridingEntity.addVelocity(0.0D, 0.1D, 0.0D);
                        ((EntityLivingBase)ridingEntity).hurtTime = 1;
                    }
                }

                this.setDead();
            }

        }
    }

    protected void initRotation() {
        if (this.prevRotationPitch == 0.0F && this.prevRotationYaw == 0.0F) {
            float f = MathHelper.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
            this.prevRotationYaw = this.rotationYaw = (float)(Math.atan2(this.motionX, this.motionZ) * 180.0D / 3.141592653589793D);
            this.prevRotationPitch = this.rotationPitch = (float)(Math.atan2(this.motionY, (double)f) * 180.0D / 3.141592653589793D);
        }

    }

    protected RayTraceResult getcollisionRayTrace() {
        Vec3d Vec3d = new Vec3d(this.posX, this.posY, this.posZ);
        Vec3d Vec3d1 = new Vec3d(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
        RayTraceResult movingobjectposition = this.world.rayTraceBlocks(Vec3d, Vec3d1);
        Vec3d = new Vec3d(this.posX, this.posY, this.posZ);
        Vec3d1 = new Vec3d(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
        if (movingobjectposition != null) {
            IBlockState state = null;
            BlockPos pos = movingobjectposition.getBlockPos();
            if (pos != null) {
                state = this.world.getBlockState(pos);
            }

            if (state != null && state.getCollisionBoundingBox(this.world, pos) == null) {
                movingobjectposition = null;
            } else {
                Vec3d1 = new Vec3d(movingobjectposition.hitVec.x, movingobjectposition.hitVec.y, movingobjectposition.hitVec.z);
            }
        }

        Entity entity = null;
        AxisAlignedBB bb = this.getEntityBoundingBox().offset(this.motionX, this.motionY, this.motionZ).grow(1.0D, 1.0D, 1.0D);
        AxisAlignedBB bb2 = this.getEntityBoundingBox().grow(1.0D, 1.0D, 1.0D);
        Predicate<Entity>[] selectors = new Predicate[]{EntitySelectorDestructable.getInstance(), EntitySelectorAttackable.getInstance()};
        Predicate[] var8 = selectors;
        int var9 = selectors.length;

        for(int var10 = 0; var10 < var9; ++var10) {
            Predicate<Entity> selector = var8[var10];
            List list = this.world.getEntitiesInAABBexcluding(this, bb, selector);
            list.removeAll(this.alreadyHitEntity);
            if (selector.equals(EntitySelectorAttackable.getInstance()) && this.getTargetEntityId() != 0) {
                Entity target = this.world.getEntityByID(this.getTargetEntityId());
                if (target != null && (target.getEntityBoundingBox().intersects(bb) || target.getEntityBoundingBox().intersects(bb2))) {
                    list.add(target);
                }
            }

            double d0 = 0.0D;

            for (Object o : list) {
                Entity entity1 = (Entity) o;
                if ((!(entity1 instanceof mods.flammpfeil.slashblade.entity.EntitySummonedSwordBase) || ((mods.flammpfeil.slashblade.entity.EntitySummonedSwordBase) entity1).getThrower() != this.getThrower()) && entity1.canBeCollidedWith()) {
                    float f1 = 0.3F;
                    AxisAlignedBB axisalignedbb1 = entity1.getEntityBoundingBox().grow((double) f1, (double) f1, (double) f1);
                    RayTraceResult movingobjectposition1 = axisalignedbb1.calculateIntercept(Vec3d1, Vec3d);
                    if (movingobjectposition1 != null) {
                        double d1 = Vec3d1.distanceTo(movingobjectposition1.hitVec);
                        if (d1 < d0 || d0 == 0.0D) {
                            entity = entity1;
                            d0 = d1;
                        }
                    }
                }
            }

            if (entity != null) {
                movingobjectposition = new RayTraceResult(entity);
                movingobjectposition.hitInfo = selector;
                break;
            }
        }

        if (movingobjectposition != null && movingobjectposition.entityHit != null && movingobjectposition.entityHit instanceof EntityPlayer) {
            EntityPlayer entityplayer = (EntityPlayer)movingobjectposition.entityHit;
            if (entityplayer.capabilities.disableDamage || this.getThrower() != null && this.getThrower() instanceof EntityPlayer && !((EntityPlayer)this.getThrower()).canAttackPlayer(entityplayer)) {
                movingobjectposition = null;
            }
        }

        return movingobjectposition;
    }

    public void doRotation() {
        if (!this.doTargeting()) {
            float f2 = MathHelper.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
            this.rotationYaw = (float)(Math.atan2(this.motionX, this.motionZ) * 180.0D / 3.141592653589793D);

            for(this.rotationPitch = (float)(Math.atan2(this.motionY, (double)f2) * 180.0D / 3.141592653589793D); this.rotationPitch - this.prevRotationPitch < -180.0F; this.prevRotationPitch -= 360.0F) {
            }

        }
    }

    public void normalizeRotation() {
        while(this.rotationPitch - this.prevRotationPitch >= 180.0F) {
            this.prevRotationPitch += 360.0F;
        }

        while(this.rotationYaw - this.prevRotationYaw < -180.0F) {
            this.prevRotationYaw -= 360.0F;
        }

        while(this.rotationYaw - this.prevRotationYaw >= 180.0F) {
            this.prevRotationYaw += 360.0F;
        }

    }

    protected void destructEntity(Entity target) {
        if (this.thrower != null) {
            StylishRankManager.setNextAttackType(this.thrower, StylishRankManager.AttackTypes.DestructObject);
            boolean isDestruction = true;
            if (target instanceof EntityFireball) {
                if (((EntityFireball)target).shootingEntity != null && ((EntityFireball)target).shootingEntity.getEntityId() == this.thrower.getEntityId()) {
                    isDestruction = false;
                } else if (this.thrower instanceof EntityLivingBase) {
                    isDestruction = !target.attackEntityFrom(DamageSource.causeMobDamage((EntityLivingBase)this.thrower), this.AttackLevel);
                }
            } else if (target instanceof EntityArrow) {
                if (((EntityArrow)target).shootingEntity != null && ((EntityArrow)target).shootingEntity.getEntityId() == this.thrower.getEntityId()) {
                    isDestruction = false;
                }
            } else if (target instanceof EntityThrowable && ((EntityThrowable)target).getThrower() != null && ((EntityThrowable)target).getThrower().getEntityId() == this.thrower.getEntityId()) {
                isDestruction = false;
            }

            if (isDestruction && target instanceof IThrowableEntity && ((IThrowableEntity)target).getThrower() != null && ((IThrowableEntity)target).getThrower().getEntityId() == this.thrower.getEntityId()) {
                isDestruction = false;
            }

            if (isDestruction) {
                ReflectionAccessHelper.setVelocity(target, 0.0D, 0.0D, 0.0D);
                target.setDead();

                for(int var1 = 0; var1 < 10; ++var1) {
                    Random rand = this.getRand();
                    double var2 = rand.nextGaussian() * 0.02D;
                    double var4 = rand.nextGaussian() * 0.02D;
                    double var6 = rand.nextGaussian() * 0.02D;
                    double var8 = 10.0D;
                    this.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, target.posX + (double)(rand.nextFloat() * target.width * 2.0F) - (double)target.width - var2 * var8, target.posY + (double)(rand.nextFloat() * target.height) - var4 * var8, target.posZ + (double)(rand.nextFloat() * target.width * 2.0F) - (double)target.width - var6 * var8, var2, var4, var6, new int[0]);
                }
            }

            StylishRankManager.doAttack(this.thrower);
            this.setDead();
        }
    }

    protected void attackEntity(Entity target) {
        if (this.thrower != null) {
            this.thrower.getEntityData().setInteger("LastHitSummonedSwords", this.getEntityId());
        }

        this.mountEntity(target);
        if (!this.world.isRemote) {
            float magicDamage = Math.max(1.0F, this.AttackLevel);
            target.hurtResistantTime = 0;
            DamageSource ds = (new EntityDamageSource("directMagic", this.getThrower())).setDamageBypassesArmor().setMagicDamage();
            target.attackEntityFrom(ds, magicDamage);
            if (!this.blade.isEmpty() && target instanceof EntityLivingBase && this.thrower != null && this.thrower instanceof EntityLivingBase) {
                StylishRankManager.setNextAttackType(this.thrower, StylishRankManager.AttackTypes.PhantomSword);
                ((ItemSlashBlade)this.blade.getItem()).hitEntity(this.blade, (EntityLivingBase)target, (EntityLivingBase)this.thrower);
                ReflectionAccessHelper.setVelocity(target, 0.0D, 0.0D, 0.0D);
                target.addVelocity(0.0D, 0.1D, 0.0D);
                ((EntityLivingBase)target).hurtTime = 1;
                ((ItemSlashBlade)this.blade.getItem()).setDaunting((EntityLivingBase)target);
            }
        }

    }

    protected void blastAttackEntity(Entity target) {
        if (!this.world.isRemote) {
            float magicDamage = 1.0F;
            target.hurtResistantTime = 0;
            DamageSource ds = (new EntityDamageSource("directMagic", this.getThrower())).setDamageBypassesArmor().setMagicDamage();
            target.attackEntityFrom(ds, magicDamage);
            if (!this.blade.isEmpty() && target instanceof EntityLivingBase && this.thrower != null && this.thrower instanceof EntityLivingBase) {
                StylishRankManager.setNextAttackType(this.thrower, StylishRankManager.AttackTypes.PhantomSword);
                ((ItemSlashBlade)this.blade.getItem()).hitEntity(this.blade, (EntityLivingBase)target, (EntityLivingBase)this.thrower);
                ReflectionAccessHelper.setVelocity(target, 0.0D, 0.0D, 0.0D);
                target.addVelocity(0.0D, 0.1D, 0.0D);
                ((EntityLivingBase)target).hurtTime = 1;
                ((ItemSlashBlade)this.blade.getItem()).setDaunting((EntityLivingBase)target);
            }
        }

    }

    protected boolean onImpact(RayTraceResult mop) {
        boolean result = true;
        if (mop.entityHit != null) {
            Entity target = mop.entityHit;
            if (mop.hitInfo.equals(EntitySelectorAttackable.getInstance())) {
                this.attackEntity(target);
            } else {
                this.destructEntity(target);
            }
        } else if (!this.world.getCollisionBoxes(this, this.getEntityBoundingBox()).isEmpty()) {
            if (this.getThrower() != null && this.getThrower() instanceof EntityPlayer) {
                ((EntityPlayer)this.getThrower()).onCriticalHit(this);
            }

            result = false;
        }

        return result;
    }

    public void spawnParticle() {
        if (this.isInWater()) {
            for(int l = 0; l < 4; ++l) {
                float trailLength = 0.25F;
                this.world.spawnParticle(EnumParticleTypes.WATER_BUBBLE, this.posX - this.motionX * (double)trailLength, this.posY - this.motionY * (double)trailLength, this.posZ - this.motionZ * (double)trailLength, this.motionX, this.motionY, this.motionZ, new int[0]);
            }
        }

    }

    public void calculateSpeed() {
        float speedReductionFactor = 1.1F;
        if (this.isInWater()) {
            speedReductionFactor = 1.0F;
        }

        this.motionX *= (double)speedReductionFactor;
        this.motionY *= (double)speedReductionFactor;
        this.motionZ *= (double)speedReductionFactor;
    }

    public void onUpdate() {
        this.lastTickPosX = this.posX;
        this.lastTickPosY = this.posY;
        this.lastTickPosZ = this.posZ;
        super.onUpdate();
        if (this.ridingEntity2 != null) {
            this.updateRidden();
        } else {
            if (this.ticksExisted >= this.getLifeTime()) {
                this.setDead();
            }

            this.initRotation();
            RayTraceResult movingobjectposition = this.getcollisionRayTrace();
            if (movingobjectposition != null && this.onImpact(movingobjectposition)) {
                return;
            }

            this.calculateSpeed();
            this.doRotation();
            if (this.getInterval() < this.ticksExisted) {
                this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
            }

            this.normalizeRotation();
            this.spawnParticle();
        }

    }

    public void setDead() {
        if (this.thrower != null && this.thrower instanceof EntityPlayer) {
            ((EntityPlayer)this.thrower).onCriticalHit(this);
        }

        this.world.playSound((EntityPlayer)null, this.prevPosX, this.prevPosY, this.prevPosZ, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.NEUTRAL, 0.25F, 1.6F);
        AxisAlignedBB bb = this.getEntityBoundingBox().grow(1.0D, 1.0D, 1.0D);
        List<Entity> list = this.world.getEntitiesInAABBexcluding(this, bb, EntitySelectorAttackable.getInstance());
        list.removeAll(this.alreadyHitEntity);
        Iterator var3 = list.iterator();

        while(var3.hasNext()) {
            Entity target = (Entity)var3.next();
            if (this.blade.isEmpty()) {
                break;
            }

            if (target != null) {
                this.blastAttackEntity(target);
            }
        }

        super.setDead();
    }

    public Random getRand() {
        return this.rand;
    }

    public boolean isOffsetPositionInLiquid(double par1, double par3, double par5) {
        return false;
    }

    public void move(MoverType moverType, double x, double y, double z) {
        super.move(moverType, x, y, z);
    }

    protected void dealFireDamage(int par1) {
    }

    public boolean handleWaterMovement() {
        return false;
    }

    public boolean isInsideOfMaterial(Material par1Material) {
        return false;
    }

    @SideOnly(Side.CLIENT)
    public int getBrightnessForRender() {
        float f1 = 0.5F;
        if (f1 < 0.0F) {
            f1 = 0.0F;
        }

        if (f1 > 1.0F) {
            f1 = 1.0F;
        }

        int i = super.getBrightnessForRender();
        int j = i & 255;
        int k = i >> 16 & 255;
        j += (int)(f1 * 15.0F * 16.0F);
        if (j > 240) {
            j = 240;
        }

        return j | k << 16;
    }

    public float getBrightness() {
        float f1 = super.getBrightness();
        float f2 = 0.9F;
        f2 = f2 * f2 * f2 * f2;
        return f1 * (1.0F - f2) + f2;
    }

    protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
    }

    protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
    }

    public Entity getRidingEntity() {
        return this.ridingEntity2;
    }

    public void mountEntity(Entity par1Entity) {
        if (par1Entity != null) {
            this.hitYaw = this.rotationYaw - par1Entity.rotationYaw;
            this.hitPitch = this.rotationPitch - par1Entity.rotationPitch;
            this.hitX = this.lastTickPosX - par1Entity.posX;
            this.hitY = this.lastTickPosY - par1Entity.posY;
            this.hitZ = this.lastTickPosZ - par1Entity.posZ;
            this.ridingEntity2 = par1Entity;
            this.ticksExisted = 0;
        }

    }

    @SideOnly(Side.CLIENT)
    public void setPositionAndRotation2(double par1, double par3, double par5, float par7, float par8, int par9) {
    }

    public boolean isBurning() {
        return false;
    }

    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }

    public void setInWeb() {
    }

    public void setThrowableHeading(double x, double y, double z, float velocity, float inaccuracy) {
    }

    public Entity getThrower() {
        if (this.thrower == null) {
            int id = this.getThrowerEntityId();
            if (id != 0) {
                this.thrower = this.getEntityWorld().getEntityByID(id);
            }
        }

        return this.thrower;
    }

    public void setThrower(Entity entity) {
        if (entity != null) {
            this.setThrowerEntityId(entity.getEntityId());
        }

        this.thrower = entity;
    }

    static {
        THROWER_ENTITY_ID = EntityDataManager.createKey(EntitySummonSwordFree.class, DataSerializers.VARINT);
        LIFETIME = EntityDataManager.createKey(EntitySummonSwordFree.class, DataSerializers.VARINT);
        ROLL = EntityDataManager.createKey(EntitySummonSwordFree.class, DataSerializers.FLOAT);
        TARGET_ENTITY_ID = EntityDataManager.createKey(EntitySummonSwordFree.class, DataSerializers.VARINT);
        INTERVAL = EntityDataManager.createKey(EntitySummonSwordFree.class, DataSerializers.VARINT);
        COLOR = EntityDataManager.createKey(EntitySummonSwordFree.class, DataSerializers.VARINT);
        LOCK_YAW = EntityDataManager.createKey(EntitySummonSwordFree.class,DataSerializers.FLOAT);
        LIGHTING = EntityDataManager.createKey(EntitySummonSwordFree.class,DataSerializers.BOOLEAN);
    }
}

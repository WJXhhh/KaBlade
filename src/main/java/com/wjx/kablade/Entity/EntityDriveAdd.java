package com.wjx.kablade.Entity;

import com.wjx.kablade.util.Vec3f;
import mods.flammpfeil.slashblade.ability.ArmorPiercing;
import mods.flammpfeil.slashblade.ability.StylishRankManager;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.entity.selector.EntitySelectorAttackable;
import mods.flammpfeil.slashblade.entity.selector.EntitySelectorDestructable;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.ReflectionAccessHelper;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IThrowableEntity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class EntityDriveAdd extends Entity implements IThrowableEntity {
    protected Entity thrower;
    protected ItemStack blade;
    protected List<Entity> alreadyHitEntity;
    protected float AttackLevel;
    private static final DataParameter<Float> ROLL;
    private static final DataParameter<Integer> LIFETIME;
    private static final DataParameter<Boolean> IS_MULTI_HIT;
    private static final DataParameter<Boolean> IS_SLASH_DIMENSION;

    public static final DataParameter<String> PARTICLE_STYLE;

    public static final DataParameter<Float> COLOR_R;
    public static final DataParameter<Float> COLOR_G;
    public static final DataParameter<Float> COLOR_B;

    public int colors=0xFFFFFF;

    public Vec3f color3f = new Vec3f(1f,1f,1f);
    public float scaleX=0.25f;
    public float scaleY=1f;
    public float scaleZ =1f;

    public String particleO;

    public EntityDriveAdd(World par1World) {
        super(par1World);
        this.blade = ItemStack.EMPTY;
        this.alreadyHitEntity = new ArrayList<>();
        this.AttackLevel = 0.0F;
    }

    public EntityDriveAdd(World par1World, EntityLivingBase entityLiving, float AttackLevel, boolean multiHit, float roll) {
        this(par1World, entityLiving, AttackLevel, multiHit);
        this.setRoll(roll);
    }

    public EntityDriveAdd(World par1World, EntityLivingBase entityLiving, float AttackLevel, boolean multiHit) {
        this(par1World, entityLiving, AttackLevel);
        this.setIsMultiHit(multiHit);
    }

    public EntityDriveAdd(World par1World, EntityLivingBase entityLiving, float AttackLevel) {
        this(par1World);
        this.AttackLevel = AttackLevel;
        this.thrower = entityLiving;
        this.blade = entityLiving.getHeldItem(EnumHand.MAIN_HAND);
        if (!this.blade.isEmpty() && !(this.blade.getItem() instanceof ItemSlashBlade)) {
            this.blade = ItemStack.EMPTY;
        }

        this.alreadyHitEntity.clear();
        this.alreadyHitEntity.add(this.thrower);
        this.alreadyHitEntity.add(this.thrower.getRidingEntity());
        this.alreadyHitEntity.addAll(this.thrower.getPassengers());
        this.ticksExisted = 0;
        this.setSize(1.0F, 2.0F);
        this.setLocationAndAngles(this.thrower.posX, this.thrower.posY + (double)this.thrower.getEyeHeight() / 2.0, this.thrower.posZ, this.thrower.rotationYaw, this.thrower.rotationPitch);
        this.setDriveVector(0.75F);
        Vec3d motion = this.thrower.getLookVec();

        motion = motion.normalize();
        this.setPosition(this.posX + motion.x * 20.0, this.posY + motion.y * 20.0, this.posZ + motion.z * 20.0);
    }

    protected void entityInit() {
        this.getDataManager().register(ROLL, 0.0F);
        this.getDataManager().register(LIFETIME, 20);
        this.getDataManager().register(IS_MULTI_HIT, false);
        this.getDataManager().register(IS_SLASH_DIMENSION, false);
        this.getDataManager().register(PARTICLE_STYLE,"EXPLOSION_NORMAL");
        this.getDataManager().register(COLOR_R,1f);
        this.getDataManager().register(COLOR_G,1f);
        this.getDataManager().register(COLOR_B,1f);

    }

    public boolean getIsMultiHit() {
        return (Boolean)this.getDataManager().get(IS_MULTI_HIT);
    }

    public void setIsMultiHit(boolean isMultiHit) {
        this.getDataManager().set(IS_MULTI_HIT, isMultiHit);
    }

    public float getRoll() {
        return (Float)this.getDataManager().get(ROLL);
    }

    public void setRoll(float roll) {
        this.getDataManager().set(ROLL, roll);
    }

    public int getLifeTime() {
        return (Integer)this.getDataManager().get(LIFETIME);
    }

    public void setLifeTime(int lifetime) {
        this.getDataManager().set(LIFETIME, lifetime);
    }

    public boolean getIsSlashDimension() {
        return (Boolean)this.getDataManager().get(IS_SLASH_DIMENSION);
    }

    public void setIsSlashDimension(boolean isSlashDimension) {
        this.getDataManager().set(IS_SLASH_DIMENSION, isSlashDimension);
    }

    public void setInitialSpeed(float f) {
        this.setLocationAndAngles(this.thrower.posX, this.thrower.posY + (double)this.thrower.getEyeHeight() / 2.0, this.thrower.posZ, this.thrower.rotationYaw, this.thrower.rotationPitch);
        this.setDriveVector(f);
        Vec3d motion = this.thrower.getLookVec();
        if (motion == null) {
            motion = new Vec3d(this.motionX, this.motionY, this.motionZ);
        }

        motion = motion.normalize();
        this.setPosition(this.posX + motion.x * 1.0, this.posY + motion.y * 1.0, this.posZ + motion.z * 1.0);
    }

    public void setDriveVector(float fYVecOfst) {
        float fYawDtoR = this.rotationYaw / 180.0F * 3.1415927F;
        float fPitDtoR = this.rotationPitch / 180.0F * 3.1415927F;
        this.motionX = (double)(-MathHelper.sin(fYawDtoR) * MathHelper.cos(fPitDtoR) * fYVecOfst);
        this.motionY = (double)(-MathHelper.sin(fPitDtoR) * fYVecOfst);
        this.motionZ = (double)(MathHelper.cos(fYawDtoR) * MathHelper.cos(fPitDtoR) * fYVecOfst);
        float f3 = MathHelper.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        this.prevRotationYaw = this.rotationYaw = (float)(Math.atan2(this.motionX, this.motionZ) * 180.0 / Math.PI);
        this.prevRotationPitch = this.rotationPitch = (float)(Math.atan2(this.motionY, (double)f3) * 180.0 / Math.PI);
    }

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
                        this.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, curEntity.posX + (double)(rand.nextFloat() * curEntity.width * 2.0F) - (double)curEntity.width - var2 * var8, curEntity.posY + (double)(rand.nextFloat() * curEntity.height) - var4 * var8, curEntity.posZ + (double)(rand.nextFloat() * curEntity.width * 2.0F) - (double)curEntity.width - var6p * var8, var2, var4, var6p, new int[0]);
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

                var6 = list.iterator();

                while(var6.hasNext()) {
                    curEntity = (Entity)var6.next();
                    if (this.blade.isEmpty()) {
                        break;
                    }

                    if (this.getIsSlashDimension()) {
                        ArmorPiercing.doAPAttack(curEntity, magicDamage);
                    }

                    curEntity.hurtResistantTime = 0;
                    DamageSource ds = (new EntityDamageSource("directMagic", this.getThrower())).setDamageBypassesArmor().setMagicDamage();
                    curEntity.attackEntityFrom(ds, magicDamage);
                    if (!this.blade.isEmpty() && curEntity instanceof EntityLivingBase) {
                        ((ItemSlashBlade)this.blade.getItem()).hitEntity(this.blade, (EntityLivingBase)curEntity, (EntityLivingBase)this.thrower);
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
        this.playParticle();

    }

    public Random getRand() {
        return this.rand;
    }

    public boolean isOffsetPositionInLiquid(double par1, double par3, double par5) {
        return false;
    }

    public void move(MoverType moverType, double par1, double par3, double par5) {
    }

    protected void dealFireDamage(int par1) {
    }

    public boolean handleWaterMovement() {
        return false;
    }

    public boolean isInsideOfMaterial(Material par1Material) {
        return false;
    }

    public boolean isInLava() {
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

    @SideOnly(Side.CLIENT)
    public void setPositionAndRotation2(double par1, double par3, double par5, float par7, float par8, int par9) {
    }

    public void setPortal(BlockPos pos) {
    }

    public boolean isBurning() {
        return false;
    }

    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }

    public void setInWeb() {
    }

    public Entity getThrower() {
        return this.thrower;
    }

    public void setThrower(Entity entity) {
        this.thrower = entity;
    }

    int i;
    public void playParticle() {
        this.i = 0;
        while (this.i < 20) {
            String particle = this.dataManager.get(PARTICLE_STYLE);
            if (!particle.isEmpty()) {
                Random rand = new Random();
                double var2 = rand.nextGaussian() * 0.12;
                double var4 = rand.nextGaussian() * 0.12;
                double var6 = rand.nextGaussian() * 0.12;
                double var8 = 10.0;
                this.world.spawnParticle(EnumParticleTypes.valueOf(particle), this.posX + (double)(rand.nextFloat() * this.width * 2.0f) - (double)this.width - var2 * var8, this.posY + (double)(rand.nextFloat() * this.height) - var4 * var8, this.posZ + (double)(rand.nextFloat() * this.width * 2.0f) - (double)this.width - var6 * var8, var2, var4, var6);
            }
            ++this.i;
        }
    }

    static {
        ROLL = EntityDataManager.createKey(EntityDriveAdd.class, DataSerializers.FLOAT);
        LIFETIME = EntityDataManager.createKey(EntityDriveAdd.class, DataSerializers.VARINT);
        IS_MULTI_HIT = EntityDataManager.createKey(EntityDriveAdd.class, DataSerializers.BOOLEAN);
        IS_SLASH_DIMENSION = EntityDataManager.createKey(EntityDriveAdd.class, DataSerializers.BOOLEAN);
        PARTICLE_STYLE = EntityDataManager.createKey(EntityDriveAdd.class,DataSerializers.STRING);
        COLOR_R = EntityDataManager.createKey(EntityDriveAdd.class,DataSerializers.FLOAT);
        COLOR_G = EntityDataManager.createKey(EntityDriveAdd.class,DataSerializers.FLOAT);
        COLOR_B = EntityDataManager.createKey(EntityDriveAdd.class,DataSerializers.FLOAT);
    }
}


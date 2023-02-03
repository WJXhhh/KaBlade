package com.wjx.kablade.util.slash;

import mods.flammpfeil.slashblade.ability.ArmorPiercing;
import mods.flammpfeil.slashblade.ability.StylishRankManager;
import mods.flammpfeil.slashblade.ability.TeleportCanceller;
import mods.flammpfeil.slashblade.entity.EntitySlashDimension;
import mods.flammpfeil.slashblade.entity.selector.EntitySelectorAttackable;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.ReflectionAccessHelper;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Enchantments;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EntityDelete extends EntitySlashDimension {
    protected Entity thrower;
    protected ItemStack blade = ItemStack.EMPTY;
    protected List<Entity> alreadyHitEntity = new ArrayList<>();

    protected float AttackLevel = 0.0f;

    public EntityDelete(World world){
        super(world);
        this.ticksExisted = 0;
        getEntityData().setInteger("seed",this.rand.nextInt(50));
    }

    public EntityDelete(World world, EntityLivingBase entityLivingBase, float AttackLevel, boolean multiHit){
        this(world,entityLivingBase,AttackLevel);
        setIsSingleHit(multiHit);
    }

    public EntityDelete(World world, EntityLivingBase entityLivingBase, float AttackLevel){
        this(world);
        this.AttackLevel = AttackLevel;
        this.thrower = (Entity) entityLivingBase;
        if(this.blade.isEmpty() && !(this.blade.getItem() instanceof ItemSlashBlade)) this.blade = ItemStack.EMPTY;
        this.alreadyHitEntity.clear();
        this.alreadyHitEntity.add(this.thrower);
        this.alreadyHitEntity.add(this.thrower.getRidingEntity());
        this.alreadyHitEntity.addAll(this.thrower.getPassengers());
        this.ticksExisted = 0;
        this.setSize(4.0f,4.0f);
    }

    private static final DataParameter<Integer> LIFETIME = EntityDataManager.createKey(EntityDelete.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> SINGLE_HIT = EntityDataManager.createKey(EntityDelete.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> IS_SLASH_DIMENSION = EntityDataManager.createKey(EntityDelete.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> THROWER_ENTITY_ID = EntityDataManager.createKey(EntityDelete.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> INTERVAL = EntityDataManager.createKey(EntityDelete.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> COLOR = EntityDataManager.createKey(EntityDelete.class, DataSerializers.VARINT);

    protected void entityInit(){
        this.dataManager.register(LIFETIME, Integer.valueOf(20));
        this.dataManager.register(SINGLE_HIT, Boolean.valueOf(false));
        this.dataManager.register(IS_SLASH_DIMENSION, Boolean.valueOf(false));
        this.dataManager.register(THROWER_ENTITY_ID, Integer.valueOf(0));
        this.dataManager.register(INTERVAL, Integer.valueOf(7));
        this.dataManager.register(COLOR, Integer.valueOf(3355647));
    }

    public boolean getIsSingleHit(){
        return ((Boolean)this.dataManager.get(SINGLE_HIT)).booleanValue();
    }

    public void setIsSingleHit(boolean isSingleHit){
        this.dataManager.set(SINGLE_HIT, Boolean.valueOf(isSingleHit));
    }

    public int getLifeTime(){
        return ((Integer)this.dataManager.get(LIFETIME)).intValue();
    }

    public void setLifeTime(int lifeTime){
        this.dataManager.set(LIFETIME, Integer.valueOf(lifeTime));
    }

    public boolean getIsSlashDimension(){
        return ((Boolean)this.dataManager.get(IS_SLASH_DIMENSION)).booleanValue();
    }

    public void setIsSlashDimension(boolean isSlashDimension){
        this.dataManager.set(IS_SLASH_DIMENSION, Boolean.valueOf(isSlashDimension));
    }

    public int getInterval(){
        return ((Integer)this.dataManager.get(INTERVAL)).intValue();
    }

    public void setInterval(int value){
        this.dataManager.set(INTERVAL, Integer.valueOf(value));
    }

    public int getColor(){
        return ((Integer)this.dataManager.get(COLOR)).intValue();
    }

    public void setColor(int value){
        this.dataManager.set(COLOR, Integer.valueOf(value));
    }

    public int getThrowerEntityId(){
        return ((Integer)this.dataManager.get(THROWER_ENTITY_ID)).intValue();
    }

    public void setThrowerEntityId(int value){
        this.dataManager.set(THROWER_ENTITY_ID, Integer.valueOf(value));
    }

    public void onUpdate(){
        super.onUpdate();
        this.lastTickPosX = this.posX;
        this.lastTickPosY = this.posY;
        this.lastTickPosZ = this.posZ;
        if(!this.world.isRemote){
            if(this.ticksExisted < 8 && this.ticksExisted % 2 == 0) playSound(SoundEvents.ENTITY_WITHER_HURT, 0.2f, 0.5f +0.25f * this.rand.nextFloat());
            if(getThrower() != null){
                AxisAlignedBB bb = getEntityBoundingBox();
                if(getThrower() instanceof EntityLivingBase){
                    EntityLivingBase entityLivingBase = (EntityLivingBase) getThrower();
                    List<Entity> list = new ArrayList<>();
                    StylishRankManager.setNextAttackType(this.thrower, StylishRankManager.AttackTypes.DestructObject);
                    list.removeAll(this.alreadyHitEntity);
                    this.alreadyHitEntity.addAll(list);
                    for(Entity curEntity : list){
                        if(this.blade.isEmpty()) break;
                        boolean isDestruction = true;
                        Delete.removeEntity(curEntity, this.thrower, Boolean.valueOf(false));
                        if(curEntity instanceof EntityFireball){
                            if(((EntityFireball)curEntity).shootingEntity != null && ((EntityFireball)curEntity).shootingEntity.getEntityId() == entityLivingBase.getEntityId()){
                                isDestruction = false;
                            } else {
                                isDestruction = !curEntity.attackEntityFrom(DamageSource.causeMobDamage(entityLivingBase), this.AttackLevel);
                            }
                        } else if(curEntity instanceof EntityArrow){
                            if(((IThrowableEntity)curEntity).getThrower() != null &&((IThrowableEntity)curEntity).getThrower().getEntityId() == entityLivingBase.getEntityId()) isDestruction = false;
                        } else if(curEntity instanceof IThrowableEntity && ((EntityThrowable)curEntity).getThrower() != null && ((EntityThrowable)curEntity).getThrower().getEntityId() == entityLivingBase.getEntityId()){
                            isDestruction = false;
                        }
                        if(isDestruction){
                            ReflectionAccessHelper.setVelocity(curEntity, 0.0d, 0.0d, 0.0d);
                            curEntity.setDead();
                            for(int var1 = 0; var1 < 10; var1++) {
                                Random random = getRand();
                                double var2 = random.nextGaussian() * 0.02D;
                                double var4 = random.nextGaussian() * 0.02D;
                                double var6 = random.nextGaussian() * 0.02D;
                                double var8 = 10.0D;
                                this.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, curEntity.posX + (double) (random.nextFloat() * curEntity.width * 2.0F) - (double) curEntity.width - var2 * var8, curEntity.posY + (double) (random.nextFloat() * curEntity.height) - var4 * var8, curEntity.posZ + (double) (random.nextFloat() * curEntity.width * 2.0F) - (double) curEntity.width - var6 * var8, var2, var4, var6, new int[0]);
                            }
                            StylishRankManager.doAttack(this.thrower);
                        }
                    }
                }
                if(getIsSingleHit() || this.ticksExisted % 2 == 0){
                    List<Entity> list1 = this.world.getEntitiesInAABBexcluding(getThrower(), bb, EntitySelectorAttackable.getInstance());
                    list1.removeAll(this.alreadyHitEntity);
                    if(getIsSingleHit()) this.alreadyHitEntity.addAll(list1);
                    float magicDamage = Math.max(1.0f,this.AttackLevel);
                    StylishRankManager.setNextAttackType(this.thrower, StylishRankManager.AttackTypes.SlashDimMagic);
                    for(Entity curEntity : list1){
                        Delete.removeEntity(curEntity,this.thrower,Boolean.valueOf(false));
                        if(this.blade.isEmpty()) break;
                        if(getIsSlashDimension()) ArmorPiercing.doAPAttack(curEntity,magicDamage);
                        Vec3d pos = curEntity.getPositionVector();
                        TeleportCanceller.setCancel(curEntity);
                        curEntity.hurtResistantTime = 0;
                        DamageSource ds = (new EntityDamageSource("transcend", getThrower())).setDamageBypassesArmor().setMagicDamage().setProjectile();
                        if(!this.blade.isEmpty() && curEntity instanceof EntityLivingBase) ((ItemSlashBlade)this.blade.getItem()).hitEntity(this.blade, (EntityLivingBase)curEntity, (EntityLivingBase)this.thrower);
                        if(!curEntity.getPositionVector().equals(pos)) curEntity.setPositionAndUpdate(pos.x, pos.y, pos.z); curEntity.motionX = 0.0d; curEntity.motionY = 0.0d; curEntity.motionZ = 0.0d;
                        if(3 < this.ticksExisted && !this.blade.isEmpty() && curEntity instanceof EntityLivingBase){
                            if(getIsSlashDimension()){
                                curEntity.addVelocity(0.0d,0.5d,0.0d);
                                continue;
                            }
                            int level = EnchantmentHelper.getEnchantmentLevel(Enchantments.PUNCH, this.blade);
                            if(level > 0){
                                curEntity.addVelocity( Math.sin(((getThrower()).rotationYaw * 3.1415927F / 180.0F)) * level * 0.5D, 0.2D,-Math.cos(((getThrower()).rotationYaw * 3.1415927F / 180.0F)) * level * 0.5D);
                                continue;
                            }
                            curEntity.addVelocity( Math.sin(((getThrower()).rotationYaw * 3.1415927F / 180.0F)) * 0.5D, 0.1D,-Math.cos(((getThrower()).rotationYaw * 3.1415927F / 180.0F)) * 0.5D);
                        }
                    }
                }
            }
        }
        if(this.ticksExisted >= getLifeTime()){
            this.alreadyHitEntity.clear();
            this.alreadyHitEntity = null;
            setDead();
            this.isDead = true;
        }
    }

    public Random getRand(){
        return this.rand;
    }

    public boolean isOffsetPositionInLiquid(double x, double y, double z){
        return false;
    }

    public void move(MoverType moverType,double x,double y,double z){}

    protected void dealFireDamage(int amount){}

    public boolean handleWaterMovement(){
        return false;
    }

    public boolean isInLava(){
        return false;
    }

    @SideOnly(Side.CLIENT)
    public int getBrightnessForRender(){
        float f = 0.5f;
        if(f < 0.0f) f = 0.0f;
        if(f > 1.0f) f = 1.0f;
        int i = super.getBrightnessForRender();
        int j = i & 0xFF;
        int k = i >> 16 & 0xFF;
        j += (int)(f * 15.0f * 16.0f);
        if(j > 240) j = 240;
        return j | k << 16;
    }

    public float getBrightness(){
        float f1 = super.getBrightness();
        float f2 = 0.9f;
        f2 = f2 * f2 * f2 * f2;
        return f1 * (1.0f - f2) + f2;
    }

    protected void readEntityFromNBT(NBTTagCompound nbtTagCompound){}

    protected void 	writeEntityToNBT(NBTTagCompound nbtTagCompound){}

    @SideOnly(Side.CLIENT)
    public void setPositionAndRotation2(double par1, double par3, double par5, float par7, float par8, int par9) {}

    public void setPortal(BlockPos pos){}

    public boolean isBurning(){
        return false;
    }

    public boolean shouldRenderInPass(int pass) {
        return (pass == 1);
    }

    public void setInWeb() {}

    public Entity getThrower(){
        if(this.thrower == null){
            int id = getThrowerEntityId();
            if(id != 0) this.thrower = getEntityWorld().getEntityByID(id);
        }
        return this.thrower;
    }

    public void setThrower(Entity entity){
        if(entity != null) setThrowerEntityId(entity.getEntityId());
        this.thrower = entity;
    }
}

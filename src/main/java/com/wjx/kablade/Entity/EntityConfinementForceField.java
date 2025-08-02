package com.wjx.kablade.Entity;

import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.ability.StylishRankManager;
import mods.flammpfeil.slashblade.entity.selector.EntitySelectorAttackable;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IThrowableEntity;

import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class EntityConfinementForceField extends Entity implements IThrowableEntity {

    //public static DataParameter<Integer> renderTick = EntityDataManager.createKey(EntityThunderEdgeAttack.class, DataSerializers.VARINT);


    public Timer timer;

    public int ftick = 0;

    private TimerTask task = new TimerTask() {
        @Override
        public void run() {
            ftick++;
        }
    };

    public EntityLivingBase owner = null;

    //public float bladeDamage = 10.0F;

    public ItemStack blade = null;

    public EntityConfinementForceField(World world) {
        super(world);
        ticksExisted = 0;
        timer = new Timer();
        timer.schedule(task, 0, 5);

    }

    public EntityConfinementForceField(World world, EntityLivingBase ownerIn) {
        this(world);
        owner = ownerIn;
        this.setPositionAndUpdate(owner.posX, owner.posY, owner.posZ);
        this.rotationYaw = ownerIn.rotationYaw;
    }


    @Override
    protected void entityInit() {

    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {

    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {

    }

    @Override
    public Entity getThrower() {
        return owner;
    }

    @Override
    public void setThrower(Entity entity) {
        owner = (EntityLivingBase) entity;
    }

    @Override
    public void onEntityUpdate() {
        super.onEntityUpdate();
        if (this.ticksExisted == 5) {
            World world1 = this.world;
            if (!world1.isRemote) {
                List<Entity> list = world.getEntitiesInAABBexcluding(this, this.getEntityBoundingBox().grow(5.0D, 2.0D, 5.0D), EntitySelectorAttackable.getInstance());
                for (Entity entity : list) {

                    {
                        if (entity instanceof EntityLivingBase && !(entity.equals(owner))) {
                            EntityLivingBase living = (EntityLivingBase) entity;
                            if (owner instanceof EntityPlayer) {
                                float extraDamage = 0;
                                if (blade != null) {
                                    blade.getItem().hitEntity(blade, living, (EntityPlayer) owner);

                                    extraDamage = MathFunc.amplifierCalc(ItemSlashBlade.BaseAttackModifier.get(blade.getTagCompound()), 5f);
                                }
                                StylishRankManager.setNextAttackType(this.owner, StylishRankManager.AttackTypes.SlashDim);

                                StylishRankManager.doAttack(this.owner);
                                ((EntityPlayer) owner).onCriticalHit( living);
                                living.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer) owner), 10.0F + extraDamage);
                                if (blade != null) {
                                    blade.getItem().hitEntity(blade, living, (EntityPlayer) owner);


                                }


                            } else {
                                living.attackEntityFrom(DamageSource.causeMobDamage(owner), 10.0F);
                            }


                        }
                    }
                }

            }
        }
        if (this.ticksExisted > 40) {
            World world1 = this.world;
            if (world1.isRemote) {
                Random rand = this.rand;
                for (int i = 0; i < 10; i++) {
                    world1.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, this.posX + (rand.nextDouble() - 0.5)*4, this.posY+1 + (rand.nextDouble() - 0.5), this.posZ + (rand.nextDouble() - 0.5)*4, (rand.nextInt(5) - 3) / 10d, (rand.nextInt(5) - 3) / 10d, (rand.nextInt(5) - 3) / 10d);
                    world1.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, this.posX + (rand.nextDouble() - 0.5)*4, this.posY+1 + (rand.nextDouble() - 0.5), this.posZ + (rand.nextDouble() - 0.5)*4, (rand.nextInt(5) - 3) / 10d, (rand.nextInt(5) - 3) / 10d, (rand.nextInt(5) - 3) / 10d);
                    world1.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, this.posX + (rand.nextDouble() - 0.5)*4, this.posY+1 + (rand.nextDouble() - 0.5), this.posZ + (rand.nextDouble() - 0.5)*4, (rand.nextInt(5) - 3) / 10d, (rand.nextInt(5) - 3) / 10d, (rand.nextInt(5) - 3) / 10d);
                }
                world1.playSound(this.posX, this.posY, this.posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.NEUTRAL, 1.0F, 1.0F, false);
            }
            if (!world1.isRemote) {
                List<Entity> list = world.getEntitiesInAABBexcluding(this, this.getEntityBoundingBox().grow(5.0D, 2.0D, 5.0D), EntitySelectorAttackable.getInstance());
                for (Entity entity : list) {
                    if (entity instanceof EntityLivingBase && !(entity.equals(owner))) {
                        EntityLivingBase living = (EntityLivingBase) entity;
                        if (owner instanceof EntityPlayer) {
                            float extraDamage = 0;
                            if (blade != null) {
                                blade.hitEntity(living, (EntityPlayer) owner);
                                extraDamage = MathFunc.amplifierCalc(ItemSlashBlade.BaseAttackModifier.get(blade.getTagCompound()), 5f);
                            }
                            living.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer) owner), 14.0F + extraDamage);
                            if (blade != null) {
                                blade.getItem().hitEntity(blade, living, (EntityPlayer) owner);


                            }
                            StylishRankManager.doAttack(this.owner);
                        } else {
                            living.attackEntityFrom(DamageSource.causeMobDamage(owner), 10.0F);
                        }
                    }
                }
            }

            this.setDead();
            if(world1.isRemote){
                this.isDead = true;
            }
            timer.cancel();
        }
    }
}

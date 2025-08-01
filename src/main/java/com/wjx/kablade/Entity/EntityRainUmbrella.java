package com.wjx.kablade.Entity;

import com.wjx.kablade.Main;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.ParticleManager;
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

public class EntityRainUmbrella extends Entity implements IThrowableEntity {

    //public static DataParameter<Integer> renderTick = EntityDataManager.createKey(EntityThunderEdgeAttack.class, DataSerializers.VARINT);



    public EntityLivingBase owner = null;

    //public float bladeDamage = 10.0F;

    public ItemStack blade = null;

    public EntityRainUmbrella(World world) {
        super(world);
        ticksExisted = 0;


    }

    public EntityRainUmbrella(World world, EntityLivingBase ownerIn) {
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
        if(ticksExisted>=0&&ticksExisted<=5){
            if(world.isRemote){
                for(int i=0;i<5;i++){
                    float speed = (float) rand.nextInt(1500) /7000;
                    float direction = (float) (rand.nextFloat()*2*Math.PI);
                    float x = (float) (Math.cos(direction)*speed);
                    float z = (float) (Math.sin(direction)*speed);
                    ParticleManager.spawnPetalParticle(this.posX, this.posY+0.8+((rand.nextFloat())/10), this.posZ, x, (rand.nextFloat()-0.5)/10, z,1+rand.nextInt(3));
                }
                for(int i=0;i<1000;i++){
                    float speed = (float) rand.nextInt(1500) /7000;
                    float direction = (float) (rand.nextFloat()*2*Math.PI);
                    float x = (float) (Math.cos(direction)*speed);
                    float z = (float) (Math.sin(direction)*speed);
                    ParticleManager.spawnCustomParticle(this.posX, this.posY+0.8+((rand.nextFloat())/10), this.posZ, x, (rand.nextFloat()-0.5)/5, z);
                }
            }
        }
        if (ticksExisted > 5&&ticksExisted < 70){
            if(!world.isRemote){
                float extradamage = 0f;
                if(blade!=null){
                    extradamage = MathFunc.amplifierCalc(ItemSlashBlade.BaseAttackModifier.get(blade.getTagCompound()), 1f);
                }
                for (int i = 0; i < 4; i++) {
                    EntitySummonHedra hedra = new EntitySummonHedra(world, owner, (2F+extradamage)/3, this.posX, this.posY+1f, this.posZ, 0, 360*rand.nextFloat());
                    world.spawnEntity(hedra);
                }
            }
        }
        if (ticksExisted > 70&&ticksExisted < 75){
            if(world.isRemote){
                for(int i=0;i<20;i++){
                    float speed = (float) rand.nextInt(1500) /7000;
                    float direction = (float) (rand.nextFloat()*2*Math.PI);
                    float x = (float) (Math.cos(direction)*speed);
                    float z = (float) (Math.sin(direction)*speed);
                    ParticleManager.spawnPetalParticle(this.posX, this.posY+0.8+((rand.nextFloat())/10), this.posZ, x, (rand.nextFloat()-0.5)/10, z,1+rand.nextInt(3));
                }
                for(int i=0;i<500;i++){
                    float speed = (float) rand.nextInt(1500) /7000;
                    float direction = (float) (rand.nextFloat()*2*Math.PI);
                    float x = (float) (Math.cos(direction)*speed);
                    float z = (float) (Math.sin(direction)*speed);
                    ParticleManager.spawnCustomParticle(this.posX, this.posY+0.8+((rand.nextFloat())/10), this.posZ, x, (rand.nextFloat()-0.5)/10, z);
                }
            }
        }
        if (this.ticksExisted > 100) {

            this.setDead();


        }
    }
}

package com.wjx.kablade.Entity;

import com.wjx.kablade.Main;
import com.wjx.kablade.network.MessageSpawnLighParticleOn;
import com.wjx.kablade.util.KaBladeEntityProperties;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.ParticleManager;
import mods.flammpfeil.slashblade.ability.StylishRankManager;
import mods.flammpfeil.slashblade.entity.selector.EntitySelectorAttackable;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.IThrowableEntity;

import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

@Mod.EventBusSubscriber(modid = Main.MODID)
public class EntityConfinementForceField extends Entity implements IThrowableEntity {

    //public static DataParameter<Integer> renderTick = EntityDataManager.createKey(EntityThunderEdgeAttack.class, DataSerializers.VARINT);




    public EntityLivingBase owner = null;

    //public float bladeDamage = 10.0F;

    public ItemStack blade = null;

    public EntityConfinementForceField(World world) {
        super(world);
        ticksExisted = 0;


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
        if(this.ticksExisted > 100){
            this.setDead();
        }
        if(!world.isRemote)
        {
            List<Entity> list = world.getEntitiesInAABBexcluding(this, this.getEntityBoundingBox().grow(6.0D, 2.0D, 6.0D), EntitySelectorAttackable.getInstance());
            for (Entity e : list) {
                if (e instanceof EntityLivingBase) {
                    if (e != owner && owner instanceof EntityPlayer) {
                        ((EntityLivingBase) e).attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer) owner), 4.0F);
                        if (blade != null && owner instanceof EntityPlayer) {
                            blade.hitEntity((EntityLivingBase) e, (EntityPlayer) owner);
                        }
                        NBTTagCompound entityProperties = KaBladeEntityProperties.getPropCompound(e);
                        entityProperties.setInteger(KaBladeEntityProperties.CONFINEMENT,2);
                        ((EntityLivingBase) e).addPotionEffect(new PotionEffect(MobEffects.SLOWNESS,5,3));
                        if(ticksExisted%2==0)
                        {
                            for (int i = 0; i < 1; i++) {
                                float speed = (float) rand.nextInt(1500) / 7000;
                                float direction = (float) (rand.nextFloat() * 2 * Math.PI);
                                float x = (float) (Math.cos(direction) * speed);
                                float z = (float) (Math.sin(direction) * speed);
                                Main.PACKET_HANDLER.sendToAll(new MessageSpawnLighParticleOn(e.posX, e.posY + 1 + ((rand.nextFloat()) / 10), e.posZ, x, (rand.nextFloat() - 0.5) / 10, z));
                            }
                        }

                    } else if(e!=owner){
                        ((EntityLivingBase) e).attackEntityFrom(DamageSource.causeMobDamage(owner), 4.0F);
                        NBTTagCompound entityProperties = KaBladeEntityProperties.getPropCompound(e);
                        entityProperties.setInteger(KaBladeEntityProperties.CONFINEMENT,2);
                        ((EntityLivingBase) e).addPotionEffect(new PotionEffect(MobEffects.SLOWNESS,5,3));
                        if(ticksExisted%2==0)
                        {
                            for (int i = 0; i < 1; i++) {
                                float speed = (float) rand.nextInt(1500) / 7000;
                                float direction = (float) (rand.nextFloat() * 2 * Math.PI);
                                float x = (float) (Math.cos(direction) * speed);
                                float z = (float) (Math.sin(direction) * speed);
                                Main.PACKET_HANDLER.sendToAll(new MessageSpawnLighParticleOn(e.posX, e.posY + 1 + ((rand.nextFloat()) / 10), e.posZ, x, (rand.nextFloat() - 0.5) / 10, z));
                            }
                        }
                    }


                }
            }
        }
        /*if (world.isRemote) {
            List<Entity> list = world.getEntitiesInAABBexcluding(this, this.getEntityBoundingBox().grow(1.0D, 1.0D, 1.0D), EntitySelectorAttackable.getInstance());
            for (Entity e : list) {
                if (e instanceof EntityLivingBase) {
                    if (e != owner && owner instanceof EntityPlayer) {
                        for(int i=0;i<1;i++){
                            float speed = (float) rand.nextInt(1500) /7000;
                            float direction = (float) (rand.nextFloat()*2*Math.PI);
                            float x = (float) (Math.cos(direction)*speed);
                            float z = (float) (Math.sin(direction)*speed);
                            ParticleManager.spawnPetalParticle(this.posX, this.posY+1+((rand.nextFloat())/10), this.posZ, x, (rand.nextFloat()-0.5)/10, z,1+rand.nextInt(3));
                        }
                    } else if(e!=owner){
                        for(int i=0;i<1;i++){
                            float speed = (float) rand.nextInt(1500) /7000;
                            float direction = (float) (rand.nextFloat()*2*Math.PI);
                            float x = (float) (Math.cos(direction)*speed);
                            float z = (float) (Math.sin(direction)*speed);
                            ParticleManager.spawnPetalParticle(this.posX, this.posY+1+((rand.nextFloat())/10), this.posZ, x, (rand.nextFloat()-0.5)/10, z,1+rand.nextInt(3));
                        }
                    }


                }
            }
        }*/

    }

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        World world = event.getEntity().world;
        if (!world.isRemote){
            NBTTagCompound entityProperties = KaBladeEntityProperties.getPropCompound(event.getEntity());
            if (entityProperties.getInteger(KaBladeEntityProperties.CONFINEMENT)>0){
                KaBladeEntityProperties.doIntegerLower(entityProperties,KaBladeEntityProperties.CONFINEMENT);
                KaBladeEntityProperties.updateNBTForClient(event.getEntity());
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if(event.getEntity() instanceof EntityLivingBase){
            EntityLivingBase e = (EntityLivingBase) event.getEntity();
            if (!e.world.isRemote){
                NBTTagCompound entityProperties = KaBladeEntityProperties.getPropCompound(e);
                if (entityProperties.getInteger(KaBladeEntityProperties.CONFINEMENT)>0){
                    event.setAmount(event.getAmount()*3.2f);
                }
            }
        }
    }
}

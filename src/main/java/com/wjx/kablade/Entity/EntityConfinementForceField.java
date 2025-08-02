package com.wjx.kablade.Entity;

import com.wjx.kablade.Main;
import com.wjx.kablade.util.KaBladeEntityProperties;
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
        if(this.ticksExisted > 60){
            this.setDead();
        }
        if(!world.isRemote)
        {
            List<Entity> list = world.getEntitiesInAABBexcluding(this, this.getEntityBoundingBox().grow(1.0D, 1.0D, 1.0D), EntitySelectorAttackable.getInstance());
            for (Entity e : list) {
                if (e instanceof EntityLivingBase) {
                    if (e != owner && owner instanceof EntityPlayer) {
                        ((EntityLivingBase) e).attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer) owner), 10.0F);
                        if (blade != null && owner instanceof EntityPlayer) {
                            blade.hitEntity((EntityLivingBase) e, (EntityPlayer) owner);
                        }
                    } else {
                        ((EntityLivingBase) e).attackEntityFrom(DamageSource.causeMobDamage(owner), 10.0F);
                    }
                    NBTTagCompound entityProperties = KaBladeEntityProperties.getPropCompound(e);
                    entityProperties.setInteger(KaBladeEntityProperties.CONFINEMENT,2);

                }
            }
        }

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

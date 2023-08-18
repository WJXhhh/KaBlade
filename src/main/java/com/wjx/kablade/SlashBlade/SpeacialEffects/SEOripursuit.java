package com.wjx.kablade.SlashBlade.SpeacialEffects;

import com.google.common.base.Predicates;
import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.util.*;
import mods.flammpfeil.slashblade.entity.EntitySummonedSwordBase;
import mods.flammpfeil.slashblade.specialeffect.IRemovable;
import mods.flammpfeil.slashblade.specialeffect.ISpecialEffect;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

public class SEOripursuit implements ISpecialEffect, IRemovable {
    @Override
    public void register() {
        MinecraftForge.EVENT_BUS.register(this);
        SaEventManager.addSaEvent(lockEnemy);
        BladeAttackEventManager.addEvent(bladeAttackEvent);
    }

    @Override
    public int getDefaultRequiredLevel() {
        return -1;
    }

    @Override
    public String getEffectKey() {
        return "kablade.oripursuit";
    }

    @Override
    public boolean canCopy(ItemStack itemStack) {
        return true;
    }

    @Override
    public boolean canRemoval(ItemStack itemStack) {
        return !itemStack.getTranslationKey().equals("wjx.blade.originyer");
    }

    public static SaEvent lockEnemy = new SaEvent() {
        @Override
        public void run(ItemStack stack, EntityPlayer par3EntityPlayer, boolean isJust) {
            if (SpecialEffects.isEffective(par3EntityPlayer,stack, BladeProxy.Oripursuit) == SpecialEffects.State.Effective){
                World world = par3EntityPlayer.getEntityWorld();
                double dist = 10;
                Vec3d vec3d = par3EntityPlayer.getPositionEyes(1.0F);
                Vec3d vec3d1 = par3EntityPlayer.getLook(1.0F);
                Vec3d vec3d2 = vec3d.add(vec3d1.x * dist, vec3d1.y * dist, vec3d1.z * dist);
                Entity pointedEntity = null;
                List<Entity> list = world.getEntitiesInAABBexcluding(par3EntityPlayer, par3EntityPlayer.getEntityBoundingBox().expand(vec3d1.x * dist, vec3d1.y * dist, vec3d1.z * dist).grow(1.0D, 1.0D, 1.0D), Predicates.and(EntitySelectors.NOT_SPECTATING, entity -> entity != null && entity.canBeCollidedWith() && (entity instanceof EntityPlayer || entity instanceof EntityLiving)));
                double d2 = dist;
                for (Entity entity1 : list) {
                    AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().grow(entity1.getCollisionBorderSize());
                    RayTraceResult raytraceresult = axisalignedbb.calculateIntercept(vec3d, vec3d2);

                    if (axisalignedbb.contains(vec3d)) {
                        if (d2 >= 0.0D) {
                            pointedEntity = entity1;
                            d2 = 0.0D;
                        }
                    } else if (raytraceresult != null) {
                        double d3 = vec3d.distanceTo(raytraceresult.hitVec);

                        if (d3 < d2 || d2 == 0.0D) {
                            if (entity1.getLowestRidingEntity() == par3EntityPlayer.getLowestRidingEntity() && !par3EntityPlayer.canRiderInteract()) {
                                if (d2 == 0.0D) {
                                    pointedEntity = entity1;
                                }
                            } else {
                                pointedEntity = entity1;
                                d2 = d3;
                            }
                        }
                    }
                }
                if (pointedEntity != null){
                    if (pointedEntity instanceof EntityLivingBase){
                        if (!world.isRemote){
                            addPursuitTargetToPlayer(par3EntityPlayer,pointedEntity);
                        }
                    }
                }
            }
        }
    };

    public static void addPursuitTargetToPlayer(EntityPlayer p,Entity e){
        if (p != null && e != null){
            if (e instanceof EntityLivingBase){
                if (!EntityUUIDManager.hasUUID(e)){
                    EntityUUIDManager.addRandomUUIDTOEntity(e);
                }
                KaBladePlayerProp.getPropCompound(p).setString(KaBladePlayerProp.LOCKING_ENTITY_UUID,EntityUUIDManager.getEntityUUID(e));
                KaBladePlayerProp.getPropCompound(p).setInteger(KaBladePlayerProp.LOCKING_ENTITY_LEFT_TIME,600);
                if (!p.world.isRemote){
                    KaBladePlayerProp.updateNBTForClient(p);
                    KaBladeEntityProperties.updateNBTForClient(e);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerUpdate(TickEvent.PlayerTickEvent event){
        if (event.phase == TickEvent.Phase.END){
             EntityPlayer p = event.player;
             if (!p.world.isRemote){
                 if (KaBladePlayerProp.getPropCompound(p).getInteger(KaBladePlayerProp.LOCKING_ENTITY_LEFT_TIME) > 0){
                     KaBladeEntityProperties.doIntegerLower(KaBladePlayerProp.getPropCompound(p),KaBladePlayerProp.LOCKING_ENTITY_LEFT_TIME);
                     KaBladePlayerProp.updateNBTForClient(p);
                 }
                 if (KaBladePlayerProp.getPropCompound(p).hasKey(KaBladePlayerProp.LOCKING_ENTITY_UUID)){
                     if (EntityUUIDManager.getEntitiesFromUUID(KaBladePlayerProp.getPropCompound(p).getString(KaBladePlayerProp.LOCKING_ENTITY_UUID),p.world).isEmpty() || KaBladePlayerProp.getPropCompound(p).getInteger(KaBladePlayerProp.LOCKING_ENTITY_LEFT_TIME) <= 0){
                         KaBladePlayerProp.getPropCompound(p).removeTag(KaBladePlayerProp.LOCKING_ENTITY_UUID);
                         KaBladePlayerProp.getPropCompound(p).setInteger(KaBladePlayerProp.LOCKING_ENTITY_LEFT_TIME,0);
                         KaBladePlayerProp.updateNBTForClient(p);
                     }
                 }
             }
        }
    }

    public static BladeAttackEvent bladeAttackEvent = new BladeAttackEvent() {
        @Override
        public void run(ItemStack stack, EntityPlayer player, Entity entity) {
            if (entity != null && player != null){
                if (entity instanceof EntityLivingBase){
                    EntityLivingBase target = (EntityLivingBase) entity;
                    if (!target.world.isRemote){
                        List<Entity> l = EntityUUIDManager.getEntitiesFromUUID(KaBladePlayerProp.getPropCompound(player).getString(KaBladePlayerProp.LOCKING_ENTITY_UUID),player.world);
                        if (l.contains(target)){
                            //target.attackEntityFrom(DamageSource.causePlayerDamage(p),2f);
                            EntitySummonedSwordBase sword = new EntitySummonedSwordBase(target.world, player,4) ;
                            sword.setColor(65535);
                            target.world.spawnEntity(sword);
                        }
                    }
                }
            }
        }
    };

}

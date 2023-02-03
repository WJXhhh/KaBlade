package com.wjx.kablade.util.slash;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

public class SlashUpdateEvent {
    private static boolean timestop = false;

    public static void setTimestop(Boolean b) {
        timestop = b.booleanValue();
    }

    public static boolean isTimestop(){
        if(timestop) return true;
        return false;
    }

    public static boolean canStop(Entity entity){
        if(entity instanceof EntityPlayer) return false;
        return true;
    }

    public static boolean hasBlade(EntityLivingBase entity){
        if(entity instanceof EntityPlayer){
            EntityPlayer p = (EntityPlayer) entity;
            for(int i = 0; i < p.inventory.getSizeInventory(); i++){
                ItemStack stack = p.inventory.getStackInSlot(i);
                if(stack != null && stack.getItem() instanceof TranscendSlash){
                    return true;
                }
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event){
        if(event.player == null) return;
    }
    /*
        @SubscribeEvent
        public void onUpdate(MinecartUpdateEvent event) {
            if (event.getEntity() == null)
                return;
            Entity entity = event.getEntity();
            if (isTimestop() &&
                    entity instanceof EntityLivingBase && !hasBlade((EntityLivingBase)entity))
                event.setCanceled(true);
            if (isTimestop())
                if (canStop(entity)) {
                    if (entity.ticksExisted > 2) {
                        entity.ticksExisted--;
                        entity.posX = entity.prevPosX;
                        entity.posY = entity.prevPosY;
                        entity.posZ = entity.prevPosZ;
                        entity.rotationYaw = entity.prevRotationYaw;
                        entity.rotationPitch = entity.prevRotationPitch;
                        if (!entity.onGround)
                            if (entity.world.isRemote) {
                                entity.motionY = -0.0D;
                            } else {
                                entity.motionY = -0.0D;
                            }
                        entity.motionX = 0.0D;
                        entity.motionZ = 0.0D;
                        entity.fallDistance -= 0.076865F;
                        if (entity instanceof EntityLivingBase) {
                            EntityLivingBase living = (EntityLivingBase)entity;
                            living.rotationYawHead = living.prevRotationYawHead;
                            if (living instanceof net.minecraft.entity.passive.EntityTameable)
                                living.motionY -= 1.0E-6D;
                        }
                    }
                    event.setCanceled(true);
                }
        }


     */
    @SubscribeEvent
    public void onUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event.getEntity() == null)
            return;
        Entity entity = event.getEntity();
        List<Entity> entities = entity.world.loadedEntityList;
        if (isTimestop() &&
                entities != null && !entities.isEmpty())
            for (Entity entity1 : entities) {
                if (entity1 != null && canStop(entity1) &&
                        entity1.ticksExisted > 2) {
                    entity1.ticksExisted--;
                    entity1.posX = entity1.prevPosX;
                    entity1.posY = entity1.prevPosY;
                    entity1.posZ = entity1.prevPosZ;
                    entity1.rotationYaw = entity1.prevRotationYaw;
                    entity1.rotationPitch = entity1.prevRotationPitch;
                    if (!entity1.onGround)
                        if (entity1.world.isRemote) {
                            entity1.motionY = -0.0D;
                        } else {
                            entity1.motionY = -0.0D;
                        }
                    entity1.motionX = 0.0D;
                    entity1.motionZ = 0.0D;
                    entity1.fallDistance -= 0.076865F;
                }
            }
        if (isTimestop())
            if (canStop(entity)) {
                if (entity.ticksExisted > 2) {
                    entity.ticksExisted--;
                    entity.posX = entity.prevPosX;
                    entity.posY = entity.prevPosY;
                    entity.posZ = entity.prevPosZ;
                    entity.rotationYaw = entity.prevRotationYaw;
                    entity.rotationPitch = entity.prevRotationPitch;
                    if (!entity.onGround)
                        if (entity.world.isRemote) {
                            entity.motionY = -0.0D;
                        } else {
                            entity.motionY = -0.0D;
                        }
                    entity.motionX = 0.0D;
                    entity.motionZ = 0.0D;
                    entity.fallDistance -= 0.076865F;
                }
                event.setCanceled(true);
            }
    }
}

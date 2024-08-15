package com.wjx.kablade.AllWeapon.event;

import mods.flammpfeil.slashblade.ItemSlashBlade;
import mods.flammpfeil.slashblade.entity.EntityBladeStand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class AWWorldEvent {

    public static void recipeEvent(PlayerInteractEvent.EntityInteract event){
        Entity entity = event.getEntity();
        EntityPlayer player = event.getEntityPlayer();
        if(entity!=null&&player!=null){
            if(entity instanceof EntityBladeStand){
                EntityBladeStand stand = (EntityBladeStand) entity;
                if(stand.hasBlade()){

                }
            }
        }


    }
}

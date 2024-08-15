package com.wjx.kablade.AllWeapon.event;

import net.minecraftforge.common.MinecraftForge;
import com.wjx.kablade.AllWeapon.recipes.AWRec;
import com.wjx.kablade.Main;
import mods.flammpfeil.slashblade.ItemSlashBlade;
import mods.flammpfeil.slashblade.entity.EntityBladeStand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

//@Mod.EventBusSubscriber
public class AWWorldEvent {

    public AWWorldEvent(){
        MinecraftForge.EVENT_BUS.register(this);
    }


    @SubscribeEvent
    public static void recipeEvent(PlayerInteractEvent.EntityInteract event){
        Entity entity = event.getTarget();
        EntityPlayer player = event.getEntityPlayer();
        //Main.logger.warn("right! ");
        if(entity!=null&&player!=null){
            if(entity instanceof EntityBladeStand){
                EntityBladeStand stand = (EntityBladeStand) entity;
                if(stand.hasBlade()){
                   // Main.logger.warn("chechehecech");
                    ItemStack blade = stand.getBlade();
                    ItemStack result = AWRec.ToRec(blade, event.getWorld(), new Vec3d(stand.posX,stand.posY,stand.posZ),player.getHeldItemMainhand());
                    if(result != ItemStack.EMPTY){
                        //Main.logger.warn("resssttt");
                        stand.setBlade(result);
                        player.getHeldItemMainhand().shrink(1);
                    }
                }
            }
        }


    }
}

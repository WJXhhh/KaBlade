package com.wjx.kablade.AllWeapon.event;

import com.wjx.kablade.AllWeapon.recipes.AWRec;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.entity.EntityBladeStand;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.SlashBladeEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.wjx.kablade.Main.bladestr;

//@Mod.EventBusSubscriber
public class AWWorldEvent {

    public AWWorldEvent(){
        MinecraftForge.EVENT_BUS.register(this);
    }


    @SubscribeEvent
    public void recipeEvent(PlayerInteractEvent.EntityInteract event){
        Entity entity = event.getTarget();
        EntityPlayer player = event.getEntityPlayer();
        //Main.logger.warn("right! ");
        if(entity!=null&&player!=null){
            if(entity instanceof EntityBladeStand){
                EntityBladeStand stand = (EntityBladeStand) entity;
                int type = stand.getStandType();//耀魂碎片:0 耀魂铁锭:1 耀魂宝珠:2 破碎的耀魂:3
                int dimension = stand.dimension;//主世界:0 下界:-1
                if(stand.hasBlade()){
                    ItemStack blade = stand.getBlade();
                    ItemStack result = AWRec.ToRec(blade, event.getWorld(), new Vec3d(stand.posX,stand.posY,stand.posZ),player.getHeldItemMainhand(),type,dimension,0);
                    if(result != ItemStack.EMPTY){
                        stand.setBlade(result);
                        player.getHeldItemMainhand().shrink(1);
                    }
                }
            }
        }
    }


    @SubscribeEvent
    public void onArrowImpact(ProjectileImpactEvent.Arrow event) {
        EntityArrow arrow = event.getArrow();
        Entity hitEntity = event.getRayTraceResult().entityHit;

       if(hitEntity instanceof EntityBladeStand){
           EntityBladeStand stand = (EntityBladeStand) hitEntity;
           int type = stand.getStandType();//耀魂碎片:0 耀魂铁锭:1 耀魂宝珠:2 破碎的耀魂:3
           int dimension = stand.dimension;//主世界:0 下界:-1
           if(stand.hasBlade()){
               ItemStack blade = stand.getBlade();
               ItemStack result = AWRec.ToRec(blade, event.getArrow().world, new Vec3d(stand.posX,stand.posY,stand.posZ),ItemStack.EMPTY,type,dimension,1);
               if(result != ItemStack.EMPTY){
                   stand.setBlade(result);

               }
           }
       }
    }
}

package com.wjx.kablade.SlashBlade.SpeacialEffects;

import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.event.WorldEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialeffect.IRemovable;
import mods.flammpfeil.slashblade.specialeffect.ISpecialEffect;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

public class SEThunderBlitz  implements ISpecialEffect, IRemovable {
    @Override
    public boolean canCopy(ItemStack itemStack) {
        return true;
    }

    @Override
    public boolean canRemoval(ItemStack itemStack) {
        return !itemStack.getTranslationKey().equals("item.wjx.blade.honkai.key_of_cas");
    }

    @Override
    public void register() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public int getDefaultRequiredLevel() {
        return -1;
    }

    @Override
    public String getEffectKey() {
        return "kablade.thunder_blitz";
    }

    @SubscribeEvent
    public void onPlayerUpdate(TickEvent.PlayerTickEvent event){
        if(event.phase== TickEvent.Phase.START){
            World world = event.player.getEntityWorld();
            if (!world.isRemote){
                if (event.player.getHeldItemMainhand().getItem() instanceof ItemSlashBlade && SpecialEffects.isEffective(event.player, event.player.getHeldItemMainhand(), BladeProxy.ThunderBlitz) == SpecialEffects.State.Effective){
                    AxisAlignedBB bb = event.player.getEntityBoundingBox().grow(10,5,10);
                    bb = bb.offset(event.player.motionX, event.player.motionY, event.player.motionZ);
                    List<Entity> entities = world.getEntitiesInAABBexcluding(event.player, bb, input -> input != event.player && input instanceof EntityLivingBase);
                    for (Entity entity : entities){
                        if(entity instanceof EntityLivingBase){
                            if(entity.getEntityData().getInteger("dizuitime") > 0){
                                if(world.getTotalWorldTime() % 10 == 0){
                                    entity.attackEntityFrom(DamageSource.causePlayerDamage(event.player),5f);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.wjx.kablade.SlashBlade.SpeacialEffects;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialeffect.IRemovable;
import mods.flammpfeil.slashblade.specialeffect.ISpecialEffect;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class SEPhoenix implements ISpecialEffect, IRemovable {
    @Override
    public boolean canCopy(ItemStack itemStack) {
        return false;
    }

    @Override
    public boolean canRemoval(ItemStack itemStack) {
        return false;
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
        return "kablade.phoenix";
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event){
        EntityLivingBase attacker = (EntityLivingBase) event.getSource().getTrueSource();
        EntityLivingBase e = event.getEntityLiving();
        if (!e.world.isRemote){
            if (attacker != null){
                if (attacker instanceof EntityPlayer && attacker.getHeldItemMainhand().getItem() instanceof ItemSlashBlade){
                    if (SpecialEffects.isEffective((EntityPlayer) attacker,attacker.getHeldItemMainhand(),this) == SpecialEffects.State.Effective){
                        if (e.isBurning()){
                            event.setAmount(event.getAmount() * 1.2f);
                        }
                        else e.setFire(5);
                    }
                }
            }
        }
    }
}
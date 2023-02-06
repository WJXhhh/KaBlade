package com.wjx.kablade.SlashBlade.SpeacialEffects;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialeffect.IRemovable;
import mods.flammpfeil.slashblade.specialeffect.ISpecialEffect;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import mods.flammpfeil.slashblade.util.SlashBladeHooks;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class SETurbulence implements ISpecialEffect, IRemovable {
    @Override
    public void register() {
        SlashBladeHooks.EventBus.register(this);
    }

    @Override
    public int getDefaultRequiredLevel() {
        return 0;
    }

    @Override
    public String getEffectKey() {
        return "kablade.SETurbulence";
    }

    @Override
    public boolean canCopy(ItemStack itemStack) {
        return false;
    }

    @Override
    public boolean canRemoval(ItemStack itemStack) {
        return false;
    }

    @SubscribeEvent
    public void onEntityHurt(LivingHurtEvent event){
        EntityLivingBase attacker = (EntityLivingBase) event.getSource().getTrueSource();
        EntityLivingBase e = event.getEntityLiving();
        if (attacker != null){
            if (attacker instanceof EntityPlayer && attacker.getHeldItemMainhand().getItem() instanceof ItemSlashBlade){
                EntityPlayer player = (EntityPlayer) attacker;
                ItemStack slash = attacker.getHeldItemMainhand();
                if (SpecialEffects.isEffective(player,slash,this) == SpecialEffects.State.Effective){

                }
            }
        }
    }
}

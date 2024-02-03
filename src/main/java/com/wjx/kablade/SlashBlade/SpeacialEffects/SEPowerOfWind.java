package com.wjx.kablade.SlashBlade.SpeacialEffects;

import mods.flammpfeil.slashblade.specialeffect.ISpecialEffect;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import mods.flammpfeil.slashblade.specialeffect.IRemovable;






public class SEPowerOfWind implements ISpecialEffect,Removeable{

    @Override
    public boolean canCopy(ItemStack itemStack) {
        return true;
    }

    @Override
    public boolean canRemoval(ItemStack itemStack) {
        return !itemStack.getTranslationKey().equals("wjx.blade.honkai.fairy_sword");
    }

    @Override
    public void register() {
        MinecraftForge.EVENT_BUS.register(this);
        BladeAttackEventManager.addEvent(EMInduction);
    }

    @Override
    public int getDefaultRequiredLevel() {
        return -1;
    }

    @Override
    public String getEffectKey() {
        return "kablade.power_of_wind";
    }


    @SubscribeEvent
    public void updateSEATKImprove(TickEvent.PlayerTickEvent event){

        if(event.phase==TickEvent.Phase.START){
            if (SpecialEffects.isEffective(player,stack, BladeProxy.PowerOfWind) == SpecialEffects.State.Effective){
                //player.getAttributeModifierAmount





            }

        }
    }




}
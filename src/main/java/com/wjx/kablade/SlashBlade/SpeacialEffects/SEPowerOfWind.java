package com.wjx.kablade.SlashBlade.SpeacialEffects;

import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.util.BladeAttackEventManager;
import mods.flammpfeil.slashblade.specialeffect.ISpecialEffect;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import mods.flammpfeil.slashblade.specialeffect.IRemovable;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.UUID;


public class SEPowerOfWind implements ISpecialEffect,IRemovable{

    public UUID powAttid = UUID.fromString("739B518D-F9EF-04F7-AD8E-98AE7D3C5FE8");

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
        //BladeAttackEventManager.addEvent(_);
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
            if (SpecialEffects.isEffective(event.player, event.player.getHeldItemMainhand(), BladeProxy.PowerOfWind) == SpecialEffects.State.Effective){
                EntityPlayer player= event.player;
                ItemStack stack=event.player.getHeldItemMainhand();
                NBTTagCompound origin = player.nbt
                double speed=player.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue();
                double damage=player.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.ATTACK_DAMAGE).getBaseValue();
                AbstractAttributeMap map = player.getAttributeMap();
                IAttributeInstance instance = map.getAttributeInstance(SharedMonsterAttributes.ATTACK_DAMAGE);
                instance.applyModifier(new AttributeModifier(powAttid,"pow_att",speed/10,0));
                player.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(damage+speed/10);




            }

        }
    }




}
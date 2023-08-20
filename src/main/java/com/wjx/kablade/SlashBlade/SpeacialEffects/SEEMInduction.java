package com.wjx.kablade.SlashBlade.SpeacialEffects;

import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.init.PotionInit;
import com.wjx.kablade.util.BladeAttackEvent;
import com.wjx.kablade.util.BladeAttackEventManager;
import mods.flammpfeil.slashblade.specialeffect.IRemovable;
import mods.flammpfeil.slashblade.specialeffect.ISpecialEffect;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.common.MinecraftForge;

public class SEEMInduction implements ISpecialEffect, IRemovable {
    @Override
    public boolean canCopy(ItemStack itemStack) {
        return true;
    }

    @Override
    public boolean canRemoval(ItemStack itemStack) {
        return !itemStack.getTranslationKey().equals("wjx.blade.honkai.mag_storm");
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
        return "kablade.em_induction";
    }

    public static final BladeAttackEvent EMInduction = new BladeAttackEvent() {
        @Override
        public void run(ItemStack stack, EntityPlayer player, Entity entity) {
            if (SpecialEffects.isEffective(player,stack, BladeProxy.EMInduction) == SpecialEffects.State.Effective){
                if (entity instanceof EntityLivingBase){
                    ((EntityLivingBase) entity).addPotionEffect(new PotionEffect(PotionInit.PARALY,60,1));
                }
            }
        }
    };


}

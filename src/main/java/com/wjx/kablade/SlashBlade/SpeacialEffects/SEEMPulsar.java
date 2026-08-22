package com.wjx.kablade.SlashBlade.SpeacialEffects;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialeffect.IRemovable;
import mods.flammpfeil.slashblade.specialeffect.ISpecialEffect;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;

/** 电磁脉冲：重磁暴启动时给予抗性提升 III。 */
public class SEEMPulsar implements ISpecialEffect, IRemovable {
    public boolean canCopy(ItemStack stack){return true;}
    public boolean canRemoval(ItemStack stack){return !"item.wjx.blade.honkai.mag_typhoon".equals(stack.getTranslationKey());}
    public void register(){}
    public int getDefaultRequiredLevel(){return -1;}
    public String getEffectKey(){return "kablade.em_pulsar";}
    public void activate(EntityPlayer player,ItemStack blade){if(!player.world.isRemote&&blade.getItem() instanceof ItemSlashBlade&&SpecialEffects.isEffective(player,blade,this)==SpecialEffects.State.Effective)player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE,120,2));}
}

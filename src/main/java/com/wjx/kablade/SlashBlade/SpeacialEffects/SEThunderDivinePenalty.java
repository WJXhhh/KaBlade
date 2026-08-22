package com.wjx.kablade.SlashBlade.SpeacialEffects;

import com.wjx.kablade.util.TargetingUtil;
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

/** 罪雷天罚：对鸣神标记目标造成 20% 额外伤害。 */
public class SEThunderDivinePenalty implements ISpecialEffect, IRemovable {
    public boolean canCopy(ItemStack stack){return true;}
    public boolean canRemoval(ItemStack stack){return !"item.wjx.blade.honkai.domain_of_sanction".equals(stack.getTranslationKey());}
    public void register(){MinecraftForge.EVENT_BUS.register(this);}
    public int getDefaultRequiredLevel(){return -1;}
    public String getEffectKey(){return "kablade.thunder_divine_penalty";}
    @SubscribeEvent public void hurt(LivingHurtEvent event){
        if(event.getEntityLiving().world.isRemote||!(event.getSource().getTrueSource() instanceof EntityPlayer))return;
        EntityPlayer player=(EntityPlayer)event.getSource().getTrueSource(); EntityLivingBase target=event.getEntityLiving();
        if(!TargetingUtil.canDamage(player,target)||target.getEntityData().getInteger("dizuitime")<=0)return;
        ItemStack blade=player.getHeldItemMainhand();
        if(blade.getItem() instanceof ItemSlashBlade&&SpecialEffects.isEffective(player,blade,this)==SpecialEffects.State.Effective)
            event.setAmount(event.getAmount()*1.20F);
    }
}

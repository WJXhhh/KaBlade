package com.wjx.kablade.SlashBlade.SpeacialEffects;

import com.wjx.kablade.init.PotionInit;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialeffect.IRemovable;
import mods.flammpfeil.slashblade.specialeffect.ISpecialEffect;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class SEDivinePenalty implements ISpecialEffect, IRemovable {
    @Override
    public boolean canCopy(ItemStack itemStack) {
        return true;
    }

    @Override
    public boolean canRemoval(ItemStack itemStack) {
        return !itemStack.getTranslationKey().equals("wjx.blade.honkai.sky_breaker");
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
        return "kablade.divine_penalty";
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event){
        EntityLivingBase attacker = (EntityLivingBase) event.getSource().getTrueSource();
        EntityLivingBase e = event.getEntityLiving();
        if (!e.world.isRemote){
            if (e instanceof EntityPlayer && attacker != null){
                if (e.getHeldItemMainhand().getItem() instanceof ItemSlashBlade){
                    if (SpecialEffects.isEffective((EntityPlayer) (e),e.getHeldItemMainhand(),this) == SpecialEffects.State.Effective ){
                        attacker.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer) e),2f);
                        e.world.addWeatherEffect(new EntityLightningBolt(e.world,e.posX,e.posY,e.posZ,true));
                        attacker.addPotionEffect(new PotionEffect(PotionInit.PARALY,60,1));
                    }
                }
            }
            if (attacker != null){
                if (e.getActivePotionEffect(PotionInit.PARALY) != null){
                    if (attacker instanceof EntityPlayer && attacker.getHeldItemMainhand().getItem() instanceof ItemSlashBlade){
                        if (SpecialEffects.isEffective((EntityPlayer) (attacker),attacker.getHeldItemMainhand(),this) == SpecialEffects.State.Effective){
                            event.setAmount(event.getAmount() * 1.4f);
                        }
                    }
                }

            }
        }
    }
}

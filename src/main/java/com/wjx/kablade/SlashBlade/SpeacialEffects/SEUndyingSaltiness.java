package com.wjx.kablade.SlashBlade.SpeacialEffects;

import com.wjx.kablade.init.PotionInit;
import com.wjx.kablade.util.KaBladeEntityProperties;
import com.wjx.kablade.util.KaBladePlayerProp;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialeffect.IRemovable;
import mods.flammpfeil.slashblade.specialeffect.ISpecialEffect;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class SEUndyingSaltiness implements ISpecialEffect, IRemovable {
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
        return "kablade.undying_saltiness";
    }

    @Override
    public boolean canCopy(ItemStack itemStack) {
        return false;
    }

    @Override
    public boolean canRemoval(ItemStack itemStack) {
        return !itemStack.getTranslationKey().equals("wjx.blade.honkai.tuna");
    }

    @SubscribeEvent
    public void onEntityHurt(LivingHurtEvent event){
        Entity e1 = event.getSource().getTrueSource();
        EntityLivingBase e = event.getEntityLiving();
        if (!e.world.isRemote && e1 instanceof EntityLivingBase){
            EntityLivingBase attacker = (EntityLivingBase) e1;
            if (e instanceof EntityPlayer && e.getHeldItemMainhand().getItem() instanceof ItemSlashBlade){
                EntityPlayer player = (EntityPlayer) e;
                ItemStack slash = e.getHeldItemMainhand();
                if (SpecialEffects.isEffective(player,slash,this) == SpecialEffects.State.Effective){
                    double healthRatio = (e.getHealth())/(e.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.MAX_HEALTH).getAttributeValue()); //calc health loss
                    event.setAmount((float)(event.getAmount() * (1 + (1-healthRatio) * 4)));
                }
            }
        }
    }
}

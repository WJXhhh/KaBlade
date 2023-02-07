package com.wjx.kablade.SlashBlade.SpeacialEffects;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.PotionInit;
import com.wjx.kablade.util.KaBladePlayerProp;
import com.wjx.kablade.util.KaBladeEntityProperties;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialeffect.IRemovable;
import mods.flammpfeil.slashblade.specialeffect.ISpecialEffect;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import mods.flammpfeil.slashblade.util.SlashBladeHooks;
import net.minecraft.entity.EntityLivingBase;
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

public class SETurbulence implements ISpecialEffect, IRemovable {
    @Override
    public void register() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public int getDefaultRequiredLevel() {
        return 1;
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
        if (!e.world.isRemote){

            if (e instanceof EntityPlayer && e.getHeldItemMainhand().getItem() instanceof ItemSlashBlade){
                EntityPlayer player = (EntityPlayer) e;
                ItemStack slash = e.getHeldItemMainhand();
                //Main.ModHelper.sendMessageToAll(SpecialEffects.isEffective(player,slash,this).toString());
                if (SpecialEffects.isEffective(player,slash,this) == SpecialEffects.State.Effective){

                    KaBladePlayerProp.getPropCompound(player).setInteger(KaBladePlayerProp.TURBULENCE,100);
                }
            }
            if (attacker != null){
                if (attacker instanceof EntityPlayer && attacker.getHeldItemMainhand().getItem() instanceof ItemSlashBlade){
                    EntityPlayer player = (EntityPlayer) attacker;
                    if (SpecialEffects.isEffective(player,player.getHeldItemMainhand(),this) == SpecialEffects.State.Effective && KaBladePlayerProp.getPropCompound(player).getInteger(KaBladePlayerProp.TURBULENCE) > 0){
                        KaBladePlayerProp.getPropCompound(player).setInteger(KaBladePlayerProp.TURBULENCE,0);
                        e.world.addWeatherEffect(new EntityLightningBolt(e.world,e.posX,e.posY,e.posZ,true));
                        e.attackEntityFrom(DamageSource.causePlayerDamage(player),4f);
                        e.addPotionEffect(new PotionEffect(PotionInit.PARALY,100,1));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerUpdate(TickEvent.PlayerTickEvent event){
        EntityPlayer player = event.player;
        NBTTagCompound compound = KaBladePlayerProp.getPropCompound(player);
        if (compound.getInteger(KaBladePlayerProp.TURBULENCE) > 0){
            KaBladeEntityProperties.doIntegerLower(compound,KaBladePlayerProp.TURBULENCE);
        }
    }
}

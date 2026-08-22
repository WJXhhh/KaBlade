package com.wjx.kablade.SlashBlade.SpeacialEffects;

import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.init.PotionInit;
import com.wjx.kablade.util.KaBladePlayerProp;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.TargetingUtil;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialeffect.IRemovable;
import mods.flammpfeil.slashblade.specialeffect.ISpecialEffect;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** 雷云咆哮：命中麻痹目标时触发可控追加雷伤。 */
public class SERoaringNimbus implements ISpecialEffect, IRemovable {
    public static final String COOLDOWN="roaring_nimbus_cooldown"; private final Set<String> active=new HashSet<String>();
    public boolean canCopy(ItemStack stack){return true;}
    public boolean canRemoval(ItemStack stack){return !"item.wjx.blade.honkai.mag_typhoon".equals(stack.getTranslationKey());}
    public void register(){MinecraftForge.EVENT_BUS.register(this);}
    public int getDefaultRequiredLevel(){return -1;}
    public String getEffectKey(){return "kablade.roaring_nimbus";}
    @SubscribeEvent public void hurt(LivingHurtEvent event){
        if(event.getEntityLiving().world.isRemote||!(event.getSource().getTrueSource() instanceof EntityPlayer))return;
        EntityPlayer player=(EntityPlayer)event.getSource().getTrueSource();EntityLivingBase target=event.getEntityLiving();String key=player.getUniqueID()+":"+target.getUniqueID();
        ItemStack blade=player.getHeldItemMainhand();
        if(active.contains(key)||!(blade.getItem() instanceof ItemSlashBlade)||target.getActivePotionEffect(PotionInit.PARALY)==null||!TargetingUtil.canDamage(player,target)||KaBladePlayerProp.getPropCompound(player).getInteger(COOLDOWN)>0||SpecialEffects.isEffective(player,blade,this)!=SpecialEffects.State.Effective)return;
        KaBladePlayerProp.getPropCompound(player).setInteger(COOLDOWN,100);target.world.addWeatherEffect(new EntityLightningBolt(target.world,target.posX,target.posY,target.posZ,true));
        active.add(key);try{target.hurtResistantTime=0;target.attackEntityFrom(DamageSource.causePlayerDamage(player).setDamageBypassesArmor(),MathFunc.amplifierCalc(ItemSlashBlade.BaseAttackModifier.get(blade.getTagCompound()),3F));target.hurtResistantTime=0;}finally{active.remove(key);}
    }
    @SubscribeEvent public void tick(TickEvent.PlayerTickEvent event){if(event.phase==TickEvent.Phase.END&&!event.player.world.isRemote){int t=KaBladePlayerProp.getPropCompound(event.player).getInteger(COOLDOWN);if(t>0)KaBladePlayerProp.getPropCompound(event.player).setInteger(COOLDOWN,t-1);}}
}

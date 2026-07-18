package com.wjx.kablade.SlashBlade.SpeacialEffects;

import com.wjx.kablade.Main;
import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.network.MessageSpawnParticle;
import com.wjx.kablade.util.BladeAttackEvent;
import com.wjx.kablade.util.BladeAttackEventManager;
import com.wjx.kablade.util.KaBladePlayerProp;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialeffect.IRemovable;
import mods.flammpfeil.slashblade.specialeffect.ISpecialEffect;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;
import java.util.Random;

public class SETrueSelf implements ISpecialEffect, IRemovable {
    @Override
    public void register() {
        MinecraftForge.EVENT_BUS.register(this);
        BladeAttackEventManager.addEvent(bladeAttackEvent);
    }

    @Override
    public int getDefaultRequiredLevel() {
        return -1;
    }

    @Override
    public String getEffectKey() {
        return "kablade.true_self";
    }

    @Override
    public boolean canCopy(ItemStack itemStack) {
        return true;
    }

    @Override
    public boolean canRemoval(ItemStack itemStack) {
        return !itemStack.getTranslationKey().equals("item.wjx.blade.honkai.key_of_limpidity");
    }

    @SubscribeEvent
    public void onPlayerUpdate(TickEvent.PlayerTickEvent event){
        if (event.phase == TickEvent.Phase.END){
             EntityPlayer p = event.player;
             if (!p.world.isRemote && p.world.getTotalWorldTime() % 100 == 0){
                 NBTTagCompound c = KaBladePlayerProp.getPropCompound(p);
                 if (c.getInteger(KaBladePlayerProp.FORESIGHT) > 0){
                     c.setInteger(KaBladePlayerProp.FORESIGHT,c.getInteger(KaBladePlayerProp.FORESIGHT) - 1);
                 }
             }
        }
    }

    public static BladeAttackEvent bladeAttackEvent = new BladeAttackEvent() {
        @Override
        public void run(ItemStack stack, EntityPlayer player, Entity entity) {
            if (entity != null && player != null){
                if (entity instanceof EntityLivingBase && stack.getItem() instanceof ItemSlashBlade){
                    EntityLivingBase target = (EntityLivingBase) entity;
                    if (!target.world.isRemote){
                        if (SpecialEffects.isEffective(player,stack,BladeProxy.TrueSelf) == SpecialEffects.State.Effective){
                            if (Math.random() < 0.3){
                                NBTTagCompound c = KaBladePlayerProp.getPropCompound(player);
                                if (c.getInteger(KaBladePlayerProp.FORESIGHT) < 3){
                                    c.setInteger(KaBladePlayerProp.FORESIGHT,c.getInteger(KaBladePlayerProp.FORESIGHT) + 1);
                                }
                            }
                        }
                    }
                }
            }
        }
    };

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event){
        Entity attacker = event.getSource().getTrueSource();
        Entity target = event.getEntity();
        if (attacker instanceof EntityPlayer && target instanceof EntityLivingBase){
            EntityPlayer player = (EntityPlayer) attacker;
            if (!player.world.isRemote){
                if (player.getHeldItemMainhand().getItem() instanceof ItemSlashBlade){
                    if (SpecialEffects.isEffective(player,player.getHeldItemMainhand(),BladeProxy.TrueSelf) == SpecialEffects.State.Effective){
                        NBTTagCompound c = KaBladePlayerProp.getPropCompound(player);
                        if (c.getInteger(KaBladePlayerProp.FORESIGHT) > 0){
                            event.setAmount(event.getAmount() * (1 + c.getInteger(KaBladePlayerProp.FORESIGHT) * 0.1f));
                        }
                    }
                }
            }
        }
    }

}

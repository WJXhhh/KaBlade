package com.wjx.kablade.SlashBlade.SpeacialEffects;

import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.util.KaBladePlayerProp;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialeffect.IRemovable;
import mods.flammpfeil.slashblade.specialeffect.ISpecialEffect;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.UUID;


public class SEPowerOfWind implements ISpecialEffect, IRemovable {

    private static final UUID POWER_OF_WIND_ATTACK_ID = UUID.fromString("739B518D-F9EF-04F7-AD8E-98AE7D3C5FE8");

    @Override
    public boolean canCopy(ItemStack itemStack) {
        return true;
    }

    @Override
    public boolean canRemoval(ItemStack itemStack) {
        return !itemStack.getTranslationKey().equals("item.wjx.blade.honkai.fairy_sword");
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
        return "kablade.power_of_wind";
    }


    @SubscribeEvent
    public void updateSEATKImprove(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        if (event.phase != TickEvent.Phase.START || player.world.isRemote) {
            return;
        }

        ItemStack heldItem = player.getHeldItemMainhand();
        boolean active = heldItem.getItem() instanceof ItemSlashBlade
                && SpecialEffects.isEffective(player, heldItem, BladeProxy.PowerOfWind) == SpecialEffects.State.Effective;

        NBTTagCompound playerProperties = KaBladePlayerProp.getPropCompound(player);
        int activeValue = active ? 1 : 0;
        if (playerProperties.getInteger(KaBladePlayerProp.FAIR_POW) != activeValue) {
            playerProperties.setInteger(KaBladePlayerProp.FAIR_POW, activeValue);
            KaBladePlayerProp.updateNBTForClient(player);
        }

        AbstractAttributeMap map = player.getAttributeMap();
        IAttributeInstance attackDamage = map.getAttributeInstance(SharedMonsterAttributes.ATTACK_DAMAGE);
        AttributeModifier oldModifier = attackDamage.getModifier(POWER_OF_WIND_ATTACK_ID);
        if (active) {
            double speed = map.getAttributeInstance(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue();
            double amount = speed / 10;
            if (oldModifier == null || Double.compare(oldModifier.getAmount(), amount) != 0) {
                if (oldModifier != null) {
                    attackDamage.removeModifier(oldModifier);
                }
                attackDamage.applyModifier(new AttributeModifier(POWER_OF_WIND_ATTACK_ID, "pow_att", amount, 0));
            }
        } else if (oldModifier != null) {
            attackDamage.removeModifier(oldModifier);
        }
    }
}

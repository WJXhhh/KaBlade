package com.wjx.kablade.SlashBlade.SpeacialEffects;

import com.wjx.kablade.SlashBlade.BladeProxy;
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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** 心行处灭：50% 获得预知，每层提升 20% 伤害。层数衰减由 SETrueSelf 统一处理。 */
public class SEUnthinkable implements ISpecialEffect, IRemovable {
    @Override
    public void register() {
        MinecraftForge.EVENT_BUS.register(this);
        BladeAttackEventManager.addEvent(onBladeHit);
    }

    @Override public int getDefaultRequiredLevel() { return -1; }
    @Override public String getEffectKey() { return "kablade.unthinkable"; }
    @Override public boolean canCopy(ItemStack stack) { return true; }
    @Override public boolean canRemoval(ItemStack stack) {
        return !"item.wjx.blade.honkai.domain_of_unity".equals(stack.getTranslationKey());
    }

    private final BladeAttackEvent onBladeHit = new BladeAttackEvent() {
        @Override
        public void run(ItemStack stack, EntityPlayer player, Entity entity) {
            if (!(entity instanceof EntityLivingBase) || player.world.isRemote
                    || SpecialEffects.isEffective(player, stack, BladeProxy.Unthinkable)
                    != SpecialEffects.State.Effective || player.getRNG().nextFloat() >= 0.50F) return;
            NBTTagCompound data = KaBladePlayerProp.getPropCompound(player);
            int current = data.getInteger(KaBladePlayerProp.FORESIGHT);
            if (current < 3) data.setInteger(KaBladePlayerProp.FORESIGHT, current + 1);
        }
    };

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        Entity source = event.getSource().getTrueSource();
        if (!(source instanceof EntityPlayer) || source.world.isRemote) return;
        EntityPlayer player = (EntityPlayer) source;
        ItemStack blade = player.getHeldItemMainhand();
        if (!(blade.getItem() instanceof ItemSlashBlade)
                || SpecialEffects.isEffective(player, blade, BladeProxy.Unthinkable)
                != SpecialEffects.State.Effective) return;
        int foresight = KaBladePlayerProp.getPropCompound(player)
                .getInteger(KaBladePlayerProp.FORESIGHT);
        if (foresight > 0) event.setAmount(event.getAmount() * (1.0F + foresight * 0.20F));
    }
}

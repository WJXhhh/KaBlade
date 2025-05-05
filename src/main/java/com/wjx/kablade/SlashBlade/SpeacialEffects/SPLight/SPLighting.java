package com.wjx.kablade.SlashBlade.SpeacialEffects.SPLight;

import com.wjx.kablade.ExSA.entity.EntityLightningSword;
import com.wjx.kablade.ExSA.entity.EntityPhantomSwordEx;
import mods.flammpfeil.slashblade.ability.StylishRankManager;
import mods.flammpfeil.slashblade.entity.selector.EntitySelectorAttackable;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialeffect.ISpecialEffect;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import mods.flammpfeil.slashblade.util.SlashBladeEvent;
import mods.flammpfeil.slashblade.util.SlashBladeHooks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

public class SPLighting implements ISpecialEffect {
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
        return "SPLighting";
    }

    protected int SPLighting = 1;

    public void setExLightingLevel(int level) {
        this.SPLighting = level;
    }

    private boolean useBlade(ItemSlashBlade.ComboSequence sequence) {
        if (sequence.useScabbard) {
            return false;
        }
        if (sequence == ItemSlashBlade.ComboSequence.None) {
            return false;
        }
        return sequence != ItemSlashBlade.ComboSequence.Noutou;
    }

    @SubscribeEvent
    public void onUpdateItemSlashBlade(SlashBladeEvent.OnUpdateEvent event) {
        int check;
        if (!SpecialEffects.isPlayer(event.entity)) {
            return;
        }
        EntityPlayer player = (EntityPlayer)event.entity;
        ItemStack blade = event.blade;
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(blade);
        if (ItemSlashBlade.IsBroken.get(tag)) {
            return;
        }
        switch (SpecialEffects.isEffective(player, event.blade, this).ordinal()) {
            case 0: {
                return;
            }
            case 1: {
                return;
            }
            case 2: {
                double d0 = player.getRNG().nextGaussian() * 0.02;
                double d1 = player.getRNG().nextGaussian() * 0.02;
                double d2 = player.getRNG().nextGaussian() * 0.02;
                double d3 = 10.0;
                event.world.spawnParticle(EnumParticleTypes.SPELL_WITCH, player.posX + (double)(player.getRNG().nextFloat() * player.width * 2.0f) - (double)player.width - d0 * d3, player.posY, player.posZ + (double)(player.getRNG().nextFloat() * player.width * 2.0f) - (double)player.width - d2 * d3, d0, d1, d2);
            }
        }
        ItemSlashBlade.ComboSequence seq = ItemSlashBlade.getComboSequence(tag);
        if (!this.useBlade(seq)) {
            return;
        }
        PotionEffect haste = player.getActivePotionEffect(MobEffects.HASTE);
        check = haste != null ? (haste.getAmplifier() != 1 ? 3 : 4) : 2;
        if (player.swingProgressInt != check) {
            return;
        }
        this.doAddAttack(event.blade, player, seq);
    }

    public void doAddAttack(ItemStack stack, EntityPlayer player, ItemSlashBlade.ComboSequence setCombo) {
        Entity tmp;
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(stack);
        World world = player.world;
        int cost = -2;
        if (!ItemSlashBlade.ProudSoul.tryAdd(tag, cost, false)) {
            ItemSlashBlade.damageItem(stack, 5, player);
            return;
        }
        Entity target = null;
        int entityId = ItemSlashBlade.TargetEntityId.get(tag);
        if (entityId != 0 && (tmp = world.getEntityByID(entityId)) != null && tmp.getDistance(player) < 30.0f) {
            target = tmp;
        }
        if (target == null) {
            target = this.getEntityToWatch(player);
        }
        if (target != null && !target.isDead && !world.isRemote) {
            EntityLightningSword entityDrive;
            float baseModif = ((ItemSlashBlade)stack.getItem()).getBaseAttackModifiers(tag);
            int level = EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, stack);
            float magicDamage = baseModif * (float)(level / 20);
            int rank = StylishRankManager.getStylishRank(player);
            if (5 <= rank) {
                magicDamage *= 2.0f;
            }
            if ((entityDrive = new EntityLightningSword(world, player, magicDamage, 90.0f)) != null) {
                entityDrive.setColor(16766720);
                entityDrive.setInterval(7);
                entityDrive.setLifeTime(15);
                entityDrive.setRoll(90.0f);
                entityDrive.setTargetEntityId(target.getEntityId());
                world.spawnEntity(entityDrive);
            }
        }
    }

    private Entity getEntityToWatch(EntityPlayer player) {
        World world = player.world;
        Entity target = null;
        for (int dist = 2; dist < 20; dist += 2) {

            AxisAlignedBB bb = player.getEntityBoundingBox();
            bb = bb.grow(4.0D, 0.0D, 4.0D);
            bb = bb.offset(player.motionX, player.motionY, player.motionZ);

            List<Entity> list = world.getEntitiesInAABBexcluding(player, bb, EntitySelectorAttackable.getInstance());
            float distance = 30.0f;
            for (Entity curEntity : list) {
                float curDist = curEntity.getDistance(player);
                if (!(curDist < distance)) continue;
                target = curEntity;
                distance = curDist;
            }
            if (target != null) break;
        }
        return target;
    }


}

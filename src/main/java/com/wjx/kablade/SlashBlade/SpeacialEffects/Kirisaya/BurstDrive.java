package com.wjx.kablade.SlashBlade.SpeacialEffects.Kirisaya;

import mods.flammpfeil.slashblade.ability.StylishRankManager;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialeffect.ISpecialEffect;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import mods.flammpfeil.slashblade.util.SlashBladeEvent;
import mods.flammpfeil.slashblade.util.SlashBladeHooks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

public class BurstDrive implements ISpecialEffect
{
    private static final String EffectKey = "BurstDrive";

    private static final int COST = 2;

    private static final int NO_COST_DAMAGE = 1;

    private boolean useBlade(ItemSlashBlade.ComboSequence sequence)
    {
        if (sequence.useScabbard) return false;
        if (sequence == ItemSlashBlade.ComboSequence.None) return false;
        if (sequence == ItemSlashBlade.ComboSequence.Noutou) return false;
        return true;
    }

    @SubscribeEvent
    public void onUpdateItemSlashBlade(SlashBladeEvent.OnUpdateEvent event)
    {
        if (!SpecialEffects.isPlayer(event.entity))
            return;
        EntityPlayer player = (EntityPlayer)event.entity;

        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(event.blade);
        ItemSlashBlade.ComboSequence seq = ItemSlashBlade.getComboSequence(tag);
        if (!useBlade(seq))
            return;
        if (ItemSlashBlade.IsBroken.get(tag))
            return;

        if (SpecialEffects.isEffective(player, event.blade, this) != SpecialEffects.State.Effective) {
            return;
        }

        spawnParticle(EnumParticleTypes.SPELL_WITCH, player, 1, 1.0);

        PotionEffect haste = player.getActivePotionEffect(MobEffects.HASTE);
        int check = haste != null ? haste.getAmplifier() != 1 ? 3 : 4 : 2;

        if (player.swingProgressInt != check)
            return;

        doAddAttack(event.blade, player, seq);
    }

    public void doAddAttack(ItemStack stack, EntityPlayer player, ItemSlashBlade.ComboSequence setCombo)
    {
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(stack);
        World world = player.world;

        if (!ItemSlashBlade.ProudSoul.tryAdd(tag, -COST, false)) {
            ItemSlashBlade.damageItem(stack, NO_COST_DAMAGE, player);
            return;
        }

        if (world.isRemote)
            return;

        float baseModif = ((ItemSlashBlade)stack.getItem()).getBaseAttackModifiers(tag);
        int level = EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, stack);

        float magicDamage = baseModif/2f + level;
        int rank = StylishRankManager.getStylishRank(player);
        if (rank >= 5) {
            magicDamage += (float) (ItemSlashBlade.AttackAmplifier.get(tag)*(level/5.0 + 0.5f));
        }

        EntityDrive entityDrive = new EntityDrive(world, player, magicDamage, false, 90.0f - setCombo.swingDirection);
        entityDrive.setInitialSpeed(1.5f);
        entityDrive.setLifeTime(10);

        world.spawnEntity(entityDrive);
    }

    @Override
    public void register()
    {
        SlashBladeHooks.EventBus.register(this);
    }

    @Override
    public int getDefaultRequiredLevel()
    {
        return 30;
    }

    @Override
    public String getEffectKey()
    {
        return EffectKey;
    }

    public void spawnParticle(EnumParticleTypes type, EntityPlayer player, int num, double rate)
    {

        World world = player.world;
        Random rand = player.getRNG();

        for (int i = 0; i < num; i++) {
            double xSpeed = rand.nextGaussian() * 0.02;
            double ySpeed = rand.nextGaussian() * 0.02;
            double zSpeed = rand.nextGaussian() * 0.02;

            double rx = rand.nextDouble();
            double rz = rand.nextDouble();

            world.spawnParticle(
                    type,
                    player.posX + ((rx*2 - 1)*player.width  - xSpeed * 10.0)*rate,
                    player.posY,
                    player.posZ + ((rz*2 - 1)*player.width  - zSpeed * 10.0)*rate,
                    xSpeed, ySpeed, zSpeed);
        }
    }
}

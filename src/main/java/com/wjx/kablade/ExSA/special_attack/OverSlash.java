package com.wjx.kablade.ExSA.special_attack;

import mods.flammpfeil.slashblade.ability.StylishRankManager;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.entity.selector.EntitySelectorAttackable;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.List;

public class OverSlash extends SpecialAttackBase {
    @Override
    public String toString() {
        return "overslash";
    }

    String AttackType = StylishRankManager.AttackTypes.registerAttackType("LaveDriveEx", 0.5f);

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.world;
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(itemStack);
        for (int i = 0; i < 20; ++i) {
            double d0 = entityPlayer.getRNG().nextGaussian() * 0.02;
            double d1 = entityPlayer.getRNG().nextGaussian() * 0.02;
            double d2 = entityPlayer.getRNG().nextGaussian() * 0.02;
            double d3 = 10.0;
            world.spawnParticle(EnumParticleTypes.SPELL_WITCH, entityPlayer.posX + (double)(entityPlayer.getRNG().nextFloat() * entityPlayer.width * 2.0f) - (double)entityPlayer.width - d0 * d3, entityPlayer.posY, entityPlayer.posZ + (double)(entityPlayer.getRNG().nextFloat() * entityPlayer.width * 2.0f) - (double)entityPlayer.width - d2 * d3, d0, d1, d2);
        }
        world.playSound(null, entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 4.0F, (1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F) * 0.7F);
        if (!world.isRemote) {
            int cost = -40;
            if (!ItemSlashBlade.ProudSoul.tryAdd(tag, -40, false)) {
                itemStack.damageItem(10, entityPlayer);
            }
            ItemSlashBlade blade = (ItemSlashBlade)((Object)itemStack.getItem());
            AxisAlignedBB bb = entityPlayer.getEntityBoundingBox();
            bb = bb.expand(5.0, 0.25, 5.0);
            List<Entity> list = world.getEntitiesInAABBexcluding(entityPlayer, bb, EntitySelectorAttackable.getInstance());
            for (Entity curEntity : list) {
                StylishRankManager.setNextAttackType(entityPlayer, AttackType);
                blade.attackTargetEntity(itemStack, curEntity, entityPlayer, true);
                entityPlayer.onCriticalHit(curEntity);
            }
            float baseModif = blade.getBaseAttackModifiers(tag);
            int level = Math.max(1, EnchantmentHelper.getEnchantmentLevel( Enchantments.POWER, (ItemStack)itemStack));
            float magicDamage = baseModif / 2.0f;
            int rank = StylishRankManager.getStylishRank(entityPlayer);
            if (5 <= rank) {
                magicDamage += ItemSlashBlade.AttackAmplifier.get(tag).floatValue() * (0.25f + (float)level / 5.0f);
            }
            world.playSound(null, entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ, SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.BLOCKS, 0.4F, (1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F) * 0.7F);
            for (int i = 0; i < 6; ++i) {
                EntityDrive entityDrive = new EntityDrive(world, entityPlayer, magicDamage, false, 0.0f);
                entityDrive.setLocationAndAngles(entityPlayer.posX, entityPlayer.posY + (double)entityPlayer.getEyeHeight() / 2.0, entityPlayer.posZ, entityPlayer.rotationYaw + (float)(60 * i) + (entityDrive.getRand().nextFloat() - 0.5f) * 60.0f, (entityDrive.getRand().nextFloat() - 0.5f) * 60.0f);
                entityDrive.setDriveVector(0.5f);
                entityDrive.setLifeTime(10);
                entityDrive.setIsMultiHit(true);
                entityDrive.setRoll(90.0f + 120.0f * (entityDrive.getRand().nextFloat() - 0.5f));
                if (entityDrive == null) continue;
                world.spawnEntity(entityDrive);
            }
        }
        ItemSlashBlade.setComboSequence(tag, ItemSlashBlade.ComboSequence.Battou);
    }
}

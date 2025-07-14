package com.wjx.kablade.ExSA.special_attack;

import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.ability.StylishRankManager;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.entity.selector.EntitySelectorAttackable;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.List;
import java.util.Objects;

public class OverSlash extends SpecialAttackBase {
    @Override
    public String toString() {
        return "overslash";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.world;
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(itemStack);
        for(int i = 0;i<20;i++){
            final double d0 = entityPlayer.getRNG().nextGaussian() * 0.02;
            final double d2 = entityPlayer.getRNG().nextGaussian() * 0.02;
            final double d3 = entityPlayer.getRNG().nextGaussian() * 0.02;
            final double d4 = 10.0;
            world.spawnParticle(EnumParticleTypes.SPELL_WITCH, entityPlayer.posX + (double)(entityPlayer.getRNG().nextFloat() * entityPlayer.width * 2.0f) - (double)entityPlayer.width - d0 * d4, entityPlayer.posY, entityPlayer.posZ + (double)(entityPlayer.getRNG().nextFloat() * entityPlayer.width * 2.0f) - (double)entityPlayer.width - d2 * d4, d0, d2, d3);

        }
        world.playSound(entityPlayer,entityPlayer.posX,entityPlayer.posY,entityPlayer.posZ, Objects.requireNonNull(SoundEvent.REGISTRY.getObject(new ResourceLocation("entity.generic.explode"))), SoundCategory.PLAYERS, 1.0f, 1.0f);
        if(!world.isRemote){
            ItemSlashBlade blade = (ItemSlashBlade)itemStack.getItem();
            AxisAlignedBB bb = entityPlayer.getEntityBoundingBox();
            bb = bb.expand(5.0, 0.25, 5.0);
            final List<Entity> list = world.getEntitiesInAABBexcluding((Entity)entityPlayer, bb, EntitySelectorAttackable.getInstance());
            for (final Entity curEntity : list) {
                StylishRankManager.setNextAttackType(entityPlayer, "OverSlash");
                blade.attackTargetEntity(itemStack, curEntity, entityPlayer, true);
                entityPlayer.onCriticalHit(curEntity);
            }
            final float baseModif = blade.getBaseAttackModifiers(tag);
            float magicDamage = baseModif / 2.0f;
            final int rank = StylishRankManager.getStylishRank(entityPlayer);
            if (5 <= rank) {
                magicDamage += MathFunc.amplifierCalc(baseModif,3);
            }
            world.playSound(entityPlayer,entityPlayer.posX,entityPlayer.posY,entityPlayer.posZ, Objects.requireNonNull(SoundEvent.REGISTRY.getObject(new ResourceLocation("entity.lightning.thunder"))), SoundCategory.BLOCKS, 0.4F, 1.0f);
            for (int j = 0; j < 6; ++j) {
                final EntityDrive entityDrive = new EntityDrive(world, entityPlayer, magicDamage, false, 0.0f);
                entityDrive.setLocationAndAngles(entityPlayer.posX, entityPlayer.posY + entityPlayer.getEyeHeight() / 2.0, entityPlayer.posZ, entityPlayer.rotationYaw + 60 * j + (entityDrive.getRand().nextFloat() - 0.5f) * 60.0f, (entityDrive.getRand().nextFloat() - 0.5f) * 60.0f);
                entityDrive.setDriveVector(0.5f);
                entityDrive.setLifeTime(10);
                entityDrive.setIsMultiHit(true);
                entityDrive.setRoll(90.0f + 120.0f * (entityDrive.getRand().nextFloat() - 0.5f));
                world.spawnEntity(entityDrive);
            }
        }
    }
}

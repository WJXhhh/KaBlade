package com.wjx.kablade.ExSA.special_attack;

import com.wjx.kablade.ExSA.entity.EntityFlareEdge;
import com.wjx.kablade.ExSA.entity.ExSaEntityDrive;
import mods.flammpfeil.slashblade.ability.StylishRankManager;
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

public class LaveDriveEx extends SpecialAttackBase {
    @Override
    public String toString() {
        return "lavedriveex";
    }

    String AttackType = StylishRankManager.AttackTypes.registerAttackType("LaveDriveEx", 0.5f);

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer player) {
        World world = player.world;
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(itemStack);
        world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 4.0F, (1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F) * 0.7F);
        for (int i = 0; i < 100; ++i) {
            final double d0 = player.getRNG().nextGaussian() * 0.02;
            final double d2 = player.getRNG().nextGaussian() * 0.02;
            final double d3 = player.getRNG().nextGaussian() * 0.02;
            final double d4 = 10.0;
            player.world.spawnParticle( EnumParticleTypes.LAVA, player.posX + (player.getRNG().nextFloat() * player.width * 2.0f - player.width - d0 * d4) * 5.0, player.posY, player.posZ + (player.getRNG().nextFloat() * player.width * 2.0f - player.width - d3 * d4) * 5.0, d0, d2, d3);
        }
        if(!world.isRemote){
            final ItemSlashBlade blade = (ItemSlashBlade)itemStack.getItem();
            AxisAlignedBB bb = player.getEntityBoundingBox();
            bb = bb.expand(5.0, 0.25, 5.0);


            List<Entity> list = player.world.getEntitiesInAABBexcluding(player, bb, EntitySelectorAttackable.getInstance());
            for (Entity curEntity : list) {
                StylishRankManager.setNextAttackType(player, AttackType);
                blade.attackTargetEntity(itemStack, curEntity, player, true);
                player.onCriticalHit(curEntity);
            }
            float baseModif = blade.getBaseAttackModifiers(tag);
            int level = Math.max(1, EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, itemStack));
            float magicDamage = baseModif * 0.4F;
            int rank = StylishRankManager.getStylishRank(player);
            if (5 <= rank) {
                magicDamage += (ItemSlashBlade.AttackAmplifier.get(tag) * (0.2F + (float)level / 5.0F))*0.5f;
            }

            if (7 <= rank) {
                magicDamage *= 2.0F;
            }

            int maxCol = 3;
            int maxCount = 3;
            maxCount = maxCount + rank;
            double radBaseRot = Math.toRadians((double)player.rotationYaw);
            double radRot = 6.283185307179586D / (double)maxCount;

            for(int j = 0; j < maxCol; ++j) {
                for(int i = 0; i < maxCount; ++i) {
                    ExSaEntityDrive entityDrive = new EntityFlareEdge(world, player, magicDamage, false, 0.0F);
                    if (entityDrive != null) {
                        double posY = player.posY + (double)player.getEyeHeight() / 2.0D;
                        if (maxCount % 2 == 1 && (double)i == Math.floor((double)(maxCount / 2)) + 1.0D && rank >= 5) {
                            posY = player.posY + (double)player.getEyeHeight();
                        }

                        entityDrive.setLocationAndAngles(player.posX + Math.cos(radBaseRot + radRot * (double) i), posY, player.posZ + Math.sin(radBaseRot + radRot * (double) i), player.rotationYaw, player.rotationPitch);
                        entityDrive.setColor(6291504);
                        entityDrive.setDriveVector(0.8F + ((float)j * 2 / 5));
                        if ((int)(Math.random() * 10.0D + 1.0D) < 3) {
                            entityDrive.getDataManager().set(ExSaEntityDrive.PL_PARTICAL, true);
                            entityDrive.getDataManager().set(ExSaEntityDrive.PARTICLE_STYLE,"LAVA");

                        }else {
                            entityDrive.getDataManager().set(ExSaEntityDrive.PL_PARTICAL, false);
                        }

                        entityDrive.setInitialSpeed(0.1f);
                        entityDrive.setNextSpeed(1.05f);
                        entityDrive.setChangeTime(5 + 2 * j + i);
                        entityDrive.setIsMultiHit(true);
                        entityDrive.setLifeTime(20 + 3 * j + i);
                        entityDrive.setRoll(90.0F);
                        world.spawnEntity(entityDrive);
                    }
                }
            }
        }
        ItemSlashBlade.setComboSequence(tag, ItemSlashBlade.ComboSequence.Battou);
    }
}

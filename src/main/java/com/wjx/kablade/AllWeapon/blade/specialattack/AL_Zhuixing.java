package com.wjx.kablade.AllWeapon.blade.specialattack;

import com.wjx.kablade.Entity.EntityDriveAdd;
import com.wjx.kablade.Entity.EntitySummonedSwordPotionEffectAdd;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SATool;
import mods.flammpfeil.slashblade.ability.StylishRankManager;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.block.BlockCompressedPowered;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.util.Random;

public class AL_Zhuixing extends SpecialAttackBase {
    @Override
    public String toString() {
        return "baoku";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.world;
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(itemStack);

        if(!world.isRemote){
            ItemSlashBlade blade = (ItemSlashBlade)(itemStack.getItem());
            float baseModif = ItemSlashBlade.BaseAttackModifier.get(tag)/3;
            int level = EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, itemStack);
            float magicDamage = baseModif;
            magicDamage+= MathFunc.amplifierCalc(ItemSlashBlade.BaseAttackModifier.get(tag),4f);
            Entity target = null;
            int entityId = ItemSlashBlade.TargetEntityId.get(tag);
            if (entityId != 0) {
                Entity tmp = world.getEntityByID(entityId);
                if (tmp != null && tmp.getDistance(entityPlayer) < 30.0F && tmp instanceof EntityLivingBase) {
                    target = tmp;
                }
            }
            if (target==null){
                target= SATool.getEntityToWatch(entityPlayer);

            }


            if (target != null) {
                ItemSlashBlade.setComboSequence(tag, ItemSlashBlade.ComboSequence.SlashDim);
                StylishRankManager.setNextAttackType(entityPlayer, StylishRankManager.AttackTypes.PhantomSword);
                blade.attackTargetEntity(itemStack, target, entityPlayer, true);
                entityPlayer.onCriticalHit(target);
                //double o = target.posY + 10.0;
                target.motionX = 0.0;
                target.motionY = 1.5;
                target.motionZ = 0.0;
                if (target instanceof EntityLivingBase) {
                    blade.setDaunting((EntityLivingBase)target);
                    ((EntityLivingBase)target).hurtTime = 0;
                    ((EntityLivingBase)target).hurtResistantTime = 0;
                }
                int count = 1 + StylishRankManager.getStylishRank(entityPlayer);
                Random rand = new Random();
                for (int i = 0; i < 32; ++i) {
                   // target.setPosition(target.posX, o, target.posZ);
                    if (world.isRemote) continue;
                    boolean isBurst = i % 2 == 0;
                    float a = rand.nextInt(100);
                    float b = rand.nextInt(20);
                    float c = rand.nextInt(100);
                    EntitySummonedSwordPotionEffectAdd entityDrive = new EntitySummonedSwordPotionEffectAdd(world, entityPlayer, magicDamage, 0.0f);
                    if (entityDrive == null) continue;
                    entityDrive.setLocationAndAngles(target.posX + (double)(50.0f - a), target.posY + (double)b, target.posZ + (double)(50.0f - c), target.rotationYaw + b * 36.0f, 0.0f);
                    entityDrive.setInterval(15);
                    entityDrive.setLifeTime(80);
                    entityDrive.setColor(0xFFFF00);
                    entityDrive.setTargetEntityId(target.getEntityId());
                    world.spawnEntity(entityDrive);
                }
            }

        }
    }
}

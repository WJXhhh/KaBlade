package com.wjx.kablade.AllWeapon.blade.specialattack;

import com.wjx.kablade.Entity.EntityDriveAdd;
import com.wjx.kablade.Entity.EntitySummonedSwordPotionEffectAdd;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SATool;
import mods.flammpfeil.slashblade.ability.StylishRankManager;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class AL_Xuepo extends SpecialAttackBase {
    @Override
    public String toString() {
        return "xuepo";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.world;
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(itemStack);
        ItemSlashBlade blade  = (ItemSlashBlade) itemStack.getItem();
        float baseModif = ItemSlashBlade.BaseAttackModifier.get(tag)/5;
        int level = EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, itemStack);
        float magicDamage = baseModif;
        magicDamage+= MathFunc.amplifierCalc(ItemSlashBlade.BaseAttackModifier.get(tag),5f);
        if(!world.isRemote){
            for (int i = 0; i < 1; ++i) {
                EntityDriveAdd entityDrive = new EntityDriveAdd(world, entityPlayer, magicDamage*3.5f, false, 0.0f - ItemSlashBlade.ComboSequence.Battou.swingDirection);
                entityDrive.setChangeTime(10);
                entityDrive.setNextSpeed(1.8f);
                entityDrive.setColor(0xFFFFFF);
                entityDrive.scaleX = 2f;
                entityDrive.scaleY = 0.5f;
                entityDrive.scaleZ = 2f;
                entityDrive.setInitialSpeed(0.008f);
                entityDrive.setLifeTime(80);
                entityDrive.getDataManager().set(EntityDriveAdd.PARTICLE_STYLE,"SNOW_SHOVEL");
                if (entityDrive == null) continue;
                world.spawnEntity(entityDrive);
            }
            Entity target;
            int entityId = ItemSlashBlade.TargetEntityId.get(tag);

            if(entityId == 0)
                target = SATool.getEntityToWatch(entityPlayer);
            else
                target = world.getEntityByID(entityId);

            if(target!=null){
                ItemSlashBlade.setComboSequence(tag, ItemSlashBlade.ComboSequence.SlashDim);
                StylishRankManager.setNextAttackType(entityPlayer, StylishRankManager.AttackTypes.PhantomSword);
                blade.attackTargetEntity(itemStack, target, entityPlayer, true);
                entityPlayer.onCriticalHit(target);
                target.motionX = 0.0;
                target.motionY = 0.0;
                target.motionZ = 0.0;
                if (target instanceof EntityLivingBase) {
                    blade.setDaunting((EntityLivingBase)target);
                    ((EntityLivingBase)target).hurtTime = 0;
                    ((EntityLivingBase)target).hurtResistantTime = 0;
                }
                int count = 1 + StylishRankManager.getStylishRank(entityPlayer);
                for (int i = 0; i < 3; ++i) {
                    if (world.isRemote) continue;
                    boolean isBurst = i % 2 == 0;
                    EntitySummonedSwordPotionEffectAdd entityDrive = new EntitySummonedSwordPotionEffectAdd(world, entityPlayer, magicDamage, 0.0f);
                    if (entityDrive == null) continue;
                    entityDrive.setLocationAndAngles(entityPlayer.posX, entityPlayer.posY + (double)(i + 1) * 0.5, entityPlayer.posZ, entityPlayer.rotationYaw, 0.0f);
                    entityDrive.setInterval(25);
                    entityDrive.setLifeTime(30);
                    entityDrive.setColor(0xFFFFFF);
                    entityDrive.setBurst(isBurst);
                    entityDrive.setTargetEntityId(target.getEntityId());
                    world.spawnEntity(entityDrive);
                }
            }
        }
    }
}

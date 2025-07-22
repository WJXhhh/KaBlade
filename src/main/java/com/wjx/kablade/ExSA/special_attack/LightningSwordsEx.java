package com.wjx.kablade.ExSA.special_attack;

import com.wjx.kablade.ExSA.entity.EntityLightningSword;
import com.wjx.kablade.ExSA.entity.EntityPhantomSwordEx;
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

public class LightningSwordsEx extends SpecialAttackBase {
    public static String AttackType = StylishRankManager.AttackTypes.registerAttackType("LightningSwordsEx", 0.5f);
    @Override
    public String toString() {
        return "lightningswordsex";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.world;
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(itemStack);
        if (!world.isRemote){
            Entity tmp;
            ItemSlashBlade blade = (ItemSlashBlade) itemStack.getItem();
            Entity target = null;
            int entityId = ItemSlashBlade.TargetEntityId.get(tag);
            if (entityId != 0 && (tmp = world.getEntityByID(entityId))!= null && tmp.getDistance(entityPlayer) < 30f) {
                target = tmp;
            }
            if(target == null){
                target = SATool.getEntityToWatch(entityPlayer);
            }
            if(target != null) {
                EntityLightningSword entityDrive;
                int dir;
                double z;
                double y;
                double x;
                double ran;
                float dist;
                EntityPhantomSwordEx entityDrive2;
                int i;
                ItemSlashBlade.setComboSequence(tag, ItemSlashBlade.ComboSequence.SlashDim);
                int cost = -100;
                StylishRankManager.setNextAttackType(entityPlayer, AttackType);
                blade.attackTargetEntity(itemStack, target, entityPlayer, Boolean.TRUE);
                entityPlayer.onCriticalHit(target);
                target.motionX = 0.0;
                target.motionY = 0.0;
                target.motionZ = 0.0;
                if (target instanceof EntityLivingBase) {
                    blade.setDaunting((EntityLivingBase)target);
                }
                int rank = StylishRankManager.getStylishRank((Entity)entityPlayer);
                int level  = EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, itemStack);
                float magicDamage = 1.0f + ItemSlashBlade.BaseAttackModifier.get(tag)*0.5f * ((float)level / 5.0f) * (1.5f + 0.2f * (float)rank);
                int count = 3;
                double rad = Math.PI * 2 / (double)(count += rank);
                for (i = 0; i < count; ++i) {
                    entityDrive2 = new EntityPhantomSwordEx(world, (EntityLivingBase)entityPlayer, magicDamage, 90.0f);
                    dist = 2.0f;
                    ran = rad * (double)i;
                    x = Math.sin(ran);
                    y = 1.0 + (double)entityDrive2.getRand().nextFloat();
                    z = Math.cos(ran);
                    dir = -((int)Math.toDegrees(Math.PI + ran));
                    entityDrive2.setLocationAndAngles(target.posX + (x *= (double)dist), target.posY + (y *= (double)dist), target.posZ + (z *= (double)dist), dir, 90.0f);
                    entityDrive2.setIniPitch(90.0f);
                    entityDrive2.setIniYaw(dir);
                    entityDrive2.setDriveVector(1.75f);
                    entityDrive2.setColor(7364008);
                    entityDrive2.setInterval(7 + i / 2);
                    entityDrive2.setLifeTime(30);
                    entityDrive2.setTargetEntityId(target.getEntityId());
                    world.spawnEntity((Entity)entityDrive2);
                }
                count = (int)Math.ceil((double) count / 3 * 2);
                rad = Math.PI * 2 / (double)count;
                for (i = 0; i < count; ++i) {
                    entityDrive2 = new EntityLightningSword(world, (EntityLivingBase)entityPlayer, magicDamage, 90.0f);
                    dist = 3.0f;
                    ran = rad * (double)i;
                    x = Math.sin(ran);
                    y = 1.6 + (double)entityDrive2.getRand().nextFloat();
                    z = Math.cos(ran);
                    dir = -((int)Math.toDegrees(Math.PI + ran));
                    entityDrive2.setLocationAndAngles(target.posX + (x *= (double)dist), target.posY + (y *= (double)dist), target.posZ + (z *= (double)dist) + 2.0, dir, 90.0f);
                    entityDrive2.setIniPitch(90.0f);
                    entityDrive2.setIniYaw(dir);
                    entityDrive2.setDriveVector(1.75f);
                    entityDrive2.setColor(16766720);
                    entityDrive2.setInterval(7 + i * 2 + 15);
                    entityDrive2.setLifeTime(40);
                    entityDrive2.setTargetEntityId(target.getEntityId());
                    world.spawnEntity((Entity)entityDrive2);
                }
                for (i = 0; i < count; ++i) {
                    entityDrive2 = new EntityLightningSword(world, (EntityLivingBase)entityPlayer, magicDamage, 90.0f);
                    entityDrive2.setColor(16766720);
                    entityDrive2.setInterval(7 + i);
                    entityDrive2.setLifeTime(30);
                    entityDrive2.setTargetEntityId(target.getEntityId());
                    world.spawnEntity((Entity)entityDrive2);
                }
                    entityDrive = new EntityLightningSword(world, (EntityLivingBase)entityPlayer, magicDamage, 90.0f);
                    float dist2 = 2.0f;
                    double ran2 = rad * (double)count;
                    int dir2 = -((int)Math.toDegrees(Math.PI + ran2));
                    entityDrive.setLocationAndAngles(target.posX, target.posY + 4.0, target.posZ, dir2, 90.0f);
                    entityDrive.setIniPitch(90.0f);
                    entityDrive.setIniYaw(dir2);
                    entityDrive.setDriveVector(1.75f);
                    entityDrive.setColor(16766720);
                    entityDrive.setInterval(7 + count / 2 + 10);
                    entityDrive.setLifeTime(40);
                    entityDrive.setTargetEntityId(target.getEntityId());
                    world.spawnEntity((Entity)entityDrive);
                }
            }
    }
}

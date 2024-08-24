package com.wjx.kablade.AllWeapon.blade.specialattack;

import com.wjx.kablade.Entity.EntityDriveAdd;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SATool;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.client.particle.Particle;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;

public class AL_WeiZhan extends SpecialAttackBase {
    @Override
    public String toString() {
        return "weizhan";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.world;
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(itemStack);

        if(!world.isRemote){
            ItemSlashBlade blade = (ItemSlashBlade)(itemStack.getItem());
            float baseModif = ItemSlashBlade.BaseAttackModifier.get(tag)/3;
            int level = EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, itemStack);
            float magicDamage = baseModif*2;
            magicDamage+= MathFunc.amplifierCalc(ItemSlashBlade.BaseAttackModifier.get(tag),1f);
            Entity target = null;
            int entityId = ItemSlashBlade.TargetEntityId.get(tag);
            if (entityId != 0) {
                Entity tmp = world.getEntityByID(entityId);
                if (tmp != null && tmp.getDistance(entityPlayer) < 100.0F && tmp instanceof EntityLivingBase) {
                    target = tmp;
                }
            }
            if (target==null){
                target= SATool.getEntityToWatch(entityPlayer);

            }

            if(target!=null){
                entityPlayer.onCriticalHit(target);
                target.motionX = 0.0;
                target.motionY = 0.0;
                target.motionZ = 0.0;
                if (target instanceof EntityLivingBase) {
                    blade.setDaunting((EntityLivingBase)target);
                    ((EntityLivingBase)target).hurtTime = 0;
                    ((EntityLivingBase)target).hurtResistantTime = 0;
                }
                for (int i = 0; i < 8; ++i) {
                    EntityDriveAdd entityDrive = new EntityDriveAdd(world, entityPlayer, magicDamage, false, 0.0f - ItemSlashBlade.ComboSequence.Battou.swingDirection);
                    entityDrive.setPositionAndRotation(target.posX, target.posY, target.posZ, target.rotationYaw, target.rotationPitch);
                    entityDrive.getDataManager().set(EntityDriveAdd.COLOR_R,1f);
                    entityDrive.getDataManager().set(EntityDriveAdd.COLOR_G,0f);
                    entityDrive.getDataManager().set(EntityDriveAdd.COLOR_B,0f);
                    entityDrive.getDataManager().set(EntityDriveAdd.SCALE_X,0f);
                    entityDrive.getDataManager().set(EntityDriveAdd.SCALE_Y,0f);
                    entityDrive.getDataManager().set(EntityDriveAdd.SCALE_Z,0f);
                    entityDrive.setLifeTime(10);
                    entityDrive.getDataManager().set(EntityDriveAdd.PARTICLE_STYLE, "ENCHANTMENT_TABLE");

                    if (entityDrive == null) continue;
                    world.spawnEntity(entityDrive);
                }
            }

        }
    }
}

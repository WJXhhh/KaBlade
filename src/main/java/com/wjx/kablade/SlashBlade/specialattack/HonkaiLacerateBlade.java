package com.wjx.kablade.SlashBlade.specialattack;

import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.entity.EntitySummonedBlade;
import mods.flammpfeil.slashblade.entity.EntitySummonedSwordBase;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

public class HonkaiLacerateBlade extends SpecialAttackBase {
    @Override
    public String toString() {
        return "lacerate_blade";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {


            World world = entityPlayer.world;
            if(!world.isRemote)
        {
            float extraDamage = (float) Math.log((-ItemSlashBlade.AttackAmplifier.get((itemStack.getTagCompound()))) * 10f) * 5f;
            EntityDrive entityDrive = new EntityDrive(world, entityPlayer, 10f + extraDamage,true,90f);
            entityDrive.setPosition(entityPlayer.posX,entityPlayer.posY + entityPlayer.getEyeHeight(),entityPlayer.posZ);
            world.spawnEntity(entityDrive);
            if(entityPlayer.getAttackingEntity()!=null ){
                entityPlayer.getAttackingEntity().addPotionEffect(new PotionEffect(MobEffects.WITHER,100,1));
            }
        }
    }
}

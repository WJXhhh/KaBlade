package com.wjx.kablade.SlashBlade.specialattack;

import com.wjx.kablade.Entity.EntityDriveAdd;
import com.wjx.kablade.util.MathFunc;
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

import java.util.Random;

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
            float extraDamage = (float) MathFunc.amplifierCalc((ItemSlashBlade.BaseAttackModifier.get((itemStack.getTagCompound()))),3f);
            Random rand = entityPlayer.getRNG();
            float a = rand.nextInt(360);
            EntityDriveAdd entityDrive = new EntityDriveAdd(world, entityPlayer, 10f+extraDamage, false, a - ItemSlashBlade.ComboSequence.Battou.swingDirection);

            entityDrive.getDataManager().set(EntityDriveAdd.COLOR_R,0.8f);
            entityDrive.getDataManager().set(EntityDriveAdd.COLOR_G,0.2f);
            entityDrive.getDataManager().set(EntityDriveAdd.COLOR_B,0.4f);
            entityDrive.setChangeTime(1);
            entityDrive.setNextSpeed(1.3f);
            entityDrive.setLocationAndAngles(entityPlayer.posX + (double)rand.nextInt(25) * 0.1, entityPlayer.posY + (double)rand.nextInt(13) + (double)entityPlayer.getEyeHeight() / 2.0, entityPlayer.posZ + (double)rand.nextInt(25) * 0.1, entityPlayer.rotationYaw, 0.0f);
            entityDrive.getDataManager().set(EntityDriveAdd.SCALE_X,1f);
            entityDrive.getDataManager().set(EntityDriveAdd.SCALE_Y,2f);
            entityDrive.getDataManager().set(EntityDriveAdd.SCALE_Z,1f);
            entityDrive.setInitialSpeed((float) (0.1 + 0.001f));
            entityDrive.getDataManager().set(EntityDriveAdd.PL_PARTICAL,false);
            entityDrive.setLifeTime(90);

            world.spawnEntity(entityDrive);
        }
            if(entityPlayer.getAttackingEntity()!=null ){
                entityPlayer.getAttackingEntity().addPotionEffect(new PotionEffect(MobEffects.WITHER,100,1));
            }
        }
    }


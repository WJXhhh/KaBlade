package com.wjx.kablade.SlashBlade.specialattack;

import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.List;

public class HonKaiFireOfSin extends SpecialAttackBase {
    @Override
    public String toString() {
        return "fire_of_sin";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.world;
        float extraDamage = (float) Math.log((-ItemSlashBlade.AttackAmplifier.get((itemStack.getTagCompound()))) * 10f)*5f;
        if (!world.isRemote){
            EntityDrive entityDrive = new EntityDrive(world, entityPlayer, 10f + extraDamage,true,90f);
            double d0 = (double)4 / 2.0D;
            entityDrive.setPosition(entityPlayer.posX,entityPlayer.posY + entityPlayer.getEyeHeight(),entityPlayer.posZ);
            entityDrive.setEntityBoundingBox(new AxisAlignedBB(entityDrive.posX - d0, entityDrive.posY, entityDrive.posZ - d0, entityDrive.posX + d0, entityDrive.posY + (double)entityDrive.height, entityDrive.posZ + d0));
            AxisAlignedBB bb = entityPlayer.getEntityBoundingBox();
            bb = bb.grow(4.0D, 0.0D, 4.0D);
            bb = bb.offset(entityPlayer.motionX, entityPlayer.motionY, entityPlayer.motionZ);
            List<Entity> list = world.getEntitiesInAABBexcluding(entityPlayer, bb, input -> input != entityPlayer && input.isEntityAlive());
            for (Entity entity : list){
                if (entity instanceof EntityLivingBase){
                    entity.setFire(4);
                }
            }
            world.spawnEntity(entityDrive);
        }
        for (int i = 0; i < 20; ++i)
        {
            int state1;
            int state2;
            if (world.rand.nextBoolean()){
                state1 = 1;
            }
            else state1 = -1;
            if (world.rand.nextBoolean()){
                state2 = 1;
            }
            else state2 = -1;
            world.spawnParticle(EnumParticleTypes.DRIP_LAVA, entityPlayer.posX + (world.rand.nextDouble() * state1*2), entityPlayer.posY + world.rand.nextDouble() * (double)entityPlayer.height/2, entityPlayer.posZ + (world.rand.nextDouble()* state2*2), 0.0D, 0.0D, 0.0D);
        }

    }
}

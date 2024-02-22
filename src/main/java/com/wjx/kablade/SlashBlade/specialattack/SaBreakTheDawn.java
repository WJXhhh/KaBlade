package com.wjx.kablade.SlashBlade.specialattack;

import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.entity.EntitySummonedSwordBase;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class SaBreakTheDawn extends SpecialAttackBase {
    @Override
    public String toString() {
        return "break_the_dawn";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.getEntityWorld();
        float extraDamage = ItemSlashBlade.AttackAmplifier.get(entityPlayer.getHeldItemMainhand().getTagCompound()) * (0.5f + (1.0f));
        for (int i=0;i<5;i++){
            EntitySummonedSwordBase s = new EntitySummonedSwordBase(world,entityPlayer,5 + extraDamage);
            s.setColor(0xfff8ca);
            EntityDrive d = new EntityDrive(world,entityPlayer,3);
            world.spawnEntity(s);
            world.spawnEntity(d);
        }
        AxisAlignedBB bb = entityPlayer.getEntityBoundingBox().grow(10,10,10).offset(entityPlayer.motionX,entityPlayer.motionY,entityPlayer.motionZ);
        List<Entity> l = world.getEntitiesInAABBexcluding(entityPlayer,bb,input -> input != entityPlayer&&input instanceof EntityLivingBase);
            for (Entity e : l){
                if (e instanceof EntityLivingBase){
                    EntityLivingBase ee = (EntityLivingBase) e;
                    ee.addPotionEffect(new PotionEffect(MobEffects.GLOWING,120,2));
                }
            }
        entityPlayer.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION,80,2));
            for (int i = 0;i<40;i++){
                double x1,z1;
                x1 = r(world.rand);
               z1 = r(world.rand);
                world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK,entityPlayer.posX + (world.rand.nextDouble() * 3 * x1),entityPlayer.posY + world.rand.nextDouble(),entityPlayer.posZ + (world.rand.nextDouble() * 3 * z1),0d,0d,0d);
            }
    }

    double r(Random rand){
        if (rand.nextBoolean()){
            return 1d;
        }else return -1d;
    }
}

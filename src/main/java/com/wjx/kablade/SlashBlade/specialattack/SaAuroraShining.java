package com.wjx.kablade.SlashBlade.specialattack;

import com.wjx.kablade.Entity.EntitySummonSwordFree;
import com.wjx.kablade.Entity.EntitySummonedSwordBasePlus;
import com.wjx.kablade.Main;
import com.wjx.kablade.event.WorldEvent;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.entity.EntitySummonedSwordBase;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.Drive;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class SaAuroraShining extends SpecialAttackBase {
    @Override
    public String toString() {
        return "aurora_shining";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.getEntityWorld();
        float extraDamage = ItemSlashBlade.AttackAmplifier.get(entityPlayer.getHeldItemMainhand().getTagCompound()) * (0.5f + (2f / 5.0f));
        for (int i=0;i<10;i++){
            EntitySummonedSwordBasePlus s = new EntitySummonedSwordBasePlus(world,entityPlayer,4 + extraDamage);
            s.getDataManager().set(EntitySummonedSwordBasePlus.BRIGHT,15728880);
            s.getDataManager().set(EntitySummonedSwordBasePlus.BRIGHTNESS,15f);
            int color = WorldEvent.auroraBladeColor.get(new Random().nextInt(60));
            s.setColor(color);
            EntityDrive d = new EntityDrive(world,entityPlayer,2 + extraDamage);
            d.setPosition(entityPlayer.posX,entityPlayer.posY+entityPlayer.eyeHeight,entityPlayer.posZ);
            world.spawnEntity(s);
            world.spawnEntity(d);
        }
        float extraDamage2 = ItemSlashBlade.AttackAmplifier.get(entityPlayer.getHeldItemMainhand().getTagCompound()) * (0.5f + (8f / 5.0f));
        AxisAlignedBB bb = entityPlayer.getEntityBoundingBox().grow(20,10,20).offset(entityPlayer.motionX,entityPlayer.motionY,entityPlayer.motionZ);
        List<Entity> l = world.getEntitiesInAABBexcluding(entityPlayer,bb,input -> input != entityPlayer&&input instanceof EntityLivingBase);
            for (Entity e : l){
                if (e instanceof EntityLivingBase){
                    EntityLivingBase ee = (EntityLivingBase) e;
                    ee.addPotionEffect(new PotionEffect(MobEffects.GLOWING,120,2));
                    if (!world.isRemote)
                    ee.attackEntityFrom(DamageSource.causePlayerDamage(entityPlayer).setDamageBypassesArmor(),8 + extraDamage2);
                    ee.setFire(5);
                }
            }
        entityPlayer.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION,240,2));
            for (int i = 0;i<80;i++){
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

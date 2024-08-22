package com.wjx.kablade.SlashBlade.specialattack;

import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class RockHit_I extends SpecialAttackBase {
    @Override
    public String toString() {
        return "rock_hit_i";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.getEntityWorld();
        double x = entityPlayer.posX;
        double y = entityPlayer.posY;
        double z = entityPlayer.posZ;
        float extraDamage = MathFunc.amplifierCalc(ItemSlashBlade.BaseAttackModifier.get(entityPlayer.getHeldItemMainhand().getTagCompound()),6f);
        world.playSound(null, x, y, z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 4.0F, (1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F) * 0.7F);
        for (int i = 0; i < 40; ++i)
        {
            Random r1 =new Random();
            Random r2 =new Random(r1.nextLong());
            int state1;
            int state2;
            if (r1.nextBoolean()){
                state1 = 1;
            }
            else state1 = -1;
            if (r2.nextBoolean()){
                state2 = 1;
            }
            else state2 = -1;
            world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, x + (world.rand.nextDouble() * 2 * state1), y + world.rand.nextDouble() * (double)entityPlayer.height, z + (world.rand.nextDouble() * 2 * state2), 0.0D, 0.0D, 0.0D);
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x + (world.rand.nextDouble() * 2 * state1), y + world.rand.nextDouble() * (double)entityPlayer.height, z + (world.rand.nextDouble() * 2 * state2), 0.0D, 0.0D, 0.0D);
        }
        AxisAlignedBB bb = entityPlayer.getEntityBoundingBox();
        bb = bb.grow(5,4,5);
        bb = bb.offset(entityPlayer.motionX,entityPlayer.motionY,entityPlayer.motionZ);
        List<Entity> entities = world.getEntitiesInAABBexcluding(entityPlayer,bb, input -> input != entityPlayer && input instanceof EntityLivingBase);
        for (Entity e : entities){
            e.attackEntityFrom(DamageSource.causeExplosionDamage(entityPlayer),6f + extraDamage);
            if (e instanceof EntityLivingBase){
                EntityLivingBase en = (EntityLivingBase)e;
                en.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS,40,2));
            }
        }
        entityPlayer.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE,40,1));
    }
}

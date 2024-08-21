package com.wjx.kablade.AllWeapon.blade.specialattack;

import com.wjx.kablade.util.SATool;
import mods.flammpfeil.slashblade.EntityDirectAttackDummy;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import mods.flammpfeil.slashblade.util.PotionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionHelper;
import net.minecraft.potion.PotionType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IThrowableEntity;

import java.util.List;
import java.util.Random;

public class AL_Liedi extends SpecialAttackBase {
    @Override
    public String toString() {
        return "liedi";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.world;
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(itemStack);
        Entity tmp;

        ItemSlashBlade blade = (ItemSlashBlade)(itemStack.getItem());
        AxisAlignedBB bb = entityPlayer.getEntityBoundingBox();
        bb = bb.grow(12.0D, 5.0D, 12.0D);
        bb = bb.offset(entityPlayer.motionX, entityPlayer.motionY, entityPlayer.motionZ);

        List<Entity> list = entityPlayer.world.getEntitiesInAABBexcluding(entityPlayer, bb, input -> input != entityPlayer && input.isEntityAlive());
        entityPlayer.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 80, 3,false, true));
        if (!list.isEmpty()){
            for (Entity entity: list){
                if (entity instanceof EntityLivingBase){
                    blade.attackTargetEntity(itemStack, entity, entityPlayer, true);
                    entityPlayer.onCriticalHit(entity);
                    entity.motionX = 0.0;
                    entity.motionY = 2.0;
                    entity.motionZ = 0.0;
                    entity.attackEntityFrom(DamageSource.causePlayerDamage(entityPlayer),5);
                    if (entity instanceof EntityLivingBase) {
                        blade.setDaunting((EntityLivingBase)entity);
                        ((EntityLivingBase)entity).hurtTime = 0;
                        ((EntityLivingBase)entity).hurtResistantTime = 0;
                    }

                }
            }
        }
        

    }
}

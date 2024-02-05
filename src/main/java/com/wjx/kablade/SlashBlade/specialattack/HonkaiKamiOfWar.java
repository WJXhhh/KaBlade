package com.wjx.kablade.SlashBlade.specialattack;

import com.wjx.kablade.init.PotionInit;
import com.wjx.kablade.util.KaBladePlayerProp;
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

public class HonkaiKamiOfWar extends SpecialAttackBase {
    @Override
    public String toString() {
        return "kami_of_war";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.getEntityWorld();
        double x = entityPlayer.posX;
        double y = entityPlayer.posY;
        double z = entityPlayer.posZ;
        KaBladePlayerProp.getPropCompound(entityPlayer).setInteger(KaBladePlayerProp.KAMI_OF_WAR_COUNT,6);
        KaBladePlayerProp.getPropCompound(entityPlayer).setInteger(KaBladePlayerProp.KAMI_OF_WAR_TICK,1);
        KaBladePlayerProp.updateNBTForClient(entityPlayer);
        entityPlayer.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE,40,1));
        entityPlayer.addPotionEffect(new PotionEffect(MobEffects.STRENGTH,140,2));
    }
}

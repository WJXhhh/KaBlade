package com.wjx.kablade.SlashBlade.specialattack;

import com.wjx.kablade.Entity.SummonBladeOfFrostBlade;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.TargetingUtil;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class HonKaiFrostBlade extends SpecialAttackBase {
    private static final int SWORD_COUNT = 6;
    private static final int SWORD_COLOR = 0x20DDF4;

    @Override
    public String toString() {
        return "frost_blade";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.world;
        if (world.isRemote) {
            return;
        }

        float extraDamage = MathFunc.amplifierCalc(
                ItemSlashBlade.BaseAttackModifier.get(ItemSlashBlade.getItemTagCompound(itemStack)), 1.0F) * 3.0F;
        Entity resolved = TargetingUtil.resolveTarget(entityPlayer, itemStack, 24.0D, 12.0D, 12.0D);
        EntityLivingBase target = resolved instanceof EntityLivingBase
                && TargetingUtil.canDamage(entityPlayer, resolved) ? (EntityLivingBase) resolved : null;
        Vec3d fallbackDirection = entityPlayer.getLookVec().normalize();

        world.playSound(null, entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ,
                SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.9F, 1.45F);

        for (int i = 0; i < SWORD_COUNT; i++) {
            SummonBladeOfFrostBlade entity = new SummonBladeOfFrostBlade(
                    world, entityPlayer, target, extraDamage, i, fallbackDirection);
            entity.setColor(SWORD_COLOR);
            world.spawnEntity(entity);
        }
    }
}

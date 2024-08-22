package com.wjx.kablade.SlashBlade.specialattack;

import com.wjx.kablade.Main;
import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.Spear;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;


import java.util.List;

public class HonKaiFrostComet extends SpecialAttackBase {
    @Override
    public String toString() {
        return "frost_comet";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.world;
        if (!world.isRemote) {

            AxisAlignedBB bb = entityPlayer.getEntityBoundingBox();
            bb = bb.grow(5.0D, 1.0D, 5.0D);
            bb = bb.offset(entityPlayer.motionX, entityPlayer.motionY, entityPlayer.motionZ);
            List<Entity> list = entityPlayer.world.getEntitiesInAABBexcluding(entityPlayer, bb, input -> input != entityPlayer && input.isEntityAlive());
            float extraDamage = (float) MathFunc.amplifierCalc((ItemSlashBlade.BaseAttackModifier.get((itemStack.getTagCompound()))),1f);
            if (list.size() != 0) {
                for (Entity entity : list) {
                    if (entity instanceof EntityLivingBase) {
                        entity.attackEntityFrom(DamageSource.causePlayerDamage(entityPlayer), extraDamage);
                        Block ice = Blocks.PACKED_ICE;
                        world.setBlockState(new BlockPos(entity.posX, entity.posY, entity.posZ), ice.getDefaultState());
                        world.setBlockState(new BlockPos(entity.posX, entity.posY + 1, entity.posZ), ice.getDefaultState());
                        entity.motionY= 0;
                        entity.motionX=0;
                        entity.motionZ=0;
                    }
                }

            }

        }
    }
}

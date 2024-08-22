package com.wjx.kablade.AllWeapon.blade.specialattack;

import com.wjx.kablade.Entity.EntitySlashDimensionAdd;
import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.entity.EntitySlashDimension;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class AL_Fengxuan extends SpecialAttackBase {
    @Override
    public String toString() {
        return "fengxuan";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.world;
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(itemStack);

        if(!world.isRemote) {
            ItemSlashBlade blade = (ItemSlashBlade) (itemStack.getItem());
            float baseModif = ItemSlashBlade.BaseAttackModifier.get(tag) / 3.4f;
            float magicDamage = baseModif + MathFunc.amplifierCalc(ItemSlashBlade.BaseAttackModifier.get(tag),3f);
            EntitySlashDimensionAdd dim = new EntitySlashDimensionAdd(world, entityPlayer, magicDamage);
            if (dim != null) {
                Vec3d tmppos;
                Block block;
                Vec3d post = entityPlayer.getLookVec();
                float scale = 5.0f;
                Vec3d pos = new Vec3d(post.x*scale,post.y*scale,post.z*scale);
                pos = pos.add(entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ);
                pos = pos.add(0.0, entityPlayer.getEyeHeight(), 0.0);
                Vec3d offset = new Vec3d((double)entityPlayer.posX, (double)entityPlayer.posY, (double)entityPlayer.posZ).add(0.0, entityPlayer.getEyeHeight(), 0.0);
                Vec3d look = entityPlayer.getLookVec();
                Vec3d offsettedLook = offset.add(look.x * 5.0, look.y * 5.0, look.z * 5.0);
                RayTraceResult movingobjectposition = world.rayTraceBlocks(offset, offsettedLook);

                if (movingobjectposition != null)
                {
                    IBlockState state = null;
                    BlockPos blockPos = movingobjectposition.getBlockPos();
                    if(blockPos != null)
                        state = world.getBlockState(blockPos);
                    if(state != null && state.getCollisionBoundingBox(world, blockPos) == null)
                        movingobjectposition = null;
                    else {
                        Vec3d tmpposi = new Vec3d(movingobjectposition.hitVec.x, movingobjectposition.hitVec.y, movingobjectposition.hitVec.z);
                        if(1 < tmpposi.distanceTo(entityPlayer.getPositionVector())){
                            pos = tmpposi;
                        }
                    }
                }
                dim.setPosition(pos.x, pos.y, pos.z);
                dim.setLifeTime(50);
                dim.setColor(0xFFFFFF);
                dim.setIsSlashDimension(true);
                dim.setIsSingleHit(false);
                world.spawnEntity(dim);
            }
        }
    }
}

package com.wjx.kablade.SlashBlade.specialattack;

import com.wjx.kablade.Entity.EntityRaizanCleave;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.TargetingUtil;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;

/** 天殛之境的两阶段浮游武器斩击。 */
public class RaizanCleave extends SpecialAttackBase {
    public String toString(){return "raizan_cleave";}
    public void doSpacialAttack(ItemStack blade,EntityPlayer player){if(player.world.isRemote||EntityRaizanCleave.isCasting(player))return;Entity target=TargetingUtil.resolveTarget(player,blade,8,8,5);Vec3d origin=player.getPositionVector();Vec3d anchor=target==null?origin.add(flatLook(player).scale(4)).add(0,player.height*.52,0):target.getEntityBoundingBox().getCenter();Vec3d dir=anchor.subtract(origin);dir=new Vec3d(dir.x,0,dir.z);if(dir.lengthSquared()<1E-6)dir=flatLook(player);else dir=dir.normalize();float yaw=(float)(Math.atan2(-dir.x,dir.z)*180/Math.PI);float attack=ItemSlashBlade.BaseAttackModifier.get(blade.getTagCompound());float damage=(50+MathFunc.amplifierCalc(attack,12))*1.4F*4F;player.world.spawnEntity(new EntityRaizanCleave(player.world,player,anchor,yaw,damage));player.world.playSound(null,player.posX,player.posY,player.posZ,SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,SoundCategory.PLAYERS,.82F,1.72F);player.world.playSound(null,player.posX,player.posY,player.posZ,SoundEvents.ENTITY_ENDERDRAGON_FLAP,SoundCategory.PLAYERS,.72F,1.34F);}
    private static Vec3d flatLook(EntityPlayer p){Vec3d v=p.getLookVec(),f=new Vec3d(v.x,0,v.z);return f.lengthSquared()<1E-6?new Vec3d(0,0,1):f.normalize();}
}

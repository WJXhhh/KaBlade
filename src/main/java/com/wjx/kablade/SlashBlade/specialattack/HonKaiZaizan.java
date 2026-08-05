package com.wjx.kablade.SlashBlade.specialattack;

import com.wjx.kablade.event.WorldEvent;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.TargetingUtil;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.Spear;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;

public class HonKaiZaizan extends SpecialAttackBase {
    @Override
    public String toString() {
        return "zaizan";
    }

    public static PotionEffect s = new PotionEffect(MobEffects.STRENGTH,140,2);

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        Spear spear = new Spear();
        spear.doSpacialAttack(itemStack,entityPlayer);
        AxisAlignedBB ax = entityPlayer.getEntityBoundingBox();
       ax= ax.grow(5,1,5);
        ax=ax.offset(entityPlayer.motionX,entityPlayer.motionY,entityPlayer.motionZ);
        if(!entityPlayer.world.isRemote){
            List<Entity> entities = entityPlayer.world.getEntitiesInAABBexcluding(entityPlayer, ax,
                    input -> TargetingUtil.canSelectForDamage(entityPlayer, input));
            float extraDamage = MathFunc.amplifierCalc((ItemSlashBlade.BaseAttackModifier.get(entityPlayer.getHeldItemMainhand().getTagCompound())),20f);
            for (Entity entity : TargetingUtil.getDistinctDamageTargets(entities)){
                EntityLivingBase effectTarget = TargetingUtil.getSelectionTarget(entity);
                if (effectTarget != null){
                    if(!(effectTarget instanceof EntityPlayer)){
                        entityPlayer.onCriticalHit(entity);
                        effectTarget.hurtResistantTime = 0;
                        entity.attackEntityFrom(DamageSource.causePlayerDamage(entityPlayer).setDamageBypassesArmor(),20 + extraDamage);
                        effectTarget.hurtResistantTime = 0;
                        itemStack.hitEntity(effectTarget,entityPlayer);
                    }
                    else{
                        effectTarget.addPotionEffect(new PotionEffect(MobEffects.STRENGTH,100,2));
                    }
                }
            }
            WorldEvent.addTickDelayTask(12, () -> entityPlayer.addPotionEffect(s));
        }
    }
}

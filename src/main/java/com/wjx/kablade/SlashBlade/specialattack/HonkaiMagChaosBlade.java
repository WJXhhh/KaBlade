package com.wjx.kablade.SlashBlade.specialattack;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.wjx.kablade.Main;
import com.wjx.kablade.init.PotionInit;
import com.wjx.kablade.network.MessageMagChaosBladeEffectUpdate;
import com.wjx.kablade.util.KaBladePlayerProp;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.special_render.MagChaosBladeEffectRenderer;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class HonkaiMagChaosBlade extends SpecialAttackBase {
    @Override
    public String toString() {
        return "mag_chaos_blade";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        doMagStormAttack(entityPlayer);
    }

    @SuppressWarnings("Guava")
    private void doMagStormAttack(EntityPlayer entityPlayer){
        World world = entityPlayer.getEntityWorld();
        if (!world.isRemote){
            float extraDamage = (float) MathFunc.amplifierCalc((ItemSlashBlade.BaseAttackModifier.get(entityPlayer.getHeldItemMainhand().getTagCompound())),40f);
            MagChaosBladeEffectRenderer.magChaosBladeEffectRenderers.add(new MagChaosBladeEffectRenderer(entityPlayer));
            Main.PACKET_HANDLER.sendToAll(new MessageMagChaosBladeEffectUpdate());
            KaBladePlayerProp.getPropCompound(entityPlayer).setInteger(KaBladePlayerProp.MAG_CHAOS_BLADE_EXTRA_ATTACK_TICK,6);
            double dist = 6;
            Vec3d vec3d = entityPlayer.getPositionEyes(1.0F);
            Vec3d vec3d1 = entityPlayer.getLook(1.0F);
            Vec3d vec3d2 = vec3d.add(vec3d1.x * dist, vec3d1.y * dist, vec3d1.z * dist);
            List<Entity> pointedEntity = Lists.newArrayList();
            List<Entity> list = world.getEntitiesInAABBexcluding(entityPlayer, entityPlayer.getEntityBoundingBox().expand(vec3d1.x * dist, vec3d1.y * dist, vec3d1.z * dist).grow(3.0D, 1.0D, 3.0D), Predicates.and(EntitySelectors.NOT_SPECTATING, entity -> entity != null && entity.canBeCollidedWith() && (entity instanceof EntityPlayer || entity instanceof EntityLiving)));
            double d2 = dist;
            if (!list.isEmpty()){
                for (Entity e : list){
                    if (e instanceof EntityLivingBase && !(e instanceof EntityPlayer)){
                        e.attackEntityFrom(DamageSource.causePlayerDamage(entityPlayer),40f + extraDamage);
                        ((EntityLivingBase) e).addPotionEffect(new PotionEffect(PotionInit.PARALY,100,3));
                    }
                }
            }
        }
    }
}

package com.wjx.kablade.SlashBlade.specialattack;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.wjx.kablade.Main;
import com.wjx.kablade.init.PotionInit;
import com.wjx.kablade.network.MessageMagChaosBladeEffectUpdate;
import com.wjx.kablade.util.KaBladePlayerProp;
import com.wjx.kablade.util.special_render.MagChaosBladeEffectRenderer;
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
            MagChaosBladeEffectRenderer.magChaosBladeEffectRenderers.add(new MagChaosBladeEffectRenderer(entityPlayer));
            Main.PACKET_HANDLER.sendToAll(new MessageMagChaosBladeEffectUpdate());
            KaBladePlayerProp.getPropCompound(entityPlayer).setInteger(KaBladePlayerProp.MAG_CHAOS_BLADE_EXTRA_ATTACK_TICK,6);
            double dist = 6;
            Vec3d vec3d = entityPlayer.getPositionEyes(1.0F);
            Vec3d vec3d1 = entityPlayer.getLook(1.0F);
            Vec3d vec3d2 = vec3d.add(vec3d1.x * dist, vec3d1.y * dist, vec3d1.z * dist);
            List<Entity> pointedEntity = Lists.newArrayList();
            List<Entity> list = world.getEntitiesInAABBexcluding(entityPlayer, entityPlayer.getEntityBoundingBox().expand(vec3d1.x * dist, vec3d1.y * dist, vec3d1.z * dist).grow(1.0D, 1.0D, 1.0D), Predicates.and(EntitySelectors.NOT_SPECTATING, entity -> entity != null && entity.canBeCollidedWith() && (entity instanceof EntityPlayer || entity instanceof EntityLiving)));
            double d2 = dist;
            for (Entity entity1 : list) {
                AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().grow(entity1.getCollisionBorderSize());
                RayTraceResult raytraceresult = axisalignedbb.calculateIntercept(vec3d, vec3d2);

                if (axisalignedbb.contains(vec3d)) {
                    if (d2 >= 0.0D) {
                        pointedEntity.add(entity1);
                        d2 = 0.0D;
                    }
                } else if (raytraceresult != null) {
                    double d3 = vec3d.distanceTo(raytraceresult.hitVec);

                    if (d3 < d2 || d2 == 0.0D) {
                        if (entity1.getLowestRidingEntity() == entityPlayer.getLowestRidingEntity() && !entityPlayer.canRiderInteract()) {
                            if (d2 == 0.0D) {
                                pointedEntity.add(entity1);
                            }
                        } else {
                            pointedEntity.add(entity1);
                            d2 = d3;
                        }
                    }
                }
            }
            if (!pointedEntity.isEmpty()){
                for (Entity e : pointedEntity){
                    if (e instanceof EntityLivingBase && !(e instanceof EntityPlayer)){
                        e.attackEntityFrom(DamageSource.causePlayerDamage(entityPlayer),40f);
                        ((EntityLivingBase) e).addPotionEffect(new PotionEffect(PotionInit.PARALY,100,3));
                    }
                }
            }
        }
    }
}

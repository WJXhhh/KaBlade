package com.wjx.kablade.SlashBlade.specialattack;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.wjx.kablade.init.PotionInit;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

public class HonkaiAbsoluteZero extends SpecialAttackBase {
    @Override
    public String toString() {
        return "absolute_zero";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.getEntityWorld();
            double dist = 8;
            Vec3d vec3d = entityPlayer.getPositionEyes(1.0F);
            Vec3d vec3d1 = entityPlayer.getLook(1.0F);
            Vec3d vec3d2 = vec3d.add(vec3d1.x * dist, vec3d1.y * dist, vec3d1.z * dist);
            double d1 = dist;
            Entity pointedEntity = null;
            List<Entity> list = world.getEntitiesInAABBexcluding(entityPlayer, entityPlayer.getEntityBoundingBox().expand(vec3d1.x * dist, vec3d1.y * dist, vec3d1.z * dist).grow(1.0D, 1.0D, 1.0D), Predicates.and(EntitySelectors.NOT_SPECTATING, entity -> {
                boolean blindness = entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isPotionActive(MobEffects.BLINDNESS);
                return entity != null && entity.canBeCollidedWith() && !blindness && (entity instanceof EntityPlayer || entity instanceof EntityLiving);
            }));
            double d2 = d1;
            for (Entity entity1 : list) {
                AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().grow(entity1.getCollisionBorderSize());
                RayTraceResult raytraceresult = axisalignedbb.calculateIntercept(vec3d, vec3d2);

                if (axisalignedbb.contains(vec3d)) {
                    if (d2 >= 0.0D) {
                        pointedEntity = entity1;
                        d2 = 0.0D;
                    }
                } else if (raytraceresult != null) {
                    double d3 = vec3d.distanceTo(raytraceresult.hitVec);

                    if (d3 < d2 || d2 == 0.0D) {
                        if (entity1.getLowestRidingEntity() == entityPlayer.getLowestRidingEntity() && !entityPlayer.canRiderInteract()) {
                            if (d2 == 0.0D) {
                                pointedEntity = entity1;
                            }
                        } else {
                            pointedEntity = entity1;
                            d2 = d3;
                        }
                    }
                }
            }
            if (pointedEntity != null){
                if (pointedEntity instanceof EntityLivingBase){
                    if (!world.isRemote){
                        ((EntityLivingBase) pointedEntity).addPotionEffect(new PotionEffect(PotionInit.FREEZE,140,1));
                        pointedEntity.attackEntityFrom(DamageSource.causePlayerDamage(entityPlayer),20);
                    }
                }
            }
        for (int i = 0;i<60;i++){
            double x1,z1;
            x1 = r(world.rand);
            z1 = r(world.rand);
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,entityPlayer.posX + (world.rand.nextDouble() * 3 * x1),entityPlayer.posY + world.rand.nextDouble(),entityPlayer.posZ + (world.rand.nextDouble() * 3 * z1),0d,0.1d,0d);
        }
    }

    double r(Random rand){
        if (rand.nextBoolean()){
            return 1d;
        }else return -1d;
    }
}

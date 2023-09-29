package com.wjx.kablade.SlashBlade.specialattack;

import com.google.common.base.Predicates;
import com.wjx.kablade.Entity.EntityFreezeDomain;
import com.wjx.kablade.Main;
import com.wjx.kablade.init.PotionInit;
import com.wjx.kablade.network.MessageSpawnParticle;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class HonkaiSnowDance extends SpecialAttackBase {
    @Override
    public String toString() {
        return "snow_dance";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.getEntityWorld();
        if (!world.isRemote) {
            double dist = 8;
            Vec3d vec3d = entityPlayer.getPositionEyes(1.0F);
            Vec3d vec3d1 = entityPlayer.getLook(1.0F);
            Vec3d vec3d2 = vec3d.add(vec3d1.x * dist, vec3d1.y * dist, vec3d1.z * dist);
            Entity pointedEntity = null;
            List<Entity> list = world.getEntitiesInAABBexcluding(entityPlayer, entityPlayer.getEntityBoundingBox().expand(vec3d1.x * dist, vec3d1.y * dist, vec3d1.z * dist).grow(1.0D, 1.0D, 1.0D), Predicates.and(EntitySelectors.NOT_SPECTATING, entity -> entity != null && entity.canBeCollidedWith() && (entity instanceof EntityPlayer || entity instanceof EntityLiving)));
            double d2 = dist;
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
                    ((EntityLivingBase) pointedEntity).addPotionEffect(new PotionEffect(PotionInit.FREEZE, 140, 1));
               }


            }
            EntityFreezeDomain domain = new EntityFreezeDomain(world,entityPlayer);
            world.spawnEntity(domain);
            AxisAlignedBB bb = entityPlayer.getEntityBoundingBox().grow(4,4,4).offset(entityPlayer.motionX,entityPlayer.motionY,entityPlayer.motionZ);
            List<Entity> l = world.getEntitiesInAABBexcluding(entityPlayer,bb, input -> input instanceof EntityLivingBase && (!(input instanceof EntityPlayer)));
            for (Entity e : l){
                if (e instanceof EntityLivingBase){
                    e.attackEntityFrom(DamageSource.causePlayerDamage(entityPlayer),20f);
                }
            }
            for (int i = 0;i<60;i++){
                double x1,z1;
                x1 = r(world.rand);
                z1 = r(world.rand);
                Main.PACKET_HANDLER.sendToAll(new MessageSpawnParticle(EnumParticleTypes.CLOUD,entityPlayer.posX + (world.rand.nextDouble() * 3 * x1),entityPlayer.posY + world.rand.nextDouble(),entityPlayer.posZ + (world.rand.nextDouble() * 3 * z1)));
            }
        }
    }
    double r(Random rand){
        if (rand.nextBoolean()){
            return 1d;
        }else return -1d;
    }
}

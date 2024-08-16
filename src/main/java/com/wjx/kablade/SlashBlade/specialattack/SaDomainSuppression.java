package com.wjx.kablade.SlashBlade.specialattack;

import com.google.common.base.Predicates;
import com.wjx.kablade.Entity.EntitySummonSwordFree;
import com.wjx.kablade.Entity.EntitySummonedSwordBasePlus;
import com.wjx.kablade.Main;
import com.wjx.kablade.network.MessageSpawnParticle;
import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.effect.EntityLightningBolt;
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

import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class SaDomainSuppression extends SpecialAttackBase {

    @Override
    public String toString() {
        return "domain_suppression";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.getEntityWorld();
        double dist = 10;
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
                if (!world.isRemote){
                    pointedEntity.motionY = 0.5f;
                    float a = 0f;
                    float radius = 2f;
                    float extraDamage = MathFunc.amplifierCalc(-ItemSlashBlade.AttackAmplifier.get(entityPlayer.getHeldItemMainhand().getTagCompound()),20f);
                    float extraDamage2 = MathFunc.amplifierCalc(-ItemSlashBlade.AttackAmplifier.get(entityPlayer.getHeldItemMainhand().getTagCompound()),4f);
                    float extraDamage3 = MathFunc.amplifierCalc(-ItemSlashBlade.AttackAmplifier.get(entityPlayer.getHeldItemMainhand().getTagCompound()),3f);

                    for (int i = 0;i < 6;i++){
                        double px = pointedEntity.posX + (Math.cos(Math.toRadians(a)))*radius;
                        double py = pointedEntity.posY + pointedEntity.getEyeHeight() + 1d;
                        double pz = pointedEntity.posZ + ((Math.sin(Math.toRadians(a)))*radius);
                        double ap1 = Math.toDegrees((pointedEntity.posY - py)/(pointedEntity.posX - px));
                        double ay;
                        double n = Math.toDegrees(Math.atan(Math.abs(pointedEntity.posX - px) / Math.abs(pointedEntity.posZ - pz)));
                        double k = Math.toDegrees(Math.atan(Math.abs(pointedEntity.posZ - pz) / Math.abs(pointedEntity.posX - px)));
                        if (pointedEntity.posY > ap1){
                            ap1 = -ap1;
                        }
                        if (pz < pointedEntity.posZ){
                            if (px > pointedEntity.posX){
                                ay = (float) n;
                            }
                            else {
                                ay = -(float) n;
                            }
                        }
                        else {
                            if (px > pointedEntity.posX){
                                ay = 90f + (float) k;
                            }
                            else {
                                ay = -90f -(float) k;
                            }
                        }
                        EntitySummonedSwordBasePlus p = new EntitySummonedSwordBasePlus(world,entityPlayer,20f + extraDamage,px,py,pz,(float) ap1,(float)ay);
                        p.setColor(65535);
                        world.spawnEntity(p);
                        a += 60;
                    }
                    radius = 6f;
                    for (int i = 0;i < 6;i++){
                        double px = pointedEntity.posX + (Math.cos(Math.toRadians(a)))*radius;
                        double py = pointedEntity.posY + pointedEntity.getEyeHeight() + 1d;
                        double pz = pointedEntity.posZ + ((Math.sin(Math.toRadians(a)))*radius);
                        double ap1 = Math.toDegrees((pointedEntity.posY - py)/(pointedEntity.posX - px));
                        double ay;
                        double n = Math.toDegrees(Math.atan(Math.abs(pointedEntity.posX - px) / Math.abs(pointedEntity.posZ - pz)));
                        double k = Math.toDegrees(Math.atan(Math.abs(pointedEntity.posZ - pz) / Math.abs(pointedEntity.posX - px)));
                        if (pointedEntity.posY > ap1){
                            ap1 = -ap1;
                        }
                        if (pz < pointedEntity.posZ){
                            if (px > pointedEntity.posX){
                                ay = (float) n;
                            }
                            else {
                                ay = -(float) n;
                            }
                        }
                        else {
                            if (px > pointedEntity.posX){
                                ay = 90f + (float) k;
                            }
                            else {
                                ay = -90f -(float) k;
                            }
                        }
                        EntitySummonedSwordBasePlus p = new EntitySummonedSwordBasePlus(world,entityPlayer,4f + extraDamage2,px,py,pz,(float) ap1,(float)ay);
                        p.setColor(65535);
                        world.spawnEntity(p);
                        a += 60;
                    }
                    for (int i = 0;i<10;i++){
                        double px = pointedEntity.posX + (Math.cos(Math.toRadians(a)))*radius;
                        double py = pointedEntity.posY + pointedEntity.getEyeHeight();
                        double pz = pointedEntity.posZ + ((Math.sin(Math.toRadians(a)))*radius);
                        EntitySummonSwordFree p = new EntitySummonSwordFree(world,entityPlayer,3f + extraDamage3,px,py,pz,-90f,0f);
                        p.setColor(65535);
                        world.spawnEntity(p);
                        a+=36;
                    }
                    radius = 1f;
                    for (int i = 0;i < 20;i++){
                        double px = pointedEntity.posX + (Math.cos(Math.toRadians(a)))*radius;
                        double py = pointedEntity.posY + 0.1d;
                        double pz = pointedEntity.posZ + ((Math.sin(Math.toRadians(a)))*radius);
                        Main.PACKET_HANDLER.sendToAll(new MessageSpawnParticle(EnumParticleTypes.CLOUD,px,py,pz,(Math.cos(Math.toRadians(a))) *0.5,0d,((Math.sin(Math.toRadians(a)))*radius) * 0.5));
                        a+=18;
                    }
                    for (int i = 0;i < 20;i++){
                        double px = pointedEntity.posX + (Math.cos(Math.toRadians(a)))*radius;
                        double py = pointedEntity.posY + 0.1d;
                        double pz = pointedEntity.posZ + ((Math.sin(Math.toRadians(a)))*radius);
                        Main.PACKET_HANDLER.sendToAll(new MessageSpawnParticle(EnumParticleTypes.CLOUD,px,py,pz,(Math.cos(Math.toRadians(a))) *0.3,0d,((Math.sin(Math.toRadians(a)))*radius) * 0.3));
                        a+=18;
                    }
                    for (int i = 0;i < 20;i++){
                        double px = pointedEntity.posX + (Math.cos(Math.toRadians(a)))*radius;
                        double py = pointedEntity.posY + 0.1d;
                        double pz = pointedEntity.posZ + ((Math.sin(Math.toRadians(a)))*radius);
                        Main.PACKET_HANDLER.sendToAll(new MessageSpawnParticle(EnumParticleTypes.CLOUD,px,py,pz,(Math.cos(Math.toRadians(a))) *0.2,0d,((Math.sin(Math.toRadians(a)))*radius) * 0.2));
                        a+=18;
                    }
                    for (int i = 0;i < 20;i++){
                        double px = pointedEntity.posX + (Math.cos(Math.toRadians(a)))*radius;
                        double py = pointedEntity.posY + 0.1d;
                        double pz = pointedEntity.posZ + ((Math.sin(Math.toRadians(a)))*radius);
                        Main.PACKET_HANDLER.sendToAll(new MessageSpawnParticle(EnumParticleTypes.CLOUD,px,py,pz,(Math.cos(Math.toRadians(a))) *0.1,0d,((Math.sin(Math.toRadians(a)))*radius) * 0.1));
                        a+=18;
                    }
                    for (int i = 0;i < 20;i++){
                        double px = pointedEntity.posX + (Math.cos(Math.toRadians(a)))*radius;
                        double py = pointedEntity.posY + 2d;
                        double pz = pointedEntity.posZ + ((Math.sin(Math.toRadians(a)))*radius);
                        Main.PACKET_HANDLER.sendToAll(new MessageSpawnParticle(EnumParticleTypes.CLOUD,px,py,pz,(Math.cos(Math.toRadians(a))) *0.5,0d,((Math.sin(Math.toRadians(a)))*radius) * 0.5));
                        a+=18;
                    }
                    for (int i = 0;i < 20;i++){
                        double px = pointedEntity.posX + (Math.cos(Math.toRadians(a)))*radius;
                        double py = pointedEntity.posY + 2d;
                        double pz = pointedEntity.posZ + ((Math.sin(Math.toRadians(a)))*radius);
                        Main.PACKET_HANDLER.sendToAll(new MessageSpawnParticle(EnumParticleTypes.CLOUD,px,py,pz,(Math.cos(Math.toRadians(a))) *0.3,0d,((Math.sin(Math.toRadians(a)))*radius) * 0.3));
                        a+=18;
                    }
                    for (int i = 0;i < 20;i++){
                        double px = pointedEntity.posX + (Math.cos(Math.toRadians(a)))*radius;
                        double py = pointedEntity.posY + 2d;
                        double pz = pointedEntity.posZ + ((Math.sin(Math.toRadians(a)))*radius);
                        Main.PACKET_HANDLER.sendToAll(new MessageSpawnParticle(EnumParticleTypes.CLOUD,px,py,pz,(Math.cos(Math.toRadians(a))) *0.2,0d,((Math.sin(Math.toRadians(a)))*radius) * 0.2));
                        a+=18;
                    }
                    for (int i = 0;i < 20;i++){
                        double px = pointedEntity.posX + (Math.cos(Math.toRadians(a)))*radius;
                        double py = pointedEntity.posY + 2d;
                        double pz = pointedEntity.posZ + ((Math.sin(Math.toRadians(a)))*radius);
                        Main.PACKET_HANDLER.sendToAll(new MessageSpawnParticle(EnumParticleTypes.CLOUD,px,py,pz,(Math.cos(Math.toRadians(a))) *0.1,0d,((Math.sin(Math.toRadians(a)))*radius) * 0.1));
                        a+=18;
                    }
                    for (int i = 0;i<6;i++){
                        for (int l = 0;l<6;l++){
                            Main.PACKET_HANDLER.sendToAll(new MessageSpawnParticle(EnumParticleTypes.EXPLOSION_LARGE,pointedEntity.posX,pointedEntity.posY,pointedEntity.posZ,(Math.cos(Math.toRadians(a))) *0.1,0d,((Math.sin(Math.toRadians(a)))*radius) * 0.1));
                        }
                        world.addWeatherEffect(new EntityLightningBolt(world,pointedEntity.posX,pointedEntity.posY,pointedEntity.posZ,true));
                    }
                }
            }
        }
    }
}

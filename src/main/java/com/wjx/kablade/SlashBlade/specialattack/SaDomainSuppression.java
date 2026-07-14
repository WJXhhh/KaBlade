package com.wjx.kablade.SlashBlade.specialattack;

import com.google.common.base.Predicates;
import com.wjx.kablade.Entity.EntitySummonSwordFree;
import com.wjx.kablade.Entity.EntitySummonedSwordBasePlus;
import com.wjx.kablade.Main;
import com.wjx.kablade.network.MessageSpawnParticleRing;
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
        AxisAlignedBB searchBox = entityPlayer.getEntityBoundingBox()
                .grow(1.0D, 1.0D, 1.0D)
                .union(new AxisAlignedBB(vec3d.x, vec3d.y, vec3d.z, vec3d2.x, vec3d2.y, vec3d2.z).grow(2.0D, 2.0D, 2.0D));
        List<Entity> list = world.getEntitiesInAABBexcluding(entityPlayer, searchBox, Predicates.and(EntitySelectors.NOT_SPECTATING, entity -> entity != null && entity.canBeCollidedWith() && (entity instanceof EntityPlayer || entity instanceof EntityLiving)));
        List<EntityLivingBase> allTargets = new java.util.ArrayList<>();
        for (Entity e : list) {
            if (e instanceof EntityLivingBase) {
                allTargets.add((EntityLivingBase) e);
            }
        }
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
        // 确定技能中心位置
        double centerX, centerY, centerZ;
        boolean hasTarget = false;
        if (pointedEntity instanceof EntityLivingBase) {
            centerX = pointedEntity.posX;
            centerY = pointedEntity.posY;
            centerZ = pointedEntity.posZ;
            hasTarget = true;
        } else {
            // 通过射线追踪找玩家看向的方块
            RayTraceResult ray = world.rayTraceBlocks(entityPlayer.getPositionEyes(1.0F), entityPlayer.getLook(1.0F).scale(10).add(entityPlayer.getPositionEyes(1.0F)));
            if (ray != null && ray.hitVec != null) {
                centerX = ray.hitVec.x;
                centerY = ray.hitVec.y;
                centerZ = ray.hitVec.z;
            } else {
                // fallback: 玩家面向方向前方10格
                Vec3d look = entityPlayer.getLookVec();
                centerX = entityPlayer.posX + look.x * 10;
                centerY = entityPlayer.posY + entityPlayer.getEyeHeight();
                centerZ = entityPlayer.posZ + look.z * 10;
            }
        }

        if (!world.isRemote){
            float extraDamage = MathFunc.amplifierCalc(ItemSlashBlade.BaseAttackModifier.get(entityPlayer.getHeldItemMainhand().getTagCompound()),3f);
            float extraDamage2 = MathFunc.amplifierCalc(ItemSlashBlade.BaseAttackModifier.get(entityPlayer.getHeldItemMainhand().getTagCompound()),2f);
            float extraDamage3 = MathFunc.amplifierCalc(ItemSlashBlade.BaseAttackModifier.get(entityPlayer.getHeldItemMainhand().getTagCompound()),2f);

            if (allTargets.isEmpty()) {
                // 无目标时沿用 fallback 位置放一次剑阵（视觉反馈）
                float a = 0f;
                float radius = 2f;
                for (int i = 0;i < 6;i++){
                    double px = centerX + (Math.cos(Math.toRadians(a)))*radius;
                    double py = centerY + 1.0d + 1d;
                    double pz = centerZ + ((Math.sin(Math.toRadians(a)))*radius);
                    double ap1 = Math.toDegrees((centerY - py)/(centerX - px));
                    double ay;
                    double n = Math.toDegrees(Math.atan(Math.abs(centerX - px) / Math.abs(centerZ - pz)));
                    double k = Math.toDegrees(Math.atan(Math.abs(centerZ - pz) / Math.abs(centerX - px)));
                    if (centerY > ap1){
                        ap1 = -ap1;
                    }
                    if (pz < centerZ){
                        if (px > centerX){
                            ay = (float) n;
                        }
                        else {
                            ay = -(float) n;
                        }
                    }
                    else {
                        if (px > centerX){
                            ay = 90f + (float) k;
                        }
                        else {
                            ay = -90f -(float) k;
                        }
                    }
                    EntitySummonedSwordBasePlus p = new EntitySummonedSwordBasePlus(world,entityPlayer,10f + extraDamage,px,py,pz,(float) ap1,(float)ay);
                    p.setColor(65535);
                    world.spawnEntity(p);
                    a += 60;
                }
                radius = 6f;
                for (int i = 0;i < 6;i++){
                    double px = centerX + (Math.cos(Math.toRadians(a)))*radius;
                    double py = centerY + 1.0d + 1d;
                    double pz = centerZ + ((Math.sin(Math.toRadians(a)))*radius);
                    double ap1 = Math.toDegrees((centerY - py)/(centerX - px));
                    double ay;
                    double n = Math.toDegrees(Math.atan(Math.abs(centerX - px) / Math.abs(centerZ - pz)));
                    double k = Math.toDegrees(Math.atan(Math.abs(centerZ - pz) / Math.abs(centerX - px)));
                    if (centerY > ap1){
                        ap1 = -ap1;
                    }
                    if (pz < centerZ){
                        if (px > centerX){
                            ay = (float) n;
                        }
                        else {
                            ay = -(float) n;
                        }
                    }
                    else {
                        if (px > centerX){
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
                    double px = centerX + (Math.cos(Math.toRadians(a)))*radius;
                    double py = centerY + 1.0d;
                    double pz = centerZ + ((Math.sin(Math.toRadians(a)))*radius);
                    EntitySummonSwordFree p = new EntitySummonSwordFree(world,entityPlayer,3f + extraDamage3,px,py,pz,-90f,0f);
                    p.setColor(65535);
                    world.spawnEntity(p);
                    a+=36;
                }
                MessageSpawnParticleRing ring = new MessageSpawnParticleRing(centerX, centerY, centerZ, a);
                ring.addGroup(EnumParticleTypes.CLOUD.getParticleID(), 1.0, 1.0, 0.1, 0.5, 20, 18);
                ring.addGroup(EnumParticleTypes.CLOUD.getParticleID(), 1.0, 1.0, 0.1, 0.3, 20, 18);
                ring.addGroup(EnumParticleTypes.CLOUD.getParticleID(), 1.0, 1.0, 0.1, 0.2, 20, 18);
                ring.addGroup(EnumParticleTypes.CLOUD.getParticleID(), 1.0, 1.0, 0.1, 0.1, 20, 18);
                ring.addGroup(EnumParticleTypes.CLOUD.getParticleID(), 1.0, 1.0, 2.0, 0.5, 20, 18);
                ring.addGroup(EnumParticleTypes.CLOUD.getParticleID(), 1.0, 1.0, 2.0, 0.3, 20, 18);
                ring.addGroup(EnumParticleTypes.CLOUD.getParticleID(), 1.0, 1.0, 2.0, 0.2, 20, 18);
                ring.addGroup(EnumParticleTypes.CLOUD.getParticleID(), 1.0, 1.0, 2.0, 0.1, 20, 18);
                ring.addGroup(EnumParticleTypes.EXPLOSION_LARGE.getParticleID(), 0.0, 1.0, 0.0, 0.1, 36, 0);
                Main.PACKET_HANDLER.sendToAll(ring);
                for (int i = 0;i<6;i++){
                    world.addWeatherEffect(new EntityLightningBolt(world,centerX,centerY,centerZ,true));
                }
            } else {
                // 每目标各自独立召唤剑阵
                for (EntityLivingBase target : allTargets) {
                    target.motionY = 0.5f;
                    double cx = target.posX;
                    double cy = target.posY;
                    double cz = target.posZ;
                    float a = 0f;
                    float radius = 2f;
                    for (int i = 0;i < 6;i++){
                        double px = cx + (Math.cos(Math.toRadians(a)))*radius;
                        double py = cy + 1.0d + 1d;
                        double pz = cz + ((Math.sin(Math.toRadians(a)))*radius);
                        double ap1 = Math.toDegrees((cy - py)/(cx - px));
                        double ay;
                        double n = Math.toDegrees(Math.atan(Math.abs(cx - px) / Math.abs(cz - pz)));
                        double k = Math.toDegrees(Math.atan(Math.abs(cz - pz) / Math.abs(cx - px)));
                        if (cy > ap1){
                            ap1 = -ap1;
                        }
                        if (pz < cz){
                            if (px > cx){
                                ay = (float) n;
                            }
                            else {
                                ay = -(float) n;
                            }
                        }
                        else {
                            if (px > cx){
                                ay = 90f + (float) k;
                            }
                            else {
                                ay = -90f -(float) k;
                            }
                        }
                        EntitySummonedSwordBasePlus p = new EntitySummonedSwordBasePlus(world,entityPlayer,10f + extraDamage,px,py,pz,(float) ap1,(float)ay);
                        p.setTargetEntityId(target.getEntityId());
                        p.setColor(65535);
                        world.spawnEntity(p);
                        a += 60;
                    }
                    radius = 6f;
                    for (int i = 0;i < 6;i++){
                        double px = cx + (Math.cos(Math.toRadians(a)))*radius;
                        double py = cy + 1.0d + 1d;
                        double pz = cz + ((Math.sin(Math.toRadians(a)))*radius);
                        double ap1 = Math.toDegrees((cy - py)/(cx - px));
                        double ay;
                        double n = Math.toDegrees(Math.atan(Math.abs(cx - px) / Math.abs(cz - pz)));
                        double k = Math.toDegrees(Math.atan(Math.abs(cz - pz) / Math.abs(cx - px)));
                        if (cy > ap1){
                            ap1 = -ap1;
                        }
                        if (pz < cz){
                            if (px > cx){
                                ay = (float) n;
                            }
                            else {
                                ay = -(float) n;
                            }
                        }
                        else {
                            if (px > cx){
                                ay = 90f + (float) k;
                            }
                            else {
                                ay = -90f -(float) k;
                            }
                        }
                        EntitySummonedSwordBasePlus p = new EntitySummonedSwordBasePlus(world,entityPlayer,4f + extraDamage2,px,py,pz,(float) ap1,(float)ay);
                        p.setTargetEntityId(target.getEntityId());
                        p.setColor(65535);
                        world.spawnEntity(p);
                        a += 60;
                    }
                    for (int i = 0;i<10;i++){
                        double px = cx + (Math.cos(Math.toRadians(a)))*radius;
                        double py = cy + 1.0d;
                        double pz = cz + ((Math.sin(Math.toRadians(a)))*radius);
                        EntitySummonSwordFree p = new EntitySummonSwordFree(world,entityPlayer,3f + extraDamage3,px,py,pz,-90f,0f);
                        p.setTargetEntityId(target.getEntityId());
                        p.setColor(65535);
                        world.spawnEntity(p);
                        a+=36;
                    }
                    // 当前目标的粒子与闪电
                    MessageSpawnParticleRing ring = new MessageSpawnParticleRing(cx, cy, cz, 0);
                    ring.addGroup(EnumParticleTypes.CLOUD.getParticleID(), 1.0, 1.0, 0.1, 0.5, 20, 18);
                    ring.addGroup(EnumParticleTypes.CLOUD.getParticleID(), 1.0, 1.0, 0.1, 0.3, 20, 18);
                    ring.addGroup(EnumParticleTypes.CLOUD.getParticleID(), 1.0, 1.0, 0.1, 0.2, 20, 18);
                    ring.addGroup(EnumParticleTypes.CLOUD.getParticleID(), 1.0, 1.0, 0.1, 0.1, 20, 18);
                    ring.addGroup(EnumParticleTypes.CLOUD.getParticleID(), 1.0, 1.0, 2.0, 0.5, 20, 18);
                    ring.addGroup(EnumParticleTypes.CLOUD.getParticleID(), 1.0, 1.0, 2.0, 0.3, 20, 18);
                    ring.addGroup(EnumParticleTypes.CLOUD.getParticleID(), 1.0, 1.0, 2.0, 0.2, 20, 18);
                    ring.addGroup(EnumParticleTypes.CLOUD.getParticleID(), 1.0, 1.0, 2.0, 0.1, 20, 18);
                    ring.addGroup(EnumParticleTypes.EXPLOSION_LARGE.getParticleID(), 0.0, 1.0, 0.0, 0.1, 36, 0);
                    Main.PACKET_HANDLER.sendToAll(ring);
                    for (int i = 0;i<6;i++){
                        world.addWeatherEffect(new EntityLightningBolt(world,cx,cy,cz,true));
                    }
                }
            }
        }
    }
}

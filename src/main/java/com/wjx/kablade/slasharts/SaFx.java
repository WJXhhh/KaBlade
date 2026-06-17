package com.wjx.kablade.slasharts;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.entity.EntityJudgementCut;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * SA 特效共享工具：颜色转换、跨 tick 编排、以及召唤飞斩 / 裂空审判斩的便捷方法。
 * 给「弧光破晓」「极光映天」等大招做分阶段演出用。
 */
final class SaFx {

    private SaFx() {
    }

    /** RGB int → joml 颜色向量（0..1），用于 Dust 粒子。 */
    static Vector3f rgb(int color) {
        return new Vector3f(
                ((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F);
    }

    /** 延迟 {@code delay} tick 在主线程执行 {@code task}；delay&le;0 立即执行。用于把大招拆成连续的演出阶段。 */
    static void schedule(ServerLevel level, int delay, Runnable task) {
        MinecraftServer server = level.getServer();
        if (delay <= 0 || server == null) {
            task.run();
            return;
        }
        server.tell(new TickTask(server.getTickCount() + delay, task));
    }

    /** 玩家视线的水平归一化方向（忽略俯仰）；视线接近垂直时退化为正北 +Z。 */
    static Vec3 flatLook(LivingEntity user) {
        Vec3 l = user.getLookAngle();
        Vec3 flat = new Vec3(l.x, 0.0, l.z);
        return flat.lengthSqr() < 1.0e-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    /** 由 Minecraft yaw（度）得到对应的水平朝向向量（与 {@code getLookAngle} 的水平分量一致），再乘上速度。 */
    static Vec3 yawDir(float yawDeg, double speed) {
        double r = Math.toRadians(yawDeg);
        return new Vec3(-Math.sin(r), 0.0, Math.cos(r)).scale(speed);
    }

    /** 玩家<b>前方</b>（水平半圆内）且 {@code range} 格内的敌对生物；贴身 2 格内一律算命中。用于「向前攻击」的范围结算。 */
    static List<LivingEntity> forwardHostiles(ServerLevel level, LivingEntity user, double range) {
        Vec3 flat = flatLook(user);
        double ox = user.getX();
        double oz = user.getZ();
        AABB box = user.getBoundingBox().inflate(range);
        return level.getEntitiesOfClass(LivingEntity.class, box, e -> {
            if (e == user || !e.isAlive() || e.isAlliedTo(user)) {
                return false;
            }
            double dx = e.getX() - ox;
            double dz = e.getZ() - oz;
            double hl = Math.sqrt(dx * dx + dz * dz);
            if (hl > range) {
                return false;
            }
            if (hl < 2.0) {
                return true;   // 贴身
            }
            return (dx * flat.x + dz * flat.z) / hl >= 0.0;   // 落在前方半圆
        });
    }

    /** 射出一道飞斩（EntityDrive）。 */
    static EntityDrive drive(ServerLevel level, LivingEntity user, Vec3 pos, Vec3 dir,
                             float speed, double damage, int color, float size, float lifetime) {
        EntityDrive d = new EntityDrive(SlashBlade.RegistryEvents.Drive, level);
        d.setPos(pos.x, pos.y, pos.z);
        d.setShooter(user);
        d.setDamage(damage);
        d.setColor(color);
        d.setBaseSize(size);
        d.setLifetime(lifetime);
        d.shoot(dir.x, dir.y, dir.z, speed, 0.6F);
        level.addFreshEntity(d);
        return d;
    }

    /** 射出一道裂空审判斩（EntityJudgementCut），开启循环命中——大招收尾的标志性大斩。 */
    static EntityJudgementCut judgementCut(ServerLevel level, LivingEntity user, Vec3 pos, Vec3 dir,
                                           int color, double damage, float rank, int lifetime, float speed) {
        EntityJudgementCut cut = new EntityJudgementCut(SlashBlade.RegistryEvents.JudgementCut, level);
        cut.setPos(pos.x, pos.y, pos.z);
        cut.setShooter(user);
        cut.setColor(color);
        cut.setDamage(damage);
        cut.setRank(rank);
        cut.setLifetime(lifetime);
        cut.setCycleHit(true);
        cut.shoot(dir.x, dir.y, dir.z, speed, 0.0F);
        level.addFreshEntity(cut);
        return cut;
    }
}

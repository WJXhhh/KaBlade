package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.util.SaTargeting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * 追星剑 —— 奉神刀「鹿」专属 SA（{@code AL_Zhuixing}「宝库」）的金色幻影剑。
 * <p>
 * 继承 {@link PhantomSwordExEntity} 的目标同步与渲染，但改为：
 * <ul>
 *   <li><b>持续归航</b>：每 tick 重新对准锁定目标，可靠追上被挑飞的目标（不再像父类那样
 *       延迟期结束后只走直线、容易被升空的目标甩开）；</li>
 *   <li><b>只打锁定目标</b>：飞行途中不会误扎别的实体；</li>
 *   <li><b>命中即爆散</b>：一击 + 金色爆闪后消失，32 把错峰连珠如流星追星。</li>
 * </ul>
 */
public class StarSwordEntity extends PhantomSwordExEntity {

    /** 归航飞行速度（格/tick）：看得清轨迹、又足够追上目标。 */
    private static final float FLIGHT_SPEED = 1.45F;
    /** 每 tick 最大转向角（度）：高转向率确保紧咬目标。 */
    private static final float TURN_RATE = 28.0F;
    /** 命中判定膨胀半径。 */
    private static final double HIT_REACH = 1.0;

    public StarSwordEntity(EntityType<? extends StarSwordEntity> type, Level level) {
        super(type, level);
    }

    /**
     * 工厂：从 {@code pos} 生成一把归航到 {@code target} 的金色追星剑。
     *
     * @param interval 蓄势悬停的延迟（tick），用于错峰连珠
     */
    public static StarSwordEntity spawn(Level level, LivingEntity thrower, LivingEntity target,
                                        Vec3 pos, float damage, int color, int interval) {
        StarSwordEntity e = new StarSwordEntity(ModEntities.STAR_SWORD.get(), level);
        Vec3 dir = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(pos);
        if (dir.lengthSqr() < 1.0E-8) {
            dir = new Vec3(0.0, -1.0, 0.0);
        }
        dir = dir.normalize();
        // 与 PhantomSwordExEntity#setDriveVectorFromIni 的 -sin 约定一致：X/Y 取反
        float yaw = (float) (Mth.atan2(-dir.x, dir.z) * Mth.RAD_TO_DEG);
        float horizontal = Mth.sqrt((float) (dir.x * dir.x + dir.z * dir.z));
        float pitch = (float) (Mth.atan2(-dir.y, horizontal) * Mth.RAD_TO_DEG);

        e.setPos(pos.x, pos.y, pos.z);
        e.setYRot(yaw);
        e.setXRot(pitch);
        e.setThrower(thrower);
        e.attackDamage = damage;
        e.setColor(color);
        e.setLifetime(60);
        e.setInterval(interval);
        e.setIniYaw(yaw);
        e.setIniPitch(pitch);
        e.setRoll(90.0F);
        e.initializeTrajectory(yaw, pitch, Vec3.ZERO);
        e.blade = thrower.getMainHandItem().copy();
        e.alreadyHit.add(thrower.getUUID());
        e.setTarget(target);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void tickFlying() {
        // 持续归航：无论悬停/俯冲都每 tick 重新锁向目标
        homeTowardTarget();

        // 悬停蓄势期不位移；延迟过后才俯冲
        if (!(getInterval() > 0 && tickCount <= getInterval())) {
            Vec3 motion = getDeltaMovement();
            setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        }

        // 撞墙即消
        BlockPos bp = blockPosition();
        if (!level().getBlockState(bp).getCollisionShape(level(), bp).isEmpty()) {
            discard();
            return;
        }

        // 命中（服务端）
        if (!level().isClientSide()) {
            findTarget().ifPresent(this::onHitEntity);
        }

        // 金色拖尾（客户端）
        if (level().isClientSide()) {
            Vec3 m = getDeltaMovement();
            if (m.lengthSqr() > 0.01) {
                Vec3 tail = m.normalize().scale(-0.4);
                level().addParticle(ParticleTypes.END_ROD,
                        getX() + tail.x, getY() + tail.y, getZ() + tail.z, 0.0, 0.0, 0.0);
            }
        }
    }

    /** 每 tick 重新对准锁定目标并设好归航速度。 */
    private void homeTowardTarget() {
        int id = getTargetEntityId();
        if (id == 0) {
            return;
        }
        Entity t = level().getEntity(id);
        if (!(t instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        faceEntity(this, t, TURN_RATE, TURN_RATE);
        setDriveVectorFromIni(FLIGHT_SPEED);
    }

    /** 只命中锁定目标，飞行途中不误扎别的实体。 */
    @Override
    protected Optional<LivingEntity> findTarget() {
        int id = getTargetEntityId();
        if (id == 0) {
            return Optional.empty();
        }
        Entity t = level().getEntity(id);
        if (t instanceof LivingEntity living && SaTargeting.canDamage(thrower, living)
                && !alreadyHit.contains(living.getUUID())
                && getBoundingBox().inflate(HIT_REACH).intersects(living.getBoundingBox())) {
            return Optional.of(living);
        }
        return Optional.empty();
    }

    /** 命中即一击爆散（不骑乘），32 把连珠各自留下一记金色爆闪。 */
    @Override
    protected void onHitEntity(LivingEntity target) {
        alreadyHit.add(target.getUUID());
        target.invulnerableTime = 0;
        com.wjx.kablade.util.SaDamage.hurtNoIFrame(target, damageSource(), attackDamage);
        target.hurtMarked = true;
        hitBlade(target);

        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(),
                    10, 0.25, 0.25, 0.25, 0.3);
            server.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(),
                    6, 0.2, 0.2, 0.2, 0.05);
            server.playSound(null, getX(), getY(), getZ(),
                    SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.8F, 1.5F);
        }
        discard();
    }
}

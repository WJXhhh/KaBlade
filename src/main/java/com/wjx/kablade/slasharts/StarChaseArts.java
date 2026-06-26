package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.StarSwordEntity;
import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/**
 * 追星剑 —— 奉神刀「鹿」专属 SA（复刻 1.12.2 {@code AL_Zhuixing}「宝库」）。
 * 锁定面前一名敌人，先一击将其挑飞，随即在其四周天穹召唤 32 把金色幻影剑
 * （{@link StarSwordEntity}）——每把<b>持续归航</b>到这名目标、错峰悬停后逐一俯冲扎入，
 * 如流星追星。
 * <p>
 * 幻影剑改用专属归航实体而非 EntityDrive：它会持续追上被挑飞的目标、只打锁定目标、
 * 看得清整条轨迹、命中即金色爆散。悬停延迟(interval)由各剑自带，形成连珠节奏。
 */
public final class StarChaseArts extends SlashArts {

    private static final int SWORDS = 32;
    private static final double LOCK_RANGE = 30.0;
    private static final float DAMAGE_RATIO = 4.0F;
    private static final int GOLD = 0xFFFF00;
    /** 第一把剑的悬停延迟（tick）：让目标先升空。 */
    private static final int FIRST_DELAY = 5;
    /** 每把剑之间的延迟步进（tick），形成错峰连珠。 */
    private static final int DELAY_STEP = 1;

    public StarChaseArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }
        final ServerLevel level = (ServerLevel) user.level();
        final RandomSource rng = level.random;
        final float bladeAttack = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier).orElse(4.0F);
        // 1.12.2: magicDamage = base/3 + amplifierCalc(base, 4)
        final float damage = bladeAttack / 3.0F + MathFunc.amplifierCalc(bladeAttack, DAMAGE_RATIO);

        final LivingEntity target = resolveTarget(level, user);
        if (target == null) {
            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8F, 1.6F);
            return super.doArts(type, user);
        }

        // ① 把目标挑飞——纯起手，不造成伤害（否则这一记 base/3+amplifierCalc(base,4) ≈ 20+ 会直接秒杀，
        //    怪在弹飞瞬间就死、根本等不到追星剑）。伤害全交给随后的 32 把幻影剑。
        target.setDeltaMovement(0.0, 1.5, 0.0);
        target.hasImpulse = true;
        target.hurtMarked = true;
        target.hurtTime = 0;
        target.invulnerableTime = 0;
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));

        Vec3 tc = target.position().add(0.0, target.getBbHeight() * 0.6, 0.0);
        for (int i = 0; i < 20; i++) {
            level.sendParticles(ParticleTypes.CRIT, tc.x, tc.y, tc.z, 1,
                    (rng.nextDouble() - 0.5), (rng.nextDouble() - 0.5), (rng.nextDouble() - 0.5), 0.5);
        }
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2F, 1.3F);

        // ② 32 把金色幻影剑：在目标四周天穹生成，错峰悬停后持续归航俯冲
        for (int i = 0; i < SWORDS; i++) {
            double ang = rng.nextDouble() * Math.PI * 2.0;
            double r = 7.0 + rng.nextDouble() * 4.0;           // 7..11 格外
            Vec3 spawn = new Vec3(tc.x + Math.cos(ang) * r,
                    tc.y + 6.0 + rng.nextDouble() * 4.0,         // 上方 6..10 格
                    tc.z + Math.sin(ang) * r);
            int interval = FIRST_DELAY + i * DELAY_STEP;
            StarSwordEntity.spawn(level, user, target, spawn, damage, GOLD, interval);
        }

        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4F, 1.4F);
        return super.doArts(type, user);
    }

    /** 锁定目标(TargetEntityId)优先；否则取准星前方最近的可攻击生物（复刻 1.12.2 getEntityToWatch）。 */
    private static LivingEntity resolveTarget(ServerLevel level, LivingEntity user) {
        int id = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getTargetEntityId).orElse(0);
        if (id != 0) {
            Entity e = level.getEntity(id);
            if (e instanceof LivingEntity le && le.isAlive() && le != user
                    && le.distanceTo(user) < LOCK_RANGE) {
                return le;
            }
        }
        Vec3 look = user.getLookAngle();
        AABB box = user.getBoundingBox().inflate(8.0)
                .move(look.x * 3.0, user.getEyeHeight() + look.y * 3.0, look.z * 3.0);
        LivingEntity best = null;
        double bestDist = LOCK_RANGE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != user && e.isAlive() && !e.isAlliedTo(user))) {
            double d = e.distanceTo(user);
            if (d < bestDist) {
                best = e;
                bestDist = d;
            }
        }
        return best;
    }
}

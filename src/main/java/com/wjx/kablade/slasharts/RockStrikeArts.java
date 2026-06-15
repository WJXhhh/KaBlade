package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.RockSpikeEntity;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * 岩石撼击 — 千岩之锋专属 SA。
 * <p>
 * 以刀顿地，一道环形冲击波由近及远向外扩散，沿途从地里顶出一圈圈<b>真正的 3D 石矛</b>
 * （{@link RockSpikeEntity}，模型/动画在客户端手搓，会出土、停留、再缩回）。被波及的敌人会先
 * 吃满伤害、再被整个掀飞到空中——先伤害后击飞，杜绝「人先飞走、攻击却追不上」。
 * <p>
 * 实现要点：
 * <ul>
 *   <li><b>伤害</b>用 AABB 直接扫描，与表现实体解耦：范围可控、一定先于击飞结算；每个敌人只被
 *       最先到达它的那一圈结算一次（{@code damaged} 去重）。</li>
 *   <li><b>表现</b>是成片的岩刺实体 + 刺根扬起的少量碎岩，不再是满地乱飞的方块碎屑和白烟。</li>
 *   <li><b>节奏</b>用 {@link TickTask} 把每一圈错开几 tick 触发，形成向外荡开的涟漪。</li>
 * </ul>
 * <b>强度：</b>
 * <ul>
 *   <li>{@code Success}（普通蓄力）— 2 圈</li>
 *   <li>{@code Jackpot}/{@code Super}（Just 时机）— 3 圈，伤害 ×1.3、掀飞更高</li>
 *   <li>精炼度每 10 级 +1 圈（上限 +2），精炼 20+ 追加爆炸轰鸣</li>
 * </ul>
 * 伤害 = 刀当前攻击力 × 环系数 × 时机倍率；config 的 attack_multiplier 已烤入刀 NBT，
 * 经 {@link ISlashBladeState#getBaseAttackModifier()} 读取后自动生效。
 */
public final class RockStrikeArts extends SlashArts {

    /** 最内圈半径（格）。 */
    private static final double INNER_RADIUS = 2.0;
    /** 相邻两圈的半径差（格）。 */
    private static final double RING_STEP = 2.2;
    /** 判定带宽：敌人离冲击波边缘这么近就算被扫到，略大于步长以免漏判。 */
    private static final double HIT_BAND = 1.6;
    /** 相邻两圈触发的间隔（tick），制造向外扩散的涟漪。 */
    private static final int RING_DELAY = 2;

    /** 各圈伤害系数（越外圈余波越弱）。数组覆盖最大圈数（2 基础 + 2 精炼 + 1 余量）。 */
    private static final float[] RING_DAMAGE_FACTOR = {1.1F, 0.95F, 0.8F, 0.65F, 0.5F};

    /** 刺根扬尘用的碎岩贴图。 */
    private static final BlockState DUST_ROCK = Blocks.STONE.defaultBlockState();

    public RockStrikeArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    private static float getBladeAttack(LivingEntity user) {
        ItemStack stack = user.getMainHandItem();
        return stack.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
    }

    private static int getRefine(LivingEntity user) {
        ItemStack stack = user.getMainHandItem();
        return stack.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getRefine)
                .orElse(0);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide()) {
            return super.doArts(type, user);
        }

        final ServerLevel level = (ServerLevel) user.level();
        final MinecraftServer server = level.getServer();
        final float bladeAttack = getBladeAttack(user);
        final int refine = getRefine(user);

        final boolean empowered = type == ArtsType.Jackpot || type == ArtsType.Super;
        final float tierMultiplier = empowered ? 1.3F : 1.0F;
        final double launchUp = empowered ? 1.15 : 0.85;   // 掀飞高度（垂直初速）
        final boolean hasExplosion = refine >= 20;

        final int refineBonus = Math.min(refine / 10, 2);
        final int rings = (empowered ? 3 : 2) + refineBonus;

        // 顿地点固定为施放瞬间脚下位置，之后玩家移动也不影响波纹原点。
        final Vec3 origin = user.position();
        final DamageSource source = user instanceof Player player
                ? level.damageSources().playerAttack(player)
                : level.damageSources().mobAttack(user);

        // 同一敌人只被最先到达它的那一圈结算一次，跨 tick 共享。
        final Set<Entity> damaged = new HashSet<>();

        // ── 顿地瞬间：中心一根大石矛 + 碎岩爆裂 ──
        impactBurst(level, origin);
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS,
                1.6F, 0.5F + level.getRandom().nextFloat() * 0.15F);
        // 一层低沉的轰鸣垫底，让顿地更有分量。
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.9F, 0.45F);
        if (hasExplosion) {
            level.playSound(null, origin.x, origin.y, origin.z,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.6F, 0.6F);
        }

        // ── 一圈圈向外荡开 ──
        for (int ring = 0; ring < rings; ring++) {
            final int r = ring;
            final double radius = INNER_RADIUS + ring * RING_STEP;
            final float factor = RING_DAMAGE_FACTOR[Math.min(ring, RING_DAMAGE_FACTOR.length - 1)];
            final double damage = bladeAttack * factor * tierMultiplier;

            final Runnable wave = () -> {
                if (!user.isAlive()) {
                    return;
                }
                spawnWave(level, user, origin, radius, r, damage, source, launchUp, damaged);
            };

            int delay = ring * RING_DELAY;
            if (delay == 0) {
                wave.run();
            } else {
                server.tell(new TickTask(server.getTickCount() + delay, wave));
            }
        }

        return super.doArts(type, user);
    }

    /** 触发一整圈：先沿圆周顶出一圈石矛，再把波及范围内的敌人掀飞。 */
    private static void spawnWave(ServerLevel level, LivingEntity user, Vec3 origin,
                                  double radius, int ring, double damage,
                                  DamageSource source, double launchUp, Set<Entity> damaged) {

        RandomSource rng = level.getRandom();

        // —— 表现：沿圆周顶出一圈岩刺 ——
        int spikes = 4 + ring * 2;
        double groundY = origin.y;
        for (int i = 0; i < spikes; i++) {
            double angle = 2.0 * Math.PI * i / spikes + ring * 0.4;
            double x = origin.x + Math.cos(angle) * radius;
            double z = origin.z + Math.sin(angle) * radius;
            float scale = 0.85F + rng.nextFloat() * 0.3F;
            spawnSpike(level, x, groundY, z, scale, rng);
        }

        // 把这一圈连成一道贴地的冲击波尘环。
        shockwaveRing(level, origin, radius);

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.STONE_BREAK, SoundSource.PLAYERS,
                1.4F, 0.55F + ring * 0.1F);

        // —— 伤害 + 击飞：扫描以 origin 为心、半径 (radius+HIT_BAND) 的圆盘 ——
        double reach = radius + HIT_BAND;
        AABB box = new AABB(
                origin.x - reach, origin.y - 1.0, origin.z - reach,
                origin.x + reach, origin.y + 3.0, origin.z + reach);
        double reachSq = reach * reach;

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != user && !damaged.contains(e))) {
            double dx = target.getX() - origin.x;
            double dz = target.getZ() - origin.z;
            if (dx * dx + dz * dz > reachSq) {
                continue;   // 在 AABB 角落但超出圆盘
            }
            damaged.add(target);
            target.hurt(source, (float) damage);
            // 先结算伤害，再覆写速度把人整个掀起来（清掉香草命中的横向击退，主要向上）。
            launchUp(target, origin, launchUp);
        }
    }

    /** 在一点顶出一根岩刺实体，并扬起少量碎岩。 */
    private static void spawnSpike(ServerLevel level, double x, double y, double z,
                                   float scale, RandomSource rng) {
        float yaw = rng.nextFloat() * 360.0F;
        int lifetime = 16 + rng.nextInt(5);
        RockSpikeEntity.spawn(level, x, y, z, yaw, scale, lifetime, rng.nextInt());
        spikeFx(level, x, y, z);
    }

    /** 刺根特效：崩起的碎岩 + 喷溅的火星余烬 + 缓缓上升的尘烟。 */
    private static void spikeFx(ServerLevel level, double x, double y, double z) {
        BlockParticleOption rock = new BlockParticleOption(ParticleTypes.BLOCK, DUST_ROCK);
        // 紧凑崩起的碎岩（不四散乱飞）
        level.sendParticles(rock, x, y + 0.1, z, 4, 0.12, 0.05, 0.12, 0.05);
        level.sendParticles(rock, x, y + 0.1, z, 0, 0.0, 1.0, 0.0, 0.4);
        level.sendParticles(rock, x, y + 0.1, z, 0, 0.15, 0.9, 0.1, 0.35);
        // 熔岩火星：随岩矛喷出、上抛后回落
        level.sendParticles(ParticleTypes.LAVA, x, y + 0.25, z, 2, 0.08, 0.1, 0.08, 0.0);
        // 升腾的尘烟
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.15, z, 1, 0.08, 0.0, 0.08, 0.015);
    }

    /** 沿半径画一道贴地的尘环，把整圈岩刺连成一道冲击波。 */
    private static void shockwaveRing(ServerLevel level, Vec3 origin, double radius) {
        BlockParticleOption rock = new BlockParticleOption(ParticleTypes.BLOCK, DUST_ROCK);
        int points = Math.min(40, Math.max(16, (int) (radius * 6.0)));
        for (int i = 0; i < points; i++) {
            double a = 2.0 * Math.PI * i / points;
            double x = origin.x + Math.cos(a) * radius;
            double z = origin.z + Math.sin(a) * radius;
            level.sendParticles(rock, x, origin.y + 0.05, z, 1, 0.04, 0.01, 0.04, 0.015);
        }
    }

    /** 将目标掀飞：垂直为主、带一点向外的水平分量。 */
    private static void launchUp(LivingEntity target, Vec3 origin, double up) {
        double dx = target.getX() - origin.x;
        double dz = target.getZ() - origin.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        double outward = 0.2;
        double ox = len < 1.0e-4 ? 0.0 : dx / len * outward;
        double oz = len < 1.0e-4 ? 0.0 : dz / len * outward;
        target.setDeltaMovement(ox, up, oz);
        target.hurtMarked = true;   // 强制把新速度同步到客户端
    }

    /** 中心顿地特效：一根更大的石矛 + 碎岩爆裂 + 火星喷泉 + 升腾尘柱（无白色爆炸球）。 */
    private static void impactBurst(ServerLevel level, Vec3 origin) {
        RandomSource rng = level.getRandom();
        RockSpikeEntity.spawn(level, origin.x, origin.y, origin.z,
                rng.nextFloat() * 360.0F, 1.7F, 22, rng.nextInt());

        BlockParticleOption rock = new BlockParticleOption(ParticleTypes.BLOCK, DUST_ROCK);
        // 向上崩开的大量碎岩
        level.sendParticles(rock, origin.x, origin.y + 0.4, origin.z, 30, 0.3, 0.3, 0.3, 0.35);
        // 火星喷泉
        level.sendParticles(ParticleTypes.LAVA, origin.x, origin.y + 0.3, origin.z, 12, 0.3, 0.25, 0.3, 0.0);
        // 升腾的尘柱
        level.sendParticles(ParticleTypes.LARGE_SMOKE, origin.x, origin.y + 0.2, origin.z, 8, 0.25, 0.1, 0.25, 0.03);
    }
}

package com.wjx.kablade.slasharts;

import com.wjx.kablade.Main;
import com.wjx.kablade.entity.FlareEdgeEntity;
import mods.flammpfeil.slashblade.capability.concentrationrank.CapabilityConcentrationRank;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 龙一文字线 SA「熔岩驱动」—— 1.12.2 {@code LaveDriveEx} 完整移植。
 *
 * <p>流程：
 * <ol>
 *   <li>爆炸音效 + 100 个熔岩粒子
 *   <li>AOE 斩击（周围 5 格）
 *   <li>飞刃按固定时间间隔逐个生成，从面前矩形区域依次弹出
 * </ol>
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LaveDriveArts extends SlashArts {

    private static final int DRIVE_COLOR = 0x600030; // 6291504

    /** 挂起的飞刃生成队列 */
    private static final List<FlareSpawnEntry> PENDING = new ArrayList<>();

    public LaveDriveArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        ItemStack blade = user.getMainHandItem();
        if (!(blade.getItem() instanceof ItemSlashBlade)) {
            return super.doArts(type, user);
        }

        // 爆炸音
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F,
                (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);

        // 100 个熔岩粒子
        for (int i = 0; i < 100; i++) {
            double d0 = user.getRandom().nextGaussian() * 0.02;
            double d2 = user.getRandom().nextGaussian() * 0.02;
            double d3 = user.getRandom().nextGaussian() * 0.02;
            double ox = (user.getRandom().nextFloat() * user.getBbWidth() * 2.0F - user.getBbWidth() - d0 * 10.0) * 5.0;
            double oz = (user.getRandom().nextFloat() * user.getBbWidth() * 2.0F - user.getBbWidth() - d3 * 10.0) * 5.0;
            level.sendParticles(ParticleTypes.LAVA,
                    user.getX() + ox, user.getY(), user.getZ() + oz,
                    1, d0, d2, d3, 0.0);
        }

        // ── 服务端逻辑 ──────────────────────────────────────
        float baseAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);

        int rank = user.getCapability(CapabilityConcentrationRank.RANK_POINT)
                .map(cap -> cap.getRank(level.getGameTime()).level)
                .orElse(0);
        int powerLevel = Math.max(1, EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, blade));

        // AOE 斩击
        AABB box = user.getBoundingBox().inflate(5.0, 0.25, 5.0);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != user && !e.isAlliedTo(user) && !e.hasEffect(MobEffects.INVISIBILITY));
        for (LivingEntity target : targets) {
            target.hurt(level.damageSources().mobAttack(user), baseAttack);
            target.invulnerableTime = 0;
            level.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY(0.5), target.getZ(),
                    8, target.getBbWidth() * 0.3, target.getBbHeight() * 0.3,
                    target.getBbWidth() * 0.3, 0.15);
        }

        // 飞刃伤害计算
        float magicDamage = baseAttack * 0.53F;
        if (rank >= 5) {
            magicDamage += (baseAttack * (0.2F + (float) powerLevel / 5.0F)) * 0.5F;
        }
        if (rank >= 7) {
            magicDamage *= 2.0F;
        }

        // ── 构建逐个生成的队列（4 发，每 3 tick 出一根） ──────
        Vec3 forward = user.getLookAngle();
        Vec3 right = new Vec3(-forward.z, 0, forward.x).normalize();
        double depth = 3.0;
        double halfW = 2.0;
        double halfH = 1.2;
        long baseTick = level.getServer().getTickCount();
        float[] driveSpeeds = {0.8F, 1.2F, 1.6F, 0.8F};

        for (int idx = 0; idx < 4; idx++) {
            double rx = (level.random.nextDouble() - 0.5) * 2.0 * halfW;
            double ry = (level.random.nextDouble() - 0.5) * 2.0 * halfH;
            double posY = user.getY() + user.getEyeHeight() / 2.0 + ry;

            Vec3 spawnPos = user.position()
                    .add(forward.scale(depth))
                    .add(right.scale(rx));
            spawnPos = new Vec3(spawnPos.x, posY, spawnPos.z);

            Vec3 vel = forward.scale(driveSpeeds[idx % driveSpeeds.length]);

            boolean particle = level.random.nextInt(10) < 3;
            String pstyle = particle ? "LAVA" : "";

            long spawnTick = baseTick + (long) idx * 3; // 每 3 tick 出一根
            PENDING.add(new FlareSpawnEntry(level, user, spawnPos, vel, magicDamage,
                    particle, pstyle, spawnTick));
        }

        return super.doArts(type, user);
    }

    /** 每 tick 检查队列，到期即生成。 */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (PENDING.isEmpty()) return;

        // 找到第一个有效 entry 所在 level 的时间
        var entry = PENDING.get(0);
        long now = entry.level.getServer().getTickCount();

        while (!PENDING.isEmpty() && PENDING.get(0).spawnTick <= now) {
            FlareSpawnEntry e = PENDING.remove(0);
            e.spawn();
        }
    }

    /** 一条待生成的飞刃记录。 */
    private static class FlareSpawnEntry {
        final ServerLevel level;
        final LivingEntity user;
        final Vec3 pos, vel;
        final float damage;
        final boolean particle;
        final String pstyle;
        final long spawnTick;

        FlareSpawnEntry(ServerLevel level, LivingEntity user,
                        Vec3 pos, Vec3 vel, float damage,
                        boolean particle, String pstyle, long spawnTick) {
            this.level = level;
            this.user = user;
            this.pos = pos;
            this.vel = vel;
            this.damage = damage;
            this.particle = particle;
            this.pstyle = pstyle;
            this.spawnTick = spawnTick;
        }

        void spawn() {
            FlareEdgeEntity flare = FlareEdgeEntity.spawn(level, user, pos, vel,
                    damage, DRIVE_COLOR, 60, 0.0F, true);
            flare.setParticleEnabled(particle);
            if (particle) {
                flare.setParticleStyle(pstyle);
            }
        }
    }
}

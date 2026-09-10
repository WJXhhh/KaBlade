package com.wjx.kablade.slasharts;

import com.wjx.kablade.Main;
import com.wjx.kablade.blades.ModSlashArts;
import com.wjx.kablade.init.KabladeCapabilities;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.function.Function;

/**
 * 盗火者 (Prometheus) —— 神火试炼专属 SA。
 * <p>
 * 施放后的 10 秒内（200 tick），玩家在「斩无不断:效果」HUD 中获得倒计时读条。
 * 期间使用带有此 SA 的拔刀剑攻击时附带额外伤害；若敌人处于点燃状态，该额外伤害进一步提高。
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PrometheusArts extends SlashArts {

    public static final String PROP_KEY = "prometheus";
    public static final int BUFF_DURATION_TICKS = 200;

    private static final float EXTRA_DAMAGE_BASE = 8.0F;
    private static final float ATTACK_FACTOR = 1.5F;
    private static final float BURNING_FACTOR = 1.75F;

    public PrometheusArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        if (user instanceof Player player) {
            player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                    .ifPresent(cap -> cap.set(PROP_KEY, BUFF_DURATION_TICKS));
        }

        if (user.level() instanceof ServerLevel level) {
            Vec3 pos = user.position();
            // 环绕火光演出
            for (int i = 0; i < 32; i++) {
                double angle = Math.PI * 2.0 * i / 32.0;
                double r = 1.5;
                double px = pos.x + Math.cos(angle) * r;
                double pz = pos.z + Math.sin(angle) * r;
                double py = pos.y + 0.2 + (i % 4) * 0.4;
                level.sendParticles(ParticleTypes.FLAME, px, py, pz, 1, 0.0, 0.04, 0.0, 0.01);
            }
            for (int i = 0; i < 8; i++) {
                level.sendParticles(ParticleTypes.LAVA,
                        pos.x + (level.random.nextDouble() - 0.5) * 2.0,
                        pos.y + 0.5 + level.random.nextDouble() * 1.5,
                        pos.z + (level.random.nextDouble() - 0.5) * 2.0,
                        1, 0.0, 0.0, 0.0, 0.0);
            }
            level.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2F, 0.8F);
            level.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.9F);
            level.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.6F);
        }

        return super.doArts(type, user);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) {
            return;
        }
        Player player = event.player;
        player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA).ifPresent(cap -> {
            int cur = cap.get(PROP_KEY);
            if (cur > 0) {
                cap.set(PROP_KEY, cur - 1);
            }
        });
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if (!(attacker instanceof Player player)) {
            return;
        }

        // 过滤玩家使用副手发射的普通箭矢或三叉戟等非拔刀剑弹射物
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile projectile
                && !projectile.getClass().getName().contains("slashblade")
                && !projectile.getClass().getName().contains("kablade")) {
            return;
        }

        LivingEntity victim = event.getEntity();
        if (!SaTargeting.canDamage(player, victim)) {
            return;
        }

        // 检查「盗火者」HUD 剩余时间
        int remainingTicks = player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                .map(cap -> cap.get(PROP_KEY))
                .orElse(0);
        if (remainingTicks <= 0) {
            return;
        }

        // 检查玩家当前手持的拔刀剑是否带有此 SA
        ItemStack blade = player.getMainHandItem();
        if (!isBladeWithPrometheus(blade)) {
            return;
        }

        float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(12.0F);

        float extra = EXTRA_DAMAGE_BASE + MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR);
        boolean isBurning = victim.isOnFire() || victim.getRemainingFireTicks() > 0;
        if (isBurning) {
            extra *= BURNING_FACTOR;
        }

        event.setAmount(event.getAmount() + extra);
        victim.setSecondsOnFire(4);

        if (player.level() instanceof ServerLevel level) {
            Vec3 pos = victim.position().add(0, victim.getBbHeight() * 0.5, 0);
            level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z,
                    isBurning ? 14 : 7, 0.25, 0.25, 0.25, 0.04);
            if (isBurning) {
                level.sendParticles(ParticleTypes.LAVA, pos.x, pos.y, pos.z, 2, 0.15, 0.15, 0.15, 0.0);
            }
            level.playSound(null, pos.x, pos.y, pos.z,
                    isBurning ? SoundEvents.BLAZE_HURT : SoundEvents.FIRECHARGE_USE,
                    SoundSource.PLAYERS, 0.8F, isBurning ? 1.3F : 1.1F);
        }
    }

    public static boolean isBladeWithPrometheus(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemSlashBlade)) {
            return false;
        }
        return stack.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> ModSlashArts.PROMETHEUS.getId().equals(state.getSlashArtsKey()))
                .orElse(false);
    }
}

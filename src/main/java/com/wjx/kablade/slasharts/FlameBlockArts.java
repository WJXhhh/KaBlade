package com.wjx.kablade.slasharts;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.KabladeCapabilities;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaDamage;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * 火焰格挡 (Flame Block) —— 游骑兵锯齿专属 SA。
 * <p>
 * 释放后 10 秒（200 tick）内：
 * <ul>
 *   <li>在「斩无不断:效果」HUD 中显示倒计时读条</li>
 *   <li>玩家移动速度降低 40%</li>
 *   <li>玩家受到伤害降低 40%</li>
 *   <li>每秒对周围 5 格内的敌人造成伤害并点燃</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FlameBlockArts extends SlashArts {

    public static final String PROP_KEY = "flame_block";
    public static final int BUFF_DURATION_TICKS = 200;

    private static final UUID SPEED_MODIFIER_UUID =
            UUID.fromString("6d7b4a2e-8c31-4f1a-9b56-1e98d7f2a3c5");

    private static final double PULSE_RADIUS = 5.0;
    private static final float PULSE_DAMAGE_BASE = 6.0F;
    private static final float ATTACK_FACTOR = 0.75F;
    private static final int FIRE_SECONDS = 4;

    public FlameBlockArts(Function<LivingEntity, ResourceLocation> state) {
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
            applySpeedDebuffIfMissing(player);
            pulseFlameDamage(player);
        }

        if (user.level() instanceof ServerLevel level) {
            Vec3 pos = user.position();
            // 激活演出：烈火光环与熔岩飞溅
            for (int i = 0; i < 32; i++) {
                double angle = Math.PI * 2.0 * i / 32.0;
                double r = 1.8;
                double px = pos.x + Math.cos(angle) * r;
                double pz = pos.z + Math.sin(angle) * r;
                double py = pos.y + 0.2 + (i % 4) * 0.4;
                level.sendParticles(ParticleTypes.FLAME, px, py, pz, 1, 0.0, 0.05, 0.0, 0.02);
            }
            for (int i = 0; i < 10; i++) {
                level.sendParticles(ParticleTypes.LAVA,
                        pos.x + (level.random.nextDouble() - 0.5) * 2.0,
                        pos.y + 0.5 + level.random.nextDouble() * 1.2,
                        pos.z + (level.random.nextDouble() - 0.5) * 2.0,
                        1, 0.0, 0.0, 0.0, 0.0);
            }
            level.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2F, 0.9F);
            level.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.8F);
            level.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.8F);
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
                applySpeedDebuffIfMissing(player);

                // 每秒对周围敌人造成伤害并点燃（每 20 tick 触发一次）
                if (cur % 20 == 0) {
                    pulseFlameDamage(player);
                }

                // 生效期间玩家周围环绕微弱火星
                if (cur % 4 == 0 && player.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.FLAME,
                            player.getX() + (level.random.nextDouble() - 0.5) * 0.9,
                            player.getY() + 0.2 + level.random.nextDouble() * 0.9,
                            player.getZ() + (level.random.nextDouble() - 0.5) * 0.9,
                            1, 0.0, 0.02, 0.0, 0.01);
                }

                cap.set(PROP_KEY, cur - 1);
                if (cur - 1 == 0) {
                    removeSpeedDebuff(player);
                }
            } else {
                removeSpeedDebuff(player);
            }
        });
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide() || event.getAmount() <= 0.0F) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }

        if (event.getEntity() instanceof Player player) {
            int remainingTicks = player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                    .map(cap -> cap.get(PROP_KEY))
                    .orElse(0);
            if (remainingTicks > 0) {
                // 受到伤害降低 40%
                event.setAmount(event.getAmount() * 0.60F);

                if (player.level() instanceof ServerLevel level) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8F, 1.2F);
                    level.sendParticles(ParticleTypes.FLAME,
                            player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(),
                            6, 0.3, 0.3, 0.3, 0.05);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        removeSpeedDebuff(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        removeSpeedDebuff(event.getEntity());
    }

    /**
     * 对周围敌人造成伤害并点燃。
     */
    private static void pulseFlameDamage(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        AABB area = player.getBoundingBox().inflate(PULSE_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area);

        ItemStack blade = player.getMainHandItem();
        float bladeAttack = 17.0F;
        if (blade.getItem() instanceof ItemSlashBlade) {
            bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                    .map(ISlashBladeState::getBaseAttackModifier)
                    .orElse(17.0F);
        }
        float pulseDamage = PULSE_DAMAGE_BASE + MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR);

        boolean hitAny = false;
        for (LivingEntity target : targets) {
            if (!SaTargeting.canDamage(player, target)) {
                continue;
            }
            if (target.distanceToSqr(player) > PULSE_RADIUS * PULSE_RADIUS) {
                continue;
            }

            hitAny = true;
            target.setSecondsOnFire(FIRE_SECONDS);
            SaDamage.hurtSlashArtNoIFrame(target, level, player, pulseDamage);

            Vec3 hitPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
            level.sendParticles(ParticleTypes.FLAME, hitPos.x, hitPos.y, hitPos.z,
                    8, 0.25, 0.25, 0.25, 0.04);
            level.sendParticles(ParticleTypes.LAVA, hitPos.x, hitPos.y, hitPos.z,
                    1, 0.1, 0.1, 0.1, 0.0);
        }

        // 周围火圈扩散演出与音效
        Vec3 pos = player.position();
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, hitAny ? 1.2F : 1.0F);

        for (int i = 0; i < 20; i++) {
            double angle = Math.PI * 2.0 * i / 20.0;
            double rx = Math.cos(angle);
            double rz = Math.sin(angle);
            level.sendParticles(ParticleTypes.FLAME,
                    pos.x + rx * 1.2, pos.y + 0.3, pos.z + rz * 1.2,
                    1, rx * 0.15, 0.03, rz * 0.15, 0.02);
        }
    }

    private static void applySpeedDebuffIfMissing(Player player) {
        AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null || attr.getModifier(SPEED_MODIFIER_UUID) != null) {
            return;
        }
        attr.addTransientModifier(new AttributeModifier(
                SPEED_MODIFIER_UUID,
                "kablade.flame_block_slow",
                -0.40,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static void removeSpeedDebuff(LivingEntity entity) {
        AttributeInstance attr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null && attr.getModifier(SPEED_MODIFIER_UUID) != null) {
            attr.removeModifier(SPEED_MODIFIER_UUID);
        }
    }
}
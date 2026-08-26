package com.wjx.kablade.specialeffect;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModSpecialEffects;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaDamage;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * FFF团之怒 (Rainbow Flames) —— 异端审问会火炬专属特殊效果。
 * <p>
 * 攻击时有 11.1% 的概率造成范围火焰伤害（异端审问会火炬上约为 8 点），并点燃周围敌人。
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RainbowFlames extends SpecialEffect {

    private static final double PROC_CHANCE = 0.111D;
    private static final float DAMAGE_BASE = 4.2F;
    private static final float DAMAGE_FACTOR = 1.0F;
    private static final double RADIUS = 4.5D;
    private static final double VERTICAL_RADIUS = 2.5D;
    private static final int FIRE_SECONDS = 5;

    private static final ThreadLocal<Boolean> IS_TRIGGERING = ThreadLocal.withInitial(() -> false);

    public RainbowFlames() {
        super(-1, true, true);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (IS_TRIGGERING.get()) {
            return;
        }

        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof Player player) || !hasEffect(player)) {
            return;
        }

        LivingEntity victim = event.getEntity();
        if (!SaTargeting.canDamage(player, victim)) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        if (level.random.nextDouble() >= PROC_CHANCE) {
            return;
        }

        IS_TRIGGERING.set(true);
        try {
            trigger(level, player, victim);
        } finally {
            IS_TRIGGERING.set(false);
        }
    }

    private static void trigger(ServerLevel level, Player player, LivingEntity centerEntity) {
        float bladeAttack = player.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float damage = DAMAGE_BASE + MathFunc.amplifierCalc(bladeAttack, DAMAGE_FACTOR);

        level.playSound(null, centerEntity.getX(), centerEntity.getY(), centerEntity.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2F,
                0.8F + level.random.nextFloat() * 0.4F);
        level.playSound(null, centerEntity.getX(), centerEntity.getY(), centerEntity.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6F,
                1.2F + level.random.nextFloat() * 0.4F);

        for (int i = 0; i < 30; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double dist = Math.sqrt(level.random.nextDouble()) * RADIUS;
            double x = centerEntity.getX() + Math.cos(angle) * dist;
            double y = centerEntity.getY() + level.random.nextDouble() * (centerEntity.getBbHeight() + 1.0D);
            double z = centerEntity.getZ() + Math.sin(angle) * dist;
            level.sendParticles(ParticleTypes.FLAME, x, y, z, 1,
                    (level.random.nextDouble() - 0.5D) * 0.1D,
                    0.05D + level.random.nextDouble() * 0.1D,
                    (level.random.nextDouble() - 0.5D) * 0.1D, 0.02D);
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 1, 0.0D, 0.02D, 0.0D, 0.01D);
            }
            if (i % 5 == 0) {
                level.sendParticles(ParticleTypes.LAVA, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }

        AABB area = centerEntity.getBoundingBox()
                .inflate(RADIUS, VERTICAL_RADIUS, RADIUS);
        var targets = SaTargeting.targets(level, player, area,
                selected -> SaTargeting.canDamage(player, selected.root()));

        for (var selected : targets) {
            LivingEntity target = selected.root();
            SaDamage.hurt(selected.hitEntity(), level.damageSources().playerAttack(player), damage);
            target.setSecondsOnFire(FIRE_SECONDS);
        }
    }

    private static boolean hasEffect(Player player) {
        ItemStack blade = player.getMainHandItem();
        if (!(blade.getItem() instanceof ItemSlashBlade)) {
            return false;
        }
        return blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.RAINBOW_FLAMES.getId()))
                .orElse(false);
    }
}

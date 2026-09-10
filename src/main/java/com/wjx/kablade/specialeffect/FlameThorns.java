package com.wjx.kablade.specialeffect;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModSpecialEffects;
import com.wjx.kablade.util.SaDamage;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 炎之荆棘 (Flame Thorns) —— 游骑兵锯齿专属特殊效果。
 * <p>
 * 当玩家手持带有此 SE 的拔刀剑被攻击时，有 50% 的概率对周围 4.5 格内的敌人造成反击伤害。
 * 伤害与玩家攻击力挂钩，基准为默认持有该刀（基础攻击力 12 点）时造成 5 点伤害。
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FlameThorns extends SpecialEffect {

    private static final float PROC_CHANCE = 0.50F;
    private static final double RADIUS = 4.5D;

    /** 游骑兵锯齿默认攻击力（玩家基础 1.0 + 拔刀剑 17.0 = 18.0），对应 5 点基准反击伤害 */
    private static final double DEFAULT_ATTACK = 18.0D;
    private static final float BASE_THORNS_DAMAGE = 5.0F;

    private static final ThreadLocal<Boolean> IS_TRIGGERING = ThreadLocal.withInitial(() -> false);

    public FlameThorns() {
        super(-1, true, true);
    }

    @Override
    public Component getDescription() {
        return Component.translatable(getDescriptionId());
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0.0F
                || event.getEntity().level().isClientSide()) {
            return;
        }
        if (IS_TRIGGERING.get()) {
            return;
        }

        if (!(event.getEntity() instanceof Player player) || !hasEffect(player)) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        if (level.random.nextFloat() >= PROC_CHANCE) {
            return;
        }

        IS_TRIGGERING.set(true);
        try {
            triggerThorns(level, player);
        } finally {
            IS_TRIGGERING.set(false);
        }
    }

    private static void triggerThorns(ServerLevel level, Player player) {
        float damage = calculateThornsDamage(player);

        AABB area = player.getBoundingBox().inflate(RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area);

        boolean hitAny = false;
        for (LivingEntity target : targets) {
            if (!SaTargeting.canDamage(player, target)) {
                continue;
            }
            if (target.distanceToSqr(player) > RADIUS * RADIUS) {
                continue;
            }

            hitAny = true;
            SaDamage.hurt(target, level.damageSources().playerAttack(player), damage);

            // 受击目标火焰与暴击火芒
            Vec3 hitPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
            level.sendParticles(ParticleTypes.FLAME, hitPos.x, hitPos.y, hitPos.z,
                    6, 0.2, 0.2, 0.2, 0.03);
            level.sendParticles(ParticleTypes.CRIT, hitPos.x, hitPos.y, hitPos.z,
                    4, 0.2, 0.2, 0.2, 0.05);
        }

        // 荆棘反击音效与环形烈焰刺芒演出
        Vec3 pos = player.position();
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.THORNS_HIT, SoundSource.PLAYERS, 1.0F, 1.2F);
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.7F, 1.4F);

        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2.0 * i / 24.0;
            double rx = Math.cos(angle);
            double rz = Math.sin(angle);
            level.sendParticles(ParticleTypes.FLAME,
                    pos.x + rx * 1.0, pos.y + 0.3 + (i % 3) * 0.25, pos.z + rz * 1.0,
                    1, rx * 0.2, 0.05, rz * 0.2, 0.03);
            if (i % 4 == 0) {
                level.sendParticles(ParticleTypes.LAVA,
                        pos.x + rx * 0.8, pos.y + 0.5, pos.z + rz * 0.8,
                        1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    private static float calculateThornsDamage(Player player) {
        double attack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        return (float) Math.max(1.0, attack * (BASE_THORNS_DAMAGE / DEFAULT_ATTACK));
    }

    private static boolean hasEffect(Player player) {
        ItemStack blade = player.getMainHandItem();
        if (!(blade.getItem() instanceof ItemSlashBlade)) {
            return false;
        }
        return blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.FLAME_THORNS.getId()))
                .orElse(false);
    }
}
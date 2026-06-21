package com.wjx.kablade.slasharts;

import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Function;

/**
 * 罪斩 —— 「影鵺」专属 SA。
 * <p>
 * 从 1.12.2 {@code HonKaiZaizan} 移植而来：先释放 SlashBlade 内置「突刺（Spear）」，
 * 再对周围 5×1×5 范围内的非玩家生物造成 20 点基础伤害 + 基于刀攻击力的对数额外伤害，
 * 并对玩家敌人附加短暂力量；最后给自身附加力量 II（7 秒）。
 */
public final class ZaizanArts extends SlashArts {

    private static final float BASE_DAMAGE = 20.0F;
    private static final double AOE_RADIUS = 5.0;
    private static final int STRENGTH_DURATION = 140;
    private static final int STRENGTH_AMPLIFIER = 2;
    private static final int DELAYED_STRENGTH_TICKS = 12;

    public ZaizanArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();

        // 先调用 SlashBlade 内置 Spear
        Registry<SlashArts> registry = level.registryAccess().registryOrThrow(SlashArts.REGISTRY_KEY);
        SlashArts spear = registry.get(ResourceLocation.fromNamespaceAndPath("slashblade", "spear"));
        if (spear != null) {
            spear.doArts(type, user);
        }

        // 计算额外伤害
        float bladeAttack = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float extraDamage = (float) MathFunc.amplifierCalc(bladeAttack, 20.0F);

        // AOE 伤害
        AABB box = user.getBoundingBox()
                .inflate(AOE_RADIUS, 1.0, AOE_RADIUS)
                .move(user.getDeltaMovement());
        List<Entity> entities = level.getEntities(user, box,
                e -> e instanceof LivingEntity && e != user && e.isAlive());

        for (Entity entity : entities) {
            LivingEntity target = (LivingEntity) entity;
            if (target instanceof Player) {
                target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,
                        100, STRENGTH_AMPLIFIER, false, true));
            } else {
                target.hurt(level.damageSources().mobAttack(user), BASE_DAMAGE + extraDamage);
            }
        }

        // 延迟给予自身力量 II（同 1.12.2 的 TickDelayTask）
        level.getServer().tell(new net.minecraft.server.TickTask(
                level.getServer().getTickCount() + DELAYED_STRENGTH_TICKS,
                () -> user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,
                        STRENGTH_DURATION, STRENGTH_AMPLIFIER, false, true))));

        return super.doArts(type, user);
    }
}

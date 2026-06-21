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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Function;

/**
 * 震击 —— 「破晓者：塔尔瓦」专属 SA。
 * <p>
 * 从 1.12.2 {@code HonkaiShockImpact} 移植而来：
 * 先释放 SlashBlade 内置的「突刺（Spear）」效果，再对周围 3×1×3 范围内的非玩家敌人造成
 * {@code 22 + amplifierCalc(attack, 10)} 伤害，并给持有者附加力量 VI（amplifier 5，与 1.12.2 一致）。
 */
public final class ShockImpactArts extends SlashArts {

    private static final float BASE_DAMAGE = 22.0F;
    private static final double AOE_RADIUS = 3.0;
    private static final int STRENGTH_DURATION = 100;
    private static final int STRENGTH_AMPLIFIER = 5;

    public ShockImpactArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();

        // 先调用 SlashBlade 内置 Spear
        Registry<SlashArts> registry = user.level().registryAccess()
                .registryOrThrow(SlashArts.REGISTRY_KEY);
        SlashArts spear = registry.get(ResourceLocation.fromNamespaceAndPath("slashblade", "spear"));
        if (spear != null) {
            spear.doArts(type, user);
        }

        // 计算额外伤害
        float bladeAttack = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float extraDamage = (float) MathFunc.amplifierCalc(bladeAttack, 10.0F);

        // AOE 伤害
        AABB box = user.getBoundingBox()
                .inflate(AOE_RADIUS, 1.0, AOE_RADIUS)
                .move(user.getDeltaMovement());
        List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != user && e.isAlive());
        for (LivingEntity target : enemies) {
            if (target instanceof Player) {
                continue;
            }
            target.hurt(level.damageSources().mobAttack(user), BASE_DAMAGE + extraDamage);
        }

        // 给持有者力量 buff
        user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,
                STRENGTH_DURATION, STRENGTH_AMPLIFIER, false, true));

        return super.doArts(type, user);
    }
}

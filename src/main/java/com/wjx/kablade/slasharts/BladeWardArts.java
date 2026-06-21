package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.RaikiriShieldEntity;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.function.Function;

/**
 * 刃盾 —— 「雷切」专属 SA。
 * <p>
 * 从 1.12.2 {@code HonkaiBladeWard} 移植而来：
 * 先清除持有者周围已有的雷切护盾，再召唤一个新的护盾实体。
 */
public final class BladeWardArts extends SlashArts {

    public BladeWardArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        // 移除该玩家已有的雷切护盾，避免叠加（同 1.12.2 逻辑：只清自己的护盾）
        List<RaikiriShieldEntity> existing = ((ServerLevel) user.level())
                .getEntitiesOfClass(RaikiriShieldEntity.class,
                        user.getBoundingBox().inflate(32.0),
                        e -> e.isAlive() && e.getThrower() == user);
        for (RaikiriShieldEntity e : existing) {
            e.discard();
        }

        // 召唤新护盾
        RaikiriShieldEntity.spawn((ServerLevel) user.level(), user);

        return super.doArts(type, user);
    }
}

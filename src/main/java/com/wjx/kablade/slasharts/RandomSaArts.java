package com.wjx.kablade.slasharts;

import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.RegistryAccess;

import java.util.function.Function;

/**
 * 随机 SA —— 从所有已注册的斩击剑 SA 中随机选一个执行（复刻 1.12.2 {@code AL_RandomSA}）。
 * <p>1.12.2 实现：遍历 {@code ItemSlashBlade.specialAttacks} 的键，随机取一个调用。
 */
public final class RandomSaArts extends SlashArts {

    public RandomSaArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }
        final ServerLevel level = (ServerLevel) user.level();

        // 获取所有已注册的 SlashArts（包括 SlashBlade 原生和 KBlade2 自定义）
        RegistryAccess registryAccess = level.registryAccess();
        var slashArtsRegistry = registryAccess.registryOrThrow(SlashArts.REGISTRY_KEY);
        var allArts = slashArtsRegistry.stream().toList();

        if (allArts.isEmpty()) {
            return super.doArts(type, user);
        }

        // 随机选择一个 SA
        var random = level.getRandom();
        SlashArts selected = allArts.get(random.nextInt(allArts.size()));

        // 执行选中的 SA（通过调用其 doArts 方法）
        return selected.doArts(type, user);
    }
}

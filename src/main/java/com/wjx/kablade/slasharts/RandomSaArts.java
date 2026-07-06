package com.wjx.kablade.slasharts;

import mods.flammpfeil.slashblade.registry.SlashArtsRegistry;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 随机 SA —— 从所有已注册的斩击剑 SA 中随机选一个执行（复刻 1.12.2 {@code AL_RandomSA}）。
 * <p>1.12.2 实现：遍历 {@code ItemSlashBlade.specialAttacks} 的键，随机取一个调用。
 */
public final class RandomSaArts extends SlashArts {

    private static final ResourceLocation NONE_ARTS =
            ResourceLocation.fromNamespaceAndPath("slashblade", "none");

    public RandomSaArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        // 获取所有已注册的 SlashArts（包括 SlashBlade 原生和 KBlade2 自定义），但不能抽到自己。
        var slashArtsRegistry = SlashArtsRegistry.REGISTRY.get();
        List<SlashArts> candidates = new ArrayList<>();
        slashArtsRegistry.getValues().forEach(arts -> {
            ResourceLocation key = slashArtsRegistry.getKey(arts);
            if (key != null && !NONE_ARTS.equals(key) && !(arts instanceof RandomSaArts)) {
                candidates.add(arts);
            }
        });

        if (candidates.isEmpty()) {
            return super.doArts(type, user);
        }

        // 随机选择一个 SA
        var random = user.level().getRandom();
        SlashArts selected = candidates.get(random.nextInt(candidates.size()));

        // 执行选中的 SA（通过调用其 doArts 方法）
        return selected.doArts(type, user);
    }
}

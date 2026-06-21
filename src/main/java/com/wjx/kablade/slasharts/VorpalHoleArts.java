package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.VorpalBlackHoleEntity;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Function;

/**
 * 时空黑洞 —— 「反力场打刀11式」专属 SA。
 * <p>
 * 从 1.12.2 {@code HonKaiVorpalHole} 移植而来：
 * 先执行一次 SlashBlade 内置的「樱花终结」，随后在玩家位置生成一个反力场黑洞，
 * 将周围敌人吸引并切割。
 */
public final class VorpalHoleArts extends SlashArts {

    private static final ResourceLocation SAKURA_END =
            ResourceLocation.fromNamespaceAndPath("slashblade", "sakura_end");

    public VorpalHoleArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        // 复刻 1.12.2：先调用樱花终结的斩击效果。
        Registry<SlashArts> registry = user.level().registryAccess()
                .registryOrThrow(SlashArts.REGISTRY_KEY);
        SlashArts sakura = registry.get(SAKURA_END);
        if (sakura != null) {
            sakura.doArts(type, user);
        }

        // 在玩家脚下生成牵引黑洞。
        ServerLevel level = (ServerLevel) user.level();
        VorpalBlackHoleEntity.spawn(level, user, user.getX(), user.getY(), user.getZ());

        return super.doArts(type, user);
    }
}

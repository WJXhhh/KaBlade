package com.wjx.kablade.event;

import com.wjx.kablade.Main;
import com.wjx.kablade.config.KabladeConfig;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 在本模组的拔刀剑被创建时（{@code SlashBladeRegistryEvent.Post}，SlashBlade 1.9+），按 {@link KabladeConfig}
 * 里配置的全局倍率缩放它的基础攻击与最大耐久。只对 {@code kablade} 命名空间的刀生效。
 *
 * <p>兼容性：{@code SlashBladeRegistryEvent.Post} 是 SlashBlade 1.9+ 新增的。
 * 为了让 mod 同时支持 SlashBlade 1.8，本类主体不引用该类型，而是通过内部类 {@link Handler} 实现；
 * 由 {@link #tryRegister()} 先反射检测事件类是否存在，确认后再注册内部类。
 */
public final class BladeAttributeOverride {

    private BladeAttributeOverride() {
    }

    /**
     * 尝试注册属性缩放到 Forge 总线。
     * 如果当前 SlashBlade 版本没有 {@code SlashBladeRegistryEvent.Post}（即 ≤1.8），
     * 则静默跳过，不做任何事。
     */
    public static void tryRegister() {
        try {
            Class.forName("mods.flammpfeil.slashblade.event.SlashBladeRegistryEvent$Post");
            MinecraftForge.EVENT_BUS.register(Handler.class);
            Main.LOGGER.info("[kablade] BladeAttributeOverride registered (SlashBlade 1.9+)");
        } catch (ClassNotFoundException e) {
            Main.LOGGER.info("[kablade] BladeAttributeOverride skipped (SlashBlade ≤1.8, no SlashBladeRegistryEvent.Post)");
        }
    }

    /**
     * 实际的事件处理器。作为内部类单独存在，仅在 {@link #tryRegister()} 确认
     * {@code SlashBladeRegistryEvent.Post} 存在后才会被 JVM 加载。
     * 这样在 SlashBlade 1.8 下，该类型引用不会触发类加载错误。
     */
    @SuppressWarnings("unused")
    private static final class Handler {
        private Handler() {
        }

        @SubscribeEvent
        public static void onBladeCreated(mods.flammpfeil.slashblade.event.SlashBladeRegistryEvent.Post event) {
            if (!Main.MODID.equals(event.getSlashBladeDefinition().getName().getNamespace())) {
                return;
            }

            if (!KabladeConfig.SPEC.isLoaded()) {
                return;
            }

            double attackMul = KabladeConfig.ATTACK_MULTIPLIER.get();
            double durabilityMul = KabladeConfig.DURABILITY_MULTIPLIER.get();
            if (attackMul == 1.0D && durabilityMul == 1.0D) {
                return;
            }

            event.getBlade().getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
                if (attackMul != 1.0D) {
                    state.setBaseAttackModifier((float) (state.getBaseAttackModifier() * attackMul));
                }
                if (durabilityMul != 1.0D) {
                    int baseMaxDamage = state.getMaxDamage();
                    if (baseMaxDamage > 0) {
                        state.setMaxDamage(Math.max(1, (int) Math.round(baseMaxDamage * durabilityMul)));
                    }
                }
            });
        }
    }
}

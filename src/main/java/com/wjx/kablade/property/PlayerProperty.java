package com.wjx.kablade.property;

import com.wjx.kablade.init.KabladeCapabilities;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * 玩家 HUD 上的一条属性/buff 条目，对应 1.12.2 中 {@code prop.<id>} 的每个属性。
 * <p>
 * 每个属性在玩家 NBT 中以 int 值存储（剩余 tick/计数），
 * 并关联一个最大参考值用于渲染 CD 进度条。
 * 通过 {@link PlayerPropertyRegistry#register(PlayerProperty)} 注册。
 *
 * <pre>{@code
 * // 方式一：从 capability 自动读取（推荐）
 * PlayerPropertyRegistry.register(PlayerProperty.builder("kami_of_war_count")
 *         .displayName(Component.translatable("prop.kami_of_war_count"))
 *         .capabilityKey("kami_of_war_count")  // 自动从 capability 中读取
 *         .maxValue(6)
 *         .build());
 *
 * // 方式二：手动指定取值逻辑（灵活）
 * PlayerPropertyRegistry.register(PlayerProperty.builder("custom")
 *         .displayName(Component.literal("自定义"))
 *         .intValue(player -> player.getPersistentData().getInt("my_key"))
 *         .maxValue(10)
 *         .build());
 * }</pre>
 */
public final class PlayerProperty {

    private final String id;
    private final Component displayName;
    private final ToIntFunction<Player> intValue;
    private final Predicate<Player> active;
    private final int maxValue;

    private PlayerProperty(String id, Component displayName,
                           ToIntFunction<Player> intValue, Predicate<Player> active, int maxValue) {
        this.id = id;
        this.displayName = displayName;
        this.intValue = intValue;
        this.active = active;
        this.maxValue = maxValue;
    }

    /** 唯一标识符，用于去重和查找。 */
    public String id() {
        return id;
    }

    /** 属性显示名（如"神威镇世"）。 */
    public Component displayName() {
        return displayName;
    }

    /** 当前 int 值。默认情况下值 ≤ 0 时隐藏，也可由 Builder 单独指定可见条件。 */
    public int getIntValue(Player player) {
        return intValue.applyAsInt(player);
    }

    /** 该属性是否应在玩家 HUD 中显示。 */
    public boolean isActive(Player player) {
        return active.test(player);
    }

    /** 最大参考值，用于渲染 CD 进度条。 */
    public int maxValue() {
        return maxValue;
    }

    // ——— Builder ———

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private Component displayName;
        private ToIntFunction<Player> intValue = p -> 0;
        private Predicate<Player> active;
        private int maxValue = 1;

        private Builder(String id) {
            if (id == null || id.isBlank())
                throw new IllegalArgumentException("Property id must not be blank");
            this.id = id;
        }

        /** 属性显示名（如 "§b神威镇世"）。 */
        public Builder displayName(Component displayName) {
            this.displayName = displayName;
            return this;
        }

        /**
         * 从玩家 NBT/capability 中读取当前 int 值。
         * 与 {@link #capabilityKey(String)} 二选一。
         */
        public Builder intValue(ToIntFunction<Player> intValue) {
            this.intValue = Objects.requireNonNull(intValue);
            return this;
        }

        /**
         * 快捷方式：从 {@link com.wjx.kablade.data.IPlayerPropertyData} capability 中
         * 按 key 读取 int 值。自动生成 {@link #intValue(ToIntFunction)}。
         * 与 {@link #intValue(ToIntFunction)} 二选一，后调用的覆盖前者。
         *
         * @param capabilityKey 属性在 capability 中的键名
         */
        public Builder capabilityKey(String capabilityKey) {
            Objects.requireNonNull(capabilityKey);
            this.intValue = player -> player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                    .map(cap -> cap.get(capabilityKey))
                    .orElse(0);
            return this;
        }

        /** 自定义 HUD 可见条件；未设置时仍以当前值大于 0 为准。 */
        public Builder activeWhen(Predicate<Player> active) {
            this.active = Objects.requireNonNull(active);
            return this;
        }

        /** 最大参考值，对应 1.12.2 的 Bufftimes。用于渲染 CD 进度条。 */
        public Builder maxValue(int maxValue) {
            this.maxValue = Math.max(maxValue, 1);
            return this;
        }

        public PlayerProperty build() {
            if (displayName == null)
                throw new IllegalStateException("displayName must be set for property " + id);
            ToIntFunction<Player> valueFunction = intValue;
            Predicate<Player> activePredicate = active != null
                    ? active
                    : player -> valueFunction.applyAsInt(player) > 0;
            return new PlayerProperty(id, displayName, valueFunction, activePredicate, maxValue);
        }
    }
}

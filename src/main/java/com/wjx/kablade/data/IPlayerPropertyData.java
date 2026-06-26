package com.wjx.kablade.data;

import net.minecraft.nbt.CompoundTag;

/**
 * 玩家属性数据 capability 接口。
 * <p>
 * 存储一组 {@code String → int} 映射，对应 1.12.2 中 {@code KaBladePlayerProp} 的 NBT 数据。
 * 每个键是一个属性 ID（如 {@code "kami_of_war_count"}），值是剩余 tick/计数。
 * <p>
 * 服务端写入 → 自动同步到客户端 → {@link com.wjx.kablade.property.PlayerPropertyOverlay} 读取渲染。
 */
public interface IPlayerPropertyData {

    /** 读取属性值，不存在或失效时返回 0。 */
    int get(String key);

    /** 设置属性值。 */
    void set(String key, int value);

    /** 增加属性值（可为负数）。 */
    default void add(String key, int amount) {
        set(key, get(key) + amount);
    }

    /** 值 > 0 且存在。 */
    default boolean isActive(String key) {
        return get(key) > 0;
    }

    /** 移除属性键。 */
    void remove(String key);

    /** 清空所有。 */
    void clear();

    /** 标记数据需要同步到客户端。 */
    void markDirty();

    /** 是否需要同步。 */
    boolean isDirty();

    /** 以全量 NBT 读取。 */
    void load(CompoundTag tag);

    /** 导出全量 NBT。 */
    CompoundTag save();
}

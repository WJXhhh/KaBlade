package com.wjx.kablade.data;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

/**
 * 玩家属性数据 capability 实现。
 * <p>
 * 内部维持 {@code HashMap<String, Integer>}，通过 {@link #markDirty()} / {@link #isDirty()}
 * 配合 {@link com.wjx.kablade.event.PlayerPropertyDataHandler} 自动同步到客户端。
 */
public class PlayerPropertyData implements IPlayerPropertyData {

    private final Map<String, Integer> data = new HashMap<>();
    private boolean dirty = false;

    @Override
    public int get(String key) {
        return data.getOrDefault(key, 0);
    }

    @Override
    public void set(String key, int value) {
        if (value <= 0) {
            data.remove(key);
        } else {
            data.put(key, value);
        }
        markDirty();
    }

    @Override
    public void remove(String key) {
        data.remove(key);
        markDirty();
    }

    @Override
    public void clear() {
        data.clear();
        markDirty();
    }

    @Override
    public void markDirty() {
        this.dirty = true;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    /** 消费脏标记（同步后调用）。 */
    public void clean() {
        this.dirty = false;
    }

    @Override
    public void load(CompoundTag tag) {
        data.clear();
        for (String key : tag.getAllKeys()) {
            int val = tag.getInt(key);
            if (val > 0) {
                data.put(key, val);
            }
        }
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            tag.putInt(entry.getKey(), entry.getValue());
        }
        return tag;
    }

    /** 获取全部数据（用于网络包）。 */
    public Map<String, Integer> getAll() {
        return Map.copyOf(data);
    }
}

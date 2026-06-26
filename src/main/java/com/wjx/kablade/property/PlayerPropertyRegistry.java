package com.wjx.kablade.property;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 玩家属性（HUD buff）注册中心。
 * <p>
 * 对应 1.12.2 中 SlashBlade 通过 {@code prop.*} lang key + 玩家 NBT 在 HUD 上显示 buff 的体系。
 * 所有 {@link PlayerProperty} 在此注册，由 {@link PlayerPropertyOverlay} 渲染。
 * <p>
 * 线程安全（CopyOnWriteArrayList）。
 */
public final class PlayerPropertyRegistry {

    private static final List<PlayerProperty> PROPERTIES = new CopyOnWriteArrayList<>();

    private PlayerPropertyRegistry() {
    }

    /** 注册一条属性。重复 id 会覆盖。 */
    public static void register(PlayerProperty property) {
        Objects.requireNonNull(property);
        PROPERTIES.removeIf(p -> p.id().equals(property.id()));
        PROPERTIES.add(property);
    }

    /** 批量注册。 */
    public static void registerAll(PlayerProperty... properties) {
        for (PlayerProperty p : properties) {
            register(p);
        }
    }

    /** 取消注册。 */
    public static void unregister(String id) {
        PROPERTIES.removeIf(p -> p.id().equals(id));
    }

    /** 获取当前对玩家活跃的所有属性（按注册顺序）。 */
    @UnmodifiableView
    public static List<PlayerProperty> getActive(Player player) {
        return PROPERTIES.stream()
                .filter(p -> p.isActive(player))
                .toList();
    }

    /** 当前玩家是否有活跃属性。 */
    public static boolean hasActive(Player player) {
        return PROPERTIES.stream().anyMatch(p -> p.isActive(player));
    }

    /** 当前注册总数。 */
    public static int size() {
        return PROPERTIES.size();
    }

    /** 清空。 */
    public static void clear() {
        PROPERTIES.clear();
    }
}

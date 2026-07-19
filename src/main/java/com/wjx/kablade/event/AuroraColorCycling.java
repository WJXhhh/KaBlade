package com.wjx.kablade.event;

import com.wjx.kablade.Main;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 极光刃「映天」的颜色轮转效果。
 * <p>
 * 从 1.12.2 的 {@code WorldEvent.loadEvent()} + {@code onWorldUpdate()} 移植而来。
 * 生成一段从青到绿再回到青的色谱，并使用服务器 tick 计算索引；
 * 当玩家主手持有极光刃时，实时更新其召唤剑颜色。
 */
@Mod.EventBusSubscriber(modid = Main.MODID)
public final class AuroraColorCycling {

    /** 极光色谱：青 → 绿 → 青（ping-pong）。 */
    private static final List<Integer> COLORS = new ArrayList<>();

    /** 色谱已初始化标志。 */
    private static boolean initialized = false;

    private AuroraColorCycling() {
    }

    /**
     * 生成极光色谱。在 mod 初始化时调用。
     * <p>
     * 与 1.12.2 原版一致：从 RGB(0, 196, 255) 到 RGB(0, 255, 196)，共 59 色，
     * 再镜像翻转拼接，总计 118 色形成平滑循环。
     */
    public static void init() {
        if (initialized) return;
        initialized = true;

        COLORS.clear();
        int g = 196;
        int b = 255;
        while (g < 255) {
            int c = (0 << 16) | (g << 8) | b; // RGB(0, g, b)
            COLORS.add(c);
            g++;
            b--;
        }
        List<Integer> mirror = new ArrayList<>(COLORS);
        Collections.reverse(mirror);
        COLORS.addAll(mirror);
    }

    /** 每个玩家 tick：若主手持有极光刃，实时更新召唤剑颜色。 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (COLORS.isEmpty()) return;

        Player player = event.player;
        if (player.level().isClientSide()) return;
        if (player.getServer() == null) return;

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemSlashBlade)) return;

        int cycleLength = Math.max(1, COLORS.size() - 2);
        int colorIndex = Math.floorMod(player.getServer().getTickCount(), cycleLength);

        stack.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            if (isAuroraBlade(state)) {
                int idx = Math.min(colorIndex, COLORS.size() - 1);
                state.setColorCode(COLORS.get(idx));
            }
        });
    }

    /**
     * 从极光色谱中随机取色。供 {@code AuroraShiningArts} 等 SA 调用。
     *
     * @param rng 随机源
     * @return 极光色 RGB int
     */
    public static int getRandomColor(RandomSource rng) {
        if (COLORS.isEmpty()) return 0x00FF7F; // fallback
        return COLORS.get(rng.nextInt(COLORS.size()));
    }

    /** 判断是否为极光刃「映天」。 */
    private static boolean isAuroraBlade(ISlashBladeState state) {
        String key = state.getTranslationKey();
        return key != null && key.equals("item.kablade.aurora_blade");
    }
}

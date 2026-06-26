package com.wjx.kablade.property;

import com.wjx.kablade.Main;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 在 HUD 上渲染"斩无不断:效果" buff 区的叠加层。
 * <p>
 * 对应 1.12.2 的 {@code EffectHUD}，在左下角从底部向上渲染，标题在最上方。
 * <p>
 * 渲染效果：
 * <pre>
 * §9斩无不断:效果         ← 标题（浅蓝）
 * §a神威镇世§r            ← 条目（绿色 CD 进度文字）
 * §a风之结界§r
 * </pre>
 */
public final class PlayerPropertyOverlay {

    public static final String OVERLAY_ID = "property_hud";

    /** 标题 lang key。 */
    public static final String LANG_KEY_TITLE = "prop.kablade.title";

    private static final int PADDING_LEFT = 5;
    private static final int BOTTOM_OFFSET = 15;
    private static final int LINE_HEIGHT = 10;
    private static final int COLOR_TITLE  = 0x0164FFFF; // 1.12.2 标题颜色
    private static final int COLOR_ENTRY  = 0xFFFFFFFF;

    private PlayerPropertyOverlay() {
    }

    /** {@link IGuiOverlay} 渲染入口。 */
    @SuppressWarnings("unused")
    public static void render(ForgeGui forgeGui, GuiGraphics gui, float partialTick,
                              int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        List<PlayerProperty> active = PlayerPropertyRegistry.getActive(player);
        if (active.isEmpty()) return;

        Font font = mc.font;
        int x = PADDING_LEFT;

        // 与 1.12.2 一致：条目从底部起算，标题在条目上方
        int y = screenHeight - BOTTOM_OFFSET - active.size() * LINE_HEIGHT;

        // 标题
        gui.drawString(font, Component.translatable(LANG_KEY_TITLE),
                x, y, COLOR_TITLE);
        y += LINE_HEIGHT;

        // 条目
        for (PlayerProperty prop : active) {
            Component cdText = buildCdText(
                    prop.displayName(),
                    prop.getIntValue(player),
                    prop.maxValue());
            gui.drawString(font, cdText, x, y, COLOR_ENTRY);
            y += LINE_HEIGHT;
        }
    }

    /**
     * CD 进度文本，对应 1.12.2 的 getCDText。
     * 满值直接显示 displayName（保留自身颜色），部分值按比例前段填充色后段白色。
     * 填充色优先 displayName 自有颜色（如 yellow），无自定义色时默认绿色。
     */
    static Component buildCdText(Component displayName, int now, int max) {
        String raw = ChatFormatting.stripFormatting(displayName.getString());
        int size = raw.length();
        double ratio = Math.min((double) now / max, 1.0);
        int insertPos = (int) Math.ceil(size * ratio);
        TextColor customColor = displayName.getStyle().getColor();

        if (insertPos >= size) {
            if (customColor != null) {
                return Component.literal("").append(displayName.copy());
            }
            return Component.literal("").append(displayName.copy().withStyle(ChatFormatting.GREEN));
        }

        // 部分值：填充段沿用 displayName 的自定义颜色，否则绿色
        Style fillStyle = customColor != null
                ? Style.EMPTY.withColor(customColor)
                : Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.GREEN));
        return Component.literal("")
                .append(Component.literal(raw.substring(0, insertPos)).withStyle(fillStyle))
                .append(Component.literal(raw.substring(insertPos)));
    }

    // ——— Overlay 注册（Mod 总线，仅客户端） ———

    @Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
            event.registerBelowAll(OVERLAY_ID, PlayerPropertyOverlay::render);
        }
    }
}

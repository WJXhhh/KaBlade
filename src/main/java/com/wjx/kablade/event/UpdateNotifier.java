package com.wjx.kablade.event;

import com.wjx.kablade.Main;
import com.wjx.kablade.update.UpdateChecker;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端版本更新提示：玩家进入世界后，若 {@link UpdateChecker} 已检测到更新，发一次聊天提示（每次游戏会话只发一次）。
 * 仿照 1.12.2 旧版用客户端 PlayerTick 轮询的做法——等玩家实体就绪后再发，确保消息能显示在聊天框。
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class UpdateNotifier {

    /** 下载/介绍页地址；附一行可点击链接。留空则不显示链接行。 */
    private static final String DOWNLOAD_URL = "https://www.mcmod.cn/download/8128.html";

    private static boolean shown = false;

    private UpdateNotifier() {
    }

    @SubscribeEvent
    public static void onClientPlayerTick(final TickEvent.PlayerTickEvent event) {
        if (shown || event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player == null || !player.level().isClientSide() || !UpdateChecker.updateAvailable) {
            return;
        }
        shown = true;

        player.displayClientMessage(Component.literal(
                "§6§l[斩无不断]§r §b检测到新版本：§6" + UpdateChecker.latestVersion
                        + " §b（当前 §6" + Main.VERSION + "§b）"), false);
        if (!DOWNLOAD_URL.isEmpty()) {
            Component link = Component.literal("§9§n" + DOWNLOAD_URL)
                    .withStyle(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, DOWNLOAD_URL))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("点击前往 MC 百科下载"))));
            player.displayClientMessage(Component.literal("§6§l[斩无不断]§r §b可从 MC 百科下载：")
                    .append(link), false);
        }
    }
}

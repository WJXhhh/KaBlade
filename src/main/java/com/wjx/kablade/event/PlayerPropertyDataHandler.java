package com.wjx.kablade.event;

import com.wjx.kablade.Main;
import com.wjx.kablade.data.PlayerPropertyData;
import com.wjx.kablade.data.PlayerPropertyDataProvider;
import com.wjx.kablade.init.KabladeCapabilities;
import com.wjx.kablade.network.KabladeNetwork;
import com.wjx.kablade.network.PropertyDataSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * 管理 {@link IPlayerPropertyData} capability 的附着、持久化和网络同步。
 * <p>
 * 职责：
 * <ol>
 *   <li>玩家诞生/重生时附着 capability</li>
 *   <li>服务端每 tick 检查脏数据并同步到客户端</li>
 *   <li>玩家死亡重生后重新同步</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = Main.MODID)
public final class PlayerPropertyDataHandler {

    private PlayerPropertyDataHandler() {
    }

    // ——— 附着 ———

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(PlayerPropertyDataProvider.KEY,
                    new PlayerPropertyDataProvider());
        }
    }

    // ——— 服务端每 tick 同步脏数据 ———

    @SubscribeEvent
    public static void onServerTick(TickEvent.PlayerTickEvent event) {
        if (event.player.level().isClientSide()) return;
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                .ifPresent(cap -> {
                    if (cap instanceof PlayerPropertyData data && data.isDirty()) {
                        data.clean();
                        KabladeNetwork.CHANNEL.send(
                                PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
                                new PropertyDataSyncPacket(player.getId(), data.save()));
                    }
                });
    }

    // ——— 玩家重生后重新发送当前数据 ———

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        syncToPlayer(player);
    }

    /** 玩家维度切换后重新发送。 */
    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        syncToPlayer(player);
    }

    /** 玩家登录后发送一次。 */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        syncToPlayer(player);
    }

    private static void syncToPlayer(ServerPlayer player) {
        player.getCapability(com.wjx.kablade.init.KabladeCapabilities.PLAYER_PROPERTY_DATA)
                .ifPresent(cap -> {
                    if (cap instanceof PlayerPropertyData data) {
                        KabladeNetwork.CHANNEL.send(
                                PacketDistributor.PLAYER.with(() -> player),
                                new PropertyDataSyncPacket(player.getId(), data.save()));
                    }
                });
    }
}

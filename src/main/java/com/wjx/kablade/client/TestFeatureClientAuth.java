package com.wjx.kablade.client;

import com.wjx.kablade.Main;
import com.wjx.kablade.network.MessageTestFeatureAuth;
import com.wjx.kablade.util.TestFeatureTokenAuth;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TestFeatureClientAuth {
    private EntityPlayer authenticatedPlayer;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Minecraft.getMinecraft().player == null) {
            return;
        }

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == authenticatedPlayer) {
            return;
        }

        String token = TestFeatureTokenAuth.getLocalTokenForAuth();
        if (token != null) {
            Main.PACKET_HANDLER.sendToServer(new MessageTestFeatureAuth(token));
            authenticatedPlayer = player;
            if (Main.logger != null) {
                Main.logger.info("测试功能 token 验证成功，正在请求服务端授权");
            }
        }
    }
}

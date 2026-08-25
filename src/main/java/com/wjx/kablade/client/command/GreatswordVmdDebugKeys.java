package com.wjx.kablade.client.command;

import com.wjx.kablade.client.renderer.GreatswordVmdAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/** 开发客户端的 VMD 逐帧验收热键。 */
public final class GreatswordVmdDebugKeys {
    private static final int[] FRAMES = {0, 17, 26, 42, 67};
    private static final KeyBinding NUCLEAR = key("key.kablade.vmd.nuclear", Keyboard.KEY_F6);
    private static final KeyBinding VALKYRIE = key("key.kablade.vmd.valkyrie", Keyboard.KEY_F7);
    private static final KeyBinding OFF = key("key.kablade.vmd.off", Keyboard.KEY_F8);
    private static final KeyBinding LEVEL_VIEW = key("key.kablade.vmd.level_view", Keyboard.KEY_F9);
    private int nuclearIndex = -1;
    private int valkyrieIndex = -1;

    private static KeyBinding key(String description, int code) {
        return new KeyBinding(description, KeyConflictContext.IN_GAME, code,
                "key.categories.kablade.debug");
    }

    public static GreatswordVmdDebugKeys register() {
        ClientRegistry.registerKeyBinding(NUCLEAR);
        ClientRegistry.registerKeyBinding(VALKYRIE);
        ClientRegistry.registerKeyBinding(OFF);
        ClientRegistry.registerKeyBinding(LEVEL_VIEW);
        GreatswordVmdDebugKeys keys = new GreatswordVmdDebugKeys();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(keys);
        return keys;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Minecraft.getMinecraft().player == null) return;
        if (NUCLEAR.isPressed()) {
            nuclearIndex = (nuclearIndex + 1) % FRAMES.length;
            show(GreatswordVmdAnimation.Skill.NUCLEAR, FRAMES[nuclearIndex]);
        }
        if (VALKYRIE.isPressed()) {
            valkyrieIndex = (valkyrieIndex + 1) % FRAMES.length;
            show(GreatswordVmdAnimation.Skill.VALKYRIE, FRAMES[valkyrieIndex]);
        }
        if (OFF.isPressed()) {
            GreatswordVmdAnimation.INSTANCE.clearDebugFrame();
            Minecraft.getMinecraft().player.sendMessage(new TextComponentString("大剑 VMD 定帧已关闭"));
        }
        if (LEVEL_VIEW.isPressed()) {
            Minecraft.getMinecraft().player.rotationPitch = 0.0F;
            Minecraft.getMinecraft().player.prevRotationPitch = 0.0F;
            Minecraft.getMinecraft().player.sendMessage(new TextComponentString("大剑 VMD 验收视线：水平"));
        }
    }

    private static void show(GreatswordVmdAnimation.Skill skill, int frame) {
        GreatswordVmdAnimation.INSTANCE.setDebugFrame(skill, frame);
        Minecraft.getMinecraft().player.sendMessage(new TextComponentString(
                "大剑 VMD 定帧：" + skill.name().toLowerCase() + " / " + frame));
    }
}

package com.wjx.kablade.event;

import com.wjx.kablade.gui.EffectHUD;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber
public class RenderGui {

    public static final EffectHUD effecthud=new EffectHUD();

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onGuiRender(RenderGameOverlayEvent event){
        if(Minecraft.getMinecraft().player.getHealth()>=0){
            effecthud.render(event);
        }
    }
}

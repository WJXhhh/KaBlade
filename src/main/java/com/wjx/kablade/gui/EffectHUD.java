package com.wjx.kablade.gui;

import com.google.common.collect.Lists;
import com.wjx.kablade.util.KaBladePlayerProp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import java.util.List;

public class EffectHUD extends Gui {

    private final Minecraft mc;
    EntityPlayer player = Minecraft.getMinecraft().player;

    private static List<String> aliveBuff = Lists.newArrayList();



    public EffectHUD(){
        mc=Minecraft.getMinecraft();
    }

    public void render(RenderGameOverlayEvent event){
        if(player!=null){
            aliveBuff.clear();
            ScaledResolution scaledresolution = new ScaledResolution(mc);
            int kd = scaledresolution.getScaledWidth();        //宽度
            int gd = scaledresolution.getScaledHeight();        //高度
            NBTTagCompound tag = KaBladePlayerProp.getPropCompound(player);
            for (String props : KaBladePlayerProp.buffs) {
                if(tag.hasKey(props)){
                    if (tag.getInteger(props) > 1) {
                        aliveBuff.add(props);
                    }
                }
            }
            int start = gd-15;
            for (String s : aliveBuff) {
                drawString(mc.fontRenderer, I18n.format(KaBladePlayerProp.getTrans(s)), 5, start, getColorFromRGB(1, 255, 255, 255));
                start -= 10;
            }
            if(!aliveBuff.isEmpty())
            {
                drawString(mc.fontRenderer, I18n.format("prop.title"), 5, start, getColorFromRGB(1, 100, 255, 255));
            }
            aliveBuff.clear();


        }
    }

    public static int getColorFromRGB(int alpha, int red, int green, int blue) {
        int color = alpha << 24;
        color += red << 16;
        color += green << 8;
        color += blue;
        return color;
    }
}

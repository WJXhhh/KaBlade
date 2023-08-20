package com.wjx.kablade.util.special_render;

import com.google.common.collect.Lists;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;

public class MagChaosBladeEffectRenderer {
    public static ArrayList<MagChaosBladeEffectRenderer> magChaosBladeEffectRenderers = Lists.newArrayList();
    public double playerX;
    public double playerY;
    public double playerZ;
    public int exitTick;

    public float playerYaw;
    public MagChaosBladeEffectRenderer(EntityPlayer player){
        playerX = player.posX;
        playerY = player.posY;
        playerZ = player.posZ;
        playerYaw = player.rotationYaw;
    }
}

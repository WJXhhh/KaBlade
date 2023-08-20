package com.wjx.kablade.util.special_render;

import com.google.common.collect.Lists;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;

public class MagChaosBladeEffectRenderer {
    public static ArrayList<MagChaosBladeEffectRenderer> magChaosBladeEffectRenderers = Lists.newArrayList();
    public int playerID;
    public int exitTick;
    public MagChaosBladeEffectRenderer(EntityPlayer player){
        playerID = player.getEntityId();
        exitTick = 4;
    }

    public MagChaosBladeEffectRenderer(int idIn){
        playerID = idIn;
        exitTick = 4;
    }
}

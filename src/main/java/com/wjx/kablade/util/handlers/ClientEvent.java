package com.wjx.kablade.util.handlers;

import com.wjx.kablade.Entity.EntityRaikiriBlade;
import com.wjx.kablade.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_SMOOTH;
import static org.lwjgl.opengl.GL20.*;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientEvent {
    @SubscribeEvent
    public static void onPlayerRender(RenderPlayerEvent.Post event) {
        /*Main.ModHelper.sendMessageToAll("rending");
        World world = event.getEntityPlayer().world;
        //EntityRaikiriBlade
        {
            ArrayList<Entity> list = (ArrayList<Entity>) PlayerThrowableHandler.getThrowableEntityForPlayer(world,event.getEntityPlayer(),EntityRaikiriBlade.class);
            if (!list.isEmpty()){
                EntityRaikiriBlade raikiriBlade = (EntityRaikiriBlade) list.get(0);
                raikiriBlade.followX = event.getX();
                raikiriBlade.followY = event.getY();
                raikiriBlade.followZ = event.getZ();
                raikiriBlade.followPlayer();
            }

        }*/
    }
}


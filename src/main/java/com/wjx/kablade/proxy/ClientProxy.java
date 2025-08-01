package com.wjx.kablade.proxy;

import com.wjx.kablade.Entity.Render.Layer.LayerFreeze;
import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.util.ParticleManager;
import com.wjx.kablade.util.handlers.RenderHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.util.Map;
import java.util.Objects;

public class ClientProxy extends CommonProxy{



    @Override
    public void registerItemRenderer(Item item, int meta, String id) {
        ModelLoader.setCustomModelResourceLocation(item,meta,new ModelResourceLocation(Objects.requireNonNull(item.getRegistryName()),id));
    }

    @Override
    public void preInit(FMLPreInitializationEvent event){
        super.preInit(event);


        if(Loader.isModLoaded("flammpfeil.slashblade"))
        {
            BladeProxy.ClientLoader();
        }
        RenderHandler.registerEntityRenders();
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        Minecraft.getMinecraft().getRenderManager().entityRenderMap.values().forEach(r -> {
            if (r instanceof RenderLivingBase) {
                attachRenderLayers((RenderLivingBase<?>) r);
            }
        });

        Map<String, RenderPlayer> skinMap = Minecraft.getMinecraft().getRenderManager().getSkinMap();
        attachRenderLayers((RenderLivingBase<?>) skinMap.get("default"));
        attachRenderLayers((RenderLivingBase<?>) skinMap.get("slim"));
    }

    private static <T extends EntityLivingBase> void attachRenderLayers(RenderLivingBase<T> renderer) {
        renderer.addLayer(new LayerFreeze(renderer));
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        ParticleManager.registerParticles();
    }
}

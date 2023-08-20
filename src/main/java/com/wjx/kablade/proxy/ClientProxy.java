package com.wjx.kablade.proxy;

import com.wjx.kablade.Entity.Render.Layer.LayerFreeze;
import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.network.*;
import com.wjx.kablade.util.Reference;
import com.wjx.kablade.util.handlers.RenderHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;

import static com.wjx.kablade.Main.PACKET_HANDLER;

public class ClientProxy extends CommonProxy{



    @Override
    public void registerItemRenderer(Item item, int meta, String id) {
        ModelLoader.setCustomModelResourceLocation(item,meta,new ModelResourceLocation(item.getRegistryName(),id));
    }

    @Override
    public void registerVariantRenderer(Item item,int meta,String filename,String id){
        ModelLoader.setCustomModelResourceLocation(item,meta,new ModelResourceLocation(new ResourceLocation(Reference.MODID,filename),id));
    }

    @Override
    public void preInit(FMLPreInitializationEvent event){
        super.preInit(event);

        PACKET_HANDLER.registerMessage(MessageHandlerAddPotion.class, MessageAddPotion.class,1, Side.CLIENT);
        PACKET_HANDLER.registerMessage(MessageHandlerSlashPotion.class, MessageSlashPotion.class,2,Side.CLIENT);
        PACKET_HANDLER.registerMessage(MessageHandlerSpawnParticle.class,MessageSpawnParticle.class,3,Side.CLIENT);
        PACKET_HANDLER.registerMessage(MessageHandlerUpdateKaBladeProp.class,MessageUpdateKaBladeProp.class,4,Side.CLIENT);
        PACKET_HANDLER.registerMessage(MessageHandlerUpdateKaBladePlayerProp.class,MessageUpdateKaBladePlayerProp.class,5,Side.CLIENT);
        PACKET_HANDLER.registerMessage(MessageHandlerResetSend.class,MessageResetSend.class,6,Side.CLIENT);
        PACKET_HANDLER.registerMessage(MessageHandlerDizuiKuo.class,MessageDizuiKuo.class,7,Side.CLIENT);
        PACKET_HANDLER.registerMessage(MessageHandlerMagChaosBladeEffectUpdate.class, MessageMagChaosBladeEffectUpdate.class,8,Side.CLIENT);

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

    }
}

package com.wjx.kablade.proxy;

import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.SlashBlade.Util.ItemSlashUtil;
import com.wjx.kablade.util.Reference;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

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
        if(Loader.isModLoaded("flammpfeil.slashblade"))
        {
            BladeProxy.ClientLoader();

        }
    }
}

package com.wjx.kablade.SlashBlade.Util;

import com.google.common.collect.Lists;
import mods.flammpfeil.slashblade.client.model.BladeModel;
import mods.flammpfeil.slashblade.tileentity.DummyTileEntity;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

public class ItemSlashUtil {
    public static final ModelResourceLocation modelLoc = new ModelResourceLocation("flammpfeil.slashblade:model/named/blade.obj");

    public static final List<Item> KAITEMBLADE = Lists.newArrayList();

    public ItemSlashUtil(){
        MinecraftForge.EVENT_BUS.register(this);

        for (Item blade : KAITEMBLADE){
            registerBlade(blade);
        }
    }

    public void registerBlade(Item blade){
        ModelLoader.setCustomModelResourceLocation(blade, 0, modelLoc);
        ForgeHooksClient.registerTESRItemStack(blade, 0, DummyTileEntity.class);
    }


    @SubscribeEvent
    public void onModelBake(ModelBakeEvent event){
        event.getModelRegistry().putObject(modelLoc, new BladeModel());
    }
}

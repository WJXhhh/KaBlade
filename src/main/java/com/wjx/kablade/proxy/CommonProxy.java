package com.wjx.kablade.proxy;

import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.capability.CapabilityLoader;
import com.wjx.kablade.event.WorldEvent;
import com.wjx.kablade.init.EnchantmentInit;
import com.wjx.kablade.network.*;
import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

import static com.wjx.kablade.Main.PACKET_HANDLER;

public class CommonProxy{

    public void registerMessage(){
        PACKET_HANDLER.registerMessage(MessageRemoteLightingHandler.class, MessageRemoteLighting.class,0, Side.SERVER);

    }

    public void registerItemRenderer(Item item, int meta, String id){

    }
    public void registerVariantRenderer(Item item, int meta, String filename, String id){}

    public void init(FMLInitializationEvent event){

        new WorldEvent();

    }

    public void preInit(FMLPreInitializationEvent event) {


        MinecraftForge.EVENT_BUS.register(this);
        EnchantmentInit.registerEnchantments();
        new CapabilityLoader(event);
        if(Loader.isModLoaded("flammpfeil.slashblade")){
            //SlashEvent(this);
            /*if(Loader.isModLoaded("the_golden_autumn")){
                Main.TABKABLADE_BLADES_GOD = new tabkablade_bladesgod("tabkablade_bladesgod");
                Main.TABKABLADE_BLADES_HONKAI = new tabkablade_honkai("tabkablade_honkai");
            }*/
            BladeProxy.CommonLoader(this);
        }
        registerMessage();
    }

    public void postInit(FMLPostInitializationEvent event){

    }
}

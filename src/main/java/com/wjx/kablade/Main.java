package com.wjx.kablade;

import com.wjx.kablade.creativeTab.tabkablade;
import com.wjx.kablade.creativeTab.tabkablade_blades;
import com.wjx.kablade.creativeTab.tabkablade_bladesgod;
import com.wjx.kablade.creativeTab.tabkablade_honkai;
import com.wjx.kablade.init.EntityInit;
import com.wjx.kablade.network.MessageRemoteLighting;
import com.wjx.kablade.network.MessageRemoteLightingHandler;
import com.wjx.kablade.proxy.CommonProxy;
import com.wjx.kablade.util.Reference;
import com.wjx.kablade.util.handlers.RenderHandler;
import mods.flammpfeil.slashblade.SlashBlade;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;

@Mod(modid = Main.MODID, name = Main.NAME, version = Main.VERSION)
public class Main
{
    public static final String MODID = "kablade";
    public static final String NAME = "Ka Blades";
    public static final String VERSION = "0.1";

    public static final SimpleNetworkWrapper PACKET_HANDLER = NetworkRegistry.INSTANCE.newSimpleChannel("kablade");

    public static String bladestr= SlashBlade.modid;

    public static Logger logger;

    public static CreativeTabs TABKABLADE = null;
    public static CreativeTabs TABKABLADE_BLADES = null;
    public static CreativeTabs TABKABLADE_BLADES_GOD = null;
    public static CreativeTabs TABKABLADE_BLADES_HONKAI = null;

    /*static {
        if(Loader.isModLoaded("the_golden_autumn")){
            TABKABLADE_BLADES_GOD = new tabkablade_bladesgod("tabkablade_bladesgod");
            TABKABLADE_BLADES_HONKAI = new tabkablade_honkai("tabkablade_honkai");
        }
    }*/
    private void sseee(){
        TABKABLADE = new tabkablade();
        TABKABLADE_BLADES = new tabkablade_blades("tabkablade_blades");
        setTab();
    }



    private void setTab(){
        if(Loader.isModLoaded("the_golden_autumn"))
        {
            TABKABLADE_BLADES_GOD = new tabkablade_bladesgod("tabkablade_bladesgod");
            TABKABLADE_BLADES_HONKAI = new tabkablade_honkai("tabkablade_honkai");
        }

    }
    @Mod.Instance
    public static Main instance;

    @SidedProxy(clientSide = Reference.CLIENT,serverSide = Reference.COMMON)
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        sseee();
        logger = event.getModLog();

        proxy.preInit(event);


        EntityInit.registerEntity();
        RenderHandler.registerEntityRenders();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)

    {
        proxy.init(event);
        registerMessage();
        // some example code
        //logger.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event){

    }

    public void registerMessage(){
        PACKET_HANDLER.registerMessage(MessageRemoteLightingHandler.class, MessageRemoteLighting.class,0, Side.SERVER);
    }
}

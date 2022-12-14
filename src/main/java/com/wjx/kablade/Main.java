package com.wjx.kablade;

import com.google.common.collect.Lists;
import com.wjx.kablade.creativeTab.*;
import com.wjx.kablade.event.OreGen;
import com.wjx.kablade.event.WorldEvent;
import com.wjx.kablade.init.*;
import com.wjx.kablade.network.*;
import com.wjx.kablade.proxy.CommonProxy;
import com.wjx.kablade.util.Reference;
import mods.flammpfeil.slashblade.SlashBlade;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Biomes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.List;

@Mod(modid = Main.MODID, name = Main.NAME, version = Main.VERSION)
public class Main
{
    public static final String MODID = "kablade";
    public static final String NAME = "Ka Blades";
    public static final String VERSION = "0.9";

    public static final SimpleNetworkWrapper PACKET_HANDLER = NetworkRegistry.INSTANCE.newSimpleChannel("kablade");

    public static String bladestr= SlashBlade.modid;

    public static boolean isBladePostLoad = false;

    public static Logger logger;

    public static CreativeTabs TABKABLADE = null;
    public static CreativeTabs TABKABLADE_BLADES = null;
    public static CreativeTabs TABKABLADE_BLADES_GOD = null;
    public static CreativeTabs TABKABLADE_BLADES_HONKAI = null;
    public static CreativeTabs TABKABLADE_ORE = null;

    /*static {
        if(Loader.isModLoaded("the_golden_autumn")){
            TABKABLADE_BLADES_GOD = new tabkablade_bladesgod("tabkablade_bladesgod");
            TABKABLADE_BLADES_HONKAI = new tabkablade_honkai("tabkablade_honkai");
        }
    }*/
    private void sseee(){
        TABKABLADE = new tabkablade();
        TABKABLADE_BLADES = new tabkablade_blades("tabkablade_blades");
        TABKABLADE_ORE = new tabOre();
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

        GameRegistry.registerWorldGenerator(new OreGen(),5);
        PotionInit.registerPotions();

        EntityInit.registerEntity();
        WorldEvent.loadEvent();
    }

    public static String GetUrlVersion;
    public static boolean YesUpdate = false;

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        proxy.init(event);
        registerMessage();
        if (!Loader.isModLoaded("networkmod"))
        {try{
            GetUrlVersion = getUpdateInfo.gettextfromurl("https://pastebin.com/raw/We9S3fmB").get(0);

            if(GetUrlVersion!=null){
                if(Float.parseFloat(VERSION)<Float.parseFloat(GetUrlVersion)){
                    YesUpdate = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }}
        OreDicHandler.registerOreDic();
        GameRegistry.addSmelting(new ItemStack(BlockInit.CHROMIUM_ORE,1),new ItemStack(ItemInit.CHROMIUM_INGOT,1),0.7f);
        GameRegistry.addSmelting(new ItemStack(BlockInit.MOLYBDENITE,1),new ItemStack(ItemInit.MOLYBDENUM_INGOT,1),0.7f);
        GameRegistry.addSmelting(new ItemStack(ItemInit.CRUDE_CHROMOLY,1),new ItemStack(ItemInit.CHROMOLY_INGOT,1),0.6f);
        GameRegistry.addSmelting(new ItemStack(ItemInit.GRAVITY_NUGGET,9),new ItemStack(ItemInit.GRAVITY_CRYSTAL,1),1f);

        // some example code
        //logger.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
    }

    public static int p = 0;

    @EventHandler
    public void postInit(FMLPostInitializationEvent event){
        proxy.postInit(event);
        p = Enchantment.getEnchantmentID(EnchantmentInit.ENCHANTMENT_SLOW);
    }

    public void registerMessage(){
        PACKET_HANDLER.registerMessage(MessageRemoteLightingHandler.class, MessageRemoteLighting.class,0, Side.SERVER);
        PACKET_HANDLER.registerMessage(MessageHandlerAddPotion.class,MessageAddPotion.class,1,Side.CLIENT);
        PACKET_HANDLER.registerMessage(MessageHandlerSlashPotion.class,MessageSlashPotion.class,2,Side.CLIENT);
        PACKET_HANDLER.registerMessage(MessageHandlerSpawnParticle.class,MessageSpawnParticle.class,3,Side.CLIENT);
    }

    public static class getUpdateInfo{
        static List<String> firstGet = null;
        public static String unicodeToStr(String unicode) {
            StringBuilder sb = new StringBuilder();
            String[] hex = unicode.split("\\\\u");
            for (int i = 1; i < hex.length; i++) {
                int index = Integer.parseInt(hex[i], 16);
                sb.append((char) index);
            }
            return sb.toString();
        }


        public static List<String> gettextfromurl(String url){
            String sTotalString;
            String sCurrentLine;
            List<String> list = Lists.newArrayList();
            sCurrentLine="";
            sTotalString="";
            InputStream l_urlStream;
            try
            {

                java.net.URL l_url = new java.net.URL(url);
                SslUtils.ignoreSsl();
                java.net.HttpURLConnection l_connection = (java.net.HttpURLConnection) l_url.openConnection();
                l_connection.setConnectTimeout(5000);
                l_connection.setReadTimeout(5000);
                l_connection.connect();
                l_urlStream = l_connection.getInputStream();
                java.io.BufferedReader l_reader = new java.io.BufferedReader(new java.io.InputStreamReader(l_urlStream));
                while ((sCurrentLine = l_reader.readLine()) != null) {
                    list.add(sCurrentLine) ;
                }
                l_reader.close();

                return list;
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }


    }
    public static class ModHelper{
        private static final MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        public static void sendMessageToAll(String s){
            server.getPlayerList().sendMessage(new TextComponentString(s));
        }

        public static boolean checkBiome(World world, BlockPos pos,Biome[] biomes){
            Biome b = world.getBiome(pos);
            boolean f = false;
            for (Biome biome : biomes){
                if (b == biome) {
                    f = true;
                    break;
                }
            }
            return f;
        }

        public static NBTTagCompound getPlayerPersistedTag(EntityPlayer player){
            if (!player.getEntityData().hasKey(EntityPlayer.PERSISTED_NBT_TAG)){
                player.getEntityData().setTag(EntityPlayer.PERSISTED_NBT_TAG,new NBTTagCompound());
            }
            return player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        }

        public static Biome[] COLD_BIOMES = {Biomes.COLD_BEACH,Biomes.COLD_TAIGA,Biomes.COLD_TAIGA_HILLS,Biomes.MUTATED_TAIGA_COLD,Biomes.ICE_MOUNTAINS,Biomes.ICE_PLAINS,Biomes.MUTATED_ICE_FLATS,Biomes.FROZEN_RIVER,Biomes.FROZEN_OCEAN};
    }
}

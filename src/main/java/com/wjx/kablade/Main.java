package com.wjx.kablade;

import com.google.common.collect.Lists;
import com.wjx.kablade.creativeTab.*;
import com.wjx.kablade.event.OreGen;
import com.wjx.kablade.event.WorldEvent;
import com.wjx.kablade.init.*;
import com.wjx.kablade.proxy.CommonProxy;
import com.wjx.kablade.util.Reference;
import mods.flammpfeil.slashblade.SlashBlade;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Biomes;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.MinecraftForge;
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
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Mod(modid = Main.MODID, name = Main.NAME, version = Main.VERSION,dependencies = "required-after:flammpfeil.slashblade")
public class Main
{
    public static final String MODID = "kablade";
    public static final String NAME = "Ka Blades";
    public static final String VERSION = "1.7.7";

    public static boolean EnableAllWeapon =true;

    public static boolean EnableAWDemo =false;

    public static final SimpleNetworkWrapper PACKET_HANDLER = NetworkRegistry.INSTANCE.newSimpleChannel("kablade");

    public static String bladestr= SlashBlade.modid;

    public static boolean isBladePostLoad = false;
    public static boolean GALoaded=false;

    public static Logger logger;

    public static boolean hasSendMessage = false;

    public static final UUID UUID_WIND_ENCHANTMENT = UUID.fromString("AC560A99-7A72-C05B-D967-319E456A078C");

    public static CreativeTabs TABKABLADE = null;
    public static CreativeTabs TABKABLADE_BLADES = null;
    public static CreativeTabs TABKABLADE_BLADES_GOD = null;
    public static CreativeTabs TABKABLADE_BLADES_HONKAI = null;
    public static CreativeTabs TABKABLADE_ORE = null;
    public static CreativeTabs TABKABLADE_BLADES_ALLWEAPON = null;

    private void sseee(){
        TABKABLADE = new tabkablade();
        TABKABLADE_BLADES = new tabkablade_blades("tabkablade_blades");
        TABKABLADE_ORE = new tabOre();
        setTab();
    }



    private void setTab(){
        if(Loader.isModLoaded("the_golden_autumn"))
        {
            GALoaded = true;
            TABKABLADE_BLADES_GOD = new tabkablade_bladesgod("tabkablade_bladesgod");
            TABKABLADE_BLADES_HONKAI = new tabkablade_honkai("tabkablade_honkai");
        }
        if(EnableAllWeapon){
            TABKABLADE_BLADES_ALLWEAPON=new CreativeTabs("tabkablade_allweapon") {
                final ItemStack stack=new ItemStack(ItemInit.ICON_AW);
                @Override
                public ItemStack createIcon() {
                    return stack;
                }
            };
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
        //int i;
        proxy.preInit(event);

        GameRegistry.registerWorldGenerator(new OreGen(),5);
        PotionInit.registerPotions();

        EntityInit.registerEntity();
        WorldEvent.loadEvent();


    }

    public static String GetUrlVersion;
    public static boolean YesUpdate = false;

    @SuppressWarnings("CallToPrintStackTrace")
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        proxy.init(event);
        if (!Loader.isModLoaded("networkmod"))
        {
            try{

                Thread t = new Thread(){
                    @Override
                    public void run() {
                        GetUrlVersion = Objects.requireNonNull(getUpdateInfo.gettextfromurl("https://gitee.com/wjx4r/FML_GERB_NetData/raw/master/OtherMODUpdate/KaBlade.txt")).get(2);
                        if(GetUrlVersion!=null){
                            String[] s = VERSION.split("\\.");
                            String[] s1 = GetUrlVersion.split("\\.");

                            if (Integer.parseInt(s1[0]) > Integer.parseInt(s[0])){
                                YesUpdate = true;
                            }
                            else if(Integer.parseInt(s1[0]) == Integer.parseInt(s[0])){
                                if (Integer.parseInt(s1[1]) > Integer.parseInt(s[1])){
                                    YesUpdate = true;
                                }
                                else if(Integer.parseInt(s1[1]) == Integer.parseInt(s[1])){
                                    if (Integer.parseInt(s1[2]) > Integer.parseInt(s[2])){
                                        YesUpdate = true;
                                    }
                                }
                            }
                        }
                        super.run();
                    }
                };
                t.start();
            //GetUrlVersion = Objects.requireNonNull(getUpdateInfo.gettextfromurl("https://pastebin.com/raw/We9S3fmB")).get(2);


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



    @SuppressWarnings("CallToPrintStackTrace")
    public static class getUpdateInfo{


        public static List<String> gettextfromurl(String url){
            String sCurrentLine;
            List<String> list = Lists.newArrayList();
            InputStream l_urlStream;
            try
            {

                java.net.URL l_url = new java.net.URL(url);
                SslUtils.ignoreSsl();
                java.net.HttpURLConnection l_connection = (java.net.HttpURLConnection) l_url.openConnection();
                l_connection.setConnectTimeout(12000);
                l_connection.setReadTimeout(12000);
                l_connection.setRequestMethod("GET");
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

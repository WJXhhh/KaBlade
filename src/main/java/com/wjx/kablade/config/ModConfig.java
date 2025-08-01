package com.wjx.kablade.config;

import com.wjx.kablade.Main;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;

@Mod.EventBusSubscriber(modid = Main.MODID)
public class ModConfig {

    public static Configuration config;

    public static File configFile;

    public static class GeneralConf {
        public static int MOLYBDENITE_SIZE;
        public static int MOLYBDENITE_CHANCE;
        public static int CHROMIUM_SIZE;
        public static int CHROMIUM_CHANCE;
        public static int AURORA_SIZE;
        public static int AURORA_CHANCE;
        public static boolean Enable_New_SA_Id;
        public static String[] TLS_forbidden;
    }


    public static void init(FMLPreInitializationEvent event) {
        // 创建并加载配置文件
        config = new Configuration(event.getSuggestedConfigurationFile());
        configFile = event.getSuggestedConfigurationFile();
        if (!config.getConfigFile().exists()) {
            // 如果配置文件不存在，则创建默认配置文件
            syncConfig();
            config.save();
        } else {
            // 如果配置文件已存在，则加载现有配置
            syncConfig();
        }
    }

    private static void syncConfig() {
        String category = "general";
        config.addCustomCategoryComment(category, "Kablade general configuration");

        GeneralConf.MOLYBDENITE_SIZE = config.getInt(
                "MOLYBDENITE_SIZE", category, 7, 1, 100,
                "Generate MOLYBDENITE ore least size"
        );

        GeneralConf.MOLYBDENITE_CHANCE = config.getInt(
                "MOLYBDENITE_CHANCE", category, 10, 1, 100,
                "Generate Molybdenite ore chance"
        );

        GeneralConf.CHROMIUM_SIZE = config.getInt(
                "CHROMIUM_SIZE", category, 7, 1, 100,
                "Generate CHROMIUM ore least size"
        );

        GeneralConf.CHROMIUM_CHANCE = config.getInt(
                "CHROMIUM_CHANCE", category, 10, 1, 100,
                "Generate CHROMIUM ore chance"
        );

        GeneralConf.AURORA_SIZE = config.getInt(
                "AURORA_SIZE", category, 7, 1, 100,
                "Generate AURORA ore least size"
        );

        GeneralConf.AURORA_CHANCE = config.getInt(
                "AURORA_CHANCE", category, 13, 1, 100,
                "Generate AURORA ore chance"
        );

        GeneralConf.Enable_New_SA_Id = config.getBoolean(
                "Enable_New_SA_Id", category, true,
                "Use new id of Special Attack to avoid conflicting with other mod."
        );

        GeneralConf.TLS_forbidden = config.getStringList("TLS_forbidden",category,new String[]{"kablade"},"To prevent TLS changing some SlashBlade id.");

        if (config.hasChanged()) {
            config.save();
        }
    }

    @SubscribeEvent
    public static void onConfigChanged(final ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(Main.MODID)) {
            syncConfig();
        }
    }
}
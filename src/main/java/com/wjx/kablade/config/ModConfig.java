package com.wjx.kablade.config;

import com.wjx.kablade.Main;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = Main.MODID, category = "")
public class ModConfig {

    @Mod.EventBusSubscriber(modid = Main.MODID)
    private static class EventHandler {

        private EventHandler() {
        }

        @SubscribeEvent
        public static void onConfigChanged(final ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(Main.MODID)) {
                ConfigManager.sync(Main.MODID, Config.Type.INSTANCE);
            }
        }


    }

    @Config.LangKey("config.kablade.general")
    @Config.Comment("Kablade general configuration")
    public static final GeneralConf GeneralConf = new GeneralConf();

    public static class GeneralConf {
        @Config.LangKey("config.kablade.msize")
        @Config.Comment("Generate MOLYBDENITE ore least size")
        public int MOLYBDENITE_SIZE = 7;

        @Config.LangKey("config.kablade.mchance")
        @Config.Comment("Generate Molybdenite ore chance")
        public int MOLYBDENITE_CHANCE = 10;

        @Config.LangKey("config.kablade.csize")
        @Config.Comment("Generate CHROMIUM ore least size")
        public int CHROMIUM_SIZE = 7;

        @Config.LangKey("config.kablade.cchance")
        @Config.Comment("Generate CHROMIUM ore chance")
        public int CHROMIUM_CHANCE = 10;

        @Config.LangKey("config.kablade.asize")
        @Config.Comment("Generate AURORA ore least size")
        public int AURORA_SIZE = 7;

        @Config.LangKey("config.kablade.achance")
        @Config.Comment("Generate AURORA ore chance")
        public int AURORA_CHANCE = 13;
    }
}

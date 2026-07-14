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
        public static boolean Ultra_Effect;
        /** When true: KaBlade blades can be repaired with cobblestone WITHOUT consuming ProudSoul.
         *  When false: only the original SlashBlade repair (which costs ProudSoul) is available. */
        public static boolean KaBladeFreeRepair;
        /** 当启用时，ExtraBotany 的 Gaia III 与空之律者允许携带拔刀剑。 */
        public static boolean ExtraBotanyGaiaAllowSlashBlade;
        /** 当启用时，ExtraBotany 的 Gaia III 与空之律者不再限制玩家装备。 */
        public static boolean ExtraBotanyGaiaDisableEquipmentRestrictions;
        /** 当启用时，在标题界面预热全部拔刀剑模型、纹理及静态 VBO。 */
        public static boolean EnableSlashBladeModelWarmup = true;
        /** 当启用时，使用 KaBlade 的 Shift 锁敌边沿触发与限频替换逻辑。 */
        public static boolean EnableShiftLockTargeting = true;
        /** 锁敌诊断日志与计数，生产环境默认关闭。 */
        public static boolean DebugTargeting;
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

        GeneralConf.Ultra_Effect = config.getBoolean(
                "Ultra_Effect", category, true,
                "Enable Ultra Effect of Render(Some SA Render Effect,such as 'Love is War!'),If you feel the screen is too laggy when using these effects, you can turn off this option to sacrifice aesthetics for smoothness [Recommend:CPU-12400f/5600X,GPU-RTX 1660{MAYBE}]"
        );

        GeneralConf.KaBladeFreeRepair = config.getBoolean(
                "KaBladeFreeRepair", category, true,
                "When enabled, KaBlade blades can be repaired with cobblestone in a crafting table WITHOUT consuming ProudSoul. " +
                "When disabled, only the original SlashBlade repair recipe (which costs ProudSoul) is available for KaBlade blades."
        );

        GeneralConf.ExtraBotanyGaiaAllowSlashBlade = config.getBoolean(
                "ExtraBotanyGaiaAllowSlashBlade", category, true,
                "When enabled, ItemSlashBlade items are allowed in ExtraBotany Gaia III and Herrscher of the Void challenges. " +
                "Other non-Botania items remain restricted unless ExtraBotanyGaiaDisableEquipmentRestrictions is enabled."
        );

        GeneralConf.ExtraBotanyGaiaDisableEquipmentRestrictions = config.getBoolean(
                "ExtraBotanyGaiaDisableEquipmentRestrictions", category, false,
                "When enabled, remove all inventory and equipment restrictions for ExtraBotany Gaia III and Herrscher of the Void. " +
                "This option takes priority over ExtraBotanyGaiaAllowSlashBlade."
        );

        GeneralConf.EnableSlashBladeModelWarmup = config.getBoolean(
                "EnableSlashBladeModelWarmup", category, true,
                "Prewarm all registered SlashBlade models and textures at the title screen and enable the static VBO cache. " +
                "Disable this option to use SlashBlade's original rendering path."
        );

        GeneralConf.EnableShiftLockTargeting = config.getBoolean(
                "EnableShiftLockTargeting", category, true,
                "Use KaBlade's edge-triggered Shift lock-on handling and suppress repeated no-target scans."
        );

        GeneralConf.DebugTargeting = config.getBoolean(
                "DebugTargeting", category, false,
                "Log rate-limited lock-on resolver diagnostics. Keep disabled on production servers unless investigating targeting."
        );
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

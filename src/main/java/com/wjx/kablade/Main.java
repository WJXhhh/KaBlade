package com.wjx.kablade;

import com.mojang.logging.LogUtils;
import com.wjx.kablade.config.KabladeConfig;
import com.wjx.kablade.config.KabladeClientConfig;
import com.wjx.kablade.init.ModBlocks;
import com.wjx.kablade.init.ModComboStates;
import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.init.ModItems;
import com.wjx.kablade.init.ModMobEffects;
import com.wjx.kablade.init.ModSpecialEffects;
import com.wjx.kablade.blades.ModSlashArts;
import com.wjx.kablade.blades.BladeLoader;
import com.wjx.kablade.property.KabladeBladeProperties;
import com.wjx.kablade.network.KabladeNetwork;
import com.wjx.kablade.util.creative_tab.CreativeTabBuilder;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import org.slf4j.Logger;

/**
 * Kablade is an addon for SlashBlade Resharped.
 *
 * Entry point: wires up the lifecycle listeners and registries, and logs basic
 * mod / dependency information at startup. Register new content (items, enchantments,
 * capabilities, etc) by adding the matching DeferredRegister to the mod event bus below.
 */
@Mod(Main.MODID)
public final class Main {

    // The three literals below are kept in sync with gradle.properties automatically by the
    // 'syncMainConstants' Gradle task (it rewrites them before each compile). Edit gradle.properties
    // (mod_id / mod_name / mod_version) are auto-synced from gradle.properties at each build.

    /** This mod's id. Matches modId in mods.toml and the mixin package owner. */
    public static final String MODID = "kablade";

    /** Human-readable mod name. */
    public static final String MOD_NAME = "Kablade";

    /** Mod version. */
    public static final String VERSION = "2.0.12-a";

    /** Shared logger. */
    public static final Logger LOGGER = LogUtils.getLogger();


    //Creative tab
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TAB_REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Main.MODID);

    public static final CreativeTabBuilder TAB_KABLADE = new CreativeTabBuilder(CreativeModeTab.builder().title(Component.translatable("itemGroup." + Main.MODID)).icon(() -> new ItemStack(ModItems.MAIN_MATER.get())));

    public static final CreativeTabBuilder TAB_KABLADE_NOTED = new CreativeTabBuilder(CreativeModeTab.builder().title(Component.translatable("itemGroup." + Main.MODID + ".noted")).icon(() -> new ItemStack(ModItems.NOTED.get())));

    public static final CreativeTabBuilder TAB_KABLADE_HONKAI = new CreativeTabBuilder(CreativeModeTab.builder().title(Component.translatable("itemGroup." + Main.MODID + ".honkai")).icon(() -> new ItemStack(ModItems.HONKAI_ICON.get())));

    public static final CreativeTabBuilder TAB_KABLADE_SP_LIGHT = new CreativeTabBuilder(CreativeModeTab.builder().title(Component.translatable("itemGroup." + Main.MODID + ".sp_light")).icon(() -> new ItemStack(ModItems.SP_LIGHT_ICON.get())));

    public static final CreativeTabBuilder TAB_KABLADE_ALLWEAPON = new CreativeTabBuilder(CreativeModeTab.builder().title(Component.translatable("itemGroup." + Main.MODID + ".allweapon")).icon(() -> new ItemStack(ModItems.ALLWEAPON_ICON.get())));

    //Load
    public Main(FMLJavaModLoadingContext context) {
        final IEventBus modBus = context.getModEventBus();

        //Re


        // --- Lifecycle setup ---
        modBus.addListener(this::commonSetup);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(this::clientSetup);
        }

        TAB_KABLADE_NOTED.addDisplayItems(BladeLoader::fillCreativeTab);
        TAB_KABLADE_HONKAI.addDisplayItems(BladeLoader::fillCreativeTabHonkai);
        TAB_KABLADE_SP_LIGHT.addDisplayItems(BladeLoader::fillCreativeTabSPLight);
        TAB_KABLADE_ALLWEAPON.addDisplayItems(BladeLoader::fillCreativeTabAllWeapon);
        // Normal items (including RIMMED_EARTH) register into TAB_KABLADE via ModItems -- not duplicated.
        TAB_KABLADE.registerTab("tab_1_kablade", CREATIVE_TAB_REGISTRY);
        TAB_KABLADE_NOTED.registerTab("tab_2_noted", CREATIVE_TAB_REGISTRY);
        TAB_KABLADE_HONKAI.registerTab("tab_3_honkai", CREATIVE_TAB_REGISTRY);
        TAB_KABLADE_SP_LIGHT.registerTab("tab_4_sp_light", CREATIVE_TAB_REGISTRY);
        TAB_KABLADE_ALLWEAPON.registerTab("tab_5_allweapon", CREATIVE_TAB_REGISTRY);

        // --- Config ---
        // --- Config --- COMMON config for global defaults; players can edit config/kablade-common.toml
        // context is an FMLJavaModLoadingContext; call registerConfig directly, no static get().
        context.registerConfig(ModConfig.Type.COMMON, KabladeConfig.SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, KabladeClientConfig.SPEC);

        // --- Content registration ---
        ModItems.ITEM_REGISTRY.register(modBus);
        ModBlocks.BLOCK_REGISTRY.register(modBus);
        ModComboStates.REGISTRY.register(modBus);
        ModSlashArts.REGISTRY.register(modBus);
        ModEntities.REGISTRY.register(modBus);
        ModSpecialEffects.REGISTRY.register(modBus);
        ModMobEffects.REGISTRY.register(modBus);
        CREATIVE_TAB_REGISTRY.register(modBus);

        // --- Forge (gameplay) event bus ---
        // Register here once this class (or a dedicated handler) has @SubscribeEvent methods:
        //   MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("[{}] constructed (dist={})", MODID, FMLEnvironment.dist);

        // BladeAttributeOverride 使用了 SlashBlade 1.9+ 的 API，在 1.8 下静默跳过
        com.wjx.kablade.event.BladeAttributeOverride.tryRegister();
        com.wjx.kablade.slasharts.PiercingChargeEvents.tryRegister();
    }

    /** Runs after all mods are constructed -- safe place to read other mods metadata. */
    private void commonSetup(final FMLCommonSetupEvent event) {
        com.wjx.kablade.event.AuroraColorCycling.init();
        event.enqueueWork(() -> {
            KabladeNetwork.register();
            KabladeBladeProperties.register();
        });
    }

    /** Client-only setup (renderers, key mappings, etc.). */
    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.debug("[{}] client setup", MODID);
        // Fetches remote version; UpdateNotifier shows on login screen if newer version exists.
        com.wjx.kablade.update.UpdateChecker.start(VERSION);
    }

    /** Builds a ResourceLocation under this mod's namespace (e.g. {@code id("my_blade")}). */
}

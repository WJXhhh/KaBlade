package com.wjx.kablade;

import com.mojang.logging.LogUtils;
import com.wjx.kablade.config.KabladeConfig;
import com.wjx.kablade.init.ModBlocks;
import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.init.ModItems;
import com.wjx.kablade.init.ModSpecialEffects;
import com.wjx.kablade.blades.ModSlashArts;
import com.wjx.kablade.blades.BladeLoader;
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
    // (mod_id / mod_name / mod_version) — 这里的值会在下次构建时自动同步。

    /** This mod's id. Matches modId in mods.toml and the mixin package owner. */
    public static final String MODID = "kablade";

    /** Human-readable mod name. */
    public static final String MOD_NAME = "Kablade";

    /** Mod version. */
    public static final String VERSION = "2.0.2-a";

    /** Shared logger. */
    public static final Logger LOGGER = LogUtils.getLogger();


    //Creative tab
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TAB_REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Main.MODID);

    public static final CreativeTabBuilder TAB_KABLADE = new CreativeTabBuilder(CreativeModeTab.builder().title(Component.translatable("itemGroup." + Main.MODID)).icon(() -> new ItemStack(ModItems.MAIN_MATER.get())));

    public static final CreativeTabBuilder TAB_KABLADE_NOTED = new CreativeTabBuilder(CreativeModeTab.builder().title(Component.translatable("itemGroup." + Main.MODID + ".noted")).icon(() -> new ItemStack(ModItems.NOTED.get())));

    public static final CreativeTabBuilder TAB_KABLADE_HONKAI = new CreativeTabBuilder(CreativeModeTab.builder().title(Component.translatable("itemGroup." + Main.MODID + ".honkai")).icon(() -> new ItemStack(ModItems.KABLADE_HONKAI_BLADE.get())));

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
        // 普通物品（如 RIMMED_EARTH）走 ModItems.registerBlockItem 找到 TAB_KABLADE，避免在此重复添加。
        TAB_KABLADE.registerTab("tab_kablade", CREATIVE_TAB_REGISTRY);
        TAB_KABLADE_NOTED.registerTab("tab_kablade_noted", CREATIVE_TAB_REGISTRY);
        TAB_KABLADE_HONKAI.registerTab("tab_kablade_honkai", CREATIVE_TAB_REGISTRY);

        // --- Config ---
        // COMMON 配置：全服共享/默认比率，可在运行前于 config/kablade-common.toml 编辑。
        // context 参数是 ForgeLoadingContext 的子类，直接调实例方法 registerConfig，无需再走静态 get()。
        context.registerConfig(ModConfig.Type.COMMON, KabladeConfig.SPEC);

        // --- Content registration ---
        ModItems.ITEM_REGISTRY.register(modBus);
        ModBlocks.BLOCK_REGISTRY.register(modBus);
        ModSlashArts.REGISTRY.register(modBus);
        ModEntities.REGISTRY.register(modBus);
        ModSpecialEffects.REGISTRY.register(modBus);
        CREATIVE_TAB_REGISTRY.register(modBus);

        // --- Forge (gameplay) event bus ---
        // Register here once this class (or a dedicated handler) has @SubscribeEvent methods:
        //   MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("[{}] constructed (dist={})", MODID, FMLEnvironment.dist);
    }

    /** Runs after all mods are constructed — safe place to read other mods' metadata. */
    private void commonSetup(final FMLCommonSetupEvent event) {
        com.wjx.kablade.event.AuroraColorCycling.init();
        event.enqueueWork(KabladeNetwork::register);
    }

    /** Client-only setup (renderers, key mappings, etc.). */
    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.debug("[{}] client setup", MODID);
        // 后台获取远程版本号，有更新时由 UpdateNotifier 在进入主界面后提示玩家。
        com.wjx.kablade.update.UpdateChecker.start(VERSION);
    }

    /** Builds a ResourceLocation under this mod's namespace (e.g. {@code id("my_blade")}). */
}

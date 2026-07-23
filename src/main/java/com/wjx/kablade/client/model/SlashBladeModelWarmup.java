package com.wjx.kablade.client.model;

import com.wjx.kablade.Main;
import com.wjx.kablade.config.ModConfig;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.client.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.ResourceLocationRaw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 对齐 1.20.1 版本的激进预热：标题界面一次性加载全部已注册刀模和纹理。
 */
public final class SlashBladeModelWarmup {
    private static final ResourceLocationRaw DEFAULT_MODEL =
            new ResourceLocationRaw("flammpfeil.slashblade", "model/blade.obj");
    private static final ResourceLocationRaw DEFAULT_TEXTURE =
            new ResourceLocationRaw("flammpfeil.slashblade", "model/blade.png");

    private boolean warmed;
    private boolean running;
    private boolean rewarmAfterStitch;
    private WarmupTask pendingWarmup;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (ModConfig.GeneralConf.EnableSlashBladeModelWarmup
                && !warmed && event.getGui() instanceof GuiMainMenu) {
            warmup("title-screen");
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTextureStitchPre(TextureStitchEvent.Pre event) {
        rewarmAfterStitch = warmed || pendingWarmup != null;
        warmed = false;
        pendingWarmup = null;
        StaticBladeMeshCache.clear();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTextureStitchPost(TextureStitchEvent.Post event) {
        if (ModConfig.GeneralConf.EnableSlashBladeModelWarmup && rewarmAfterStitch) {
            rewarmAfterStitch = false;
            warmup("resource-reload");
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (!ModConfig.GeneralConf.EnableSlashBladeModelWarmup) {
            if (warmed || pendingWarmup != null || StaticBladeMeshCache.getCachedBytes() > 0L) {
                warmed = false;
                rewarmAfterStitch = false;
                pendingWarmup = null;
                StaticBladeMeshCache.clear();
                Main.logger.info("SlashBlade model warmup disabled; static model cache cleared");
            }
            return;
        }

        StaticBladeMeshCache.refreshPolicy();

        if (pendingWarmup != null) {
            BladeRenderHardwareProfile.Snapshot profile = StaticBladeMeshCache.getHardwareProfile();
            processWarmup(pendingWarmup, profile.getModelWarmupBatchSize(),
                    profile.getTextureWarmupBatchSize());
            return;
        }

        if (!warmed) {
            if (minecraft.currentScreen instanceof GuiMainMenu || minecraft.world != null) {
                warmup("config-enabled");
            }
        }
    }

    private void warmup(String reason) {
        if (!ModConfig.GeneralConf.EnableSlashBladeModelWarmup || running || pendingWarmup != null) {
            return;
        }
        running = true;
        Main.logger.info("[KaBlade]正在进行拔刀剑模型预热");
        try {
            Map<String, ItemStack> stacks = collectBladeStacks();
            Set<ResourceLocationRaw> models = new LinkedHashSet<ResourceLocationRaw>();
            Set<ResourceLocationRaw> textures = new LinkedHashSet<ResourceLocationRaw>();
            models.add(DEFAULT_MODEL);
            models.add(BladeModelManager.resourceDurabilityModel);
            textures.add(DEFAULT_TEXTURE);
            textures.add(BladeModelManager.resourceDurabilityTexture);

            for (ItemStack stack : stacks.values()) {
                if (stack.isEmpty() || !(stack.getItem() instanceof ItemSlashBlade)) {
                    continue;
                }
                ItemSlashBlade blade = (ItemSlashBlade) stack.getItem();
                models.add(blade.getModelLocation(stack));
                textures.add(blade.getModelTexture(stack));
            }

            BladeRenderHardwareProfile.Snapshot profile = StaticBladeMeshCache.getHardwareProfile();
            WarmupTask task = new WarmupTask(reason, stacks.size(),
                    new ArrayList<ResourceLocationRaw>(models),
                    new ArrayList<ResourceLocationRaw>(textures));

            if (profile.usesIncrementalWarmup()) {
                pendingWarmup = task;
                Main.logger.info(
                        "SlashBlade model warmup will run incrementally: hardwareTier={}, modelsPerTick={}, texturesPerTick={}",
                        profile.getTier(), profile.getModelWarmupBatchSize(), profile.getTextureWarmupBatchSize());
            } else {
                processWarmup(task, Integer.MAX_VALUE, Integer.MAX_VALUE);
            }
        } finally {
            running = false;
        }
    }

    private void processWarmup(WarmupTask task, int modelBudget, int textureBudget) {
        BladeModelManager manager = BladeModelManager.getInstance();
        int processedModels = 0;
        while (task.modelIndex < task.models.size() && processedModels < modelBudget) {
            ResourceLocationRaw modelLocation = task.models.get(task.modelIndex++);
            processedModels++;
            try {
                WavefrontObject model = manager.getModel(modelLocation);
                task.warmedVbos += StaticBladeMeshCache.prewarmGuiParts(model);
            } catch (RuntimeException ex) {
                task.failedModels++;
                Main.logger.debug("Failed to prewarm SlashBlade model {}", modelLocation, ex);
            }
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        int processedTextures = 0;
        while (task.textureIndex < task.textures.size() && processedTextures < textureBudget) {
            ResourceLocationRaw texture = task.textures.get(task.textureIndex++);
            processedTextures++;
            try {
                minecraft.getTextureManager().bindTexture(texture);
            } catch (RuntimeException ex) {
                task.failedTextures++;
                Main.logger.debug("Failed to prewarm SlashBlade texture {}", texture, ex);
            }
        }

        if (task.modelIndex < task.models.size() || task.textureIndex < task.textures.size()) {
            return;
        }

        pendingWarmup = null;
        warmed = true;
        long elapsedMs = (System.nanoTime() - task.started) / 1_000_000L;
        BladeRenderHardwareProfile.Snapshot profile = StaticBladeMeshCache.getHardwareProfile();
        Main.logger.info(
                "SlashBlade model warmup finished: reason={}, blades={}, models={}, textures={}, vbos={}, vboMiB={}, vboLimitMiB={}, vboMaxMiB={}, hardwareTier={}, vboEnabled={}, incremental={}, failedModels={}, failedTextures={}, elapsed={}ms",
                task.reason, task.bladeCount, task.models.size(), task.textures.size(), task.warmedVbos,
                StaticBladeMeshCache.getCachedBytes() / 1024L / 1024L,
                StaticBladeMeshCache.getCacheLimitBytes() / 1024L / 1024L,
                StaticBladeMeshCache.getMaximumCacheBytes() / 1024L / 1024L,
                profile.getTier(), profile.isVboEnabled(), profile.usesIncrementalWarmup(),
                task.failedModels, task.failedTextures, elapsedMs);
    }

    private static Map<String, ItemStack> collectBladeStacks() {
        Map<String, ItemStack> result = new LinkedHashMap<String, ItemStack>();

        for (Map.Entry<ResourceLocationRaw, ItemStack> entry : SlashBlade.BladeRegistry.entrySet()) {
            addStack(result, entry.getKey().toString(), entry.getValue());
        }
        for (String bladeName : new ArrayList<String>(ItemSlashBladeNamed.NamedBlades)) {
            addStack(result, bladeName, SlashBlade.getCustomBlade(bladeName));
        }
        for (Item item : ForgeRegistries.ITEMS.getValuesCollection()) {
            if (item instanceof ItemSlashBlade) {
                addStack(result, String.valueOf(item.getRegistryName()), new ItemStack(item));
            }
        }
        return result;
    }

    private static void addStack(Map<String, ItemStack> target, String key, ItemStack stack) {
        if (stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemSlashBlade) {
            target.put(key, stack.copy());
        }
    }

    private static final class WarmupTask {
        private final String reason;
        private final int bladeCount;
        private final List<ResourceLocationRaw> models;
        private final List<ResourceLocationRaw> textures;
        private final long started = System.nanoTime();
        private int modelIndex;
        private int textureIndex;
        private int warmedVbos;
        private int failedModels;
        private int failedTextures;

        private WarmupTask(String reason, int bladeCount, List<ResourceLocationRaw> models,
                           List<ResourceLocationRaw> textures) {
            this.reason = reason;
            this.bladeCount = bladeCount;
            this.models = models;
            this.textures = textures;
        }
    }
}

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
import java.util.Collection;
import java.util.Collections;
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
    private int registryFingerprint;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (ModConfig.GeneralConf.EnableSlashBladeModelWarmup
                && !warmed && event.getGui() instanceof GuiMainMenu) {
            warmup("title-screen");
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTextureStitchPre(TextureStitchEvent.Pre event) {
        rewarmAfterStitch = warmed;
        warmed = false;
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
            if (warmed || StaticBladeMeshCache.getCachedBytes() > 0L) {
                warmed = false;
                rewarmAfterStitch = false;
                StaticBladeMeshCache.clear();
                Main.logger.info("SlashBlade model warmup disabled; static model cache cleared");
            }
            return;
        }

        if (!warmed) {
            if (minecraft.currentScreen instanceof GuiMainMenu || minecraft.world != null) {
                warmup("config-enabled");
            }
            return;
        }
        if (minecraft.world == null) {
            return;
        }
        int currentFingerprint = computeRegistryFingerprint();
        if (currentFingerprint != registryFingerprint) {
            warmup("blade-registry-changed");
        }
    }

    private void warmup(String reason) {
        if (!ModConfig.GeneralConf.EnableSlashBladeModelWarmup || running) {
            return;
        }
        running = true;
        Main.logger.info("[KaBlade]正在进行拔刀剑模型预热");
        long started = System.nanoTime();

        int failedModels = 0;
        int failedTextures = 0;
        int warmedVbos = 0;
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

            BladeModelManager manager = BladeModelManager.getInstance();
            for (ResourceLocationRaw modelLocation : models) {
                try {
                    WavefrontObject model = manager.getModel(modelLocation);
                    warmedVbos += StaticBladeMeshCache.prewarmGuiParts(model);
                } catch (RuntimeException ex) {
                    failedModels++;
                    Main.logger.debug("Failed to prewarm SlashBlade model {}", modelLocation, ex);
                }
            }

            Minecraft minecraft = Minecraft.getMinecraft();
            for (ResourceLocationRaw texture : textures) {
                try {
                    minecraft.getTextureManager().bindTexture(texture);
                } catch (RuntimeException ex) {
                    failedTextures++;
                    Main.logger.debug("Failed to prewarm SlashBlade texture {}", texture, ex);
                }
            }

            registryFingerprint = computeRegistryFingerprint(stacks.values());
            warmed = true;
            long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
            Main.logger.info(
                    "SlashBlade model warmup finished: reason={}, blades={}, models={}, textures={}, vbos={}, vboMiB={}, failedModels={}, failedTextures={}, elapsed={}ms",
                    reason, stacks.size(), models.size(), textures.size(), warmedVbos,
                    StaticBladeMeshCache.getCachedBytes() / 1024L / 1024L,
                    failedModels, failedTextures, elapsedMs);
        } finally {
            running = false;
        }
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

    private static int computeRegistryFingerprint() {
        return computeRegistryFingerprint(collectBladeStacks().values());
    }

    private static int computeRegistryFingerprint(Collection<ItemStack> stacks) {
        List<String> entries = new ArrayList<String>();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemSlashBlade)) {
                continue;
            }
            ItemSlashBlade blade = (ItemSlashBlade) stack.getItem();
            entries.add(blade.getModelLocation(stack).toString()
                    + '\u0000' + blade.getModelTexture(stack).toString()
                    + '\u0000' + stack.getTranslationKey());
        }
        Collections.sort(entries);
        return entries.hashCode();
    }
}

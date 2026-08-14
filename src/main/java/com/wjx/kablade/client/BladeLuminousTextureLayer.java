package com.wjx.kablade.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.shader.BladeLuminousHandOculusPipeline;
import com.wjx.kablade.client.shader.ShaderCompat;
import mods.flammpfeil.slashblade.client.renderer.model.obj.GroupObject;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import mods.flammpfeil.slashblade.event.client.RenderOverrideEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lets a blade opt into a texture-masked luminous pass without duplicating OBJ groups.
 *
 * <p>For a base texture such as {@code model/example/tex.png}, placing
 * {@code model/example/tex_luminous.png} in the same namespace makes SlashBlade's normal
 * {@code *_luminous} pass render the corresponding base group with that texture. If the
 * luminous texture is absent, SlashBlade's original luminous-group rendering is left untouched.</p>
 */
@Mod.EventBusSubscriber(modid = Main.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BladeLuminousTextureLayer implements ResourceManagerReloadListener {

    public static final BladeLuminousTextureLayer INSTANCE = new BladeLuminousTextureLayer();

    private static final String LUMINOUS_GROUP_SUFFIX = "_luminous";
    private static final String PNG_EXTENSION = ".png";
    private static final String LUMINOUS_TEXTURE_SUFFIX = "_luminous.png";
    private static final String DEFERRED_LUMINOUS_TARGET = "__kablade_luminous_post__";

    /**
     * Empty values are cached as well, because most blade textures do not have a luminous mask.
     */
    private final Map<ResourceLocation, Optional<ResourceLocation>> luminousTextures =
            new ConcurrentHashMap<>();

    private BladeLuminousTextureLayer() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderOverride(RenderOverrideEvent event) {
        INSTANCE.applyLuminousTexture(event);
    }

    private void applyLuminousTexture(RenderOverrideEvent event) {
        String luminousTarget = event.getTarget();
        if (luminousTarget == null || !luminousTarget.endsWith(LUMINOUS_GROUP_SUFFIX)) {
            return;
        }

        ResourceLocation baseTexture = event.getTexture();
        if (baseTexture == null || !Main.MODID.equals(baseTexture.getNamespace())) {
            return;
        }

        String baseTarget = luminousTarget.substring(
                0, luminousTarget.length() - LUMINOUS_GROUP_SUFFIX.length());
        if (baseTarget.isEmpty() || !hasRenderableGroup(event.getModel(), baseTarget)) {
            return;
        }

        luminousTexture(baseTexture).ifPresent(texture -> {
            boolean shaderPackHand = isShaderPackHand(event);
            boolean queuedForPost = shouldUseShaderPackPost()
                    && BladeLuminousHandOculusPipeline.enqueue(
                    event.getModel(), baseTarget, texture, event.getPoseStack());
            if (queuedForPost) {
                // Use an intentionally absent group instead of canceling the event. SlashBlade
                // can then run its normal per-draw color/UV cleanup without emitting vertices.
                event.setTarget(DEFERRED_LUMINOUS_TARGET);
                event.setTexture(texture);
                return;
            }
            event.setTarget(baseTarget);
            event.setTexture(texture);
            event.setGetRenderType(shaderPackHand
                    ? KabladeRenderTypes::bladeLuminousHandTexture
                    : KabladeRenderTypes::bladeLuminousTexture);
            // Shader packs commonly route first-person items through a regular hand shader,
            // discarding SlashBlade's emissive shader. Full light keeps the mask visibly
            // self-lit in that fallback path while preserving the luminous blend state.
            event.setPackedLightIn(BladeRenderState.MAX_LIGHT);
        });
    }

    private Optional<ResourceLocation> luminousTexture(ResourceLocation baseTexture) {
        return luminousTextures.computeIfAbsent(baseTexture, texture -> {
            String path = texture.getPath();
            if (!path.endsWith(PNG_EXTENSION) || path.endsWith(LUMINOUS_TEXTURE_SUFFIX)) {
                return Optional.empty();
            }

            String luminousPath = path.substring(0, path.length() - PNG_EXTENSION.length())
                    + LUMINOUS_TEXTURE_SUFFIX;
            ResourceLocation luminousTexture = ResourceLocation.fromNamespaceAndPath(
                    texture.getNamespace(), luminousPath);
            return Minecraft.getInstance().getResourceManager().getResource(luminousTexture).isPresent()
                    ? Optional.of(luminousTexture)
                    : Optional.empty();
        });
    }

    private static boolean hasRenderableGroup(WavefrontObject model, String target) {
        if (model == null || model.groupObjects == null) {
            return false;
        }

        for (GroupObject group : model.groupObjects) {
            if (group != null && group.name != null && target.equalsIgnoreCase(group.name)
                    && group.faces != null && !group.faces.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isShaderPackHand(RenderOverrideEvent event) {
        if (ShaderCompat.isRenderingShaderPackHand()) {
            return true;
        }

        // Oculus supplies this unflushable wrapper only while it is collecting a hand pass.
        // It also covers versions where the internal HandRenderer method was renamed.
        return ShaderCompat.shouldUseOculusPostPath()
                && event.getBuffer() != null
                && event.getBuffer().getClass().getName()
                .endsWith("FullyBufferedMultiBufferSource$UnflushableWrapper");
    }

    private static boolean shouldUseShaderPackPost() {
        if (!ShaderCompat.shouldUseOculusPostPath()
                || ShaderCompat.isRenderingShaderPackShadow()
                || Minecraft.getInstance().level == null) {
            return false;
        }

        // Perspective projections cover first/third person, item entities, and in-world blade
        // displays. Orthographic inventory/GUI renders happen after the level post stage and
        // must remain on the immediate RenderType path.
        return Math.abs(RenderSystem.getProjectionMatrix().m33()) < 1.0E-5F;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        luminousTextures.clear();
        BladeLuminousHandOculusPipeline.invalidateResources();
    }
}

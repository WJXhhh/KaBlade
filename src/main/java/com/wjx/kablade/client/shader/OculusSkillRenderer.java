package com.wjx.kablade.client.shader;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.wjx.kablade.Main;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Optional;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class OculusSkillRenderer {
    private static final SkillEffectFramebuffer EFFECT_FRAMEBUFFER = new SkillEffectFramebuffer();
    private static final ThreadLocal<Boolean> RENDERING_PASS = ThreadLocal.withInitial(() -> false);
    private static boolean postDisabled;

    private OculusSkillRenderer() {
    }

    public static boolean runIfNeeded(Consumer<MultiBufferSource.BufferSource> renderer) {
        // Oculus shaderpacks do not reliably accept vanilla ShaderInstance-based
        // entity effects here, and doing a copy/blur/composite per effect entity is
        // too expensive. The renderers use lightweight fallback RenderTypes instead.
        if (ShaderCompat.shouldUseOculusPostPath()) {
            return false;
        }

        if (postDisabled || RENDERING_PASS.get() || !ShaderCompat.shouldUseOculusPostPath()) {
            return false;
        }

        Optional<SkillShaderTarget> oculusTarget = OculusFramebufferAccess.findTranslucentTarget();
        if (oculusTarget.isEmpty() || !oculusTarget.get().isComplete()) {
            return false;
        }

        SkillShaderTarget target = oculusTarget.get();
        RENDERING_PASS.set(true);
        try {
            renderColorPass(target, renderer);
            renderMaskPass(target, renderer);
            EFFECT_FRAMEBUFFER.composite(target);
        } catch (RuntimeException ex) {
            postDisabled = true;
            Main.LOGGER.warn("Disabling KBlade Oculus skill post-processing for this session.", ex);
        } finally {
            EFFECT_FRAMEBUFFER.end(target);
            RENDERING_PASS.set(false);
        }
        return true;
    }

    private static void renderColorPass(SkillShaderTarget target, Consumer<MultiBufferSource.BufferSource> renderer) {
        EFFECT_FRAMEBUFFER.beginColor(target);
        renderImmediate(renderer);
    }

    private static void renderMaskPass(SkillShaderTarget target, Consumer<MultiBufferSource.BufferSource> renderer) {
        EFFECT_FRAMEBUFFER.beginMask(target);
        renderImmediate(renderer);
    }

    private static void renderImmediate(Consumer<MultiBufferSource.BufferSource> renderer) {
        MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(new BufferBuilder(65536));
        renderer.accept(immediate);
        immediate.endBatch();
    }

    public static Optional<Pass> beginPass() {
        if (!ShaderCompat.shouldUseOculusPostPath()) {
            return Optional.empty();
        }

        Optional<SkillShaderTarget> oculusTarget = OculusFramebufferAccess.findTranslucentTarget();
        if (oculusTarget.isEmpty()) {
            return Optional.empty();
        }

        SkillShaderTarget target = oculusTarget.get();
        if (!target.isComplete()) {
            return Optional.empty();
        }

        EFFECT_FRAMEBUFFER.beginColor(target);
        return Optional.of(new Pass(target, EFFECT_FRAMEBUFFER.maskTextureId()));
    }

    public record Pass(SkillShaderTarget target, int maskTextureId) implements AutoCloseable {
        @Override
        public void close() {
            EFFECT_FRAMEBUFFER.end(target);
        }
    }
}

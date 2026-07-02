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
    private static boolean loggedNoPostPath;
    private static boolean loggedNoTarget;
    private static boolean loggedIncompleteTarget;
    private static boolean loggedPostActive;

    private OculusSkillRenderer() {
    }

    public static boolean runIfNeeded(Consumer<MultiBufferSource.BufferSource> renderer) {
        // Oculus shaderpacks do not reliably accept vanilla ShaderInstance-based
        // entity effects here, and doing a copy/blur/composite per effect entity is
        // too expensive. The renderers use lightweight fallback RenderTypes instead.
        if (ShaderCompat.shouldUseOculusPostPath()) {
            return false;
        }

        return runPostIfNeeded(renderer);
    }

    public static boolean runPostIfNeeded(Consumer<MultiBufferSource.BufferSource> renderer) {
        return runPostIfNeeded(() -> renderImmediate(renderer));
    }

    public static boolean runPostIfNeeded(Runnable renderer) {
        if (postDisabled || RENDERING_PASS.get()) {
            return false;
        }
        if (!ShaderCompat.shouldUseOculusPostPath()) {
            if (!loggedNoPostPath) {
                loggedNoPostPath = true;
                Main.LOGGER.info("KBlade Oculus skill post-processing is not active; using the normal renderer path.");
            }
            return false;
        }

        Optional<SkillShaderTarget> oculusTarget = OculusFramebufferAccess.findTranslucentTarget();
        if (oculusTarget.isEmpty()) {
            if (!loggedNoTarget) {
                loggedNoTarget = true;
                Main.LOGGER.warn("KBlade could not locate the Oculus/Iris translucent framebuffer; using shaderpack fallback geometry.");
            }
            return false;
        }
        if (!oculusTarget.get().isComplete()) {
            if (!loggedIncompleteTarget) {
                loggedIncompleteTarget = true;
                Main.LOGGER.warn("KBlade found an Oculus/Iris framebuffer but it is incomplete: {}. Using shaderpack fallback geometry.",
                        oculusTarget.get());
            }
            return false;
        }

        SkillShaderTarget target = oculusTarget.get();
        if (!loggedPostActive) {
            loggedPostActive = true;
            Main.LOGGER.info("KBlade Oculus skill post-processing active: fbo={}, color={}, depth={}, size={}x{}",
                    target.framebufferId(), target.colorTextureId(), target.depthTextureId(), target.width(), target.height());
        }
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

    public static boolean isRenderingPass() {
        return RENDERING_PASS.get();
    }

    private static void renderColorPass(SkillShaderTarget target, Runnable renderer) {
        EFFECT_FRAMEBUFFER.beginColor(target);
        renderer.run();
    }

    private static void renderMaskPass(SkillShaderTarget target, Runnable renderer) {
        EFFECT_FRAMEBUFFER.beginMask(target);
        renderer.run();
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

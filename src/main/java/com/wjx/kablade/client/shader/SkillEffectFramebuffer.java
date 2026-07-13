package com.wjx.kablade.client.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

@OnlyIn(Dist.CLIENT)
public final class SkillEffectFramebuffer implements AutoCloseable {
    private final IntBuffer drawBuffer = BufferUtils.createIntBuffer(1);
    private final FloatBuffer transparent = BufferUtils.createFloatBuffer(4);
    private int framebufferId;
    private int copyFramebufferId;
    private int maskTextureId;
    private int blurTextureId;
    private int sceneCopyTextureId;
    private int width;
    private int height;

    public void beginColor(SkillShaderTarget target) {
        RenderSystem.assertOnRenderThread();
        ensureAllocated(target.width(), target.height());

        bindSingleColorTarget(target.colorTextureId(), target.depthTextureId());
        GL11.glViewport(0, 0, target.width(), target.height());
    }

    public void beginMask(SkillShaderTarget target) {
        RenderSystem.assertOnRenderThread();
        ensureAllocated(target.width(), target.height());

        bindSingleColorTarget(maskTextureId, target.depthTextureId());
        GL11.glViewport(0, 0, target.width(), target.height());
        clearColorAttachment();
    }

    public void composite(SkillShaderTarget target) {
        RenderSystem.assertOnRenderThread();
        ensureAllocated(target.width(), target.height());

        copySceneColor(target);
        blurMask();

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.framebufferId());
        GL11.glViewport(0, 0, target.width(), target.height());
        SkillPostShaders.composite(sceneCopyTextureId, maskTextureId, target.width(), target.height());
    }

    public void end(SkillShaderTarget target) {
        RenderSystem.assertOnRenderThread();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.framebufferId());
        GL11.glViewport(0, 0, target.width(), target.height());
    }

    public int maskTextureId() {
        return maskTextureId;
    }

    private void bindSingleColorTarget(int colorTextureId, int depthTextureId) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebufferId);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, colorTextureId, 0);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
                GL11.GL_TEXTURE_2D, depthTextureId, 0);
        drawBuffer.clear();
        drawBuffer.put(GL30.GL_COLOR_ATTACHMENT0);
        drawBuffer.flip();
        GL20.glDrawBuffers(drawBuffer);
        checkFramebuffer("skill geometry");
    }

    private void copySceneColor(SkillShaderTarget target) {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, framebufferId);
        GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, target.colorTextureId(), 0);
        GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);

        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, copyFramebufferId);
        GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, sceneCopyTextureId, 0);
        drawBuffer.clear();
        drawBuffer.put(GL30.GL_COLOR_ATTACHMENT0);
        drawBuffer.flip();
        GL20.glDrawBuffers(drawBuffer);
        checkFramebuffer("skill scene copy");

        GL30.glBlitFramebuffer(
                0, 0, target.width(), target.height(),
                0, 0, target.width(), target.height(),
                GL11.GL_COLOR_BUFFER_BIT,
                GL11.GL_NEAREST);
    }

    private void blurMask() {
        for (int i = 0; i < 2; i++) {
            bindPostColor(blurTextureId);
            SkillPostShaders.blur(maskTextureId, width, height, true);
            bindPostColor(maskTextureId);
            SkillPostShaders.blur(blurTextureId, width, height, false);
        }
    }

    private void bindPostColor(int colorTextureId) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, copyFramebufferId);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, colorTextureId, 0);
        drawBuffer.clear();
        drawBuffer.put(GL30.GL_COLOR_ATTACHMENT0);
        drawBuffer.flip();
        GL20.glDrawBuffers(drawBuffer);
        checkFramebuffer("skill post");
        GL11.glViewport(0, 0, width, height);
    }

    private void clearColorAttachment() {
        transparent.clear();
        transparent.put(0.0F).put(0.0F).put(0.0F).put(0.0F);
        transparent.flip();
        GL30.glClearBufferfv(GL11.GL_COLOR, 0, transparent);
    }

    private void ensureAllocated(int nextWidth, int nextHeight) {
        if (framebufferId == 0) {
            framebufferId = GL30.glGenFramebuffers();
        }
        if (copyFramebufferId == 0) {
            copyFramebufferId = GL30.glGenFramebuffers();
        }
        if (maskTextureId == 0) {
            maskTextureId = GL11.glGenTextures();
        }
        if (blurTextureId == 0) {
            blurTextureId = GL11.glGenTextures();
        }
        if (sceneCopyTextureId == 0) {
            sceneCopyTextureId = GL11.glGenTextures();
        }
        if (width == nextWidth && height == nextHeight) {
            return;
        }

        width = nextWidth;
        height = nextHeight;
        allocateTexture(maskTextureId, GL30.GL_RGBA16F, GL11.GL_RGBA, GL11.GL_FLOAT);
        allocateTexture(blurTextureId, GL30.GL_RGBA16F, GL11.GL_RGBA, GL11.GL_FLOAT);
        allocateTexture(sceneCopyTextureId, GL11.GL_RGBA8, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE);
    }

    private void allocateTexture(int textureId, int internalFormat, int format, int type) {
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat,
                    width, height, 0, format, type, 0L);
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
        }
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        deleteTexture(maskTextureId);
        deleteTexture(blurTextureId);
        deleteTexture(sceneCopyTextureId);
        maskTextureId = 0;
        blurTextureId = 0;
        sceneCopyTextureId = 0;
        if (framebufferId != 0) {
            GL30.glDeleteFramebuffers(framebufferId);
            framebufferId = 0;
        }
        if (copyFramebufferId != 0) {
            GL30.glDeleteFramebuffers(copyFramebufferId);
            copyFramebufferId = 0;
        }
    }

    private static void deleteTexture(int textureId) {
        if (textureId != 0) {
            GL11.glDeleteTextures(textureId);
        }
    }

    private static void checkFramebuffer(String stage) {
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Incomplete KBlade " + stage + " framebuffer: 0x"
                    + Integer.toHexString(status));
        }
    }
}

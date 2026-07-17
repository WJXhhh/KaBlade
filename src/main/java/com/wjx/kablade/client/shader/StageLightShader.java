package com.wjx.kablade.client.shader;

import com.wjx.kablade.Entity.EntityStageLight;
import com.wjx.kablade.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.shader.ShaderManager;
import net.minecraft.client.shader.ShaderUniform;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Method;

/** Lazy 1.12 shader lifecycle and compatibility gate for Lights on Stage. */
public final class StageLightShader implements IResourceManagerReloadListener {

    private static final int GL_CURRENT_PROGRAM = 0x8B8D;
    private static final StageLightShader INSTANCE = new StageLightShader();

    private ShaderManager shader;
    private boolean failed;
    private boolean failureLogged;
    private boolean registered;
    private int previousProgram;

    private boolean optifineChecked;
    private Method optifineIsShaders;

    private StageLightShader() {
    }

    public static void registerReloadListener() {
        INSTANCE.register();
    }

    public static boolean begin(EntityStageLight entity, float partialTicks) {
        return INSTANCE.beginShader(entity, partialTicks);
    }

    public static void end() {
        INSTANCE.endShader();
    }

    private void register() {
        if (this.registered) {
            return;
        }
        IResourceManager manager = Minecraft.getMinecraft().getResourceManager();
        if (manager instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) manager).registerReloadListener(this);
            this.registered = true;
        }
    }

    private boolean beginShader(EntityStageLight entity, float partialTicks) {
        if (this.failed || !OpenGlHelper.areShadersSupported() || isOptifineShaderPackActive()) {
            return false;
        }

        try {
            if (this.shader == null) {
                this.shader = new ShaderManager(
                        Minecraft.getMinecraft().getResourceManager(), "kablade:stage_light");
            }

            ShaderUniform gameTime = this.shader.getShaderUniform("GameTime");
            if (gameTime != null) {
                float time = (float) ((entity.world.getTotalWorldTime() % 24000L) + partialTicks) / 24000.0F;
                gameTime.set(time);
            }
            ShaderUniform color = this.shader.getShaderUniform("ColorModulator");
            if (color != null) {
                color.set(1.0F, 1.0F, 1.0F, 1.0F);
            }

            this.previousProgram = GL11.glGetInteger(GL_CURRENT_PROGRAM);
            this.shader.useShader();
            return true;
        } catch (Throwable error) {
            try {
                if (this.shader != null) {
                    this.shader.endShader();
                }
            } catch (Throwable ignored) {
                OpenGlHelper.glUseProgram(0);
            }
            if (this.previousProgram != 0) {
                OpenGlHelper.glUseProgram(this.previousProgram);
            }
            this.previousProgram = 0;
            fail(error);
            return false;
        }
    }

    private void endShader() {
        if (this.shader == null) {
            return;
        }
        try {
            this.shader.endShader();
        } catch (Throwable error) {
            fail(error);
        } finally {
            if (this.previousProgram != 0) {
                OpenGlHelper.glUseProgram(this.previousProgram);
            }
            this.previousProgram = 0;
        }
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        dispose();
        this.failed = false;
        this.failureLogged = false;
    }

    private void fail(Throwable error) {
        if (!this.failureLogged && Main.logger != null) {
            Main.logger.error("Unable to use kablade:stage_light shader; using geometry fallback", error);
            this.failureLogged = true;
        }
        this.failed = true;
        dispose();
    }

    private void dispose() {
        if (this.shader != null) {
            try {
                this.shader.deleteShader();
            } catch (Throwable ignored) {
                // The GL context may already be rebuilding during a resource reload.
            }
            this.shader = null;
        }
    }

    private boolean isOptifineShaderPackActive() {
        if (!this.optifineChecked) {
            this.optifineChecked = true;
            try {
                Class<?> config = Class.forName("Config");
                this.optifineIsShaders = config.getMethod("isShaders");
            } catch (ReflectiveOperationException ignored) {
                this.optifineIsShaders = null;
            }
        }
        if (this.optifineIsShaders == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(this.optifineIsShaders.invoke(null));
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}

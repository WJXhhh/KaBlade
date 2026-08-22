package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** 地藏御魂外壳的 GLSL 1.20 Fresnel 辉光材质。 */
@SideOnly(Side.CLIENT)
public final class JizoSoulShader implements IResourceManagerReloadListener {
    public static final JizoSoulShader INSTANCE = new JizoSoulShader();
    public static final int UNAVAILABLE = Integer.MIN_VALUE;

    private int program = -1;
    private int gameTimeLocation = -1;
    private int samplerLocation = -1;

    private JizoSoulShader() {
    }

    /** 返回旧 program；无法安全接管时返回 {@link #UNAVAILABLE}。 */
    public int bind() {
        if (!OpenGlHelper.shadersSupported) {
            return UNAVAILABLE;
        }
        int oldProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        // OptiFine 光影会占用 program；不要破坏其世界着色器，交给固定管线回退。
        if (oldProgram != 0) {
            return UNAVAILABLE;
        }
        if (this.program < 0) {
            this.program = createProgram();
        }
        if (this.program == 0) {
            return UNAVAILABLE;
        }

        GL20.glUseProgram(this.program);
        long time = Minecraft.getMinecraft().world == null
                ? 0L : Minecraft.getMinecraft().world.getTotalWorldTime();
        GL20.glUniform1f(this.gameTimeLocation, (time % 24000L) / 24000.0F);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL20.glUniform1i(this.samplerLocation, 0);
        return oldProgram;
    }

    public void restore(int oldProgram) {
        if (oldProgram != UNAVAILABLE
                && GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) != oldProgram) {
            GL20.glUseProgram(oldProgram);
        }
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        reset();
    }

    public void reset() {
        if (this.program > 0) {
            GL20.glDeleteProgram(this.program);
        }
        this.program = -1;
        this.gameTimeLocation = -1;
        this.samplerLocation = -1;
    }

    private int createProgram() {
        int vertex = 0;
        int fragment = 0;
        int created = 0;
        try {
            vertex = compile(GL20.GL_VERTEX_SHADER,
                    read(new ResourceLocation(Main.MODID, "shaders/jizo_soul_120.vsh")));
            fragment = compile(GL20.GL_FRAGMENT_SHADER,
                    read(new ResourceLocation(Main.MODID, "shaders/jizo_soul_120.fsh")));
            created = GL20.glCreateProgram();
            GL20.glAttachShader(created, vertex);
            GL20.glAttachShader(created, fragment);
            GL20.glLinkProgram(created);
            if (GL20.glGetProgrami(created, GL20.GL_LINK_STATUS) == 0) {
                throw new IllegalStateException(GL20.glGetProgramInfoLog(created, 8192));
            }
            this.gameTimeLocation = GL20.glGetUniformLocation(created, "GameTime");
            this.samplerLocation = GL20.glGetUniformLocation(created, "Sampler0");
            return created;
        } catch (Throwable error) {
            if (created != 0) {
                GL20.glDeleteProgram(created);
            }
            if (Main.logger != null) {
                Main.logger.warn("Jizo Soul shader unavailable; using additive fallback", error);
            }
            return 0;
        } finally {
            if (vertex != 0) {
                GL20.glDeleteShader(vertex);
            }
            if (fragment != 0) {
                GL20.glDeleteShader(fragment);
            }
        }
    }

    private static int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            String log = GL20.glGetShaderInfoLog(shader, 8192);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException(log);
        }
        return shader;
    }

    private static String read(ResourceLocation location) throws IOException {
        StringBuilder output = new StringBuilder();
        try (InputStream stream = Minecraft.getMinecraft().getResourceManager()
                .getResource(location).getInputStream();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }
        return output.toString();
    }
}

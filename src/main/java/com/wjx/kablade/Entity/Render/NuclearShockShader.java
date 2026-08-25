package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** 核爆穹顶的 GLSL 1.20 菲涅尔/等离子渐变；不可用时自动退回顶点渐变。 */
final class NuclearShockShader {
    private static final ResourceLocation VERT = new ResourceLocation(Main.MODID,
            "shaders/nuclear_shock_120.vsh");
    private static final ResourceLocation FRAG = new ResourceLocation(Main.MODID,
            "shaders/nuclear_shock_120.fsh");
    private static int program = -1;
    private static int timeUniform;

    private NuclearShockShader() {}

    static int bind(float time) {
        if (!OpenGlHelper.shadersSupported) return 0;
        int previous = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        if (previous != 0) return previous;
        if (program < 0) program = create();
        if (program == 0) return previous;
        GL20.glUseProgram(program);
        GL20.glUniform1f(timeUniform, time);
        return previous;
    }

    static void restore(int previous) {
        if (OpenGlHelper.shadersSupported && GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) != previous) {
            GL20.glUseProgram(previous);
        }
    }

    private static int create() {
        int vertex = 0, fragment = 0, linked = 0;
        try {
            vertex = compile(GL20.GL_VERTEX_SHADER, read(VERT));
            fragment = compile(GL20.GL_FRAGMENT_SHADER, read(FRAG));
            linked = GL20.glCreateProgram();
            GL20.glAttachShader(linked, vertex);
            GL20.glAttachShader(linked, fragment);
            GL20.glLinkProgram(linked);
            if (GL20.glGetProgrami(linked, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                throw new IllegalStateException(GL20.glGetProgramInfoLog(linked, 4096));
            }
            timeUniform = GL20.glGetUniformLocation(linked, "uTime");
            return linked;
        } catch (Throwable error) {
            if (linked != 0) GL20.glDeleteProgram(linked);
            if (Main.logger != null) Main.logger.warn(
                    "Nuclear Shock GLSL unavailable; using geometry fallback", error);
            return 0;
        } finally {
            if (vertex != 0) GL20.glDeleteShader(vertex);
            if (fragment != 0) GL20.glDeleteShader(fragment);
        }
    }

    private static int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader, 4096);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException(log);
        }
        return shader;
    }

    private static String read(ResourceLocation location) throws Exception {
        StringBuilder source = new StringBuilder();
        try (InputStream input = Minecraft.getMinecraft().getResourceManager()
                .getResource(location).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input,
                     StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) source.append(line).append('\n');
        }
        return source.toString();
    }
}

package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 聚光舞台的可选 GLSL 1.20 增强层。
 * 外部光影已经占用 program 时绝不接管，任何编译错误也只会禁用增强层并回退固定管线。
 */
final class StageLightShader {

    private static final ResourceLocation VERTEX =
            new ResourceLocation(Main.MODID, "shaders/stage_light_120.vsh");
    private static final ResourceLocation FRAGMENT =
            new ResourceLocation(Main.MODID, "shaders/stage_light_120.fsh");

    private static int program = -1;
    private static int timeUniform = -1;
    private static int strengthUniform = -1;

    private StageLightShader() {
    }

    static int bind(float age, float strength) {
        if (!OpenGlHelper.shadersSupported) {
            return 0;
        }

        int previous = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        // OptiFine/Iris 等已有光影程序时保持其状态，使用几何柔边回退路径。
        if (previous != 0) {
            return previous;
        }

        if (program < 0) {
            program = createProgram();
        }
        if (program == 0) {
            return previous;
        }

        GL20.glUseProgram(program);
        GL20.glUniform1f(timeUniform, age);
        GL20.glUniform1f(strengthUniform, strength);
        return previous;
    }

    static void restore(int previousProgram) {
        if (OpenGlHelper.shadersSupported
                && GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) != previousProgram) {
            GL20.glUseProgram(previousProgram);
        }
    }

    private static int createProgram() {
        int vertex = 0;
        int fragment = 0;
        int linkedProgram = 0;
        try {
            vertex = compile(GL20.GL_VERTEX_SHADER, read(VERTEX), "vertex");
            fragment = compile(GL20.GL_FRAGMENT_SHADER, read(FRAGMENT), "fragment");
            linkedProgram = GL20.glCreateProgram();
            GL20.glAttachShader(linkedProgram, vertex);
            GL20.glAttachShader(linkedProgram, fragment);
            GL20.glLinkProgram(linkedProgram);
            if (GL20.glGetProgrami(linkedProgram, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                throw new IllegalStateException("link: " + GL20.glGetProgramInfoLog(linkedProgram, 4096));
            }
            timeUniform = GL20.glGetUniformLocation(linkedProgram, "uTime");
            strengthUniform = GL20.glGetUniformLocation(linkedProgram, "uStrength");
            return linkedProgram;
        } catch (Throwable error) {
            if (linkedProgram != 0) {
                GL20.glDeleteProgram(linkedProgram);
            }
            if (Main.logger != null) {
                Main.logger.warn("Stage light GLSL enhancement unavailable; using fixed-pipeline fallback", error);
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

    private static int compile(int type, String source, String label) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader, 4096);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException(label + ": " + log);
        }
        return shader;
    }

    private static String read(ResourceLocation location) throws IOException {
        StringBuilder source = new StringBuilder();
        try (InputStream stream = Minecraft.getMinecraft().getResourceManager()
                .getResource(location).getInputStream();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                source.append(line).append('\n');
            }
        }
        return source.toString();
    }
}

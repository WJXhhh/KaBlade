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
 * 时空黑洞 GLSL 1.20 着色器管理器。
 * <p>
 * 外部光影已经占用 program 时绝不接管，保证与各类光影模组的兼容；
 * 正确处理多 Pass 之间的 program 保持与 uniform 切换，并在结束时严格还原。
 */
final class VorpalBlackHoleShader {

    private static final ResourceLocation VERTEX =
            new ResourceLocation(Main.MODID, "shaders/vorpal_hole_120.vsh");
    private static final ResourceLocation FRAGMENT =
            new ResourceLocation(Main.MODID, "shaders/vorpal_hole_120.fsh");

    private static int program = -1;
    private static int timeUniform = -1;
    private static int strengthUniform = -1;
    private static int passUniform = -1;

    private VorpalBlackHoleShader() {
    }

    static int bind(float time, float strength, int pass) {
        if (!OpenGlHelper.shadersSupported) {
            return 0;
        }

        int previous = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);

        if (program < 0) {
            program = createProgram();
        }
        if (program <= 0) {
            return previous;
        }

        // 如果当前使用的是外部光影程序（非 0 且不是我们自己的 program），则不强制切换，使用固定管线顶点色
        if (previous != 0 && previous != program) {
            return previous;
        }

        if (previous != program) {
            GL20.glUseProgram(program);
        }

        if (timeUniform >= 0) {
            GL20.glUniform1f(timeUniform, time);
        }
        if (strengthUniform >= 0) {
            GL20.glUniform1f(strengthUniform, strength);
        }
        if (passUniform >= 0) {
            GL20.glUniform1i(passUniform, pass);
        }

        return previous;
    }

    static void restore(int previousProgram) {
        if (OpenGlHelper.shadersSupported) {
            int current = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            if (current != previousProgram) {
                GL20.glUseProgram(previousProgram);
            }
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
            passUniform = GL20.glGetUniformLocation(linkedProgram, "uPass");
            return linkedProgram;
        } catch (Throwable error) {
            if (linkedProgram != 0) {
                GL20.glDeleteProgram(linkedProgram);
            }
            if (Main.logger != null) {
                Main.logger.warn("Vorpal black hole GLSL enhancement unavailable; using fixed-pipeline fallback", error);
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

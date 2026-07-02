package com.wjx.kablade.client.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

@OnlyIn(Dist.CLIENT)
final class SkillPostShaders {
    private static final String FULLSCREEN_VERTEX = """
            #version 150

            out vec2 vUv;

            void main() {
                vec2 p;
                if (gl_VertexID == 0) {
                    p = vec2(-1.0, -1.0);
                } else if (gl_VertexID == 1) {
                    p = vec2(3.0, -1.0);
                } else {
                    p = vec2(-1.0, 3.0);
                }
                vUv = p * 0.5 + 0.5;
                gl_Position = vec4(p, 0.0, 1.0);
            }
            """;

    private static final String BLUR_FRAGMENT = """
            #version 150

            uniform sampler2D Source;
            uniform vec2 TexelStep;

            in vec2 vUv;
            out vec4 fragColor;

            void main() {
                vec4 sum = texture(Source, vUv) * 0.227027;
                sum += texture(Source, vUv + TexelStep * 1.384615) * 0.316216;
                sum += texture(Source, vUv - TexelStep * 1.384615) * 0.316216;
                sum += texture(Source, vUv + TexelStep * 3.230769) * 0.070270;
                sum += texture(Source, vUv - TexelStep * 3.230769) * 0.070270;
                fragColor = sum;
            }
            """;

    private static final String COMPOSITE_FRAGMENT = """
            #version 150

            uniform sampler2D Scene;
            uniform sampler2D Mask;
            uniform vec2 TexelSize;
            uniform float GlowStrength;
            uniform float DistortionStrength;

            in vec2 vUv;
            out vec4 fragColor;

            float maskPower(vec4 c) {
                return max(c.a, max(max(c.r, c.g), c.b));
            }

            void main() {
                vec4 mask = texture(Mask, vUv);
                float m = clamp(maskPower(mask), 0.0, 1.0);
                float left = maskPower(texture(Mask, vUv - vec2(TexelSize.x, 0.0)));
                float right = maskPower(texture(Mask, vUv + vec2(TexelSize.x, 0.0)));
                float down = maskPower(texture(Mask, vUv - vec2(0.0, TexelSize.y)));
                float up = maskPower(texture(Mask, vUv + vec2(0.0, TexelSize.y)));
                vec2 warp = vec2(right - left, up - down) * DistortionStrength * TexelSize * 28.0;

                vec4 scene = texture(Scene, clamp(vUv + warp, vec2(0.001), vec2(0.999)));
                vec3 glowColor = max(mask.rgb, vec3(m));
                vec3 bloom = glowColor * GlowStrength * (0.35 + m * 1.35);
                fragColor = vec4(scene.rgb + bloom, scene.a);
            }
            """;

    private static int blurProgram;
    private static int compositeProgram;
    private static int vertexArray;

    private SkillPostShaders() {
    }

    static void blur(int sourceTextureId, int width, int height, boolean horizontal) {
        ensurePrograms();
        withFullscreenState(() -> {
            GL20.glUseProgram(blurProgram);
            bindTexture(0, sourceTextureId);
            GL20.glUniform1i(GL20.glGetUniformLocation(blurProgram, "Source"), 0);
            float x = horizontal ? 1.0F / Math.max(1, width) : 0.0F;
            float y = horizontal ? 0.0F : 1.0F / Math.max(1, height);
            GL20.glUniform2f(GL20.glGetUniformLocation(blurProgram, "TexelStep"), x, y);
            drawFullscreenTriangle();
        });
    }

    static void composite(int sceneTextureId, int maskTextureId, int width, int height) {
        ensurePrograms();
        withFullscreenState(() -> {
            GL20.glUseProgram(compositeProgram);
            bindTexture(0, sceneTextureId);
            bindTexture(1, maskTextureId);
            GL20.glUniform1i(GL20.glGetUniformLocation(compositeProgram, "Scene"), 0);
            GL20.glUniform1i(GL20.glGetUniformLocation(compositeProgram, "Mask"), 1);
            GL20.glUniform2f(GL20.glGetUniformLocation(compositeProgram, "TexelSize"),
                    1.0F / Math.max(1, width), 1.0F / Math.max(1, height));
            GL20.glUniform1f(GL20.glGetUniformLocation(compositeProgram, "GlowStrength"), 0.72F);
            GL20.glUniform1f(GL20.glGetUniformLocation(compositeProgram, "DistortionStrength"), 0.45F);
            drawFullscreenTriangle();
        });
    }

    private static void ensurePrograms() {
        if (vertexArray == 0) {
            vertexArray = GL30.glGenVertexArrays();
        }
        if (blurProgram == 0) {
            blurProgram = createProgram(BLUR_FRAGMENT);
        }
        if (compositeProgram == 0) {
            compositeProgram = createProgram(COMPOSITE_FRAGMENT);
        }
    }

    private static int createProgram(String fragmentSource) {
        int vertex = compile(GL20.GL_VERTEX_SHADER, FULLSCREEN_VERTEX);
        int fragment = compile(GL20.GL_FRAGMENT_SHADER, fragmentSource);
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertex);
        GL20.glAttachShader(program, fragment);
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program);
            GL20.glDeleteProgram(program);
            throw new IllegalStateException("KBlade skill post shader link failed: " + log);
        }
        GL20.glDetachShader(program, vertex);
        GL20.glDetachShader(program, fragment);
        GL20.glDeleteShader(vertex);
        GL20.glDeleteShader(fragment);
        return program;
    }

    private static int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException("KBlade skill post shader compile failed: " + log);
        }
        return shader;
    }

    private static void withFullscreenState(Runnable draw) {
        RenderSystem.assertOnRenderThread();
        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int previousVertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean depthWriteEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(false);
        GL30.glBindVertexArray(vertexArray);
        try {
            draw.run();
        } finally {
            GL20.glUseProgram(previousProgram);
            GL30.glBindVertexArray(previousVertexArray);
            GL13.glActiveTexture(previousActiveTexture);
            GL11.glDepthMask(depthWriteEnabled);
            setEnabled(GL11.GL_DEPTH_TEST, depthEnabled);
            setEnabled(GL11.GL_BLEND, blendEnabled);
            setEnabled(GL11.GL_CULL_FACE, cullEnabled);
        }
    }

    private static void bindTexture(int unit, int textureId) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
    }

    private static void drawFullscreenTriangle() {
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
    }

    private static void setEnabled(int capability, boolean enabled) {
        if (enabled) {
            GL11.glEnable(capability);
        } else {
            GL11.glDisable(capability);
        }
    }
}

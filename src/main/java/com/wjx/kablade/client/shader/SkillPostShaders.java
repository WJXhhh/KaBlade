package com.wjx.kablade.client.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

@OnlyIn(Dist.CLIENT)
public final class SkillPostShaders {
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

    private static final String BLOODFYRE_COMPOSITE_FRAGMENT = """
            #version 150

            uniform sampler2D Scene;
            uniform sampler2D Effect;
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
                vec3 maskRgb = max(mask.rgb, vec3(0.0));
                float m = clamp(maskPower(mask), 0.0, 4.0);
                float left = maskPower(texture(Mask, vUv - vec2(TexelSize.x, 0.0)));
                float right = maskPower(texture(Mask, vUv + vec2(TexelSize.x, 0.0)));
                float down = maskPower(texture(Mask, vUv - vec2(0.0, TexelSize.y)));
                float up = maskPower(texture(Mask, vUv + vec2(0.0, TexelSize.y)));
                vec2 warp = vec2(right - left, up - down)
                        * DistortionStrength * TexelSize * 18.0;

                vec4 scene = texture(Scene, clamp(vUv + warp, vec2(0.001), vec2(0.999)));
                vec4 effect = texture(Effect, vUv);

                // Saturate only where the emissive mask is present. Dark smoke and the
                // charred blade body therefore retain their contrast instead of turning red.
                float energy = 1.0 - exp(-m * 1.55);
                float effectLuma = dot(effect.rgb, vec3(0.2126, 0.7152, 0.0722));
                vec3 saturatedEffect = max(vec3(0.0), mix(vec3(effectLuma), effect.rgb,
                        1.0 + energy * 0.34));
                vec3 base = scene.rgb * (1.0 - clamp(effect.a, 0.0, 1.0)) + saturatedEffect;

                // Preserve the mask hue instead of raising every channel to its maximum.
                // A nearly white source gets a warm-gold halo while its geometric core
                // remains white-hot in Effect; colored sources keep and strengthen hue.
                float peak = max(max(maskRgb.r, maskRgb.g), maskRgb.b);
                vec3 normalizedHue = peak > 0.0001 ? maskRgb / peak : vec3(1.0, 0.42, 0.06);
                float hueFloor = min(min(normalizedHue.r, normalizedHue.g), normalizedHue.b);
                float chroma = 1.0 - hueFloor;
                vec3 pureHue = chroma > 0.001
                        ? (normalizedHue - vec3(hueFloor)) / chroma
                        : vec3(1.0, 0.42, 0.06);
                vec3 bloomHue = mix(normalizedHue, pureHue, 0.68);
                vec3 bloom = bloomHue * GlowStrength * energy * (0.55 + energy * 0.90);
                fragColor = vec4(base + bloom, scene.a);
            }
            """;

    private static int blurProgram;
    private static int compositeProgram;
    private static int bloodfyreCompositeProgram;
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

    public static void compositeBloodfyre(int sceneTextureId, int effectTextureId,
                                          int maskTextureId, int width, int height) {
        ensurePrograms();
        withFullscreenState(() -> {
            GL20.glUseProgram(bloodfyreCompositeProgram);
            bindTexture(0, sceneTextureId);
            bindTexture(1, effectTextureId);
            bindTexture(2, maskTextureId);
            GL20.glUniform1i(GL20.glGetUniformLocation(bloodfyreCompositeProgram, "Scene"), 0);
            GL20.glUniform1i(GL20.glGetUniformLocation(bloodfyreCompositeProgram, "Effect"), 1);
            GL20.glUniform1i(GL20.glGetUniformLocation(bloodfyreCompositeProgram, "Mask"), 2);
            GL20.glUniform2f(GL20.glGetUniformLocation(bloodfyreCompositeProgram, "TexelSize"),
                    1.0F / Math.max(1, width), 1.0F / Math.max(1, height));
            // Broad colored bloom: brightness comes from halo area and chroma instead of
            // clipping all three channels to white at the geometric core.
            GL20.glUniform1f(GL20.glGetUniformLocation(bloodfyreCompositeProgram, "GlowStrength"), 0.90F);
            GL20.glUniform1f(GL20.glGetUniformLocation(bloodfyreCompositeProgram, "DistortionStrength"), 0.12F);
            drawFullscreenTriangle();
        });
    }

    public static void blurBloodfyre(int sourceTextureId, int width, int height, boolean horizontal) {
        blur(sourceTextureId, width, height, horizontal);
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
        if (bloodfyreCompositeProgram == 0) {
            bloodfyreCompositeProgram = createProgram(BLOODFYRE_COMPOSITE_FRAGMENT);
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
        int previousTexture0 = textureBinding2D(0);
        int previousTexture1 = textureBinding2D(1);
        int previousTexture2 = textureBinding2D(2);
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
            restoreTextureBinding2D(0, previousTexture0);
            restoreTextureBinding2D(1, previousTexture1);
            restoreTextureBinding2D(2, previousTexture2);
            GL13.glActiveTexture(previousActiveTexture);
            GL11.glDepthMask(depthWriteEnabled);
            setEnabled(GL11.GL_DEPTH_TEST, depthEnabled);
            setEnabled(GL11.GL_BLEND, blendEnabled);
            setEnabled(GL11.GL_CULL_FACE, cullEnabled);
        }
    }

    private static int textureBinding2D(int unit) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        return GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
    }

    private static void restoreTextureBinding2D(int unit, int textureId) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
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

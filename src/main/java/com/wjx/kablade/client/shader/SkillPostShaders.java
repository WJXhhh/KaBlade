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
            uniform sampler2D Glow;
            uniform sampler2D Bloom;
            uniform vec2 TexelSize;
            uniform float CoreStrength;
            uniform float GlowStrength;
            uniform float DistortionStrength;

            in vec2 vUv;
            out vec4 fragColor;

            float glowPower(vec4 c) {
                return max(c.a, max(max(c.r, c.g), c.b));
            }

            void main() {
                vec4 rawGlow = texture(Glow, vUv);
                vec4 bloomSample = texture(Bloom, vUv);
                vec3 bloomRgb = max(bloomSample.rgb, vec3(0.0));
                float m = clamp(glowPower(bloomSample), 0.0, 6.0);
                float left = glowPower(texture(Bloom, vUv - vec2(TexelSize.x, 0.0)));
                float right = glowPower(texture(Bloom, vUv + vec2(TexelSize.x, 0.0)));
                float down = glowPower(texture(Bloom, vUv - vec2(0.0, TexelSize.y)));
                float up = glowPower(texture(Bloom, vUv + vec2(0.0, TexelSize.y)));
                vec2 warp = vec2(right - left, up - down)
                        * DistortionStrength * TexelSize * 18.0;

                vec4 scene = texture(Scene, clamp(vUv + warp, vec2(0.001), vec2(0.999)));
                vec4 effect = texture(Effect, vUv);

                // Saturate only where the blurred emissive mask is present. Dark smoke and the
                // charred blade body therefore retain their contrast instead of turning red.
                float energy = 1.0 - exp(-m * 1.25);
                float effectLuma = dot(effect.rgb, vec3(0.2126, 0.7152, 0.0722));
                vec3 saturatedEffect = max(vec3(0.0), mix(vec3(effectLuma), effect.rgb,
                        1.0 + energy * 0.42));
                vec3 base = scene.rgb * (1.0 - clamp(effect.a, 0.0, 1.0)) + saturatedEffect;

                // The broad halo is intentionally blood-red/orange even when its source core
                // is almost white. A small sampled-hue contribution keeps embers and petals alive.
                float bloomPeak = max(max(bloomRgb.r, bloomRgb.g), bloomRgb.b);
                vec3 sampledHue = bloomPeak > 0.0001
                        ? bloomRgb / bloomPeak : vec3(1.0, 0.10, 0.018);
                float orangeAmount = clamp(sampledHue.g * 0.72 + sampledHue.b * 0.10, 0.0, 0.62);
                vec3 bloodHue = mix(vec3(1.0, 0.035, 0.012),
                        vec3(1.0, 0.34, 0.045), orangeAmount);
                vec3 bloomHue = mix(bloodHue, sampledHue, 0.18);
                vec3 bloom = bloomHue * GlowStrength * energy * (0.55 + energy * 1.25);

                // The unblurred texture retains the small, HDR-hot source that the old
                // in-place blur erased. Push strong sources from gold into near-white.
                vec3 rawRgb = max(rawGlow.rgb, vec3(0.0));
                float rawPower = clamp(glowPower(rawGlow), 0.0, 6.0);
                float rawPeak = max(max(rawRgb.r, rawRgb.g), rawRgb.b);
                vec3 rawHue = rawPeak > 0.0001 ? rawRgb / rawPeak : vec3(1.0, 0.82, 0.42);
                float coreEnergy = 1.0 - exp(-rawPower * 1.35);
                vec3 coreHue = mix(rawHue, vec3(1.0, 0.98, 0.86),
                        smoothstep(0.18, 0.88, coreEnergy));
                vec3 core = coreHue * CoreStrength * coreEnergy * (0.65 + coreEnergy * 1.45);
                fragColor = vec4(base + bloom + core, scene.a);
            }
            """;

    private static final String RAIDEN_COMPOSITE_FRAGMENT = """
            #version 150

            uniform sampler2D Scene;
            uniform sampler2D Effect;
            uniform sampler2D Glow;
            uniform sampler2D Bloom;
            uniform vec2 TexelSize;
            uniform float GlowStrength;
            uniform float DistortionStrength;

            in vec2 vUv;
            out vec4 fragColor;

            float powerOf(vec4 c) {
                return max(c.a, max(max(c.r, c.g), c.b));
            }

            void main() {
                vec4 bloomSample = texture(Bloom, vUv);
                float m = clamp(powerOf(bloomSample), 0.0, 5.0);
                float left = powerOf(texture(Bloom, vUv - vec2(TexelSize.x, 0.0)));
                float right = powerOf(texture(Bloom, vUv + vec2(TexelSize.x, 0.0)));
                float down = powerOf(texture(Bloom, vUv - vec2(0.0, TexelSize.y)));
                float up = powerOf(texture(Bloom, vUv + vec2(0.0, TexelSize.y)));
                vec2 warp = vec2(right - left, up - down)
                    * DistortionStrength * TexelSize * 15.0;

                vec4 scene = texture(Scene, clamp(vUv + warp, vec2(0.001), vec2(0.999)));
                vec4 effect = texture(Effect, vUv);
                vec4 rawGlow = texture(Glow, vUv);
                float coverage = clamp(effect.a, 0.0, 1.0);
                vec3 base = scene.rgb * (1.0 - coverage) + max(effect.rgb, vec3(0.0));

                vec3 bloomRgb = max(bloomSample.rgb, vec3(0.0));
                float peak = max(max(bloomRgb.r, bloomRgb.g), bloomRgb.b);
                vec3 sampledHue = peak > 0.0001
                    ? bloomRgb / peak : vec3(0.12, 0.78, 1.0);
                float floorChannel = min(min(sampledHue.r, sampledHue.g), sampledHue.b);
                float chroma = 1.0 - floorChannel;
                vec3 cyanBias = vec3(0.10, 0.72, 1.0);
                vec3 bloomHue = mix(cyanBias, sampledHue, 0.34 + chroma * 0.46);
                float halo = 1.0 - exp(-m * 1.28);
                vec3 bloom = bloomHue * GlowStrength * halo * (0.42 + halo * 1.08);
                vec3 core = max(rawGlow.rgb, vec3(0.0)) * 0.30;
                fragColor = vec4(base + core + bloom, scene.a);
            }
            """;

    private static final String HONKAI_COMPOSITE_FRAGMENT = """
            #version 150

            uniform sampler2D Scene;
            uniform sampler2D Effect;
            uniform sampler2D Bloom;
            uniform vec2 TexelSize;
            uniform float GlowStrength;
            uniform float DistortionStrength;

            in vec2 vUv;
            out vec4 fragColor;

            float powerOf(vec4 c) {
                return max(c.a, max(max(c.r, c.g), c.b));
            }

            void main() {
                vec4 bloomSample = texture(Bloom, vUv);
                float m = clamp(powerOf(bloomSample), 0.0, 5.0);
                float left = powerOf(texture(Bloom, vUv - vec2(TexelSize.x, 0.0)));
                float right = powerOf(texture(Bloom, vUv + vec2(TexelSize.x, 0.0)));
                float down = powerOf(texture(Bloom, vUv - vec2(0.0, TexelSize.y)));
                float up = powerOf(texture(Bloom, vUv + vec2(0.0, TexelSize.y)));
                vec2 warp = vec2(right - left, up - down)
                        * DistortionStrength * TexelSize * 14.0;

                vec4 scene = texture(Scene, clamp(vUv + warp, vec2(0.001), vec2(0.999)));
                vec4 effect = texture(Effect, vUv);
                float coverage = clamp(effect.a, 0.0, 1.0);
                vec3 base = scene.rgb * (1.0 - coverage) + max(effect.rgb, vec3(0.0));

                // Keep the sampled cyan/violet hue. Unlike the Bloodfyre compositor there is
                // deliberately no warm fallback, so white ice cores and amethyst filaments do
                // not collapse into a shared yellow halo under a shader pack.
                vec3 bloomRgb = max(bloomSample.rgb, vec3(0.0));
                float peak = max(max(bloomRgb.r, bloomRgb.g), bloomRgb.b);
                vec3 hue = peak > 0.0001 ? bloomRgb / peak : vec3(0.82, 0.90, 1.0);
                float floorChannel = min(min(hue.r, hue.g), hue.b);
                float chroma = 1.0 - floorChannel;
                vec3 pureHue = chroma > 0.001
                        ? (hue - vec3(floorChannel)) / chroma : hue;
                vec3 bloomHue = mix(hue, pureHue, chroma * 0.42);
                float halo = 1.0 - exp(-m * 1.34);
                vec3 bloom = bloomHue * GlowStrength * halo * (0.38 + halo * 1.02);
                fragColor = vec4(base + bloom, scene.a);
            }
            """;

    private static final String RAIZAN_COMPOSITE_FRAGMENT = """
            #version 150

            uniform sampler2D Scene;
            uniform sampler2D Effect;
            uniform sampler2D Glow;
            uniform sampler2D Bloom;
            uniform vec2 TexelSize;
            uniform float GlowStrength;
            uniform float DistortionStrength;
            uniform float ChromaticStrength;
            uniform float FlashScale;

            in vec2 vUv;
            out vec4 fragColor;

            float powerOf(vec4 c) {
                return max(c.a, max(max(c.r, c.g), c.b));
            }

            void main() {
                vec4 bloomSample = texture(Bloom, vUv);
                float m = clamp(powerOf(bloomSample), 0.0, 6.0);
                float left = powerOf(texture(Bloom, vUv - vec2(TexelSize.x, 0.0)));
                float right = powerOf(texture(Bloom, vUv + vec2(TexelSize.x, 0.0)));
                float down = powerOf(texture(Bloom, vUv - vec2(0.0, TexelSize.y)));
                float up = powerOf(texture(Bloom, vUv + vec2(0.0, TexelSize.y)));
                vec2 gradient = vec2(right - left, up - down);
                vec2 warp = gradient * DistortionStrength * TexelSize * 13.0;

                vec4 scene = texture(Scene, clamp(vUv + warp, vec2(0.001), vec2(0.999)));
                vec4 effect = texture(Effect, vUv);
                float energy = 1.0 - exp(-m * 1.35);

                // The split is sampled only inside the Raizan mask and never exceeds 1.5 px.
                vec2 split = normalize(gradient + vec2(0.0001)) * TexelSize
                        * min(1.5, ChromaticStrength * energy * 1.5);
                vec4 effectR = texture(Effect, clamp(vUv + split, vec2(0.001), vec2(0.999)));
                vec4 effectB = texture(Effect, clamp(vUv - split, vec2(0.001), vec2(0.999)));
                vec3 splitEffect = vec3(effectR.r, effect.g, effectB.b);
                vec3 effectRgb = mix(max(effect.rgb, vec3(0.0)), max(splitEffect, vec3(0.0)),
                        energy * 0.34);
                // Every Raizan layer is emissive.  Never use its accumulated alpha
                // to subtract the scene: broad halos otherwise wash the whole view
                // grey, especially when an Oculus HDR target is composited back.
                vec3 base = scene.rgb + effectRgb;

                vec3 bloomRgb = max(bloomSample.rgb, vec3(0.0));
                float peak = max(max(bloomRgb.r, bloomRgb.g), bloomRgb.b);
                vec3 hue = peak > 0.0001 ? bloomRgb / peak : vec3(0.68, 0.18, 1.0);
                vec3 violet = mix(vec3(0.38, 0.06, 0.92), hue, 0.58);
                vec3 bloom = violet * GlowStrength * energy * (0.42 + energy * 1.10);

                vec3 raw = max(texture(Glow, vUv).rgb, vec3(0.0));
                float rawPower = clamp(powerOf(texture(Glow, vUv)), 0.0, 5.0);
                float coreEnergy = (1.0 - exp(-rawPower * 1.55)) * FlashScale;
                vec3 coreHue = mix(raw, vec3(0.96, 0.90, 1.0), coreEnergy * 0.72);
                vec3 core = coreHue * coreEnergy * 0.62;
                fragColor = vec4(base + bloom + core, scene.a);
            }
            """;

    private static int blurProgram;
    private static int compositeProgram;
    private static int bloodfyreCompositeProgram;
    private static int raidenCompositeProgram;
    private static int honkaiCompositeProgram;
    private static int raizanCompositeProgram;
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
                                          int glowTextureId, int bloomTextureId,
                                          int width, int height) {
        ensurePrograms();
        withFullscreenState(() -> {
            GL20.glUseProgram(bloodfyreCompositeProgram);
            bindTexture(0, sceneTextureId);
            bindTexture(1, effectTextureId);
            bindTexture(2, glowTextureId);
            bindTexture(3, bloomTextureId);
            GL20.glUniform1i(GL20.glGetUniformLocation(bloodfyreCompositeProgram, "Scene"), 0);
            GL20.glUniform1i(GL20.glGetUniformLocation(bloodfyreCompositeProgram, "Effect"), 1);
            GL20.glUniform1i(GL20.glGetUniformLocation(bloodfyreCompositeProgram, "Glow"), 2);
            GL20.glUniform1i(GL20.glGetUniformLocation(bloodfyreCompositeProgram, "Bloom"), 3);
            GL20.glUniform2f(GL20.glGetUniformLocation(bloodfyreCompositeProgram, "TexelSize"),
                    1.0F / Math.max(1, width), 1.0F / Math.max(1, height));
            GL20.glUniform1f(GL20.glGetUniformLocation(bloodfyreCompositeProgram, "CoreStrength"), 0.85F);
            GL20.glUniform1f(GL20.glGetUniformLocation(bloodfyreCompositeProgram, "GlowStrength"), 1.65F);
            GL20.glUniform1f(GL20.glGetUniformLocation(bloodfyreCompositeProgram, "DistortionStrength"), 0.12F);
            drawFullscreenTriangle();
        });
    }

    public static void compositeRaiden(int sceneTextureId, int effectTextureId,
                                       int glowTextureId, int bloomTextureId,
                                       int width, int height) {
        ensurePrograms();
        withFullscreenState(() -> {
            GL20.glUseProgram(raidenCompositeProgram);
            bindTexture(0, sceneTextureId);
            bindTexture(1, effectTextureId);
            bindTexture(2, glowTextureId);
            bindTexture(3, bloomTextureId);
            GL20.glUniform1i(GL20.glGetUniformLocation(raidenCompositeProgram, "Scene"), 0);
            GL20.glUniform1i(GL20.glGetUniformLocation(raidenCompositeProgram, "Effect"), 1);
            GL20.glUniform1i(GL20.glGetUniformLocation(raidenCompositeProgram, "Glow"), 2);
            GL20.glUniform1i(GL20.glGetUniformLocation(raidenCompositeProgram, "Bloom"), 3);
            GL20.glUniform2f(GL20.glGetUniformLocation(raidenCompositeProgram, "TexelSize"),
                    1.0F / Math.max(1, width), 1.0F / Math.max(1, height));
            GL20.glUniform1f(GL20.glGetUniformLocation(raidenCompositeProgram, "GlowStrength"), 0.92F);
            GL20.glUniform1f(GL20.glGetUniformLocation(raidenCompositeProgram, "DistortionStrength"), 0.10F);
            drawFullscreenTriangle();
        });
    }

    public static void compositeHonkai(int sceneTextureId, int effectTextureId,
                                       int bloomTextureId, int width, int height) {
        ensurePrograms();
        withFullscreenState(() -> {
            GL20.glUseProgram(honkaiCompositeProgram);
            bindTexture(0, sceneTextureId);
            bindTexture(1, effectTextureId);
            bindTexture(2, bloomTextureId);
            GL20.glUniform1i(GL20.glGetUniformLocation(honkaiCompositeProgram, "Scene"), 0);
            GL20.glUniform1i(GL20.glGetUniformLocation(honkaiCompositeProgram, "Effect"), 1);
            GL20.glUniform1i(GL20.glGetUniformLocation(honkaiCompositeProgram, "Bloom"), 2);
            GL20.glUniform2f(GL20.glGetUniformLocation(honkaiCompositeProgram, "TexelSize"),
                    1.0F / Math.max(1, width), 1.0F / Math.max(1, height));
            GL20.glUniform1f(GL20.glGetUniformLocation(honkaiCompositeProgram, "GlowStrength"), 0.96F);
            GL20.glUniform1f(GL20.glGetUniformLocation(honkaiCompositeProgram, "DistortionStrength"), 0.10F);
            drawFullscreenTriangle();
        });
    }

    public static void compositeRaizan(int sceneTextureId, int effectTextureId,
                                       int glowTextureId, int bloomTextureId,
                                       int width, int height, float glowStrength,
                                       float chromaticStrength, float flashScale) {
        ensurePrograms();
        withFullscreenState(() -> {
            GL20.glUseProgram(raizanCompositeProgram);
            bindTexture(0, sceneTextureId);
            bindTexture(1, effectTextureId);
            bindTexture(2, glowTextureId);
            bindTexture(3, bloomTextureId);
            GL20.glUniform1i(GL20.glGetUniformLocation(raizanCompositeProgram, "Scene"), 0);
            GL20.glUniform1i(GL20.glGetUniformLocation(raizanCompositeProgram, "Effect"), 1);
            GL20.glUniform1i(GL20.glGetUniformLocation(raizanCompositeProgram, "Glow"), 2);
            GL20.glUniform1i(GL20.glGetUniformLocation(raizanCompositeProgram, "Bloom"), 3);
            GL20.glUniform2f(GL20.glGetUniformLocation(raizanCompositeProgram, "TexelSize"),
                    1.0F / Math.max(1, width), 1.0F / Math.max(1, height));
            GL20.glUniform1f(GL20.glGetUniformLocation(raizanCompositeProgram, "GlowStrength"), glowStrength);
            GL20.glUniform1f(GL20.glGetUniformLocation(raizanCompositeProgram, "DistortionStrength"), 0.09F);
            GL20.glUniform1f(GL20.glGetUniformLocation(raizanCompositeProgram, "ChromaticStrength"), chromaticStrength);
            GL20.glUniform1f(GL20.glGetUniformLocation(raizanCompositeProgram, "FlashScale"), flashScale);
            drawFullscreenTriangle();
        });
    }

    /** Recreates the Raizan-only compositor after F3+T or a render-target transition. */
    public static void resetRaizan() {
        RenderSystem.assertOnRenderThread();
        if (raizanCompositeProgram != 0) {
            GL20.glDeleteProgram(raizanCompositeProgram);
            raizanCompositeProgram = 0;
        }
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
        if (raidenCompositeProgram == 0) {
            raidenCompositeProgram = createProgram(RAIDEN_COMPOSITE_FRAGMENT);
        }
        if (honkaiCompositeProgram == 0) {
            honkaiCompositeProgram = createProgram(HONKAI_COMPOSITE_FRAGMENT);
        }
        if (raizanCompositeProgram == 0) {
            raizanCompositeProgram = createProgram(RAIZAN_COMPOSITE_FRAGMENT);
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
        int previousTexture3 = textureBinding2D(3);
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
            restoreTextureBinding2D(3, previousTexture3);
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

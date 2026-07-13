package com.wjx.kablade.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.wjx.kablade.client.shader.ShaderCompat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Client render types that need state combinations not exposed by vanilla helpers. */
public final class KabladeRenderTypes extends RenderType {
    private static final ResourceLocation FALLBACK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");

    private static final RenderStateShard.ShaderStateShard STAGE_LIGHT_SHADER =
            new RenderStateShard.ShaderStateShard(KabladeShaders::stageLight);
    private static final RenderStateShard.ShaderStateShard SHOCK_IMPACT_SHADER =
            new RenderStateShard.ShaderStateShard(KabladeShaders::shockImpact);
    private static final RenderStateShard.ShaderStateShard ZAIZAN_SHADER =
            new RenderStateShard.ShaderStateShard(KabladeShaders::zaizan);
    private static final RenderStateShard.ShaderStateShard UTPALA_AURA_SHADER =
            new RenderStateShard.ShaderStateShard(KabladeShaders::utpalaAura);
    private static final RenderStateShard.ShaderStateShard SWORD_ENLIGHTENMENT_SHADER =
            new RenderStateShard.ShaderStateShard(KabladeShaders::swordEnlightenment);
    private static final RenderStateShard.ShaderStateShard BLOODFYRE_FRENZY_SHADER =
            new RenderStateShard.ShaderStateShard(KabladeShaders::bloodfyreFrenzy);
    private static final RenderStateShard.ShaderStateShard BLOODFYRE_RUPTURE_SHADER =
            new RenderStateShard.ShaderStateShard(KabladeShaders::bloodfyreRupture);
    private static final RenderStateShard.ShaderStateShard BLOODFYRE_SMOKE_SHADER =
            new RenderStateShard.ShaderStateShard(KabladeShaders::bloodfyreSmoke);
    private static final RenderStateShard.ShaderStateShard BLOODFYRE_SCAR_SHADER =
            new RenderStateShard.ShaderStateShard(KabladeShaders::bloodfyreScar);
    private static final RenderStateShard.ShaderStateShard BLOODFYRE_PARTICLE_SHADER =
            new RenderStateShard.ShaderStateShard(KabladeShaders::bloodfyreParticle);

    private static final RenderType INDUCTION_COLLAPSE = create(
            "kablade_induction_collapse",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            65536,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private static final RenderType UTPALA_AURA = create(
            "kablade_utpala_aura",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,
            131072,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(UTPALA_AURA_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private static final RenderType UTPALA_AURA_VEIL = create(
            "kablade_utpala_aura_veil",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,
            32768,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(UTPALA_AURA_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private static final RenderType UTPALA_AURA_FALLBACK = shaderFallback(
            "kablade_utpala_aura_fallback",
            131072,
            LIGHTNING_TRANSPARENCY);

    private static final RenderType UTPALA_AURA_VEIL_FALLBACK = shaderFallback(
            "kablade_utpala_aura_veil_fallback",
            32768,
            TRANSLUCENT_TRANSPARENCY);

    private static final RenderType SWORD_ENLIGHTENMENT = create(
            "kablade_sword_enlightenment",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,
            131072,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(SWORD_ENLIGHTENMENT_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private static final RenderType SWORD_ENLIGHTENMENT_FALLBACK = shaderFallback(
            "kablade_sword_enlightenment_fallback",
            131072,
            LIGHTNING_TRANSPARENCY);

    private static final RenderType BLOODFYRE_FRENZY = create(
            "kablade_bloodfyre_frenzy",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,
            262144,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(BLOODFYRE_FRENZY_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private static final RenderType BLOODFYRE_BLADE_DARK = create(
            "kablade_bloodfyre_blade_dark",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,
            262144,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(BLOODFYRE_FRENZY_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private static final RenderType BLOODFYRE_SMOKE = create(
            "kablade_bloodfyre_smoke",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,
            131072,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(BLOODFYRE_SMOKE_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private static final RenderType BLOODFYRE_VOLUME = create(
            "kablade_bloodfyre_volume",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,
            262144,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(BLOODFYRE_RUPTURE_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private static final RenderType BLOODFYRE_SMOKE_VOLUME = create(
            "kablade_bloodfyre_smoke_volume",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,
            131072,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(BLOODFYRE_SCAR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private static final RenderType BLOODFYRE_SCAR_GLOW = create(
            "kablade_bloodfyre_scar_glow",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,
            131072,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(BLOODFYRE_SCAR_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private static final RenderType BLOODFYRE_PARTICLE = create(
            "kablade_bloodfyre_particle",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,
            131072,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(BLOODFYRE_PARTICLE_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private static final RenderType BLOODFYRE_FRENZY_FALLBACK = shaderFallback(
            "kablade_bloodfyre_frenzy_fallback",
            FALLBACK_TEXTURE,
            262144,
            LIGHTNING_TRANSPARENCY);

    private static final RenderType BLOODFYRE_BLADE_DARK_FALLBACK = shaderFallback(
            "kablade_bloodfyre_blade_dark_fallback", FALLBACK_TEXTURE, 262144, TRANSLUCENT_TRANSPARENCY);

    private static final RenderType BLOODFYRE_SMOKE_FALLBACK = shaderFallback(
            "kablade_bloodfyre_smoke_fallback",
            FALLBACK_TEXTURE,
            131072,
            TRANSLUCENT_TRANSPARENCY);

    private static final RenderType BLOODFYRE_VOLUME_FALLBACK = bloodfyreVolumeFallback(
            "kablade_bloodfyre_volume_fallback", 262144, TRANSLUCENT_TRANSPARENCY);

    private static final RenderType BLOODFYRE_SMOKE_VOLUME_FALLBACK = bloodfyreVolumeFallback(
            "kablade_bloodfyre_smoke_volume_fallback", 131072, TRANSLUCENT_TRANSPARENCY);

    private static final RenderType BLOODFYRE_SCAR_GLOW_FALLBACK = shaderFallback(
            "kablade_bloodfyre_scar_glow_fallback", FALLBACK_TEXTURE, 131072, TRANSLUCENT_TRANSPARENCY);

    private static final RenderType BLOODFYRE_PARTICLE_FALLBACK = shaderFallback(
            "kablade_bloodfyre_particle_fallback", FALLBACK_TEXTURE, 131072, TRANSLUCENT_TRANSPARENCY);

    // A second, low-alpha pass through Minecraft's stock textured/additive shader.
    // Shader packs recognize this path more reliably than KBlade's analytic shaders.
    private static final RenderType BLOODFYRE_VANILLA_GLOW = shaderFallback(
            "kablade_bloodfyre_vanilla_glow", FALLBACK_TEXTURE, 262144, LIGHTNING_TRANSPARENCY);

    // Shader-pack-safe path: no texture sampling and no QUADS conversion. Explicit
    // triangles avoid the per-quad diagonal seams produced by some Oculus pipelines.
    private static final RenderType BLOODFYRE_SHADER_PACK_FALLBACK = create(
            "kablade_bloodfyre_shader_pack_fallback",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLES,
            262144,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private static final RenderType STAGE_LIGHT = create(
            "kablade_stage_light",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,
            4096,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(STAGE_LIGHT_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private static final RenderType STAGE_LIGHT_FALLBACK = shaderFallback(
            "kablade_stage_light_fallback",
            4096,
            LIGHTNING_TRANSPARENCY);

    /** Opaque singularity surface; writes depth so rear accretion geometry is occluded. */
    private static final RenderType VORPAL_BLACK_HOLE_CORE = create(
            "kablade_vorpal_black_hole_core",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLES,
            131072,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .createCompositeState(false));

    /** Alpha-blended smoke, dark funnel, black rupture wedges, and blue-black streaks. */
    private static final RenderType VORPAL_BLACK_HOLE_DARK = create(
            "kablade_vorpal_black_hole_dark",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLES,
            262144,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    /** Additive hot funnel, lensing, spikes, slash ribbons, sparks, and impact core. */
    private static final RenderType VORPAL_BLACK_HOLE_GLOW = create(
            "kablade_vorpal_black_hole_glow",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLES,
            524288,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private static final RenderType SHOCK_IMPACT = create(
            "kablade_shock_impact",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,
            32768,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(SHOCK_IMPACT_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private static final RenderType SHOCK_IMPACT_FALLBACK = shaderFallback(
            "kablade_shock_impact_fallback",
            FALLBACK_TEXTURE,
            32768,
            TRANSLUCENT_TRANSPARENCY);

    private static final RenderType SHOCK_IMPACT_LINES = create(
            "kablade_shock_impact_lines",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            32768,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private static final RenderType ZAIZAN = create(
            "kablade_zaizan",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,
            65536,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(ZAIZAN_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private static final RenderType ZAIZAN_FALLBACK = shaderFallback(
            "kablade_zaizan_fallback",
            FALLBACK_TEXTURE,
            65536,
            LIGHTNING_TRANSPARENCY);

    private KabladeRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                               boolean affectsCrumbling, boolean sortOnUpload,
                               Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType huntingLocker(ResourceLocation texture) {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setDepthTestState(NO_DEPTH_TEST)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
        return RenderType.create("kablade_hunting_locker", DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, 256, false, true, state);
    }

    public static RenderType magChaosBlade(ResourceLocation texture) {
        CompositeState state = CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
        return create("kablade_mag_chaos_blade",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, 256, false, true, state);
    }

    public static RenderType stageLight() {
        return useShaderFallbackTextures() ? STAGE_LIGHT_FALLBACK : STAGE_LIGHT;
    }

    public static RenderType vorpalBlackHoleCore() {
        return VORPAL_BLACK_HOLE_CORE;
    }

    public static RenderType vorpalBlackHoleDark() {
        return VORPAL_BLACK_HOLE_DARK;
    }

    public static RenderType vorpalBlackHoleGlow() {
        return VORPAL_BLACK_HOLE_GLOW;
    }

    public static RenderType shockImpact() {
        return useShaderFallbackTextures() ? SHOCK_IMPACT_FALLBACK : SHOCK_IMPACT;
    }

    public static RenderType shockImpactLines() {
        return SHOCK_IMPACT_LINES;
    }

    public static RenderType zaizan() {
        return useShaderFallbackTextures() ? ZAIZAN_FALLBACK : ZAIZAN;
    }

    public static RenderType inductionCollapse() {
        return INDUCTION_COLLAPSE;
    }

    public static RenderType utpalaAura() {
        return useShaderFallbackTextures() ? UTPALA_AURA_FALLBACK : UTPALA_AURA;
    }

    public static RenderType utpalaAuraVeil() {
        return useShaderFallbackTextures() ? UTPALA_AURA_VEIL_FALLBACK : UTPALA_AURA_VEIL;
    }

    public static RenderType swordEnlightenment() {
        return useShaderFallbackTextures() ? SWORD_ENLIGHTENMENT_FALLBACK : SWORD_ENLIGHTENMENT;
    }

    public static RenderType bloodfyreFrenzy() {
        return useShaderFallbackTextures() ? BLOODFYRE_FRENZY_FALLBACK : BLOODFYRE_FRENZY;
    }

    public static RenderType bloodfyreBlade() {
        return bloodfyreFrenzy();
    }

    public static RenderType bloodfyreBladeDark() {
        return useShaderFallbackTextures() ? BLOODFYRE_BLADE_DARK_FALLBACK : BLOODFYRE_BLADE_DARK;
    }

    public static RenderType bloodfyreRupture() {
        return useShaderFallbackTextures() ? BLOODFYRE_VOLUME_FALLBACK : BLOODFYRE_VOLUME;
    }

    public static RenderType bloodfyreSmoke() {
        return useShaderFallbackTextures() ? BLOODFYRE_SMOKE_FALLBACK : BLOODFYRE_SMOKE;
    }

    public static RenderType bloodfyreVolume() {
        return useShaderFallbackTextures() ? BLOODFYRE_VOLUME_FALLBACK : BLOODFYRE_VOLUME;
    }

    public static RenderType bloodfyreSmokeVolume() {
        return useShaderFallbackTextures() ? BLOODFYRE_SMOKE_VOLUME_FALLBACK : BLOODFYRE_SMOKE_VOLUME;
    }

    public static RenderType bloodfyreScar() {
        return bloodfyreSmokeVolume();
    }

    public static RenderType bloodfyreScarGlow() {
        return useShaderFallbackTextures() ? BLOODFYRE_SCAR_GLOW_FALLBACK : BLOODFYRE_SCAR_GLOW;
    }

    public static RenderType bloodfyreParticle() {
        return useShaderFallbackTextures() ? BLOODFYRE_PARTICLE_FALLBACK : BLOODFYRE_PARTICLE;
    }

    public static RenderType bloodfyreVanillaGlow() {
        return BLOODFYRE_VANILLA_GLOW;
    }

    public static RenderType bloodfyreShaderPackFallback() {
        return BLOODFYRE_SHADER_PACK_FALLBACK;
    }

    public static boolean useShaderFallbackTextures() {
        return ShaderCompat.shouldUseOculusPostPath();
    }

    public static float stageLightU(float u) {
        return u;
    }

    public static float shockImpactU(float u) {
        return u;
    }

    public static float zaizanU(float u) {
        return u;
    }

    public static float swordEnlightenmentU(float u) {
        return u;
    }

    public static float fallbackAlpha(float alpha, float multiplier) {
        return useShaderFallbackTextures() ? alpha * multiplier : alpha;
    }

    private static RenderType shaderFallback(String name, int bufferSize,
                                             RenderStateShard.TransparencyStateShard transparency) {
        CompositeState state = CompositeState.builder()
                .setShaderState(POSITION_COLOR_SHADER)
                .setTransparencyState(transparency)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
        return create(name, DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS, bufferSize, false, true, state);
    }

    private static RenderType shaderFallback(String name, ResourceLocation texture, int bufferSize,
                                             RenderStateShard.TransparencyStateShard transparency) {
        CompositeState state = CompositeState.builder()
                .setShaderState(POSITION_COLOR_TEX_SHADER)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(transparency)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
        return create(name, DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.QUADS, bufferSize, false, true, state);
    }

    private static RenderType bloodfyreVolumeFallback(String name, int bufferSize,
                                                       RenderStateShard.TransparencyStateShard transparency) {
        CompositeState state = CompositeState.builder()
                .setShaderState(POSITION_COLOR_TEX_SHADER)
                .setTextureState(new TextureStateShard(FALLBACK_TEXTURE, false, false))
                .setTransparencyState(transparency)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
        return create(name, DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.QUADS, bufferSize, false, true, state);
    }

    /**
     * 风之结界 / 风之力纹理用 additive 混合 RenderType。
     * 使用 {@link DefaultVertexFormat#POSITION_COLOR_TEX} 简化顶点格式，
     * 只需要 vertex → color → uv，无需 lightmap/overlay/normal。
     */
    public static RenderType windEnchantment(ResourceLocation tex) {
        CompositeState state = CompositeState.builder()
                .setShaderState(POSITION_COLOR_TEX_SHADER)
                .setTextureState(new TextureStateShard(tex, false, false))
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
        return create("kablade_wind_enchantment",
                DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.QUADS, 256, false, true, state);
    }

    /** Key of Castigation's Thunder Edge uses additive textured quads like the original 1.12 renderer. */
    public static RenderType thunderEdge(ResourceLocation tex) {
        CompositeState state = CompositeState.builder()
                .setShaderState(POSITION_COLOR_TEX_SHADER)
                .setTextureState(new TextureStateShard(tex, false, false))
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
        return create("kablade_thunder_edge",
                DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.QUADS, 4096, false, true, state);
    }

    /** Snow Dance's freeze domain uses the same additive blend as the 1.12.2 renderer. */
    public static RenderType freezeDomain(ResourceLocation tex) {
        CompositeState state = CompositeState.builder()
                .setShaderState(POSITION_COLOR_TEX_SHADER)
                .setTextureState(new TextureStateShard(tex, false, false))
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
        return create("kablade_freeze_domain",
                DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.QUADS, 256, false, true, state);
    }

    /** Pledge of Rain petals use the standard entity layout so shader packs read the vertex data correctly. */
    public static RenderType rainPetal(ResourceLocation tex) {
        CompositeState state = CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                .setTextureState(new TextureStateShard(tex, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
        return create("kablade_rain_petal",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, 2048, false, true, state);
    }

    public static RenderType rainPetalBloom(ResourceLocation tex) {
        CompositeState state = CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                .setTextureState(new TextureStateShard(tex, false, false))
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
        return create("kablade_rain_petal_bloom",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, 2048, false, true, state);
    }

    public static RenderType rainEndingRing() {
        CompositeState state = CompositeState.builder()
                .setShaderState(POSITION_COLOR_SHADER)
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setDepthTestState(NO_DEPTH_TEST)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
        return create("kablade_rain_ending_ring",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS, 4096, false, true, state);
    }
}

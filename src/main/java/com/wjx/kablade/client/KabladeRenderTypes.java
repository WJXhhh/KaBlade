package com.wjx.kablade.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Client render types that need state combinations not exposed by vanilla helpers. */
public final class KabladeRenderTypes extends RenderType {

    private static final RenderStateShard.ShaderStateShard STAGE_LIGHT_SHADER =
            new RenderStateShard.ShaderStateShard(KabladeShaders::stageLight);
    private static final RenderStateShard.ShaderStateShard VORPAL_BLACK_HOLE_SHADER =
            new RenderStateShard.ShaderStateShard(KabladeShaders::vorpalBlackHole);
    private static final RenderStateShard.ShaderStateShard SHOCK_IMPACT_SHADER =
            new RenderStateShard.ShaderStateShard(KabladeShaders::shockImpact);
    private static final RenderStateShard.ShaderStateShard ZAIZAN_SHADER =
            new RenderStateShard.ShaderStateShard(KabladeShaders::zaizan);

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

    private static final RenderType VORPAL_BLACK_HOLE = create(
            "kablade_vorpal_black_hole",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,
            65536,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(VORPAL_BLACK_HOLE_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
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

    public static RenderType stageLight() {
        return STAGE_LIGHT;
    }

    public static RenderType vorpalBlackHole() {
        return VORPAL_BLACK_HOLE;
    }

    public static RenderType shockImpact() {
        return SHOCK_IMPACT;
    }

    public static RenderType zaizan() {
        return ZAIZAN;
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
                .setCullState(CULL)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
        return create("kablade_wind_enchantment",
                DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.QUADS, 256, false, true, state);
    }
}

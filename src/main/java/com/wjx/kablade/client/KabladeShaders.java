package com.wjx.kablade.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.wjx.kablade.Main;
import com.wjx.kablade.client.renderer.BloodfyreOculusPipeline;
import com.wjx.kablade.client.renderer.RaidenCycloneOculusPipeline;
import com.wjx.kablade.client.renderer.ShockImpactOculusPipeline;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

/** Client shader registration for analytic SA effects. */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class KabladeShaders {

    private static ShaderInstance stageLight;
    private static ShaderInstance shockImpact;
    private static ShaderInstance raidenCyclone;
    private static ShaderInstance zaizan;
    private static ShaderInstance utpalaAura;
    private static ShaderInstance swordEnlightenment;
    private static ShaderInstance bloodfyreFrenzy;
    private static ShaderInstance bloodfyreRupture;
    private static ShaderInstance bloodfyreSmoke;
    private static ShaderInstance bloodfyreScar;
    private static ShaderInstance bloodfyreParticle;

    private KabladeShaders() {
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        BloodfyreOculusPipeline.invalidateResources();
        RaidenCycloneOculusPipeline.invalidateResources();
        ShockImpactOculusPipeline.invalidateResources();
        event.registerShader(new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(Main.MODID, "stage_light"),
                        DefaultVertexFormat.POSITION_COLOR_TEX),
                shader -> stageLight = shader);
        event.registerShader(new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(Main.MODID, "shock_impact"),
                        DefaultVertexFormat.POSITION_COLOR_TEX),
                shader -> shockImpact = shader);
        event.registerShader(new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(Main.MODID, "raiden_cyclone"),
                        DefaultVertexFormat.POSITION_COLOR_TEX),
                shader -> raidenCyclone = shader);
        event.registerShader(new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(Main.MODID, "zaizan"),
                        DefaultVertexFormat.POSITION_COLOR_TEX),
                shader -> zaizan = shader);
        event.registerShader(new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(Main.MODID, "utpala_aura"),
                        DefaultVertexFormat.POSITION_COLOR_TEX),
                shader -> utpalaAura = shader);
        event.registerShader(new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(Main.MODID, "sword_enlightenment"),
                        DefaultVertexFormat.POSITION_COLOR_TEX),
                shader -> swordEnlightenment = shader);
        event.registerShader(new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(Main.MODID, "bloodfyre_frenzy"),
                        DefaultVertexFormat.POSITION_COLOR_TEX),
                shader -> bloodfyreFrenzy = shader);
        event.registerShader(new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(Main.MODID, "bloodfyre_rupture"),
                        DefaultVertexFormat.POSITION_COLOR_TEX),
                shader -> bloodfyreRupture = shader);
        event.registerShader(new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(Main.MODID, "bloodfyre_smoke"),
                        DefaultVertexFormat.POSITION_COLOR_TEX),
                shader -> bloodfyreSmoke = shader);
        event.registerShader(new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(Main.MODID, "bloodfyre_scar"),
                        DefaultVertexFormat.POSITION_COLOR_TEX),
                shader -> bloodfyreScar = shader);
        event.registerShader(new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(Main.MODID, "bloodfyre_particle"),
                        DefaultVertexFormat.POSITION_COLOR_TEX),
                shader -> bloodfyreParticle = shader);
    }

    public static ShaderInstance stageLight() {
        return stageLight;
    }

    public static ShaderInstance shockImpact() {
        return shockImpact;
    }

    public static ShaderInstance raidenCyclone() {
        return raidenCyclone;
    }

    public static ShaderInstance zaizan() {
        return zaizan;
    }

    public static ShaderInstance utpalaAura() {
        return utpalaAura;
    }

    public static ShaderInstance swordEnlightenment() {
        return swordEnlightenment;
    }

    public static ShaderInstance bloodfyreFrenzy() {
        return bloodfyreFrenzy;
    }

    public static ShaderInstance bloodfyreRupture() {
        return bloodfyreRupture;
    }

    public static ShaderInstance bloodfyreSmoke() {
        return bloodfyreSmoke;
    }

    public static ShaderInstance bloodfyreScar() {
        return bloodfyreScar;
    }

    public static ShaderInstance bloodfyreParticle() {
        return bloodfyreParticle;
    }
}

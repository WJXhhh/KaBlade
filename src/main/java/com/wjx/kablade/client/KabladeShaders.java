package com.wjx.kablade.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.wjx.kablade.Main;
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
    private static ShaderInstance vorpalBlackHole;
    private static ShaderInstance shockImpact;
    private static ShaderInstance zaizan;
    private static ShaderInstance utpalaAura;

    private KabladeShaders() {
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(Main.MODID, "stage_light"),
                        DefaultVertexFormat.POSITION_COLOR_TEX),
                shader -> stageLight = shader);
        event.registerShader(new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(Main.MODID, "vorpal_black_hole"),
                        DefaultVertexFormat.POSITION_COLOR_TEX),
                shader -> vorpalBlackHole = shader);
        event.registerShader(new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(Main.MODID, "shock_impact"),
                        DefaultVertexFormat.POSITION_COLOR_TEX),
                shader -> shockImpact = shader);
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
    }

    public static ShaderInstance stageLight() {
        return stageLight;
    }

    public static ShaderInstance vorpalBlackHole() {
        return vorpalBlackHole;
    }

    public static ShaderInstance shockImpact() {
        return shockImpact;
    }

    public static ShaderInstance zaizan() {
        return zaizan;
    }

    public static ShaderInstance utpalaAura() {
        return utpalaAura;
    }
}

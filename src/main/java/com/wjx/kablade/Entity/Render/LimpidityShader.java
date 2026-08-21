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
import java.util.EnumMap;
import java.util.Map;

/** 1.12.2 / GLSL 1.20 多材质管线，对应 1.20 的七个解析材质层。 */
final class LimpidityShader {
    enum Material {
        SWORD("sword_enlightenment_120",null), CONCEPTUAL("conceptual_metaphor_120",null),
        ENERGY("thunderbolt_energy_120","textures/effect/raizan_noise.png"),
        LIGHTNING("thunderbolt_lightning_120","textures/effect/raizan_noise.png"),
        CROSS("thunderbolt_cross_120","textures/effect/raizan_slash_gradient.png"),
        PARTICLE("thunderbolt_particle_120","textures/effect/raizan_particle_mask.png"),
        COMPOSITE("thunderbolt_composite_120","textures/effect/raizan_slash_gradient.png");
        final String path, texture;
        Material(String path,String texture) { this.path = path; this.texture=texture; }
    }
    private static final ResourceLocation VERTEX = new ResourceLocation(Main.MODID, "shaders/analytic_120.vsh");
    private static final Map<Material, Program> PROGRAMS = new EnumMap<>(Material.class);
    private LimpidityShader() {}

    static int bind(Material material, float age) {
        if(material.texture!=null)Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(Main.MODID,material.texture));
        if (!OpenGlHelper.shadersSupported) return 0;
        int previous = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        if (previous != 0) return previous;
        Program program = PROGRAMS.get(material);
        if (program == null) { program = createProgram(material); PROGRAMS.put(material, program); }
        if (program.id == 0) return previous;
        GL20.glUseProgram(program.id); GL20.glUniform1f(program.timeUniform, age);
        if(program.samplerUniform>=0)GL20.glUniform1i(program.samplerUniform,0);
        return previous;
    }
    static void restore(int previous) {
        if (OpenGlHelper.shadersSupported && GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) != previous)
            GL20.glUseProgram(previous);
    }
    private static Program createProgram(Material material) {
        int vertex=0,fragment=0,linked=0;
        try {
            vertex=compile(GL20.GL_VERTEX_SHADER,read(VERTEX));
            fragment=compile(GL20.GL_FRAGMENT_SHADER,read(new ResourceLocation(Main.MODID,"shaders/"+material.path+".fsh")));
            linked=GL20.glCreateProgram(); GL20.glAttachShader(linked,vertex); GL20.glAttachShader(linked,fragment); GL20.glLinkProgram(linked);
            if(GL20.glGetProgrami(linked,GL20.GL_LINK_STATUS)==GL11.GL_FALSE)
                throw new IllegalStateException(GL20.glGetProgramInfoLog(linked,8192));
            return new Program(linked,GL20.glGetUniformLocation(linked,"uTime"),
                    GL20.glGetUniformLocation(linked,"uSampler"));
        } catch(Throwable error) {
            if(linked!=0)GL20.glDeleteProgram(linked);
            if(Main.logger!=null)Main.logger.warn("Analytic shader {} unavailable; using geometry fallback",material,error);
            return new Program(0,-1,-1);
        } finally {if(vertex!=0)GL20.glDeleteShader(vertex);if(fragment!=0)GL20.glDeleteShader(fragment);}
    }
    private static int compile(int type,String source){int shader=GL20.glCreateShader(type);GL20.glShaderSource(shader,source);GL20.glCompileShader(shader);
        if(GL20.glGetShaderi(shader,GL20.GL_COMPILE_STATUS)==GL11.GL_FALSE){String log=GL20.glGetShaderInfoLog(shader,8192);GL20.glDeleteShader(shader);throw new IllegalStateException(log);}return shader;}
    private static String read(ResourceLocation location)throws Exception{StringBuilder result=new StringBuilder();
        try(InputStream stream=Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream();BufferedReader reader=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8))){String line;while((line=reader.readLine())!=null)result.append(line).append('\n');}return result.toString();}
    private static final class Program{final int id,timeUniform,samplerUniform;Program(int id,int timeUniform,int samplerUniform){this.id=id;this.timeUniform=timeUniform;this.samplerUniform=samplerUniform;}}
}

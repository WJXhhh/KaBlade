package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.io.*;
import java.nio.charset.StandardCharsets;

/** 天殛/重磁暴共用的 GLSL 1.20 材质着色器；光影占用 program 时自动回退顶点色。 */
public final class ElectricSkillShader {
    private static int program=-1,time=-1,material=-1;
    private ElectricSkillShader(){}
    public static int bind(float age,int pass){if(!OpenGlHelper.shadersSupported)return 0;int old=GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);if(old!=0)return old;if(program<0)program=create();if(program==0)return old;GL20.glUseProgram(program);GL20.glUniform1f(time,age);GL20.glUniform1i(material,pass);return old;}
    public static void restore(int old){if(OpenGlHelper.shadersSupported&&GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM)!=old)GL20.glUseProgram(old);}
    public static void reset(){if(program>0)GL20.glDeleteProgram(program);program=-1;}
    private static int create(){int v=0,f=0,p=0;try{v=compile(GL20.GL_VERTEX_SHADER,read(new ResourceLocation(Main.MODID,"shaders/electric_skill_120.vsh")));f=compile(GL20.GL_FRAGMENT_SHADER,read(new ResourceLocation(Main.MODID,"shaders/electric_skill_120.fsh")));p=GL20.glCreateProgram();GL20.glAttachShader(p,v);GL20.glAttachShader(p,f);GL20.glLinkProgram(p);if(GL20.glGetProgrami(p,GL20.GL_LINK_STATUS)==0)throw new IllegalStateException(GL20.glGetProgramInfoLog(p,4096));time=GL20.glGetUniformLocation(p,"uTime");material=GL20.glGetUniformLocation(p,"uMaterial");return p;}catch(Throwable e){if(p!=0)GL20.glDeleteProgram(p);if(Main.logger!=null)Main.logger.warn("Electric skill shader unavailable; using additive fallback",e);return 0;}finally{if(v!=0)GL20.glDeleteShader(v);if(f!=0)GL20.glDeleteShader(f);}}
    private static int compile(int type,String source){int s=GL20.glCreateShader(type);GL20.glShaderSource(s,source);GL20.glCompileShader(s);if(GL20.glGetShaderi(s,GL20.GL_COMPILE_STATUS)==0){String log=GL20.glGetShaderInfoLog(s,4096);GL20.glDeleteShader(s);throw new IllegalStateException(log);}return s;}
    private static String read(ResourceLocation r)throws IOException{StringBuilder s=new StringBuilder();try(InputStream in=Minecraft.getMinecraft().getResourceManager().getResource(r).getInputStream();BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String l;while((l=br.readLine())!=null)s.append(l).append('\n');}return s.toString();}
}

package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.EntityConceptualField;
import com.wjx.kablade.Main;
import com.wjx.kablade.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Forge 1.12.2 原生 FBO：颜色层、选择性辉光遮罩、五轮分离模糊与合成。 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid=Main.MODID,value=Side.CLIENT)
public final class LimpidityPostPipeline {
    private static final Map<Integer,Queued> QUEUED=new LinkedHashMap<>();
    private static final Target TARGET=new Target();
    private static boolean disabledForSession;
    private LimpidityPostPipeline(){}

    static boolean enqueue(EntityConceptualField entity,float partial){
        if(!available())return false;
        QUEUED.put(entity.getEntityId(),new Queued(entity,partial));return true;
    }

    private static boolean available(){
        if(disabledForSession||!ModConfig.GeneralConf.Ultra_Effect||!OpenGlHelper.framebufferSupported||!OpenGlHelper.shadersSupported)return false;
        try{return GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM)==0;}
        catch(Throwable error){disabledForSession=true;if(Main.logger!=null)Main.logger.warn("Limpidity post pipeline state query failed; using direct renderer",error);return false;}
    }

    @SubscribeEvent public static void render(RenderWorldLastEvent event){
        if(QUEUED.isEmpty())return;Map<Integer,Queued> work=new LinkedHashMap<>(QUEUED);QUEUED.clear();Minecraft mc=Minecraft.getMinecraft();
        if(mc.world==null||mc.getRenderViewEntity()==null)return;
        if(!available()){drawFallback(work,event.getPartialTicks());return;}
        GlState state;try{state=GlState.capture();}catch(Throwable error){fallback(mc,work,event.getPartialTicks(),error);return;}
        Throwable failure=null;
        try{Framebuffer main=mc.getFramebuffer();TARGET.ensure(Math.max(1,main.framebufferTextureWidth),Math.max(1,main.framebufferTextureHeight));TARGET.color(main);drawAll(work,false,event.getPartialTicks());TARGET.mask(main);drawAll(work,true,event.getPartialTicks());TARGET.composite(main);}
        catch(Throwable error){failure=error;}
        finally{try{state.restore();}catch(Throwable restoreError){if(failure==null)failure=restoreError;else failure.addSuppressed(restoreError);}}
        if(failure!=null)fallback(mc,work,event.getPartialTicks(),failure);
    }

    @SubscribeEvent public static void reload(TextureStitchEvent.Pre event){release();}
    @SubscribeEvent public static void unload(WorldEvent.Unload event){if(event.getWorld().isRemote){QUEUED.clear();release();}}
    private static void release(){TARGET.release();disabledForSession=false;}

    private static void fallback(Minecraft mc,Map<Integer,Queued> work,float partial,Throwable cause){
        disabledForSession=true;if(Main.logger!=null)Main.logger.warn("Limpidity post pipeline disabled; using direct renderer",cause);GlState state=null;try{state=GlState.capture();}catch(Throwable ignored){}
        try{mc.getFramebuffer().bindFramebuffer(true);GL20.glUseProgram(0);GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glDisable(GL11.GL_SCISSOR_TEST);GL11.glColorMask(true,true,true,true);GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glDepthFunc(GL11.GL_LEQUAL);drawFallback(work,partial);}
        catch(Throwable fallbackError){if(Main.logger!=null)Main.logger.warn("Limpidity direct renderer also failed; skipping this frame",fallbackError);}
        finally{if(state!=null)try{state.restore();}catch(Throwable ignored){}}
    }

    private static void drawAll(Map<Integer,Queued> work,boolean mask,float eventPartial){Minecraft mc=Minecraft.getMinecraft();double vx=lerp(mc.getRenderViewEntity().lastTickPosX,mc.getRenderViewEntity().posX,eventPartial),vy=lerp(mc.getRenderViewEntity().lastTickPosY,mc.getRenderViewEntity().posY,eventPartial),vz=lerp(mc.getRenderViewEntity().lastTickPosZ,mc.getRenderViewEntity().posZ,eventPartial);
        for(Queued q:work.values()){EntityConceptualField e=q.entity;if(e.isDead)continue;float age=MathHelper.clamp(e.ticksExisted+q.partial,0,EntityConceptualField.LIFETIME);double x=lerp(e.lastTickPosX,e.posX,q.partial)-vx,y=lerp(e.lastTickPosY,e.posY,q.partial)-vy,z=lerp(e.lastTickPosZ,e.posZ,q.partial)-vz;
            RenderConceptualField.drawPass(e,x,y,z,age,mask?LimpidityGeometry.Pass.BASE_GLOW:LimpidityGeometry.Pass.BASE_COLOR,LimpidityShader.Material.SWORD);
            if(e.getMode()==EntityConceptualField.MODE_UNITY)RenderConceptualField.drawPass(e,x,y,z,age,mask?LimpidityGeometry.Pass.UNITY_GLOW:LimpidityGeometry.Pass.UNITY_COLOR,LimpidityShader.Material.CONCEPTUAL);}}
    private static void drawFallback(Map<Integer,Queued> work,float partial){Minecraft mc=Minecraft.getMinecraft();double vx=lerp(mc.getRenderViewEntity().lastTickPosX,mc.getRenderViewEntity().posX,partial),vy=lerp(mc.getRenderViewEntity().lastTickPosY,mc.getRenderViewEntity().posY,partial),vz=lerp(mc.getRenderViewEntity().lastTickPosZ,mc.getRenderViewEntity().posZ,partial);for(Queued q:work.values()){EntityConceptualField e=q.entity;float age=e.ticksExisted+q.partial;RenderConceptualField.drawPass(e,e.posX-vx,e.posY-vy,e.posZ-vz,age,LimpidityGeometry.Pass.BASE_COLOR,LimpidityShader.Material.SWORD);if(e.getMode()==1)RenderConceptualField.drawPass(e,e.posX-vx,e.posY-vy,e.posZ-vz,age,LimpidityGeometry.Pass.UNITY_COLOR,LimpidityShader.Material.CONCEPTUAL);}}
    private static double lerp(double a,double b,float t){return a+(b-a)*t;}
    private static final class Queued{final EntityConceptualField entity;final float partial;Queued(EntityConceptualField e,float p){entity=e;partial=p;}}

    private static final class Target{
        int fbo,postFbo,color,mask,blurA,blurB,scene,w,h,blurProgram,compositeProgram;
        void ensure(int width,int height)throws Exception{if(fbo==0)fbo=OpenGlHelper.glGenFramebuffers();if(postFbo==0)postFbo=OpenGlHelper.glGenFramebuffers();if(color==0)color=GL11.glGenTextures();if(mask==0)mask=GL11.glGenTextures();if(blurA==0)blurA=GL11.glGenTextures();if(blurB==0)blurB=GL11.glGenTextures();if(scene==0)scene=GL11.glGenTextures();if(blurProgram==0)blurProgram=program("post_120.vsh","limpidity_blur_120.fsh");if(compositeProgram==0)compositeProgram=program("post_120.vsh","limpidity_composite_120.fsh");if(w==width&&h==height)return;w=width;h=height;allocate(color);allocate(mask);allocate(blurA);allocate(blurB);allocate(scene);}
        void color(Framebuffer main){bindWithDepth(color,main.depthBuffer,main.framebufferWidth,main.framebufferHeight);clear();prepareWorld();}
        void mask(Framebuffer main){bindWithDepth(mask,main.depthBuffer,main.framebufferWidth,main.framebufferHeight);clear();prepareWorld();}
        void bindWithDepth(int texture,int depth,int viewportWidth,int viewportHeight){OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER,fbo);OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER,OpenGlHelper.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,texture,0);OpenGlHelper.glFramebufferRenderbuffer(OpenGlHelper.GL_FRAMEBUFFER,OpenGlHelper.GL_DEPTH_ATTACHMENT,OpenGlHelper.GL_RENDERBUFFER,depth);GL11.glDrawBuffer(OpenGlHelper.GL_COLOR_ATTACHMENT0);check();GL11.glViewport(0,0,viewportWidth,viewportHeight);}
        void clear(){GL11.glDisable(GL11.GL_SCISSOR_TEST);GL11.glColorMask(true,true,true,true);GL11.glClearColor(0,0,0,0);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);}
        void prepareWorld(){GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glDepthFunc(GL11.GL_LEQUAL);GL11.glDepthMask(false);GL11.glDisable(GL11.GL_CULL_FACE);}
        void composite(Framebuffer main){int bloom=mask;for(int i=0;i<5;i++){postTarget(blurA);fullBlur(bloom,true);postTarget(blurB);fullBlur(blurA,false);bloom=blurB;}postTarget(scene);main.bindFramebuffer(true);GL11.glBindTexture(GL11.GL_TEXTURE_2D,scene);GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D,0,0,0,0,0,Math.min(w,main.framebufferWidth),Math.min(h,main.framebufferHeight));main.bindFramebuffer(true);fullComposite(scene,color,bloom,Math.min(1F,main.framebufferWidth/(float)w),Math.min(1F,main.framebufferHeight/(float)h));}
        void postTarget(int texture){OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER,postFbo);OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER,OpenGlHelper.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,texture,0);OpenGlHelper.glFramebufferRenderbuffer(OpenGlHelper.GL_FRAMEBUFFER,OpenGlHelper.GL_DEPTH_ATTACHMENT,OpenGlHelper.GL_RENDERBUFFER,0);GL11.glDrawBuffer(OpenGlHelper.GL_COLOR_ATTACHMENT0);check();GL11.glViewport(0,0,w,h);GL11.glDisable(GL11.GL_SCISSOR_TEST);GL11.glColorMask(true,true,true,true);GL11.glClearColor(0,0,0,0);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);}
        void fullBlur(int source,boolean horizontal){beginPost(blurProgram);bindTex(blurProgram,"Source",0,source);int size=GL20.glGetUniformLocation(blurProgram,"TexelSize");if(size>=0)GL20.glUniform2f(size,1F/w,1F/h);int dir=GL20.glGetUniformLocation(blurProgram,"Direction");if(dir>=0)GL20.glUniform2f(dir,horizontal?1:0,horizontal?0:1);quad();endPost();}
        void fullComposite(int sceneTex,int effectTex,int bloomTex,float scaleX,float scaleY){beginPost(compositeProgram);bindTex(compositeProgram,"Scene",0,sceneTex);bindTex(compositeProgram,"Effect",1,effectTex);bindTex(compositeProgram,"Bloom",2,bloomTex);int s=GL20.glGetUniformLocation(compositeProgram,"TexelSize");if(s>=0)GL20.glUniform2f(s,1F/w,1F/h);int scale=GL20.glGetUniformLocation(compositeProgram,"UvScale");if(scale>=0)GL20.glUniform2f(scale,scaleX,scaleY);quad();endPost();}
        void beginPost(int program){GL11.glDisable(GL11.GL_SCISSOR_TEST);GL11.glColorMask(true,true,true,true);GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_ALPHA_TEST);GL11.glDepthMask(false);GL11.glDisable(GL11.GL_BLEND);GL20.glUseProgram(program);}
        void endPost(){GL20.glUseProgram(0);GL13.glActiveTexture(GL13.GL_TEXTURE0);}
        void bindTex(int p,String name,int unit,int tex){GL13.glActiveTexture(GL13.GL_TEXTURE0+unit);GL11.glBindTexture(GL11.GL_TEXTURE_2D,tex);int u=GL20.glGetUniformLocation(p,name);if(u>=0)GL20.glUniform1i(u,unit);}
        void allocate(int texture){GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL12.GL_CLAMP_TO_EDGE);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL12.GL_CLAMP_TO_EDGE);GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_RGBA16F,w,h,0,GL11.GL_RGBA,GL11.GL_FLOAT,(java.nio.ByteBuffer)null);}
        void check(){int status=OpenGlHelper.glCheckFramebufferStatus(OpenGlHelper.GL_FRAMEBUFFER);if(status!=OpenGlHelper.GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Limpidity framebuffer incomplete: 0x"+Integer.toHexString(status));}
        void release(){if(color!=0)GL11.glDeleteTextures(color);if(mask!=0)GL11.glDeleteTextures(mask);if(blurA!=0)GL11.glDeleteTextures(blurA);if(blurB!=0)GL11.glDeleteTextures(blurB);if(scene!=0)GL11.glDeleteTextures(scene);if(fbo!=0)OpenGlHelper.glDeleteFramebuffers(fbo);if(postFbo!=0)OpenGlHelper.glDeleteFramebuffers(postFbo);if(blurProgram!=0)GL20.glDeleteProgram(blurProgram);if(compositeProgram!=0)GL20.glDeleteProgram(compositeProgram);fbo=postFbo=color=mask=blurA=blurB=scene=blurProgram=compositeProgram=w=h=0;}
    }

    private static int program(String vs,String fs)throws Exception{int v=compile(GL20.GL_VERTEX_SHADER,read(vs)),f=compile(GL20.GL_FRAGMENT_SHADER,read(fs)),p=GL20.glCreateProgram();GL20.glAttachShader(p,v);GL20.glAttachShader(p,f);GL20.glLinkProgram(p);GL20.glDeleteShader(v);GL20.glDeleteShader(f);if(GL20.glGetProgrami(p,GL20.GL_LINK_STATUS)==0)throw new IllegalStateException(GL20.glGetProgramInfoLog(p,8192));return p;}
    private static int compile(int type,String source){int s=GL20.glCreateShader(type);GL20.glShaderSource(s,source);GL20.glCompileShader(s);if(GL20.glGetShaderi(s,GL20.GL_COMPILE_STATUS)==0)throw new IllegalStateException(GL20.glGetShaderInfoLog(s,8192));return s;}
    private static String read(String name)throws Exception{StringBuilder s=new StringBuilder();try(BufferedReader r=new BufferedReader(new InputStreamReader(Minecraft.getMinecraft().getResourceManager().getResource(new net.minecraft.util.ResourceLocation(Main.MODID,"shaders/"+name)).getInputStream(),StandardCharsets.UTF_8))){String line;while((line=r.readLine())!=null)s.append(line).append('\n');}return s.toString();}
    private static void quad(){GL11.glBegin(GL11.GL_QUADS);GL11.glTexCoord2f(0,0);GL11.glVertex2f(-1,-1);GL11.glTexCoord2f(1,0);GL11.glVertex2f(1,-1);GL11.glTexCoord2f(1,1);GL11.glVertex2f(1,1);GL11.glTexCoord2f(0,1);GL11.glVertex2f(-1,1);GL11.glEnd();}

    private static final class GlState{
        // LWJGL 2 的 glGet* 会按查询可能返回的最大项数校验容量，四分量状态也保留 16 项。
        final int fbo,program,active,blendSrcRgb,blendDstRgb,blendSrcAlpha,blendDstAlpha,depthFunc;
        final IntBuffer viewport=BufferUtils.createIntBuffer(16),scissorBox=BufferUtils.createIntBuffer(16);
        final ByteBuffer colorMask=BufferUtils.createByteBuffer(16);
        final FloatBuffer clearColor=BufferUtils.createFloatBuffer(16);
        final int[] textures=new int[3];
        final boolean depth,blend,cull,lighting,texture,alphaTest,scissor,depthMask;
        GlState(){fbo=GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);program=GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);blendSrcRgb=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);blendDstRgb=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);blendSrcAlpha=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);blendDstAlpha=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);depthFunc=GL11.glGetInteger(GL11.GL_DEPTH_FUNC);for(int i=0;i<3;i++){GL13.glActiveTexture(GL13.GL_TEXTURE0+i);textures[i]=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);}GL13.glActiveTexture(active);GL11.glGetInteger(GL11.GL_VIEWPORT,viewport);GL11.glGetInteger(GL11.GL_SCISSOR_BOX,scissorBox);GL11.glGetBoolean(GL11.GL_COLOR_WRITEMASK,colorMask);GL11.glGetFloat(GL11.GL_COLOR_CLEAR_VALUE,clearColor);depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST);blend=GL11.glIsEnabled(GL11.GL_BLEND);cull=GL11.glIsEnabled(GL11.GL_CULL_FACE);lighting=GL11.glIsEnabled(GL11.GL_LIGHTING);texture=GL11.glIsEnabled(GL11.GL_TEXTURE_2D);alphaTest=GL11.glIsEnabled(GL11.GL_ALPHA_TEST);scissor=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);depthMask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);}
        static GlState capture(){return new GlState();}
        void restore(){OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER,fbo);GL20.glUseProgram(program);for(int i=0;i<3;i++){GL13.glActiveTexture(GL13.GL_TEXTURE0+i);GL11.glBindTexture(GL11.GL_TEXTURE_2D,textures[i]);}GL13.glActiveTexture(active);GL11.glViewport(viewport.get(0),viewport.get(1),viewport.get(2),viewport.get(3));GL11.glScissor(scissorBox.get(0),scissorBox.get(1),scissorBox.get(2),scissorBox.get(3));GL11.glColorMask(colorMask.get(0)!=0,colorMask.get(1)!=0,colorMask.get(2)!=0,colorMask.get(3)!=0);GL11.glClearColor(clearColor.get(0),clearColor.get(1),clearColor.get(2),clearColor.get(3));GL14.glBlendFuncSeparate(blendSrcRgb,blendDstRgb,blendSrcAlpha,blendDstAlpha);GL11.glDepthFunc(depthFunc);set(GL11.GL_DEPTH_TEST,depth);set(GL11.GL_BLEND,blend);set(GL11.GL_CULL_FACE,cull);set(GL11.GL_LIGHTING,lighting);set(GL11.GL_TEXTURE_2D,texture);set(GL11.GL_ALPHA_TEST,alphaTest);set(GL11.GL_SCISSOR_TEST,scissor);GL11.glDepthMask(depthMask);}
        static void set(int cap,boolean on){if(on)GL11.glEnable(cap);else GL11.glDisable(cap);}
    }
}

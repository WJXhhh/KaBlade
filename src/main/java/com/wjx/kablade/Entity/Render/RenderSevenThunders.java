package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.EntitySevenThunders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;

/** 唤霆霓/鸣雷神的五材质分层 renderer；几何按 1.20 的 F0-F40 参考帧重建。 */
@SideOnly(Side.CLIENT)
public class RenderSevenThunders extends Render<EntitySevenThunders>{
    private static final float TAU=(float)Math.PI*2;
    private static final Cue[] CUES={
            new Cue(13.92F,14.16F,14.70F,15.62F,16.05F,1.00F),new Cue(14.42F,14.78F,15.46F,16.72F,17.28F,0.80F),
            new Cue(14.92F,15.28F,15.92F,17.12F,17.76F,0.46F),new Cue(15.52F,15.92F,16.62F,18.12F,18.76F,0.42F),
            new Cue(16.20F,16.62F,17.28F,18.86F,19.62F,0.38F),new Cue(16.82F,17.22F,17.92F,18.92F,20.02F,1.00F),
            new Cue(17.56F,17.94F,18.70F,19.86F,21.02F,0.98F),new Cue(18.42F,18.84F,19.62F,20.92F,22.18F,0.92F),
            new Cue(19.42F,19.86F,20.62F,21.96F,23.16F,0.96F),new Cue(20.42F,20.86F,21.58F,22.88F,24.08F,0.88F),
            new Cue(19.72F,20.12F,21.12F,23.08F,24.68F,0.92F),new Cue(22.08F,22.48F,23.28F,27.18F,28.72F,0.80F),
            new Cue(23.42F,23.86F,25.10F,28.10F,29.00F,0.48F)};

    public RenderSevenThunders(RenderManager manager){super(manager);shadowSize=0;shadowOpaque=0;}
    @Nullable @Override protected ResourceLocation getEntityTexture(EntitySevenThunders entity){return null;}

    @Override public void doRender(EntitySevenThunders entity,double x,double y,double z,float yaw,float partial){
        float age=MathHelper.clamp(entity.ticksExisted+partial,0,EntitySevenThunders.LIFETIME),frame=age*40/54;
        Vec3d entityWorld=new Vec3d(entity.prevPosX+(entity.posX-entity.prevPosX)*partial,
                entity.prevPosY+(entity.posY-entity.prevPosY)*partial,
                entity.prevPosZ+(entity.posZ-entity.prevPosZ)*partial);
        Vec3d owner=entity.getOwnerAnchor(partial).subtract(entityWorld);
        Vec3d target=entity.getTargetAnchor(partial).subtract(entityWorld);
        Vec3d camera=new Vec3d(renderManager.viewerPosX,renderManager.viewerPosY,renderManager.viewerPosZ).subtract(entityWorld);
        float oldX=OpenGlHelper.lastBrightnessX,oldY=OpenGlHelper.lastBrightnessY;boolean pushed=false;
        try{
            GlStateManager.disableLighting();GlStateManager.disableCull();GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();GlStateManager.depthMask(false);OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,240,240);
            GlStateManager.pushMatrix();pushed=true;GlStateManager.translate(x,y,z);
            Basis basis=basis(entity.getDirection());
            drawPass(entity,age,frame,owner,target,camera,basis,LimpidityShader.Material.COMPOSITE,false);
            drawPass(entity,age,frame,owner,target,camera,basis,LimpidityShader.Material.ENERGY,true);
            drawPass(entity,age,frame,owner,target,camera,basis,LimpidityShader.Material.LIGHTNING,true);
            drawPass(entity,age,frame,owner,target,camera,basis,LimpidityShader.Material.CROSS,true);
            drawPass(entity,age,frame,owner,target,camera,basis,LimpidityShader.Material.PARTICLE,true);
        }finally{if(pushed)GlStateManager.popMatrix();OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,oldX,oldY);
            GlStateManager.enableTexture2D();GlStateManager.depthMask(true);GlStateManager.disableBlend();GlStateManager.enableCull();GlStateManager.enableLighting();}
        super.doRender(entity,x,y,z,yaw,partial);
    }

    private static void drawPass(EntitySevenThunders e,float age,float frame,Vec3d owner,Vec3d target,Vec3d camera,Basis basis,
                                 LimpidityShader.Material material,boolean additive){
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                additive?GlStateManager.DestFactor.ONE:GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,GlStateManager.DestFactor.ZERO);
        int previous=LimpidityShader.bind(material,age);BufferBuilder b=Tessellator.getInstance().getBuffer();
        b.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_TEX_COLOR);
        if(e.getMode()==EntitySevenThunders.MODE_THUNDERBOLT_CALL)renderThunderboltPass(b,e,frame,target,basis,material);
        else NarukamiRenderLayer.render(b,material,frame,owner,target,
                NarukamiRenderLayer.basis(e.getDirection()),camera,e.getSeed());
        Tessellator.getInstance().draw();LimpidityShader.restore(previous);
    }

    private static void renderThunderboltPass(BufferBuilder b,EntitySevenThunders e,float f,Vec3d target,Basis q,LimpidityShader.Material m){
        Vec3d owner=new Vec3d(0,1.15,0),stored=e.getStoredTargetAnchor().subtract(e.getPositionVector());
        if(m==LimpidityShader.Material.COMPOSITE){
            float charge=plateau(f,1.2F,2.2F,8.4F,11.0F);disc(b,owner.add(0,0.45,0),q.right,q.up,1.25F,0.10F,0.01F,0.20F,charge*0.70F);
            float impact=plateau(f,3.0F,3.6F,8.0F,10.4F);disc(b,target,q.right,q.up,3.8F,0.08F,0.005F,0.14F,impact*0.82F);
            float residual=plateau(f,11.0F,12.0F,23.5F,31.0F);ring(b,target.add(0,-target.y+0.08,0),q.right,q.forward,4.6F,0.16F,0.08F,0.006F,0.15F,residual*0.68F);
        }else if(m==LimpidityShader.Material.ENERGY){
            renderChargeEnergy(b,e,f,owner,target,q);renderThunderTargetEnergy(b,e,f,target,q);
        }else if(m==LimpidityShader.Material.LIGHTNING){
            renderChargeLightning(b,e,f,owner,target,q);renderThunderResidualLightning(b,e,f,target,q);
        }else if(m==LimpidityShader.Material.CROSS){renderThunderCross(b,e,f,target,q);
        }else if(m==LimpidityShader.Material.PARTICLE){renderThunderParticles(b,e,f,owner,target,stored,q);}
    }

    private static void renderChargeEnergy(BufferBuilder b,EntitySevenThunders e,float f,Vec3d owner,Vec3d target,Basis q){
        float a=plateau(f,1.0F,2.2F,10.2F,13.2F);if(a<.002F)return;
        for(int i=0;i<5;i++){float angle=i*TAU/5+f*0.18F,radius=0.75F+i*0.18F;
            Vec3d end=owner.add(q.right.scale(MathHelper.cos(angle)*radius)).add(q.up.scale(MathHelper.sin(angle)*radius)).add(q.forward.scale(0.5+i*0.18));
            beam(b,owner.add(q.up.scale(0.45)),end,0.035F,0.72F,0.30F,1,a*(0.45F+i*.08F));}
        float dash=smooth(f,5.1F,9.8F);Vec3d dashEnd=owner.add(target.subtract(owner).scale(dash));
        beam(b,owner,dashEnd,0.11F,0.70F,0.24F,1,a*0.72F);beam(b,owner,dashEnd,0.028F,1,0.96F,1,a);
    }

    private static void renderChargeLightning(BufferBuilder b,EntitySevenThunders e,float f,Vec3d owner,Vec3d target,Basis q){
        float a=plateau(f,1.6F,2.8F,11.0F,14.0F);for(int i=0;i<8&&a>.002F;i++)
            lightning(b,owner.add(q.up.scale(.45)),target,e.getSeed()+i*37,24,.026F+i*.002F,.68F,.22F,1,a*(.30F+(i%4)*.13F));
    }

    private static void renderThunderTargetEnergy(BufferBuilder b,EntitySevenThunders e,float f,Vec3d target,Basis q){
        float first=plateau(f,2.8F,3.5F,8.0F,10.0F),second=plateau(f,10.4F,11.5F,25.8F,29.7F),a=Math.max(first*.75F,second);
        if(a<.002F)return;for(int i=0;i<4;i++)ring(b,target,q.right,q.up,1.2F+i*.68F,.035F,.72F,.24F,1,a*(.78F-i*.12F));
        disc(b,target,q.right,q.up,1.7F,1,.68F,1,a*.48F);beam(b,target,target.add(q.forward.scale(6.4)),.065F,.74F,.30F,1,a*.76F);
    }

    private static void renderThunderCross(BufferBuilder b,EntitySevenThunders e,float f,Vec3d target,Basis q){
        float a=plateau(f,10.2F,11.4F,21.8F,25.6F);if(a<.002F)return;
        float travel=easeOut(smooth(f,11.05F,17.15F))*4.95F+smooth(f,17.15F,24.85F)*1.35F;
        Vec3d center=target.add(q.forward.scale(travel));float scale=Math.max(.78F,1.025F-smooth(f,11.05F,17.15F)*.105F-smooth(f,17.15F,24.85F)*.075F);
        slashTrack(b,center,q.right.add(q.up).normalize(),6.4F*scale,.54F,.67F,.16F,1,a);
        slashTrack(b,center,q.right.subtract(q.up).normalize(),6.4F*scale,.54F,.67F,.16F,1,a);
        float orbit=plateau(f,13,13.7F,17.2F,19.1F);for(int i=0;i<3&&orbit>.002F;i++)
            ring(b,target.add(q.forward.scale(i*.18)),q.right,q.up,2.2F+i*.44F,.045F,.72F,.25F,1,orbit*(.82F-i*.16F));
    }

    private static void renderThunderResidualLightning(BufferBuilder b,EntitySevenThunders e,float f,Vec3d target,Basis q){
        float a=plateau(f,10.8F,11.7F,20.4F,24.7F);for(int i=0;i<16&&a>.002F;i++){
            float angle=i*TAU/16+f*.04F;Vec3d ground=target.add(MathHelper.cos(angle)*(.8+i%5),-target.y+.08,MathHelper.sin(angle)*(.8+i%5));
            Vec3d end=target.add(q.right.scale(MathHelper.cos(angle)*2.8)).add(q.up.scale(.4+(i%4)*.75));
            lightning(b,ground,end,e.getSeed()+i*91,12,.024F,.68F,.19F,1,a*(.28F+(i%4)*.15F));}}

    private static void renderThunderParticles(BufferBuilder b,EntitySevenThunders e,float f,Vec3d owner,Vec3d target,Vec3d stored,Basis q){
        float a=plateau(f,2,3,27.2F,32.4F);for(int i=0;i<54&&a>.002F;i++){float born=(i%18)*1.35F,life=f-born;if(life<0)continue;
            float fade=1-smooth(life,8,17),angle=det(i,3.2F)*TAU+life*.09F,rad=.4F+det(i,7.1F)*4.8F+life*.025F;
            Vec3d p=target.add(MathHelper.cos(angle)*rad,(det(i,11.4F)-.35F)*3.2F+life*.035F,MathHelper.sin(angle)*rad);
            billboard(b,p,.045F+det(i,14.8F)*.13F,.78F,.34F,1,a*fade*(.34F+det(i,5.7F)*.62F));}
        float anchors=plateau(f,1.0F,2.0F,10.0F,13.0F);billboard(b,owner.add(0,.55,0),.42F,1,.72F,1,anchors);
        billboard(b,target,.55F,1,.62F,1,anchors);billboard(b,stored,.32F,.72F,.28F,1,anchors*.72F);
    }

    private static void renderNarukamiPass(BufferBuilder b,EntitySevenThunders e,float f,Vec3d target,Basis q,LimpidityShader.Material m){
        Vec3d owner=new Vec3d(0,1.15,0);
        if(m==LimpidityShader.Material.COMPOSITE){float open=plateau(f,0,0.3F,8.5F,11.2F);disc(b,target,q.right,q.up,3.4F,.07F,.004F,.13F,open*.75F);
            float cage=plateau(f,14,15,28.4F,32);ring(b,new Vec3d(0,.08,0),q.right,q.forward,4.35F,.18F,.065F,.004F,.12F,cage*.72F);
        }else if(m==LimpidityShader.Material.ENERGY){renderNarukamiEnergy(b,e,f,owner,target,q);
        }else if(m==LimpidityShader.Material.LIGHTNING){renderNarukamiLightning(b,e,f,owner,target,q);
        }else if(m==LimpidityShader.Material.CROSS){renderNarukamiTracks(b,e,f,owner,target,q);
        }else if(m==LimpidityShader.Material.PARTICLE){renderNarukamiParticles(b,e,f,owner,target,q);}
    }

    private static void renderNarukamiEnergy(BufferBuilder b,EntitySevenThunders e,float f,Vec3d owner,Vec3d target,Basis q){
        float opening=plateau(f,0,0.4F,8.2F,11.5F);for(int i=0;i<5&&opening>.002F;i++)
            ring(b,target.add(q.forward.scale(i*.08)),q.right,q.up,.7F+i*.72F,.04F,.72F,.26F,1,opening*(.82F-i*.11F));
        float cage=plateau(f,13.8F,14.8F,29,33);for(int i=0;i<4&&cage>.002F;i++)
            ring(b,owner.add(0,-1.07+i*.62,0),q.right,q.forward,4.35F-i*.34F,.045F,.70F,.21F,1,cage*(.70F-i*.10F));
        float ground=groundPulse(f);ring(b,new Vec3d(0,.08,0),q.right,q.forward,.7F+fastOut(smooth(f,20,25))*6.0F,.10F,1,.74F,1,ground);
    }

    private static void renderNarukamiLightning(BufferBuilder b,EntitySevenThunders e,float f,Vec3d owner,Vec3d target,Basis q){
        float opening=plateau(f,0,.35F,8.4F,11.4F);for(int i=0;i<9&&opening>.002F;i++)lightning(b,owner,target,e.getSeed()+i*53,22,.025F,.69F,.20F,1,opening*(.30F+(i%4)*.14F));
        for(int i=0;i<CUES.length;i++){Cue cue=CUES[i];float a=cue.alpha(f);if(a<.002F)continue;
            Vec3d start=trackPoint(owner,target,q,i,0),end=trackPoint(owner,target,q,i,1);
            lightning(b,start,end,e.getSeed()+i*131,18,.026F+(i%3)*.008F,.70F,.22F,1,a*(.52F+cue.bloom*.42F));}
    }

    private static void renderNarukamiTracks(BufferBuilder b,EntitySevenThunders e,float f,Vec3d owner,Vec3d target,Basis q){
        float cross=plateau(f,5.6F,6.4F,9.3F,11.1F),travel=easeOut(smooth(f,5.8F,8.8F))*2.7F;Vec3d center=target.add(q.forward.scale(travel));
        slashTrack(b,center,q.right.add(q.up.scale(.72)).normalize(),5.5F,.48F,.72F,.22F,1,cross);
        slashTrack(b,center,q.right.subtract(q.up.scale(.72)).normalize(),5.5F,.48F,.72F,.22F,1,cross*.94F);
        for(int i=0;i<CUES.length;i++){float a=CUES[i].alpha(f);if(a<.002F)continue;Vec3d start=trackPoint(owner,target,q,i,0),end=trackPoint(owner,target,q,i,1);
            slashTrack(b,start.add(end).scale(.5),end.subtract(start).normalize(),(float)(start.distanceTo(end)*.58F),.20F,.70F,.20F,1,a);}
    }

    private static void renderNarukamiParticles(BufferBuilder b,EntitySevenThunders e,float f,Vec3d owner,Vec3d target,Basis q){
        for(int i=0;i<72;i++){float born=13+(i%24)*.56F,life=f-born;if(life<0)continue;float fade=1-smooth(life,6,13),angle=det(i,5.2F)*TAU+life*.08F;
            Vec3d p=owner.add(MathHelper.cos(angle)*(1+det(i,7.8F)*4.2F),.1+det(i,9.3F)*3.8F+life*.025F,MathHelper.sin(angle)*(1+det(i,7.8F)*4.2F));
            billboard(b,p,.04F+det(i,12.1F)*.12F,.80F,.36F,1,fade*(.25F+det(i,2.4F)*.65F));}
        for(int i=0;i<CUES.length;i++){float a=CUES[i].alpha(f);if(a>.01F)billboard(b,trackPoint(owner,target,q,i,1),.12F+.08F*CUES[i].bloom,1,.72F,1,a);}
    }

    private static Vec3d trackPoint(Vec3d owner,Vec3d target,Basis q,int index,int end){
        float angle=index*2.3999632F;double radius=2.1+(index%5)*.54;Vec3d center=index<5?owner:target;
        Vec3d offset=q.right.scale(MathHelper.cos(angle)*radius).add(q.up.scale(.25+(index%6)*.62)).add(q.forward.scale(MathHelper.sin(angle)*radius));
        if(end==0)return center.add(offset);Vec3d inward=center.add(q.forward.scale(index<5?1.2:-.8)).add(q.up.scale(1.0+(index%3)*.4));return inward;}

    private static void slashTrack(BufferBuilder b,Vec3d center,Vec3d axis,float half,float width,float r,float g,float blue,float a){
        if(a<.002F)return;Vec3d from=center.subtract(axis.scale(half)),to=center.add(axis.scale(half));
        beam(b,from,to,width,r,g,blue,a*.46F);beam(b,from,to,width*.34F,1,.96F,1,a);beam(b,from,to,width*.10F,1,1,1,a);}
    private static void lightning(BufferBuilder b,Vec3d from,Vec3d to,long seed,int parts,float width,float r,float g,float blue,float a){
        Vec3d delta=to.subtract(from),side=delta.crossProduct(new Vec3d(0,1,0));if(side.lengthSquared()<1E-8)side=new Vec3d(1,0,0);else side=side.normalize();Vec3d up=delta.normalize().crossProduct(side).normalize();Vec3d prev=from;
        for(int i=1;i<=parts;i++){float t=i/(float)parts,envelope=MathHelper.sin((float)Math.PI*t);Vec3d base=from.add(delta.scale(t));
            Vec3d next=base.add(side.scale(noise(seed+i*17)*.38*envelope)).add(up.scale(noise(seed+i*43)*.31*envelope));beam(b,prev,next,width,r,g,blue,a);prev=next;}}
    private static void beam(BufferBuilder b,Vec3d from,Vec3d to,float width,float r,float g,float blue,float a){
        Vec3d dir=to.subtract(from),side=dir.crossProduct(cameraForward());if(side.lengthSquared()<1E-8)side=dir.crossProduct(new Vec3d(0,1,0));if(side.lengthSquared()<1E-8)side=new Vec3d(1,0,0);else side=side.normalize();side=side.scale(width);
        quad(b,from.subtract(side),from.add(side),to.add(side),to.subtract(side),r,g,blue,a);}
    private static void ring(BufferBuilder b,Vec3d center,Vec3d axisA,Vec3d axisB,float radius,float width,float r,float g,float blue,float a){
        int seg=96;for(int i=0;i<seg;i++){float p0=i*TAU/seg,p1=(i+1)*TAU/seg;Vec3d a0=center.add(axisA.scale(MathHelper.cos(p0)*(radius-width))).add(axisB.scale(MathHelper.sin(p0)*(radius-width))),
                b0=center.add(axisA.scale(MathHelper.cos(p0)*(radius+width))).add(axisB.scale(MathHelper.sin(p0)*(radius+width))),
                b1=center.add(axisA.scale(MathHelper.cos(p1)*(radius+width))).add(axisB.scale(MathHelper.sin(p1)*(radius+width))),
                a1=center.add(axisA.scale(MathHelper.cos(p1)*(radius-width))).add(axisB.scale(MathHelper.sin(p1)*(radius-width)));quad(b,a0,b0,b1,a1,r,g,blue,a);}}
    private static void disc(BufferBuilder b,Vec3d center,Vec3d right,Vec3d up,float size,float r,float g,float blue,float a){quadUv(b,center.subtract(right.scale(size)).subtract(up.scale(size)),center.add(right.scale(size)).subtract(up.scale(size)),center.add(right.scale(size)).add(up.scale(size)),center.subtract(right.scale(size)).add(up.scale(size)),r,g,blue,a);}
    private static void billboard(BufferBuilder b,Vec3d center,float size,float r,float g,float blue,float a){Vec3d forward=cameraForward(),right=forward.crossProduct(new Vec3d(0,1,0));if(right.lengthSquared()<1E-8)right=new Vec3d(1,0,0);else right=right.normalize();Vec3d up=right.crossProduct(forward).normalize();disc(b,center,right,up,size,r,g,blue,a);}
    private static Vec3d cameraForward(){float yaw=Minecraft.getMinecraft().getRenderManager().playerViewY*0.017453292F,pitch=Minecraft.getMinecraft().getRenderManager().playerViewX*0.017453292F;return new Vec3d(-MathHelper.sin(yaw)*MathHelper.cos(pitch),-MathHelper.sin(pitch),MathHelper.cos(yaw)*MathHelper.cos(pitch));}
    private static void quad(BufferBuilder b,Vec3d a,Vec3d c,Vec3d d,Vec3d e,float r,float g,float blue,float alpha){quadUv(b,a,c,d,e,r,g,blue,alpha);}
    private static void quadUv(BufferBuilder b,Vec3d a,Vec3d c,Vec3d d,Vec3d e,float r,float g,float blue,float alpha){vertex(b,a,0,0,r,g,blue,alpha);vertex(b,c,0,1,r,g,blue,alpha);vertex(b,d,1,1,r,g,blue,alpha);vertex(b,e,1,0,r,g,blue,alpha);}
    private static void vertex(BufferBuilder b,Vec3d p,float u,float v,float r,float g,float blue,float a){b.pos(p.x,p.y,p.z).tex(u,v).color(r,g,blue,MathHelper.clamp(a,0,1)).endVertex();}
    private static Basis basis(Vec3d forward){Vec3d f=forward.normalize(),right=new Vec3d(0,1,0).crossProduct(f);if(right.lengthSquared()<1E-8)right=new Vec3d(1,0,0);else right=right.normalize();return new Basis(f,right,f.crossProduct(right).normalize());}
    private static float smooth(float v,float a,float c){if(Math.abs(c-a)<1E-6)return v>=c?1:0;float x=MathHelper.clamp((v-a)/(c-a),0,1);return x*x*(3-2*x);}private static float plateau(float f,float start,float full,float fade,float end){return f<start||f>end?0:Math.min(smooth(f,start,full),1-smooth(f,fade,end));}
    private static float fastOut(float v){float i=1-MathHelper.clamp(v,0,1);return 1-i*i*i;}private static float easeOut(float v){return fastOut(v);}private static float gaussian(float v,float c,float w){float x=(v-c)/w;return(float)Math.exp(-x*x);}private static float groundPulse(float f){return MathHelper.clamp(gaussian(f,17.9F,1.2F)*.55F+gaussian(f,21.6F,1.35F)+gaussian(f,23.1F,1.1F)*.8F,0,1);}private static float det(int i,float salt){float v=MathHelper.sin(i*12.9898F+salt*78.233F)*43758.547F;return v-(float)Math.floor(v);}private static double noise(long v){double n=Math.sin(v*12.9898)*43758.5453;return(n-Math.floor(n))*2-1;}
    private static final class Basis{final Vec3d forward,right,up;Basis(Vec3d f,Vec3d r,Vec3d u){forward=f;right=r;up=u;}}
    private static final class Cue{final float anticipate,start,impact,release,end,bloom;Cue(float a,float s,float i,float r,float e,float b){anticipate=a;start=s;impact=i;release=r;end=e;bloom=b;}float alpha(float f){return bloom*Math.min(smooth(f,start,impact),1-smooth(f,release,end));}}
}

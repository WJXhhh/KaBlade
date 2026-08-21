package com.wjx.kablade.Entity.Render;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** 1.20 澄凝系 SA 的解析几何数据，数值与原 renderer 保持一致。 */
final class LimpidityGeometry {
    enum Pass { BASE_COLOR, BASE_GLOW, UNITY_COLOR, UNITY_GLOW }
    private static final float TAU=(float)Math.PI*2F,DEG=(float)Math.PI/180F,CENTER_Z=2.15F;
    private static final int RING_SEGMENTS=92,LOOP_SEGMENTS=58,SWEEP_SEGMENTS=54,SHARD_COUNT=58;
    private static final float GOLD_R=.95F,GOLD_G=.86F,GOLD_B=.63F,LAV_R=.74F,LAV_G=.76F,LAV_B=1F;
    private LimpidityGeometry(){}

    static void draw(BufferBuilder b,float age,int seed,float viewYaw,float viewPitch,float entityYaw,Pass pass){
        if(pass==Pass.BASE_COLOR){groundRings(b,age);haloShell(b,age);slashTimeline(b,age);verticalArcs(b,age);blades(b,age);fragments(b,age);billboards(b,age,viewYaw,viewPitch,entityYaw);}
        else if(pass==Pass.BASE_GLOW){slashTimeline(b,age);verticalArcs(b,age);blades(b,age);}
        else {if(pass==Pass.UNITY_COLOR)mandala(b,age);wingSigils(b,age);unityFinisher(b,age);afterglow(b,age);}
    }

    private static void groundRings(BufferBuilder b,float age){
        float open=smoother(stage(age,5,10)),fade=1-smoother(stage(age,38,16)),a=open*fade;if(a<=.01F)return;
        float pulse=.5F+.5F*MathHelper.sin(age*.38F);
        brokenRing(b,.055F,lerp(open,.64F,4.95F),lerp(open,.08F,.42F),.20F+open*.52F,.76F,.28F,1,a*.44F);
        ring(b,.075F,1.35F+open*5.05F+pulse*.10F,.10F+open*.32F,.94F,.82F,1,a*.28F);
        for(int i=0;i<5;i++){float start=6+i*3.2F,t=stage(age,start,18);if(t>=1)continue;float grow=fastOut(t),local=(1-smoother(stage(age,start+8,10)))*fade;
            brokenRing(b,.10F+i*.012F,.55F+grow*(4.9F+i*.55F),.12F+grow*(.34F+i*.03F),grow,i%2==0?.92F:.56F,i%2==0?.72F:.24F,1,local*(.26F-i*.025F));}
    }
    private static void haloShell(BufferBuilder b,float age){
        float appear=smoother(stage(age,7,3)),fade=1-smoother(stage(age,36,10)),a=appear*fade;if(a<=.01F)return;
        for(int i=0;i<11;i++){float localOpen=smoother(stage(age,7.4F+i*.55F,8)),localFade=1-smoother(stage(age,25+i*.75F,12)),local=a*localOpen*localFade;if(local<=.01F)continue;
            float rx=2.05F+i*.22F+localOpen*1.25F,rz=1.55F+i*.18F+localOpen*.88F,y=.82F+(i%4)*.20F,lift=.48F+localOpen*(1.05F+(i%3)*.20F);
            liftedLoop(b,age*(.11F+i*.008F)+i*.713F,(.58F+localOpen*.66F)*TAU,rx,rz,y,lift,.045F+i*.006F,i%3==0?.98F:.58F,i%3==0?.86F:.28F,1,local*(.20F+(i%3)*.035F));}
    }
    private static void slashTimeline(BufferBuilder b,float age){
        crescent(b,age,6.4F,5.6F,4.65F,116,-58,.18F,2.12F,.15F,.70F,.94F,.78F,1,1);
        crescent(b,age,9,6.8F,5.15F,158,-26,.42F,2.62F,.45F,.48F,.68F,.34F,1,.84F);
        crescent(b,age,13.2F,8,5.90F,210,18,.22F,2.92F,.82F,.40F,.92F,.82F,1,.74F);
        crescent(b,age,17.2F,9,6.45F,255,52,.18F,3.34F,1.20F,.38F,.58F,.30F,1,.72F);
        crescent(b,age,21.6F,10,6.85F,318,112,.34F,3.76F,1.62F,.30F,.96F,.86F,1,.55F);
    }
    private static void verticalArcs(BufferBuilder b,float age){
        vArc(b,age,8.2F,4.8F,-.25F,1.62F,2.45F,5.45F,2.05F,.72F,206,344,.62F,.66F,.24F,1,.88F);
        vArc(b,age,10.6F,5.8F,.32F,1.78F,2.75F,4.85F,2.55F,-.56F,-24,174,.46F,.82F,.42F,1,.68F);
        vArc(b,age,14.4F,6.5F,-.46F,1.96F,3,5.9F,2.9F,.48F,222,396,.40F,.58F,.20F,1,.70F);
        vArc(b,age,17.8F,6.8F,.50F,2.06F,3.20F,5.15F,2.70F,-.66F,-40,152,.36F,.74F,.34F,1,.62F);
        vArc(b,age,21.5F,7.2F,-.18F,2.22F,3.45F,6.35F,3.10F,.82F,198,358,.34F,.92F,.62F,1,.54F);
        vArc(b,age,27,7,.20F,2.26F,3.18F,5.45F,2.80F,-.44F,18,188,.28F,.62F,.28F,1,.42F);
    }
    private static void blades(BufferBuilder b,float age){
        blade(b,age,14,9,-5.6F,.36F,.85F,4.6F,4.20F,5.15F,.13F,.90F,.82F,1,.78F);
        blade(b,age,16.2F,9.5F,4.8F,.32F,.20F,-4.4F,4.85F,5.45F,.12F,.62F,.36F,1,.78F);
        blade(b,age,18.4F,10,-5,.70F,5.15F,5.2F,4.55F,1.35F,.10F,.98F,.88F,1,.64F);
        blade(b,age,20.4F,12,2.45F,.20F,2.15F,5.25F,5.25F,6.35F,.11F,.72F,.42F,1,.62F);
        blade(b,age,22.5F,12,-2.70F,.24F,1.45F,-5.75F,5.05F,5.95F,.10F,.88F,.62F,1,.54F);
        for(int i=0;i<9;i++){float start=18+i*1.15F,side=-4.5F+i*1.15F;blade(b,age,start,8,side,.15F,3.7F+(i%2)*.9F,side+1.45F,4.30F+(i%3)*.35F,6.8F,.055F+(i%3)*.012F,i%2==0?.92F:.58F,i%2==0?.86F:.32F,1,.34F);}
    }
    private static void fragments(BufferBuilder b,float age){
        float global=1-smoother(stage(age,48,9));if(global<=.01F)return;
        for(int i=0;i<SHARD_COUNT;i++){float start=7+det(i,2.2F)*27,raw=stage(age,start,20+det(i,3.1F)*6);if(raw<=0||raw>=1)continue;
            float launch=fastOut(MathHelper.clamp(raw/.42F,0,1)),fade=(1-smoother(raw))*global,angle=det(i,4)*TAU+age*.018F;
            float radius=.72F+det(i,5)*4.70F+launch*(.35F+det(i,6)*1.45F),x=MathHelper.cos(angle)*radius,z=.45F+MathHelper.sin(angle)*radius*.68F+det(i,7)*2.4F;
            float y=.35F+det(i,8)*2.75F+launch*(.35F+det(i,9)*1.35F),size=.045F+det(i,10)*.11F,alpha=fade*(.26F+det(i,11)*.44F);
            diamond(b,x,y,z,size,age*.16F+i*.91F,i%3==0?.96F:.62F,i%3==0?.82F:.34F,1,alpha);}
    }
    private static void billboards(BufferBuilder b,float age,float viewYaw,float viewPitch,float entityYaw){
        pulse(b,age,viewYaw,viewPitch,entityYaw,7.4F,4.2F,0,1.25F,2.35F,6.4F,.86F,.48F);
        pulse(b,age,viewYaw,viewPitch,entityYaw,11.5F,6,.15F,1.35F,2.55F,4.8F,.74F,-12);
        pulse(b,age,viewYaw,viewPitch,entityYaw,17,7,0,1.58F,2.75F,4.5F,.68F,22);
        pulse(b,age,viewYaw,viewPitch,entityYaw,23,7.5F,-.10F,1.72F,3.05F,4.1F,.54F,-28);
        pulse(b,age,viewYaw,viewPitch,entityYaw,31,9,0,1.80F,3.30F,5.1F,.62F,10);
    }
    private static void pulse(BufferBuilder b,float age,float viewYaw,float viewPitch,float entityYaw,float center,float duration,float x,float y,float z,float scale,float alphaScale,float rotation){
        float local=1-Math.abs(age-center)/duration,p=smoother(MathHelper.clamp(local,0,1));if(p<=.01F)return;float flicker=.82F+.18F*MathHelper.sin(age*1.7F+center),half=scale*(.70F+p*.38F)*.5F;
        // 精确复现新版的 right = cameraLeft.negate()、up = cameraUp。
        // 两者不能由同一个加 180° 的 yaw 生成：额外反向只属于 right；若 up
        // 也跟着反向，俯仰观察时面片就会镜像、扭转，Z 轴旋转方向也会颠倒。
        float delta=(viewYaw-entityYaw)*DEG,pitch=viewPitch*DEG;
        float sinYaw=MathHelper.sin(delta),cosYaw=MathHelper.cos(delta),sinPitch=MathHelper.sin(pitch),cosPitch=MathHelper.cos(pitch);
        Vec3d right=new Vec3d(-cosYaw,0,-sinYaw);
        Vec3d up=new Vec3d(-sinYaw*sinPitch,cosPitch,cosYaw*sinPitch);
        float rot=(rotation+age*2.4F)*DEG,c=MathHelper.cos(rot),s=MathHelper.sin(rot);Vec3d sx=right.scale(c).add(up.scale(s)).scale(half),sy=up.scale(c).subtract(right.scale(s)).scale(half),o=new Vec3d(x,y,z);
        quad(b,o.subtract(sx).subtract(sy),o.add(sx).subtract(sy),o.add(sx).add(sy),o.subtract(sx).add(sy),6,1,7,1,7,0,6,0,.96F,.78F,1,p*alphaScale*flicker);
    }

    private static void mandala(BufferBuilder b,float age){float open=smoother(stage(age,7,8)),fade=1-smoother(stage(age,39,12)),a=open*fade;if(a<=.01F)return;float rot=age*.022F;
        segmentedRing(b,.09F,CENTER_Z,3.65F+open*.22F,.075F,rot,6,GOLD_R,GOLD_G,GOLD_B,a*.50F);segmentedRing(b,.115F,CENTER_Z,4.42F,.045F,-rot*1.35F,9,LAV_R,LAV_G,LAV_B,a*.34F);
        for(int i=0;i<3;i++){float a0=rot+i*TAU/3,a1=rot+(i+1)*TAU/3;hRibbon(b,MathHelper.cos(a0)*3.28F,.13F,CENTER_Z+MathHelper.sin(a0)*3.28F,MathHelper.cos(a1)*3.28F,.13F,CENTER_Z+MathHelper.sin(a1)*3.28F,.035F,GOLD_R,GOLD_G,GOLD_B,a*.28F,4);}}
    private static void wingSigils(BufferBuilder b,float age){float appear=smoother(stage(age,7,4)),disappear=1-smoother(stage(age,36,7)),a=appear*disappear;if(a<=.01F)return;float gather=smoother(stage(age,30,6)),radius=lerp(gather,4.25F,.42F),spin=age*(.24F+gather*.20F),scale=.86F+appear*.26F+gather*.18F;
        for(int i=0;i<3;i++){float angle=spin+i*TAU/3,x=MathHelper.cos(angle)*radius,z=CENTER_Z+MathHelper.sin(angle)*radius,y=1.05F+MathHelper.sin(angle*2)*.22F;wing(b,x,y,z,angle+(float)Math.PI/2,scale,a*(.72F+gather*.28F));float trail=angle-.34F-gather*.18F;hRibbon(b,MathHelper.cos(trail)*radius,y-.02F,CENTER_Z+MathHelper.sin(trail)*radius,x,y,z,.075F+gather*.035F,LAV_R,LAV_G,LAV_B,a*.52F,2);}}
    private static void wing(BufferBuilder b,float x,float y,float z,float angle,float scale,float a){float fx=MathHelper.cos(angle),fz=MathHelper.sin(angle),sx=-fz,sz=fx,front=1.18F*scale,back=.72F*scale,w=.34F*scale;
        quad(b,new Vec3d(x+fx*front,y,z+fz*front),new Vec3d(x+sx*w,y+.12F*scale,z+sz*w),new Vec3d(x-fx*back,y,z-fz*back),new Vec3d(x-sx*w,y-.12F*scale,z-sz*w),8,.5F,8.5F,0,9,.5F,8.5F,1,GOLD_R,GOLD_G,GOLD_B,a);
        hRibbon(b,x-fx*.18F*scale,y-.025F,z-fz*.18F*scale,x-fx*.74F*scale+sx*.66F*scale,y-.025F,z-fz*.74F*scale+sz*.66F*scale,.095F*scale,LAV_R,LAV_G,LAV_B,a*.82F,2);
        hRibbon(b,x-fx*.18F*scale,y+.025F,z-fz*.18F*scale,x-fx*.74F*scale-sx*.66F*scale,y+.025F,z-fz*.74F*scale-sz*.66F*scale,.095F*scale,LAV_R,LAV_G,LAV_B,a*.82F,2);}
    private static void unityFinisher(BufferBuilder b,float age){float charge=smoother(stage(age,32.5F,3.5F)),fade=1-smoother(stage(age,36,9)),a=charge*fade;if(a<=.01F)return;float half=5.15F*charge;
        for(int i=0;i<3;i++){float angle=i*TAU/3+.12F,fx=MathHelper.cos(angle),fz=MathHelper.sin(angle);hRibbon(b,-fx*half,1.12F,CENTER_Z-fz*half,fx*half,1.12F,CENTER_Z+fz*half,.34F+charge*.22F,GOLD_R,GOLD_G,GOLD_B,a*.82F,0);hRibbon(b,-fx*half,1.16F,CENTER_Z-fz*half,fx*half,1.16F,CENTER_Z+fz*half,.095F,1,.98F,.90F,a,2);}
        float shock=smoother(stage(age,35.5F,6.5F)),shockFade=1-smoother(stage(age,40,7));segmentedRing(b,.16F,CENTER_Z,lerp(shock,.38F,6.15F),lerp(shock,.34F,.055F),age*.015F,12,GOLD_R,GOLD_G,GOLD_B,shockFade*(1-shock*.52F));
        float column=smoother(stage(age,35,1))*(1-smoother(stage(age,36,5)));lightColumn(b,0,CENTER_Z,.18F,5.15F,.44F,1,.96F,.82F,column*.78F);lightColumn(b,0,CENTER_Z,.42F,4.65F,.13F,1,1,.96F,column);}
    private static void afterglow(BufferBuilder b,float age){float open=smoother(stage(age,35.5F,2)),fade=1-smoother(stage(age,45,10)),a=open*fade;if(a<=.01F)return;for(int i=0;i<18;i++){float seed=det(i,4.71F),angle=seed*TAU+age*(.012F+(i%3)*.003F),radius=.65F+det(i,8.13F)*4.7F+stage(age,36,14)*1.2F;diamond(b,MathHelper.cos(angle)*radius,.35F+det(i,2.27F)*2.8F+stage(age,36,12)*.65F,CENTER_Z+MathHelper.sin(angle)*radius,.10F+det(i,6.41F)*.20F,angle+age*.025F,i%3==0?GOLD_R:LAV_R,i%3==0?GOLD_G:LAV_G,i%3==0?GOLD_B:LAV_B,a*(.35F+seed*.38F));}}

    private static void crescent(BufferBuilder b,float age,float start,float duration,float radius,float startDeg,float endDeg,float y0,float y1,float zOff,float width,float r,float g,float bl,float alphaScale){radius*=1.35F;float reveal=smoother(stage(age,start,duration)),fade=1-smoother(stage(age,start+duration+9,9)),a=reveal*fade*alphaScale;if(a<=.01F)return;int visible=MathHelper.clamp((int)Math.ceil(SWEEP_SEGMENTS*reveal),2,SWEEP_SEGMENTS);
        for(int i=0;i<visible;i++){float t0=i/(float)SWEEP_SEGMENTS,t1=Math.min((i+1)/(float)SWEEP_SEGMENTS,reveal);if(t1<=t0)continue;Vec3d p0=sweepPoint(t0,radius,startDeg,endDeg,y0,y1,zOff),p1=sweepPoint(t1,radius,startDeg,endDeg,y0,y1,zOff);float angle0=(startDeg+(endDeg-startDeg)*t0)*DEG,angle1=(startDeg+(endDeg-startDeg)*t1)*DEG,w0=width*sweepWidth(t0),w1=width*sweepWidth(t1),a0=a*sweepAlpha(t0,reveal),a1=a*sweepAlpha(t1,reveal);Vec3d s0=new Vec3d(MathHelper.sin(angle0),0,MathHelper.cos(angle0)),s1=new Vec3d(MathHelper.sin(angle1),0,MathHelper.cos(angle1));quadA(b,p0.add(s0.scale(w0)),p1.add(s1.scale(w1)),p1.subtract(s1.scale(w1)),p0.subtract(s0.scale(w0)),t0,0,t1,0,t1,1,t0,1,r,g,bl,a0,a1,a1*.88F,a0*.88F);quadA(b,p0.add(s0.scale(w0*.32F)).add(0,.04F,0),p1.add(s1.scale(w1*.32F)).add(0,.04F,0),p1.subtract(s1.scale(w1*.32F)).add(0,-.04F,0),p0.subtract(s0.scale(w0*.32F)).add(0,-.04F,0),2+t0,0,2+t1,0,2+t1,1,2+t0,1,1,.88F,1,a0*1.25F,a1*1.25F,a1*1.05F,a0*1.05F);}}
    private static Vec3d sweepPoint(float t,float radius,float start,float end,float y0,float y1,float z){float angle=(start+(end-start)*t)*DEG;return new Vec3d(MathHelper.sin(angle)*radius,lerp(t,y0,y1)+MathHelper.sin(t*(float)Math.PI)*.56F,MathHelper.cos(angle)*radius+z);}
    private static float sweepWidth(float t){return .16F+(float)Math.pow(MathHelper.sin(t*(float)Math.PI),.55D)*.84F;}private static float sweepAlpha(float t,float reveal){float head=smoother(MathHelper.clamp(t/Math.max(.001F,reveal),0,1)),tail=smoother(MathHelper.clamp(t/.14F,0,1)),end=1-smoother(MathHelper.clamp((t-.92F)/.08F,0,1));return(.26F+head*.74F)*tail*end;}
    private static void vArc(BufferBuilder b,float age,float start,float duration,float cx,float cy,float cz,float rx,float ry,float bend,float startDeg,float endDeg,float width,float r,float g,float bl,float alphaScale){float reveal=smoother(stage(age,start,duration)),fade=1-smoother(stage(age,start+duration+5,8)),a=reveal*fade*alphaScale;if(a<=.01F)return;int visible=MathHelper.clamp((int)Math.ceil(SWEEP_SEGMENTS*reveal),2,SWEEP_SEGMENTS);for(int i=0;i<visible;i++){float t0=i/(float)SWEEP_SEGMENTS,t1=Math.min((i+1)/(float)SWEEP_SEGMENTS,reveal);if(t1<=t0)continue;Arc p0=arcPoint(t0,cx,cy,cz,rx,ry,bend,startDeg,endDeg),p1=arcPoint(t1,cx,cy,cz,rx,ry,bend,startDeg,endDeg);float w0=width*vWidth(t0),w1=width*vWidth(t1),a0=a*vAlpha(t0,reveal),a1=a*vAlpha(t1,reveal);quadA(b,p0.plus(w0,0),p1.plus(w1,0),p1.plus(-w1,0),p0.plus(-w0,0),t0,0,t1,0,t1,1,t0,1,r,g,bl,a0,a1,a1*.9F,a0*.9F);quadA(b,p0.plus(w0*.26F,-.018F),p1.plus(w1*.26F,-.018F),p1.plus(-w1*.26F,.018F),p0.plus(-w0*.26F,.018F),2+t0,0,2+t1,0,2+t1,1,2+t0,1,1,.84F,1,a0*1.36F,a1*1.36F,a1*1.14F,a0*1.14F);}}
    private static Arc arcPoint(float t,float cx,float cy,float cz,float rx,float ry,float bend,float start,float end){float angle=(start+(end-start)*t)*DEG,x=cx+MathHelper.cos(angle)*rx,y=cy+MathHelper.sin(angle)*ry,z=cz+MathHelper.sin(t*(float)Math.PI)*bend,span=(end-start)*DEG,dx=-MathHelper.sin(angle)*rx*span,dy=MathHelper.cos(angle)*ry*span,len=MathHelper.sqrt(dx*dx+dy*dy);return len<=1E-5F?new Arc(x,y,z,0,1):new Arc(x,y,z,-dy/len,dx/len);}
    private static float vWidth(float t){return .20F+(float)Math.pow(Math.max(0,MathHelper.sin(t*(float)Math.PI)),.48D)*.94F;}private static float vAlpha(float t,float reveal){float head=smoother(MathHelper.clamp(t/Math.max(.001F,reveal),0,1)),tail=smoother(MathHelper.clamp(t/.10F,0,1)),end=1-smoother(MathHelper.clamp((t-.94F)/.06F,0,1));return(.32F+head*.68F)*tail*end;}
    private static final class Arc{final float x,y,z,sx,sy;Arc(float x,float y,float z,float sx,float sy){this.x=x;this.y=y;this.z=z;this.sx=sx;this.sy=sy;}Vec3d plus(float w,float dz){return new Vec3d(x+sx*w,y+sy*w,z+dz);}}
    private static void blade(BufferBuilder b,float age,float start,float duration,float x0,float y0,float z0,float x1,float y1,float z1,float width,float r,float g,float bl,float alphaScale){float in=smoother(stage(age,start,1.2F)),out=1-smoother(stage(age,start+duration-2.2F,2.2F)),a=in*out*alphaScale;if(a<=.01F)return;float jitter=MathHelper.sin(age*.66F+start)*.05F;line(b,new Vec3d(x0,y0+jitter,z0),new Vec3d(x1,y1-jitter,z1),width*(.75F+in*.55F),r,g,bl,a,2);}
    private static void liftedLoop(BufferBuilder b,float phase,float arc,float rx,float rz,float baseY,float lift,float width,float r,float g,float bl,float a){for(int i=0;i<LOOP_SEGMENTS;i++){float t0=i/(float)LOOP_SEGMENTS,t1=(i+1)/(float)LOOP_SEGMENTS;line(b,loopPoint(phase+(t0-.5F)*arc,rx,rz,baseY,lift),loopPoint(phase+(t1-.5F)*arc,rx,rz,baseY,lift),width,r,g,bl,edge(t0)*a,2);}}
    private static Vec3d loopPoint(float angle,float rx,float rz,float baseY,float lift){return new Vec3d(MathHelper.cos(angle)*rx,baseY+MathHelper.sin(angle*.62F+.7F)*lift+MathHelper.sin(angle*1.7F)*.16F,MathHelper.sin(angle)*rz+2);}
    private static void ring(BufferBuilder b,float y,float radius,float width,float r,float g,float bl,float a){ringInternal(b,y,radius,width,0,r,g,bl,a,false);}private static void brokenRing(BufferBuilder b,float y,float radius,float width,float scatter,float r,float g,float bl,float a){ringInternal(b,y,radius,width,scatter,r,g,bl,a,true);}
    private static void ringInternal(BufferBuilder b,float y,float radius,float width,float scatter,float r,float g,float bl,float a,boolean broken){float inner=Math.max(.04F,radius-width*.5F),outer=radius+width*.5F;int gap=Math.max(2,8-(int)Math.floor(scatter*5));for(int i=0;i<RING_SEGMENTS;i++){float noise=.5F+.5F*MathHelper.sin(i*1.73F+scatter*9);if(broken&&scatter>.22F&&((i+(int)Math.floor(scatter*13))%gap==0||noise<scatter*.18F))continue;float t0=i/(float)RING_SEGMENTS,t1=(i+1)/(float)RING_SEGMENTS,a0=t0*TAU,a1=t1*TAU,local=a*(broken?(.72F+noise*.28F)*(1-scatter*.28F):1);quad(b,new Vec3d(MathHelper.cos(a0)*outer,y,MathHelper.sin(a0)*outer),new Vec3d(MathHelper.cos(a1)*outer,y,MathHelper.sin(a1)*outer),new Vec3d(MathHelper.cos(a1)*inner,y,MathHelper.sin(a1)*inner),new Vec3d(MathHelper.cos(a0)*inner,y,MathHelper.sin(a0)*inner),4+t0,0,4+t1,0,4+t1,1,4+t0,1,r,g,bl,local);}}
    private static void segmentedRing(BufferBuilder b,float y,float centerZ,float radius,float width,float rotation,int gap,float r,float g,float bl,float a){float inner=Math.max(.02F,radius-width*.5F),outer=radius+width*.5F;for(int i=0;i<72;i++){if(i%gap==gap-1)continue;float t0=i/72F,t1=(i+1)/72F,a0=rotation+t0*TAU,a1=rotation+t1*TAU;quad(b,new Vec3d(MathHelper.cos(a0)*outer,y,centerZ+MathHelper.sin(a0)*outer),new Vec3d(MathHelper.cos(a1)*outer,y,centerZ+MathHelper.sin(a1)*outer),new Vec3d(MathHelper.cos(a1)*inner,y,centerZ+MathHelper.sin(a1)*inner),new Vec3d(MathHelper.cos(a0)*inner,y,centerZ+MathHelper.sin(a0)*inner),4+t0,0,4+t1,0,4+t1,1,4+t0,1,r,g,bl,a);}}
    private static void hRibbon(BufferBuilder b,float x0,float y0,float z0,float x1,float y1,float z1,float width,float r,float g,float bl,float a,float u){Vec3d d=new Vec3d(x1-x0,0,z1-z0);if(d.lengthSquared()<=1E-5)return;d=d.normalize();Vec3d s=new Vec3d(-d.z*width,0,d.x*width),p0=new Vec3d(x0,y0,z0),p1=new Vec3d(x1,y1,z1);quad(b,p0.add(s),p1.add(s),p1.subtract(s),p0.subtract(s),u,0,u+1,0,u+1,1,u,1,r,g,bl,a);}
    private static void line(BufferBuilder b,Vec3d p0,Vec3d p1,float width,float r,float g,float bl,float a,float u){Vec3d d=p1.subtract(p0);if(d.lengthSquared()<=1E-5)return;d=d.normalize();Vec3d s=new Vec3d(-d.z,0,d.x);if(s.lengthSquared()<=1E-5)s=new Vec3d(1,0,0);else s=s.normalize();s=s.scale(width);quad(b,p0.add(s),p1.add(s),p1.subtract(s),p0.subtract(s),u,0,u+1,0,u+1,1,u,1,r,g,bl,a);}
    private static void diamond(BufferBuilder b,float x,float y,float z,float size,float rot,float r,float g,float bl,float a){float c=MathHelper.cos(rot),s=MathHelper.sin(rot),mx=c*size,mz=s*size,sx=-s*size*.35F,sz=c*size*.35F;quad(b,new Vec3d(x-mx,y,z-mz),new Vec3d(x+sx,y+size*1.42F,z+sz),new Vec3d(x+mx,y,z+mz),new Vec3d(x-sx,y-size*1.42F,z-sz),8,.5F,8.5F,0,9,.5F,8.5F,1,r,g,bl,a);}
    private static void lightColumn(BufferBuilder b,float x,float z,float y0,float y1,float half,float r,float g,float bl,float a){quad(b,new Vec3d(x-half,y0,z),new Vec3d(x-half*.45F,y1,z),new Vec3d(x+half*.45F,y1,z),new Vec3d(x+half,y0,z),2,1,2,0,3,0,3,1,r,g,bl,a);quad(b,new Vec3d(x,y0,z-half),new Vec3d(x,y1,z-half*.45F),new Vec3d(x,y1,z+half*.45F),new Vec3d(x,y0,z+half),2,1,2,0,3,0,3,1,r,g,bl,a);}
    private static void quad(BufferBuilder b,Vec3d p0,Vec3d p1,Vec3d p2,Vec3d p3,float u0,float v0,float u1,float v1,float u2,float v2,float u3,float v3,float r,float g,float bl,float a){quadA(b,p0,p1,p2,p3,u0,v0,u1,v1,u2,v2,u3,v3,r,g,bl,a,a,a,a);}private static void quadA(BufferBuilder b,Vec3d p0,Vec3d p1,Vec3d p2,Vec3d p3,float u0,float v0,float u1,float v1,float u2,float v2,float u3,float v3,float r,float g,float bl,float a0,float a1,float a2,float a3){v(b,p0,u0,v0,r,g,bl,a0);v(b,p1,u1,v1,r,g,bl,a1);v(b,p2,u2,v2,r,g,bl,a2);v(b,p3,u3,v3,r,g,bl,a3);}private static void v(BufferBuilder b,Vec3d p,float u,float vv,float r,float g,float bl,float a){b.pos(p.x,p.y,p.z).tex(u,vv).color(r,g,bl,MathHelper.clamp(a,0,1)).endVertex();}
    private static float stage(float age,float start,float duration){return MathHelper.clamp((age-start)/duration,0,1);}private static float smoother(float t){t=MathHelper.clamp(t,0,1);return t*t*t*(t*(t*6-15)+10);}private static float fastOut(float t){t=MathHelper.clamp(t,0,1);float i=1-t;return 1-i*i*i*i;}private static float edge(float t){return smoother(MathHelper.clamp(t/.12F,0,1))*(1-smoother(MathHelper.clamp((t-.88F)/.12F,0,1)));}private static float det(int i,float salt){float v=MathHelper.sin(i*12.9898F+salt*78.233F)*43758.547F;return v-(float)Math.floor(v);}private static float lerp(float t,float a,float b){return a+(b-a)*t;}
}

package com.wjx.kablade.Entity.Render;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** 相机感知 ribbon、圆盘、星爆与确定性闪电；参数直接对应 1.20 几何层。 */
final class NarukamiGeometry {
    interface Curve { Vec3d point(float u); }
    private static final Vec3d UP=new Vec3d(0,1,0);
    private static final float TAU=(float)Math.PI*2;
    private NarukamiGeometry(){}

    static void ribbon(BufferBuilder out,Curve curve,int segments,float head,float tail,float width,Vec3d camera,int color,float alpha,float erode,long seed,float frame){
        float start=MathHelper.clamp(tail,0,1),end=MathHelper.clamp(head,0,1);if(alpha<=.001F||end<=start+1E-4F)return;int count=Math.max(2,segments);
        for(int i=0;i<count;i++){float u0=i/(float)count,u1=(i+1)/(float)count;if(u1<start||u0>end)continue;float a=Math.max(start,u0),b=Math.min(end,u1),middle=(a+b)*.5F;
            float noise=hash01(seed+i*0x9E3779B97F4A7C15L+(long)(frame*13)*0x632BE59BD9B4E019L),edge=erode*(.34F+.66F*middle);if(noise<edge*.72F)continue;
            Vec3d p0=curve.point(a),p1=curve.point(b);float t0=(float)Math.pow(Math.max(0,Math.sin(Math.PI*a)),.72),t1=(float)Math.pow(Math.max(0,Math.sin(Math.PI*b)),.72);
            beamQuad(out,p0,p1,width*(.12F+.88F*t0),width*(.12F+.88F*t1),camera,color,alpha*(1-edge*.46F),a,b);}}

    static void ribbonFixed(BufferBuilder out,Curve curve,int segments,float head,float tail,float width,Vec3d normal,int color,float alpha,float erode,long seed,float frame){
        float start=MathHelper.clamp(tail,0,1),end=MathHelper.clamp(head,0,1);if(alpha<=.001F||end<=start+1E-4F)return;int count=Math.max(2,segments);
        for(int i=0;i<count;i++){float u0=i/(float)count,u1=(i+1)/(float)count;if(u1<start||u0>end)continue;float a=Math.max(start,u0),b=Math.min(end,u1),middle=(a+b)*.5F;
            float noise=hash01(seed+i*0x9E3779B97F4A7C15L+(long)(frame*13)*0x632BE59BD9B4E019L),edge=erode*(.34F+.66F*middle);if(noise<edge*.72F)continue;
            fixedBeamQuad(out,curve.point(a),curve.point(b),width*(.12F+.88F*endTaper(a)),width*(.12F+.88F*endTaper(b)),normal,color,alpha*(1-edge*.46F),a,b);}}

    static void beam(BufferBuilder out,Vec3d start,Vec3d end,float width,Vec3d camera,int color,float alpha){beamQuad(out,start,end,width,width,camera,color,alpha,0,1);}
    static void lightning(BufferBuilder outer,BufferBuilder core,Vec3d start,Vec3d end,int segments,float jitter,float width,Vec3d camera,int outerColor,int coreColor,float alpha,long seed){
        Vec3d direction=end.subtract(start);if(direction.lengthSquared()<1E-8||alpha<=.001F)return;Vec3d dir=direction.normalize(),reference=Math.abs(dir.y)<.86?UP:new Vec3d(1,0,0),sideA=dir.crossProduct(reference).normalize(),sideB=dir.crossProduct(sideA).normalize(),previous=start;int count=Math.max(3,segments);
        for(int i=1;i<=count;i++){float u=i/(float)count,envelope=MathHelper.sin((float)Math.PI*u);double a=(hash01(seed+i*31L)-.5)*2*jitter*envelope,b=(hash01(seed+i*47L+19)-.5)*jitter*envelope;Vec3d point=start.add(direction.scale(u)).add(sideA.scale(a)).add(sideB.scale(b));float flicker=.66F+hash01(seed+i*73L)*.34F;
            beam(outer,previous,point,width,camera,outerColor,alpha*.58F*flicker);beam(core,previous,point,width*.34F,camera,coreColor,alpha*flicker);previous=point;}}

    static void lightningLayer(BufferBuilder out,boolean coreLayer,Vec3d start,Vec3d end,int segments,float jitter,float width,Vec3d camera,int outerColor,int coreColor,float alpha,long seed){
        Vec3d direction=end.subtract(start);if(direction.lengthSquared()<1E-8||alpha<=.001F)return;Vec3d dir=direction.normalize(),reference=Math.abs(dir.y)<.86?UP:new Vec3d(1,0,0),sideA=dir.crossProduct(reference).normalize(),sideB=dir.crossProduct(sideA).normalize(),previous=start;int count=Math.max(3,segments);
        for(int i=1;i<=count;i++){float u=i/(float)count,envelope=MathHelper.sin((float)Math.PI*u);double a=(hash01(seed+i*31L)-.5)*2*jitter*envelope,b=(hash01(seed+i*47L+19)-.5)*jitter*envelope;Vec3d point=start.add(direction.scale(u)).add(sideA.scale(a)).add(sideB.scale(b));float flicker=.66F+hash01(seed+i*73L)*.34F;
            beam(out,previous,point,coreLayer?width*.34F:width,camera,coreLayer?coreColor:outerColor,alpha*(coreLayer?1:.58F)*flicker);previous=point;}}

    static void billboard(BufferBuilder out,Vec3d center,Vec3d camera,float halfWidth,float halfHeight,float rotation,int color,float alpha){
        if(alpha<=.001F)return;Axes axes=cameraAxes(center,camera,rotation);Vec3d x=axes.x.scale(halfWidth),y=axes.y.scale(halfHeight);quad(out,center.subtract(x).subtract(y),center.add(x).subtract(y),center.add(x).add(y),center.subtract(x).add(y),color,alpha,0,1,0,1);}
    static void discBillboard(BufferBuilder out,Vec3d center,Vec3d camera,float halfWidth,float halfHeight,float rotation,int color,float alpha){if(alpha<=.001F)return;Axes axes=cameraAxes(center,camera,rotation);disc(out,center,axes.x,axes.y,halfWidth,halfHeight,color,alpha,24);}
    static void discOriented(BufferBuilder out,Vec3d center,Vec3d axisX,Vec3d axisY,float halfWidth,float halfHeight,int color,float alpha){if(alpha<=.001F||axisX.lengthSquared()<1E-8||axisY.lengthSquared()<1E-8)return;disc(out,center,axisX.normalize(),axisY.normalize(),halfWidth,halfHeight,color,alpha,32);}
    private static void disc(BufferBuilder out,Vec3d center,Vec3d x,Vec3d y,float hw,float hh,int color,float alpha,int segments){for(int i=0;i<segments;i++){double a0=i*TAU/segments,a1=(i+1)*TAU/segments;Vec3d e0=center.add(x.scale(Math.cos(a0)*hw)).add(y.scale(Math.sin(a0)*hh)),e1=center.add(x.scale(Math.cos(a1)*hw)).add(y.scale(Math.sin(a1)*hh));float u0=.5F+MathHelper.cos((float)a0)*.5F,v0=.5F+MathHelper.sin((float)a0)*.5F,u1=.5F+MathHelper.cos((float)a1)*.5F,v1=.5F+MathHelper.sin((float)a1)*.5F;vertex(out,center,.5F,.5F,color,alpha);vertex(out,e0,u0,v0,color,alpha);vertex(out,e1,u1,v1,color,alpha);vertex(out,center,.5F,.5F,color,alpha);}}
    static void starBurst(BufferBuilder out,Vec3d center,Vec3d camera,int rays,float minLength,float maxLength,float width,int color,float alpha,long seed,float rotation){Axes axes=cameraAxes(center,camera,0);for(int i=0;i<rays;i++){double angle=rotation+i*TAU/(double)rays+(hash01(seed+i*17L)-.5)*.18;float length=minLength+(maxLength-minLength)*hash01(seed+i*29L);Vec3d d=axes.x.scale(Math.cos(angle)).add(axes.y.scale(Math.sin(angle)));beam(out,center.add(d.scale(.08)),center.add(d.scale(length)),width,camera,color,alpha*(.62F+hash01(seed+i*43L)*.38F));}}

    static float hash01(long value){long x=value;x^=x>>>33;x*=0xff51afd7ed558ccdL;x^=x>>>33;x*=0xc4ceb9fe1a85ec53L;x^=x>>>33;return(x>>>40)/(float)(1L<<24);}
    private static Axes cameraAxes(Vec3d center,Vec3d camera,float rotation){Vec3d view=camera.subtract(center);if(view.lengthSquared()<1E-8)view=new Vec3d(0,0,1);else view=view.normalize();Vec3d right=UP.crossProduct(view);if(right.lengthSquared()<1E-8)right=new Vec3d(1,0,0);else right=right.normalize();Vec3d up=view.crossProduct(right).normalize();double c=Math.cos(rotation),s=Math.sin(rotation);return new Axes(right.scale(c).add(up.scale(s)),up.scale(c).subtract(right.scale(s)));}
    private static void beamQuad(BufferBuilder out,Vec3d start,Vec3d end,float sw,float ew,Vec3d camera,int color,float alpha,float u0,float u1){Vec3d direction=end.subtract(start);if(direction.lengthSquared()<1E-10||alpha<=.001F)return;Vec3d side=direction.crossProduct(camera.subtract(start.add(end).scale(.5)));if(side.lengthSquared()<1E-10)side=direction.crossProduct(UP);if(side.lengthSquared()<1E-10)side=direction.crossProduct(new Vec3d(1,0,0));side=side.normalize();Vec3d s0=side.scale(sw),s1=side.scale(ew);quad(out,start.subtract(s0),start.add(s0),end.add(s1),end.subtract(s1),color,alpha,u0,u1,0,1);}
    private static void fixedBeamQuad(BufferBuilder out,Vec3d start,Vec3d end,float sw,float ew,Vec3d normal,int color,float alpha,float u0,float u1){Vec3d direction=end.subtract(start),side=normal.crossProduct(direction);if(direction.lengthSquared()<1E-10||side.lengthSquared()<1E-10||alpha<=.001F)return;side=side.normalize();Vec3d s0=side.scale(sw),s1=side.scale(ew);quad(out,start.subtract(s0),start.add(s0),end.add(s1),end.subtract(s1),color,alpha,u0,u1,0,1);}
    private static float endTaper(float u){float e=Math.min(MathHelper.clamp(u/.075F,0,1),MathHelper.clamp((1-u)/.075F,0,1));return e*e*(3-2*e);}
    private static void quad(BufferBuilder out,Vec3d a,Vec3d b,Vec3d c,Vec3d d,int color,float alpha,float u0,float u1,float v0,float v1){vertex(out,a,u0,v1,color,alpha);vertex(out,b,u0,v0,color,alpha);vertex(out,c,u1,v0,color,alpha);vertex(out,d,u1,v1,color,alpha);}
    private static void vertex(BufferBuilder out,Vec3d p,float u,float v,int color,float alpha){out.pos(p.x,p.y,p.z).tex(u,v).color((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,MathHelper.clamp(alpha,0,1)).endVertex();}
    private static final class Axes{final Vec3d x,y;Axes(Vec3d x,Vec3d y){this.x=x;this.y=y;}}
}

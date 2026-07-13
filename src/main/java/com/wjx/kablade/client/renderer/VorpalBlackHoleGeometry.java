package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

/** Allocation-free mesh emitters used by the Vorpal Hole renderer. */
final class VorpalBlackHoleGeometry {

    private static final float PHI = 1.61803398875F;
    private static final float[][] ICOSA_VERTICES = {
            {-1, PHI, 0}, {1, PHI, 0}, {-1, -PHI, 0}, {1, -PHI, 0},
            {0, -1, PHI}, {0, 1, PHI}, {0, -1, -PHI}, {0, 1, -PHI},
            {PHI, 0, -1}, {PHI, 0, 1}, {-PHI, 0, -1}, {-PHI, 0, 1}
    };
    private static final int[][] ICOSA_FACES = {
            {0,11,5},{0,5,1},{0,1,7},{0,7,10},{0,10,11},
            {1,5,9},{5,11,4},{11,10,2},{10,7,6},{7,1,8},
            {3,9,4},{3,4,2},{3,2,6},{3,6,8},{3,8,9},
            {4,9,5},{2,4,11},{6,2,10},{8,6,7},{9,8,1}
    };
    private static final ThreadLocal<float[]> SCRATCH = ThreadLocal.withInitial(() -> new float[16]);

    private VorpalBlackHoleGeometry() {
    }

    static void ellipsoid(VertexConsumer out, Matrix4f mat,
                          float cx, float cy, float cz, float rx, float ry, float rz,
                          int latitudes, int longitudes, int color, float alpha) {
        for (int lat = 0; lat < latitudes; lat++) {
            float v0 = lat / (float) latitudes;
            float v1 = (lat + 1) / (float) latitudes;
            float p0 = (v0 - 0.5F) * (float) Math.PI;
            float p1 = (v1 - 0.5F) * (float) Math.PI;
            float cp0 = (float) Math.cos(p0), sp0 = (float) Math.sin(p0);
            float cp1 = (float) Math.cos(p1), sp1 = (float) Math.sin(p1);
            for (int lon = 0; lon < longitudes; lon++) {
                float a0 = lon / (float) longitudes * (float) (Math.PI * 2.0);
                float a1 = (lon + 1) / (float) longitudes * (float) (Math.PI * 2.0);
                float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
                float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                quad(out, mat,
                        cx + c0 * cp0 * rx, cy + sp0 * ry, cz + s0 * cp0 * rz,
                        cx + c0 * cp1 * rx, cy + sp1 * ry, cz + s0 * cp1 * rz,
                        cx + c1 * cp1 * rx, cy + sp1 * ry, cz + s1 * cp1 * rz,
                        cx + c1 * cp0 * rx, cy + sp0 * ry, cz + s1 * cp0 * rz,
                        color, alpha);
            }
        }
    }

    static void funnel(VertexConsumer out, Matrix4f mat, float inner, float outer, float halfDepth,
                       int radialSegments, int depthSegments, float twist,
                       int innerColor, int outerColor, float alpha, float time) {
        for (int zi = 0; zi < depthSegments; zi++) {
            float v0 = zi / (float) depthSegments;
            float v1 = (zi + 1) / (float) depthSegments;
            float z0 = (v0 * 2.0F - 1.0F) * halfDepth;
            float z1 = (v1 * 2.0F - 1.0F) * halfDepth;
            float d0 = Math.abs(z0) / halfDepth;
            float d1 = Math.abs(z1) / halfDepth;
            for (int ri = 0; ri < radialSegments; ri++) {
                float u0 = ri / (float) radialSegments;
                float u1 = (ri + 1) / (float) radialSegments;
                float a00 = u0 * tau() + z0 / halfDepth * twist + time;
                float a01 = u1 * tau() + z0 / halfDepth * twist + time;
                float a10 = u0 * tau() + z1 / halfDepth * twist + time;
                float a11 = u1 * tau() + z1 / halfDepth * twist + time;
                float r00 = funnelRadius(inner, outer, d0, a00, z0);
                float r01 = funnelRadius(inner, outer, d0, a01, z0);
                float r10 = funnelRadius(inner, outer, d1, a10, z1);
                float r11 = funnelRadius(inner, outer, d1, a11, z1);
                int color = mixColor(innerColor, outerColor, (d0 + d1) * 0.5F);
                quad(out, mat,
                        (float) Math.cos(a00) * r00, (float) Math.sin(a00) * r00 * 0.88F, z0,
                        (float) Math.cos(a10) * r10, (float) Math.sin(a10) * r10 * 0.88F, z1,
                        (float) Math.cos(a11) * r11, (float) Math.sin(a11) * r11 * 0.88F, z1,
                        (float) Math.cos(a01) * r01, (float) Math.sin(a01) * r01 * 0.88F, z0,
                        color, alpha);
            }
        }
    }

    private static float funnelRadius(float inner, float outer, float depth, float angle, float z) {
        float corrugation = (float) Math.sin(angle * 5.0F - z * 5.8F) * (0.026F + depth * 0.040F)
                + (float) Math.sin(angle * 11.0F + z * 7.1F) * 0.015F;
        return inner + (outer - inner) * (float) Math.pow(depth, 1.34) + corrugation;
    }

    static void torus(VertexConsumer out, Matrix4f mat, float cx, float cy, float cz,
                      float radius, float tube, float start, float arc,
                      int majorSegments, int minorSegments, float rx, float ry, float rz,
                      int color, float alpha) {
        float[] p = SCRATCH.get();
        for (int i = 0; i < majorSegments; i++) {
            float a0 = start + arc * i / majorSegments;
            float a1 = start + arc * (i + 1) / majorSegments;
            for (int j = 0; j < minorSegments; j++) {
                float b0 = tau() * j / minorSegments;
                float b1 = tau() * (j + 1) / minorSegments;
                torusPoint(p, 0, radius, tube, a0, b0, rx, ry, rz);
                torusPoint(p, 3, radius, tube, a1, b0, rx, ry, rz);
                torusPoint(p, 6, radius, tube, a1, b1, rx, ry, rz);
                torusPoint(p, 9, radius, tube, a0, b1, rx, ry, rz);
                quad(out, mat,
                        cx+p[0],cy+p[1],cz+p[2], cx+p[3],cy+p[4],cz+p[5],
                        cx+p[6],cy+p[7],cz+p[8], cx+p[9],cy+p[10],cz+p[11], color, alpha);
            }
        }
    }

    private static void torusPoint(float[] out, int offset, float radius, float tube, float a, float b,
                                   float rx, float ry, float rz) {
        float ring = radius + tube * (float) Math.cos(b);
        float x = ring * (float) Math.cos(a);
        float y = ring * (float) Math.sin(a);
        float z = tube * (float) Math.sin(b);
        rotateInto(out, offset, x, y, z, rx, ry, rz);
    }

    static void tube(VertexConsumer out, Matrix4f mat,
                     float x0, float y0, float z0, float x1, float y1, float z1,
                     float radius, int sides, int color, float alpha) {
        float dx=x1-x0, dy=y1-y0, dz=z1-z0;
        float len=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
        if (len < 1.0E-5F) return;
        dx/=len; dy/=len; dz/=len;
        float rx=Math.abs(dy)<0.88F?0.0F:1.0F, ry=Math.abs(dy)<0.88F?1.0F:0.0F, rz=0.0F;
        float ux=dy*rz-dz*ry, uy=dz*rx-dx*rz, uz=dx*ry-dy*rx;
        float ul=(float)Math.sqrt(ux*ux+uy*uy+uz*uz); ux/=ul;uy/=ul;uz/=ul;
        float vx=dy*uz-dz*uy, vy=dz*ux-dx*uz, vz=dx*uy-dy*ux;
        for(int i=0;i<sides;i++){
            float a0=tau()*i/sides,a1=tau()*(i+1)/sides;
            float c0=(float)Math.cos(a0)*radius,s0=(float)Math.sin(a0)*radius;
            float c1=(float)Math.cos(a1)*radius,s1=(float)Math.sin(a1)*radius;
            float ox0=ux*c0+vx*s0,oy0=uy*c0+vy*s0,oz0=uz*c0+vz*s0;
            float ox1=ux*c1+vx*s1,oy1=uy*c1+vy*s1,oz1=uz*c1+vz*s1;
            quad(out,mat,x0+ox0,y0+oy0,z0+oz0,x1+ox0,y1+oy0,z1+oz0,
                    x1+ox1,y1+oy1,z1+oz1,x0+ox1,y0+oy1,z0+oz1,color,alpha);
        }
    }

    static void icosahedron(VertexConsumer out, Matrix4f mat, float cx,float cy,float cz,
                            float sx,float sy,float sz,float rx,float ry,float rz,int color,float alpha) {
        float[] p = SCRATCH.get();
        for (int[] face : ICOSA_FACES) {
            icoPoint(p, 0, face[0], sx, sy, sz, rx, ry, rz);
            icoPoint(p, 3, face[1], sx, sy, sz, rx, ry, rz);
            icoPoint(p, 6, face[2], sx, sy, sz, rx, ry, rz);
            tri(out,mat,cx+p[0],cy+p[1],cz+p[2],cx+p[3],cy+p[4],cz+p[5],cx+p[6],cy+p[7],cz+p[8],color,alpha);
        }
    }

    private static void icoPoint(float[] out, int offset, int index,float sx,float sy,float sz,float rx,float ry,float rz){
        float[] p=ICOSA_VERTICES[index];
        rotateInto(out,offset,p[0]*sx/PHI,p[1]*sy/PHI,p[2]*sz/PHI,rx,ry,rz);
    }

    static void tetrahedron(VertexConsumer out, Matrix4f mat, float cx,float cy,float cz,float size,
                            float rx,float ry,float rz,int color,float alpha){
        float[] q=SCRATCH.get();
        rotateInto(q,0,size,size,size,rx,ry,rz);
        rotateInto(q,3,-size,-size,size,rx,ry,rz);
        rotateInto(q,6,-size,size,-size,rx,ry,rz);
        rotateInto(q,9,size,-size,-size,rx,ry,rz);
        tetraFace(out,mat,q,cx,cy,cz,0,2,1,color,alpha);
        tetraFace(out,mat,q,cx,cy,cz,0,1,3,color,alpha);
        tetraFace(out,mat,q,cx,cy,cz,0,3,2,color,alpha);
        tetraFace(out,mat,q,cx,cy,cz,1,2,3,color,alpha);
    }

    private static void tetraFace(VertexConsumer out, Matrix4f mat, float[] q, float cx, float cy, float cz,
                                  int ia, int ib, int ic, int color, float alpha) {
        int a=ia*3,b=ib*3,c=ic*3;
        tri(out,mat,cx+q[a],cy+q[a+1],cz+q[a+2],cx+q[b],cy+q[b+1],cz+q[b+2],
                cx+q[c],cy+q[c+1],cz+q[c+2],color,alpha);
    }

    static void wedge(VertexConsumer out, Matrix4f mat, float angle, float inner, float length,
                      float width, float depth, float thickness, int color, float alpha, float jagged) {
        float dx=(float)Math.cos(angle),dy=(float)Math.sin(angle),px=-dy,py=dx;
        float x0=dx*inner,y0=dy*inner;
        float x1=dx*(inner+length*.34F),y1=dy*(inner+length*.34F);
        float x2=dx*(inner+length*.72F),y2=dy*(inner+length*.72F);
        float xt=dx*(inner+length),yt=dy*(inner+length);
        float[] points=SCRATCH.get();
        points[0]=x0-px*width*.10F; points[7]=y0-py*width*.10F;
        points[1]=x0+px*width*.12F; points[8]=y0+py*width*.12F;
        points[2]=x1+px*width*.52F; points[9]=y1+py*width*.52F;
        points[3]=x2+px*width*(.22F+jagged*.12F); points[10]=y2+py*width*(.22F+jagged*.12F);
        points[4]=xt; points[11]=yt;
        points[5]=x2-px*width*(.28F-jagged*.08F); points[12]=y2-py*width*(.28F-jagged*.08F);
        points[6]=x1-px*width*.58F; points[13]=y1-py*width*.58F;
        float front=depth+thickness,back=depth-thickness;
        for(int i=1;i<6;i++){
            tri(out,mat,points[0],points[7],front,points[i],points[i+7],front,points[i+1],points[i+8],front,color,alpha);
            tri(out,mat,points[0],points[7],back,points[i+1],points[i+8],back,points[i],points[i+7],back,color,alpha);
        }
        for(int i=0;i<7;i++){
            int n=(i+1)%7;
            quad(out,mat,points[i],points[i+7],back,points[n],points[n+7],back,
                    points[n],points[n+7],front,points[i],points[i+7],front,color,alpha);
        }
    }

    static void glowDisc(VertexConsumer out, Matrix4f mat, float radius, float z, int segments,
                         int centerColor, int edgeColor, float alpha) {
        for(int i=0;i<segments;i++){
            float a0=tau()*i/segments,a1=tau()*(i+1)/segments;
            colorTri(out,mat,0,0,z,(float)Math.cos(a0)*radius,(float)Math.sin(a0)*radius,z,
                    (float)Math.cos(a1)*radius,(float)Math.sin(a1)*radius,z,centerColor,edgeColor,edgeColor,alpha);
        }
    }

    static void ribbon(VertexConsumer out, Matrix4f mat, float[] points, int pointCount,
                       float cameraX,float cameraY,float cameraZ,float width,float alpha,int color,
                       float tail,float head) {
        int first=Math.max(0,(int)Math.floor(tail*(pointCount-1)));
        int last=Math.min(pointCount-1,(int)Math.ceil(head*(pointCount-1)));
        float psx=0,psy=0,psz=0;
        for(int i=first;i<last;i++){
            int a=i*3,b=(i+1)*3;
            float x0=points[a],y0=points[a+1],z0=points[a+2];
            float x1=points[b],y1=points[b+1],z1=points[b+2];
            float tx=x1-x0,ty=y1-y0,tz=z1-z0;
            float mx=(x0+x1)*.5F,my=(y0+y1)*.5F,mz=(z0+z1)*.5F;
            float vx=cameraX-mx,vy=cameraY-my,vz=cameraZ-mz;
            float sx=vy*tz-vz*ty,sy=vz*tx-vx*tz,sz=vx*ty-vy*tx;
            float sl=(float)Math.sqrt(sx*sx+sy*sy+sz*sz);
            if(sl<1.0E-5F){sx=-tz;sy=0;sz=tx;sl=(float)Math.sqrt(sx*sx+sz*sz);}
            if(sl<1.0E-5F){sx=1;sy=0;sz=0;sl=1;}
            sx/=sl;sy/=sl;sz/=sl;
            if(i>first && sx*psx+sy*psy+sz*psz<0){sx=-sx;sy=-sy;sz=-sz;}
            psx=sx;psy=sy;psz=sz;
            float u=(i+.5F)/(pointCount-1.0F);
            float w=width*(.18F+(float)Math.sin(Math.PI*u)*.82F);
            quad(out,mat,x0+sx*w,y0+sy*w,z0+sz*w,x1+sx*w,y1+sy*w,z1+sz*w,
                    x1-sx*w,y1-sy*w,z1-sz*w,x0-sx*w,y0-sy*w,z0-sz*w,color,alpha);
        }
    }

    private static void rotateInto(float[] out,int offset,float x,float y,float z,float rx,float ry,float rz){
        float cx=(float)Math.cos(rx),sx=(float)Math.sin(rx),cy=(float)Math.cos(ry),sy=(float)Math.sin(ry),cz=(float)Math.cos(rz),sz=(float)Math.sin(rz);
        float y1=y*cx-z*sx,z1=y*sx+z*cx;
        float x2=x*cy+z1*sy,z2=-x*sy+z1*cy;
        out[offset]=x2*cz-y1*sz;
        out[offset+1]=x2*sz+y1*cz;
        out[offset+2]=z2;
    }

    private static int mixColor(int a,int b,float t){
        t=Math.max(0,Math.min(1,t));
        int r=(int)(((a>>16)&255)*(1-t)+((b>>16)&255)*t);
        int g=(int)(((a>>8)&255)*(1-t)+((b>>8)&255)*t);
        int bl=(int)((a&255)*(1-t)+(b&255)*t);
        return (r<<16)|(g<<8)|bl;
    }

    private static float tau(){return (float)(Math.PI*2.0);}

    private static void quad(VertexConsumer out,Matrix4f mat,
                             float x0,float y0,float z0,float x1,float y1,float z1,
                             float x2,float y2,float z2,float x3,float y3,float z3,int color,float alpha){
        tri(out,mat,x0,y0,z0,x1,y1,z1,x2,y2,z2,color,alpha);
        tri(out,mat,x0,y0,z0,x2,y2,z2,x3,y3,z3,color,alpha);
    }

    private static void tri(VertexConsumer out,Matrix4f mat,
                            float x0,float y0,float z0,float x1,float y1,float z1,float x2,float y2,float z2,int color,float alpha){
        vertex(out,mat,x0,y0,z0,color,alpha);vertex(out,mat,x1,y1,z1,color,alpha);vertex(out,mat,x2,y2,z2,color,alpha);
    }

    private static void colorTri(VertexConsumer out,Matrix4f mat,
                                 float x0,float y0,float z0,float x1,float y1,float z1,float x2,float y2,float z2,
                                 int c0,int c1,int c2,float alpha){
        vertex(out,mat,x0,y0,z0,c0,alpha);vertex(out,mat,x1,y1,z1,c1,0);vertex(out,mat,x2,y2,z2,c2,0);
    }

    private static void vertex(VertexConsumer out,Matrix4f mat,float x,float y,float z,int color,float alpha){
        out.vertex(mat,x,y,z).color((color>>16)&255,(color>>8)&255,color&255,(int)(Math.max(0,Math.min(1,alpha))*255)).endVertex();
    }
}

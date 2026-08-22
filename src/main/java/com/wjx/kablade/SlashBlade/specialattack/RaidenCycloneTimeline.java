package com.wjx.kablade.SlashBlade.specialattack;

import net.minecraft.util.math.MathHelper;

/** 重磁暴·斩的确定性位移、命中与视觉时间轴。 */
public final class RaidenCycloneTimeline {
    public static final float DURATION_SECONDS = 5.04F;
    public static final int DURATION_TICKS = 101;
    public static final int[] HIT_TICKS = {3, 8, 14, 21, 28, 44, 56, 60, 70, 79, 84};
    public static final float[] HIT_SECONDS = {0.13F,0.39F,0.72F,1.05F,1.39F,2.18F,2.78F,3.02F,3.48F,3.96F,4.20F};
    public static final float[] DAMAGE_WEIGHTS = {0.04F,0.04F,0.04F,0.04F,0.04F,0.13F,0.10F,0.10F,0.11F,0.14F,0.22F};
    public static final SlashSpec[] SLASHES = {
            new SlashSpec(.06F,.138F,.058F,.232F,.300F,.40F,-2.55F,1,3.24F,1.43F,.72F,.86F,.18F,.08F,-.08F,-.76F,-.10F,true),
            new SlashSpec(.30F,.130F,.054F,.218F,.286F,.38F,2.70F,-1,3.50F,1.66F,.68F,1.08F,.22F,-.05F,.14F,-.74F,-.08F,true),
            new SlashSpec(.61F,.148F,.066F,.245F,.318F,.42F,-2.15F,1,3.90F,1.84F,.74F,1.25F,.16F,.12F,-.18F,-.70F,-.04F,false),
            new SlashSpec(.94F,.136F,.058F,.228F,.298F,.39F,2.82F,-1,3.72F,2.02F,.69F,.98F,.21F,-.08F,.18F,-.69F,-.02F,false),
            new SlashSpec(1.27F,.128F,.054F,.214F,.282F,.37F,-2.30F,1,3.62F,2.18F,.63F,1.18F,.14F,.05F,-.10F,-.66F,-.02F,false),
            new SlashSpec(2.58F,.126F,.052F,.212F,.278F,.38F,-2.62F,1,2.92F,1.25F,.64F,1.18F,.18F,.16F,-.28F,0,0,true),
            new SlashSpec(3.30F,.118F,.050F,.202F,.265F,.36F,2.78F,-1,3.08F,1.42F,.70F,1.36F,.24F,-.22F,.36F,0,0,false),
            new SlashSpec(3.76F,.112F,.047F,.194F,.255F,.35F,-2.72F,1,3.22F,1.56F,.78F,1.08F,.20F,.22F,-.40F,0,0,true),
            new SlashSpec(4.04F,.108F,.045F,.188F,.248F,.34F,2.90F,-1,3.38F,1.72F,.82F,1.46F,.28F,-.18F,.50F,0,0,false)
    };
    private static final float[][] X = {{0,-.82F},{.25F,-.68F},{.60F,-.28F},{.96F,.05F},{1.30F,-.22F},{1.64F,-.56F},{1.84F,-.82F},{2.12F,-1.02F},{2.52F,-.96F},{2.78F,-.20F},{3.08F,-.72F},{3.34F,-.92F},{3.56F,-.16F},{3.82F,-.92F},{4.05F,-.18F},{4.28F,-.78F},{4.52F,-1.04F},{5.04F,-.84F}};
    private static final float[][] Z = {{0,-.28F},{.25F,-.18F},{.60F,-.54F},{.96F,-.24F},{1.30F,.18F},{1.64F,.05F},{1.84F,-.12F},{2.12F,-.22F},{2.52F,-.14F},{2.78F,.10F},{3.08F,.28F},{3.34F,.42F},{3.56F,.02F},{3.82F,-.28F},{4.05F,.25F},{4.28F,.42F},{4.52F,.05F},{5.04F,-.12F}};
    private static final float[][] YAW = {{0,-.45F},{.20F,-.85F},{.55F,1.50F},{.92F,3.72F},{1.28F,5.74F},{1.62F,7.45F},{1.84F,8.24F},{2.04F,8.05F},{2.52F,7.82F},{2.78F,8.72F},{3.10F,8.10F},{3.34F,8.88F},{3.58F,8.22F},{3.82F,9.10F},{4.08F,8.42F},{4.28F,9.28F},{4.50F,8.52F},{5.04F,8.12F}};
    private RaidenCycloneTimeline() {}
    public static Pose sample(float seconds) { float t=MathHelper.clamp(seconds,0,DURATION_SECONDS); return new Pose(key(X,t)-.72F,key(Z,t)-.12F,key(YAW,t)); }
    public static float smooth(float v){float t=MathHelper.clamp(v,0,1);return t*t*(3-2*t);}
    public static float envelope(float t,float start,float peak,float end){if(t<=start||t>=end)return 0;if(t<peak)return smooth((t-start)/Math.max(peak-start,1E-4F));return 1-smooth((t-peak)/Math.max(end-peak,1E-4F));}
    public static float gaussian(float t,float center,float width){float x=(t-center)/Math.max(width,1E-4F);return (float)Math.exp(-x*x*.5F);}
    private static float key(float[][] keys,float t){if(t<=keys[0][0])return keys[0][1];for(int i=1;i<keys.length;i++)if(t<=keys[i][0]){float u=(t-keys[i-1][0])/(keys[i][0]-keys[i-1][0]);return keys[i-1][1]+(keys[i][1]-keys[i-1][1])*smooth(u);}return keys[keys.length-1][1];}
    public static final class Pose { public final float x,z,yaw; Pose(float x,float z,float yaw){this.x=x;this.z=z;this.yaw=yaw;} }
    public static final class SlashSpec {
        public final float start,write,eraseLead,eraseEnd,life,maxSpan,angle,direction,arc,radius,width,y,lift,rotateX,rotateZ,centerX,centerZ;public final boolean dark;
        SlashSpec(float start,float write,float eraseLead,float eraseEnd,float life,float maxSpan,float angle,float direction,float arc,float radius,float width,float y,float lift,float rotateX,float rotateZ,float centerX,float centerZ,boolean dark){this.start=start;this.write=write;this.eraseLead=eraseLead;this.eraseEnd=eraseEnd;this.life=life;this.maxSpan=maxSpan;this.angle=angle;this.direction=direction;this.arc=arc;this.radius=radius;this.width=width;this.y=y;this.lift=lift;this.rotateX=rotateX;this.rotateZ=rotateZ;this.centerX=centerX;this.centerZ=centerZ;this.dark=dark;}
    }
}

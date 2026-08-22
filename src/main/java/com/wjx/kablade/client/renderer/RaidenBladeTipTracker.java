package com.wjx.kablade.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

/** 从 SlashBlade 已应用姿态的固定管线矩阵中提取重磁暴实际刀尖。 */
public final class RaidenBladeTipTracker {
    private static final float X=-280.934692F,Y=23.079803F,Z=.400531F;private static final ThreadLocal<EntityLivingBase> OWNER=new ThreadLocal<EntityLivingBase>();private static final FloatBuffer MATRIX=BufferUtils.createFloatBuffer(16);
    private RaidenBladeTipTracker(){}
    public static void begin(EntityLivingBase owner){if(RaidenCycloneRenderer.isActive(owner.getEntityId()))OWNER.set(owner);}
    public static void end(){OWNER.remove();}
    public static void capture(String part){EntityLivingBase owner=OWNER.get();if(owner==null||part==null||!(part.equals("blade")||part.equals("blade_luminous")||part.equals("blade_damaged")))return;MATRIX.clear();GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX,MATRIX);float[] m=new float[16];MATRIX.get(m);double ex=m[0]*X+m[4]*Y+m[8]*Z+m[12],ey=m[1]*X+m[5]*Y+m[9]*Z+m[13],ez=m[2]*X+m[6]*Y+m[10]*Z+m[14];Minecraft mc=Minecraft.getMinecraft();if(mc.getRenderViewEntity()==null)return;float pitch=(float)Math.toRadians(-mc.getRenderViewEntity().rotationPitch),yaw=(float)Math.toRadians(-mc.getRenderViewEntity().rotationYaw);Vec3d world=new Vec3d(ex,ey,ez).rotatePitch(pitch).rotateYaw(yaw).add(mc.getRenderManager().viewerPosX,mc.getRenderManager().viewerPosY,mc.getRenderManager().viewerPosZ);RaidenCycloneRenderer.recordBladeTip(owner.getEntityId(),world);}
}

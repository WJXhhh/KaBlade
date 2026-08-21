package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.EntityConceptualField;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;

/** 澄凝系 SA：基础剑阵始终使用剑体材质，念相部件作为独立第二 pass 叠加。 */
@SideOnly(Side.CLIENT)
public class RenderConceptualField extends Render<EntityConceptualField> {
    public RenderConceptualField(RenderManager manager){super(manager);shadowSize=0;shadowOpaque=0;}
    @Nullable @Override protected ResourceLocation getEntityTexture(EntityConceptualField entity){return null;}

    @Override public void doRender(EntityConceptualField entity,double x,double y,double z,float yaw,float partial){
        if(LimpidityPostPipeline.enqueue(entity,partial)){super.doRender(entity,x,y,z,yaw,partial);return;}
        float age=MathHelper.clamp(entity.ticksExisted+partial,0,EntityConceptualField.LIFETIME);
        drawPass(entity,x,y,z,age,LimpidityGeometry.Pass.BASE_COLOR,LimpidityShader.Material.SWORD);
        if(entity.getMode()==EntityConceptualField.MODE_UNITY)
            drawPass(entity,x,y,z,age,LimpidityGeometry.Pass.UNITY_COLOR,LimpidityShader.Material.CONCEPTUAL);
        super.doRender(entity,x,y,z,yaw,partial);
    }

    static void drawPass(EntityConceptualField entity,double x,double y,double z,float age,
                         LimpidityGeometry.Pass pass,LimpidityShader.Material material){
        float oldX=OpenGlHelper.lastBrightnessX,oldY=OpenGlHelper.lastBrightnessY;boolean pushed=false;
        int previous=LimpidityShader.bind(material,age);
        try{
            GlStateManager.disableLighting();GlStateManager.disableCull();GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();GlStateManager.depthMask(false);
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE,GlStateManager.SourceFactor.ONE,GlStateManager.DestFactor.ZERO);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,240,240);
            GlStateManager.pushMatrix();pushed=true;GlStateManager.translate(x,y+.025D,z);
            // 1.20 原实现是绕 Y 轴旋转 -yaw：局部 +Z 即施术者正前方。
            // 这里若额外加 180°，所有以正 Z authored 的星芒、斩弧和收束中心都会翻到身后。
            GlStateManager.rotate(-entity.rotationYaw,0,1,0);
            BufferBuilder b=Tessellator.getInstance().getBuffer();b.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_TEX_COLOR);
            RenderManager manager=Minecraft.getMinecraft().getRenderManager();
            // 1.12 正面第三人称只给 playerViewY 加 180°，不会像新版 Camera
            // 那样同时翻转 pitch；billboard 需要在这里补齐这项语义。
            float viewPitch=manager.options!=null&&manager.options.thirdPersonView==2
                    ?-manager.playerViewX:manager.playerViewX;
            LimpidityGeometry.draw(b,age,entity.getEntityId(),manager.playerViewY,viewPitch,entity.rotationYaw,pass);
            Tessellator.getInstance().draw();
        }finally{
            if(pushed)GlStateManager.popMatrix();LimpidityShader.restore(previous);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,oldX,oldY);
            GlStateManager.enableTexture2D();GlStateManager.depthMask(true);GlStateManager.disableBlend();
            GlStateManager.enableCull();GlStateManager.enableLighting();
        }
    }
}

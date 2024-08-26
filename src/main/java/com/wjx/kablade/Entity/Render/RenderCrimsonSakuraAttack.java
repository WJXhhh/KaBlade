package com.wjx.kablade.Entity.Render;

import com.google.common.collect.Lists;
import com.wjx.kablade.Entity.EntityCrimsonSakuraAttack;
import com.wjx.kablade.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.ArrayList;

import static com.wjx.kablade.Main.logger;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_LIGHTING;

public class RenderCrimsonSakuraAttack extends Render<Entity> {
    static ResourceLocation TEXTURE1 = new ResourceLocation(Main.MODID + ":textures/entity/crimson_sakura/crimson_sakura_edge.png");





    static ArrayList<ResourceLocation> EffectTEXS = Lists.newArrayList();
    public static ArrayList<Vector3d> inVertex = Lists.newArrayList();

    public static double ferh = 1d/59d;

    public void drawPoint(int i,BufferBuilder bufferBuilder,double thickness){

        if(bufferBuilder==null)
            logger.error("bn");
        int xxx = i/2;
        Vector3d vecx = inVertex.get(xxx);

        if(vecx==null)
            logger.error("ven");

        int yy = i/2;

        double ry;

        if(yy>32){
            ry=0d+(2d/26)*(yy-32);
        }else {
            ry=0;
        }

        if(i%2==0){
            int xx = i/2;
            Vector3d vec = inVertex.get(xx);
            bufferBuilder.pos(vec.x * 1.75,ry,vec.z * 1.75).tex(ferh*xx,0).endVertex();

        }else{
            int xx = i/2;
            Vector3d vec = inVertex.get(xx);
            bufferBuilder.pos(vec.x * thickness,ry,vec.z * thickness).tex(ferh*xx,1).endVertex();
        }
    }

    static{



        double inR = 720d;
        for(;inR>=0;inR-=12d){
            double th = Math.toRadians(inR);
            double tx = Math.cos(th) * 1;
            double ty = Math.sin(th) * 1;
            Vector3d v = new Vector3d();
            v.x = tx;
            v.z = ty;
            inVertex.add(v);
        }


    }

    public RenderCrimsonSakuraAttack(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0f;
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return null;
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
        Tessellator tessellator = Tessellator.getInstance();
        EntityCrimsonSakuraAttack eee =(EntityCrimsonSakuraAttack)entity;
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        GL11.glDisable(GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL_BLEND);//开启混合
        Minecraft mc = Minecraft.getMinecraft();

        float lastx = OpenGlHelper.lastBrightnessX;
        float lasty = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);




        //RenderEdge
        GlStateManager.pushMatrix();
        GlStateManager.color(1f,1f,1f,((EntityCrimsonSakuraAttack) entity).alpha);
        GlStateManager.translate(x,y + 0.75,z);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.rotate(-entity.rotationYaw + ((EntityCrimsonSakuraAttack) entity).ang,0f,1f,0f);
        bufferBuilder.begin(4, DefaultVertexFormats.POSITION_TEX);
        this.bindTexture(TEXTURE1);


        double tIn = ((EntityCrimsonSakuraAttack) entity).thickness;
            if(((EntityCrimsonSakuraAttack) entity).tC > 30){
                tIn = ((EntityCrimsonSakuraAttack) entity).thick2;
            }
            for (int i = 117; i >= ((EntityCrimsonSakuraAttack) entity).progress; i--) {

                drawPoint(i, bufferBuilder,tIn);
                drawPoint(i + 1, bufferBuilder,tIn);
                drawPoint(i + 2, bufferBuilder,tIn);

            }

        tessellator.draw();

        GlStateManager.popMatrix();
        /*GlStateManager.pushMatrix();
        GlStateManager.color(1f,1f,1f,((EntityCrimsonSakuraAttack) entity).alpha);
        GlStateManager.translate(x,y + 0.75,z);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.rotate(15f,0f,0f,1f);
        GlStateManager.rotate(-entity.rotationYaw + ((EntityCrimsonSakuraAttack) entity).ang,0f,1f,0f);

        bufferBuilder.begin(4, DefaultVertexFormats.POSITION_TEX);
        this.bindTexture(TEXTURE1);


        double tIn1 = ((EntityCrimsonSakuraAttack) entity).thickness;
        if(((EntityCrimsonSakuraAttack) entity).tC > 30){
            tIn = ((EntityCrimsonSakuraAttack) entity).thick2;
        }
        for (int i = 117; i >= ((EntityCrimsonSakuraAttack) entity).progress; i--) {

            drawPoint(i, bufferBuilder,tIn);
            drawPoint(i + 1, bufferBuilder,tIn1);
            drawPoint(i + 2, bufferBuilder,tIn1);

        }

        tessellator.draw();

        GlStateManager.popMatrix();*/
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastx, lasty);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glEnable(GL_LIGHTING);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL_BLEND);//开启混合
    }
}

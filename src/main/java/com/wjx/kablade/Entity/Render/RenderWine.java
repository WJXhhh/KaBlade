package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.EntityWine;
import com.wjx.kablade.Entity.model.mdlRaikiriBlade;
import com.wjx.kablade.Main;
import com.wjx.kablade.util.KaBladeProperties;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLightningBolt;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

@SideOnly(Side.CLIENT)
public class RenderWine extends Render<EntityWine> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(Main.MODID + ":textures/effect/tex_raikiri_blade.png");

    public RenderWine(RenderManager renderManagerIn) {
        super(renderManagerIn);
        this.shadowSize = 0.0F;
    }


    @Override
    public void doRender(EntityWine entity, double x, double y, double z, float entityYaw, float partialTicks) {
        double longNess;

        longNess = entity.longNess;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        this.bindEntityTexture(entity);
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x,y,z);
        GlStateManager.rotate(-entity.rotationYaw,0,1,0);
        GlStateManager.rotate(entity.rotationPitch,1,0,0);

        GlStateManager.color(0.2f,0.7f,0.2f);
        bufferbuilder.begin(6, DefaultVertexFormats.POSITION_TEX);//1两端点划线；2按顺序首尾连接划线；3按顺序连接划线但首尾不相连；4每三个端点画出一个三角形；5反面为正面的镜像反转；6绘制矩形
        //front
        bufferbuilder.pos(-0.15,0.3,0).tex(0,4).endVertex();
        bufferbuilder.pos(0.15,0.3,0).tex(4,4).endVertex();
        bufferbuilder.pos(0.15,0,0).tex(4,0).endVertex();
        bufferbuilder.pos(-0.15,0,0).tex(0,0).endVertex();
        tessellator.draw();
        //right;view Left

        bufferbuilder.begin(6, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(0.15,0.3,0).tex(0,4).endVertex();
        bufferbuilder.pos(0.15,0.3,longNess).tex(4,4).endVertex();
        bufferbuilder.pos(0.15,0,longNess).tex(4,0).endVertex();
        bufferbuilder.pos(0.15,0,0).tex(0,0).endVertex();
        tessellator.draw();

        //left;view Right
        GlStateManager.translate(-0.3,0,0);
        GlStateManager.rotate(180,0,0,1);
        GlStateManager.translate(0,-0.3,0);
        bufferbuilder.begin(6, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(-0.15,0.3,0).tex(0,4).endVertex();
        bufferbuilder.pos(-0.15,0.3,longNess).tex(4,4).endVertex();
        bufferbuilder.pos(-0.15,0,longNess).tex(4,0).endVertex();
        bufferbuilder.pos(-0.15,0,0).tex(0,0).endVertex();
        tessellator.draw();
        GlStateManager.translate(0,0.3,0);
        GlStateManager.rotate(180,0,0,1);
        GlStateManager.translate(0.3,0,0);
        //top
        bufferbuilder.begin(6, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(-0.15,0.3,0).tex(0,4).endVertex();
        bufferbuilder.pos(-0.15,0.3,longNess).tex(4,4).endVertex();
        bufferbuilder.pos(0.15,0.3,longNess).tex(4,0).endVertex();
        bufferbuilder.pos(0.15,0.3,0).tex(0,0).endVertex();
        tessellator.draw();
        //bottom
        GlStateManager.rotate(180,0,0,1);
        bufferbuilder.begin(6, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(-0.15,0,0).tex(0,4).endVertex();
        bufferbuilder.pos(-0.15,0,longNess).tex(4,4).endVertex();
        bufferbuilder.pos(0.15,0,longNess).tex(4,0).endVertex();
        bufferbuilder.pos(0.15,0,0).tex(0,0).endVertex();
        tessellator.draw();
        GlStateManager.rotate(180,0,0,1);
        //back
        GlStateManager.translate(0,0,longNess);
        GlStateManager.rotate(180,0,1,0);
        bufferbuilder.begin(6, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(-0.15,0.3,0).tex(0,4).endVertex();
        bufferbuilder.pos(0.15,0.3,0).tex(4,4).endVertex();
        bufferbuilder.pos(0.15,0,0).tex(4,0).endVertex();
        bufferbuilder.pos(-0.15,0,0).tex(0,0).endVertex();
        tessellator.draw();
        GlStateManager.rotate(180,0,1,0);
        GlStateManager.translate(0,0,-longNess);
        GlStateManager.popMatrix();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityWine entity) {
        return TEXTURE;
    }
}

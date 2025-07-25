package com.wjx.kablade.ExSA.entity.render;

import com.wjx.kablade.ExSA.entity.EntityPhantomSwordEx;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_LIGHTING;
import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_DST_COLOR;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_ZERO;

public class RenderPhantomSwordEx extends Render<Entity> {
    private static double[][] dVec = new double[][]{{0.0D, 0.0D, 417.7431D}, {0.0D, -44.6113D, 0.0D}, {38.9907D, 0.0D, 50.0D}, {0.0D, 44.6113D, 0.0D}, {-38.9907D, 0.0D, 50.0D}, {38.9907D, 0.0D, -50.0D}, {-38.9907D, 0.0D, -50.0D}, {0.0D, 0.0D, -214.0305D}, {159.1439D, 0.0D, -49.6611D}, {-159.1439D, 0.0D, -49.6611D}};
    private static int[][] nVecPos = new int[][]{{0, 2, 1}, {0, 3, 2}, {0, 4, 3}, {0, 1, 4}, {1, 5, 7}, {5, 3, 7}, {3, 6, 7}, {6, 1, 7}, {2, 8, 1}, {5, 8, 3}, {4, 9, 3}, {6, 9, 1}, {1, 8, 5}, {1, 9, 4}, {3, 8, 2}, {3, 9, 6}};

    public RenderPhantomSwordEx(RenderManager renderManager) {
        super(renderManager);
    }

    public void doRender(Entity entity, double d0, double d1, double d2, float f, float f1) {
        if (entity instanceof EntityPhantomSwordEx) {
            this.doDriveRender((EntityPhantomSwordEx)entity, d0, d1, d2, f, f1);
        }

    }

    protected ResourceLocation getEntityTexture(Entity var1) {
        return null;
    }

    private void doDriveRender(EntityPhantomSwordEx entityPhantomSword, double dX, double dY, double dZ, float f, float f1) {

        Tessellator tessellator = Tessellator.getInstance();
        GL11.glDisable(GL_TEXTURE_2D);
        GL11.glDisable(GL_LIGHTING);
        GL11.glEnable(GL_BLEND);//开启混合
        int color = entityPhantomSword.getColor();
        boolean inverse = color < 0;
        color = Math.abs(color);
        if (!inverse) {
            GL11.glBlendFunc(GL_SRC_ALPHA, GL_ONE);//混合 使用源颜色的alpha值来作为因子;使用1.0作为因子;新颜色：RsSr+RdDr, GsSg+GdDg, BsSb+BdDb, AsSa+AdDa
        } else {
            GL11.glBlendFunc(GL_ONE_MINUS_DST_COLOR, GL_ZERO);
        }

        GL11.glPushMatrix();
        GL11.glTranslatef((float)dX, (float)dY + 0.5F, (float)dZ);
        GL11.glRotatef(this.lerpDegrees(entityPhantomSword.prevRotationYaw, entityPhantomSword.rotationYaw, f1), 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-this.lerpDegrees(entityPhantomSword.prevRotationPitch, entityPhantomSword.rotationPitch, f1), 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(entityPhantomSword.getRoll(), 0.0F, 0.0F, 1.0F);
        float scale = 0.0045F;
        GL11.glScalef(scale, scale, scale);
        GL11.glScalef(0.5F, 0.5F, 1.0F);
        float lifetime = (float)entityPhantomSword.getLifeTime();
        float ticks = (float)entityPhantomSword.ticksExisted;
        BufferBuilder wr = tessellator.getBuffer();
        wr.begin(4, DefaultVertexFormats.POSITION_COLOR);
        int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;
        double dScale = 1.0D;

        for (int[] nVecPo : nVecPos) {
            wr.pos(dVec[nVecPo[0]][0] * dScale, dVec[nVecPo[0]][1] * dScale, dVec[nVecPo[0]][2] * dScale).color(r, g, b, 255).endVertex();
            wr.pos(dVec[nVecPo[1]][0] * dScale, dVec[nVecPo[1]][1] * dScale, dVec[nVecPo[1]][2] * dScale).color(r, g, b, 255).endVertex();
            wr.pos(dVec[nVecPo[2]][0] * dScale, dVec[nVecPo[2]][1] * dScale, dVec[nVecPo[2]][2] * dScale).color(r, g, b, 255).endVertex();
        }

        tessellator.draw();
        GL11.glPopMatrix();
        GL11.glDisable(GL_BLEND);
        GL11.glEnable(GL_LIGHTING);
        GL11.glEnable(GL_TEXTURE_2D);
    }

    float lerpDegrees(float start, float end, float percent) {
        float diff;
        for(diff = end - start; diff < -180.0F; diff += 360.0F) {
        }

        while(diff >= 180.0F) {
            diff -= 360.0F;
        }

        return start + percent * diff;
    }
}

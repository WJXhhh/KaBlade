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

public class RenderPhantomSwordEx extends Render {
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
        GL11.glDisable(3553);
        GL11.glDisable(2896);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 1);
        GL11.glPushMatrix();
        GL11.glTranslatef((float)dX, (float)dY, (float)dZ);
        GL11.glRotatef(entityPhantomSword.rotationYaw, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-entityPhantomSword.rotationPitch, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(entityPhantomSword.getRoll(), 0.0F, 0.0F, 1.0F);
        float scale = 0.0045F;
        GL11.glScalef(scale, scale, scale);
        GL11.glScalef(0.5F, 0.5F, 1.0F);
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(4, DefaultVertexFormats.POSITION_COLOR);
        int color = entityPhantomSword.getColor();
        int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;
        buffer.color(r,g,b,255);
        double dScale = 1.0D;

        for(int idx = 0; idx < nVecPos.length; ++idx) {
            buffer.pos(dVec[nVecPos[idx][0]][0] * dScale, dVec[nVecPos[idx][0]][1] * dScale, dVec[nVecPos[idx][0]][2] * dScale);
            buffer.pos(dVec[nVecPos[idx][1]][0] * dScale, dVec[nVecPos[idx][1]][1] * dScale, dVec[nVecPos[idx][1]][2] * dScale);
            buffer.pos(dVec[nVecPos[idx][2]][0] * dScale, dVec[nVecPos[idx][2]][1] * dScale, dVec[nVecPos[idx][2]][2] * dScale);
        }

        tessellator.draw();
        GL11.glPopMatrix();
        GL11.glDisable(3042);
        GL11.glEnable(2896);
        GL11.glEnable(3553);
    }
}

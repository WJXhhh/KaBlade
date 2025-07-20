package com.wjx.kablade.ExSA.entity.render;

import com.wjx.kablade.Entity.EntityDriveAdd;
import com.wjx.kablade.ExSA.entity.ExSaEntityDrive;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;

@SideOnly(Side.CLIENT)
public class RenderDriveEx extends Render<ExSaEntityDrive> {
    public RenderDriveEx(RenderManager renderManager) {
        super(renderManager);
    }
    private static double[][] dVec = new double[][]{{0.0D, 1.0D, -0.5D}, {0.0D, 0.75D, 0.0D}, {0.1D, 0.6D, -0.15D}, {0.0D, 0.5D, -0.25D}, {-0.1D, 0.6D, -0.15D}, {0.0D, 0.0D, 0.25D}, {0.25D, 0.0D, 0.0D}, {0.0D, 0.0D, -0.25D}, {-0.25D, 0.0D, 0.0D}, {0.0D, -0.75D, 0.0D}, {0.1D, -0.6D, -0.15D}, {0.0D, -0.5D, -0.25D}, {-0.1D, -0.6D, -0.15D}, {0.0D, -1.0D, -0.5D}};
    private static int[][] nVecPos = new int[][]{{0, 1, 2, 3}, {0, 3, 4, 1}, {1, 5, 6, 2}, {3, 2, 6, 7}, {3, 7, 8, 4}, {1, 4, 8, 5}, {6, 5, 9, 10}, {6, 10, 11, 7}, {8, 7, 11, 12}, {8, 12, 9, 5}, {10, 9, 13, 11}, {12, 11, 13, 9}};

    public void doRender(ExSaEntityDrive entity, double d0, double d1, double d2, float f, float f1) {
        if (entity instanceof ExSaEntityDrive) {
            this.doDriveRender((ExSaEntityDrive)entity, d0, d1, d2, f, f1);
        }

    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(ExSaEntityDrive entity) {
        return null;
    }


    private void doDriveRender(ExSaEntityDrive entityDrive, double dX, double dY, double dZ, float f, float f1) {
        Tessellator tessellator = Tessellator.getInstance();
        GL11.glDisable(3553);
        GL11.glDisable(2896);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 1);
        GL11.glPushMatrix();
        GL11.glTranslatef((float)dX, (float)dY, (float)dZ);
        GL11.glRotatef(entityDrive.rotationYaw, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-entityDrive.rotationPitch, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(entityDrive.getRoll(), 0.0F, 0.0F, 1.0F);
        GL11.glScalef(0.25F, 1.0F, 1.0F);
        float lifetime = (float)entityDrive.getLifeTime();
        float ticks = (float)entityDrive.ticksExisted;

        float r = entityDrive.getDataManager().get(EntityDriveAdd.COLOR_R);
        float g = entityDrive.getDataManager().get(EntityDriveAdd.COLOR_G);
        float b = entityDrive.getDataManager().get(EntityDriveAdd.COLOR_B);

        BufferBuilder wr = tessellator.getBuffer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        float alpha = (float)Math.pow((double)((lifetime - Math.min(lifetime, ticks)) / lifetime), 2.0);


        double dScale = 1.0;

        for(int idx = 0; idx < nVecPos.length; ++idx) {
            wr.pos(dVec[nVecPos[idx][0]][0] * dScale, dVec[nVecPos[idx][0]][1] * dScale, dVec[nVecPos[idx][0]][2] * dScale).color(r, g, b, alpha).endVertex();
            wr.pos(dVec[nVecPos[idx][1]][0] * dScale, dVec[nVecPos[idx][1]][1] * dScale, dVec[nVecPos[idx][1]][2] * dScale).color(r, g, b, alpha).endVertex();
            wr.pos(dVec[nVecPos[idx][2]][0] * dScale, dVec[nVecPos[idx][2]][1] * dScale, dVec[nVecPos[idx][2]][2] * dScale).color(r, g, b, alpha).endVertex();
            wr.pos(dVec[nVecPos[idx][3]][0] * dScale, dVec[nVecPos[idx][3]][1] * dScale, dVec[nVecPos[idx][3]][2] * dScale).color(r, g, b, alpha).endVertex();
        }

        tessellator.draw();
        GL11.glPopMatrix();
        GL11.glDisable(3042);
        GL11.glEnable(2896);
        GL11.glEnable(3553);
    }
}

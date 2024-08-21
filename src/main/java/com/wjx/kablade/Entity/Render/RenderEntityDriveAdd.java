package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.EntityDriveAdd;
import com.wjx.kablade.util.Vec3f;
import mods.flammpfeil.slashblade.util.ResourceLocationRaw;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;


public class RenderEntityDriveAdd extends Render<EntityDriveAdd> {
    private static double[][] dVec = new double[][]{{0.0, 1.0, -0.5}, {0.0, 0.75, 0.0}, {0.1, 0.6, -0.15}, {0.0, 0.5, -0.25}, {-0.1, 0.6, -0.15}, {0.0, 0.0, 0.25}, {0.25, 0.0, 0.0}, {0.0, 0.0, -0.25}, {-0.25, 0.0, 0.0}, {0.0, -0.75, 0.0}, {0.1, -0.6, -0.15}, {0.0, -0.5, -0.25}, {-0.1, -0.6, -0.15}, {0.0, -1.0, -0.5}};
    private static int[][] nVecPos = new int[][]{{0, 1, 2, 3}, {0, 3, 4, 1}, {1, 5, 6, 2}, {3, 2, 6, 7}, {3, 7, 8, 4}, {1, 4, 8, 5}, {6, 5, 9, 10}, {6, 10, 11, 7}, {8, 7, 11, 12}, {8, 12, 9, 5}, {10, 9, 13, 11}, {12, 11, 13, 9}};

    public RenderEntityDriveAdd(RenderManager renderManager) {
        super(renderManager);
    }

    public void doRender(EntityDriveAdd entity, double d0, double d1, double d2, float f, float f1) {
        if (entity instanceof EntityDriveAdd) {
            this.doDriveRender((EntityDriveAdd)entity, d0, d1, d2, f, f1);
        }

    }

    protected ResourceLocationRaw getEntityTexture(EntityDriveAdd var1) {
        return null;
    }

    private void doDriveRender(EntityDriveAdd entityDrive, double dX, double dY, double dZ, float f, float f1) {
        Tessellator tessellator = Tessellator.getInstance();
        GL11.glDisable(3553);
        GL11.glDisable(2896);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 1);
        GL11.glPushMatrix();
        GL11.glTranslatef((float)dX, (float)dY + 0.5F, (float)dZ);
        GL11.glRotatef(entityDrive.rotationYaw, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-entityDrive.rotationPitch, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(entityDrive.getRoll(), 0.0F, 0.0F, 1.0F);
        GL11.glScalef(entityDrive.getDataManager().get(EntityDriveAdd.SCALE_X),entityDrive.getDataManager().get(EntityDriveAdd.SCALE_Y),entityDrive.getDataManager().get(EntityDriveAdd.SCALE_Z));
        //GL11.glScalef(0.25F, 1.0F, 1.0F);




        float r = entityDrive.getDataManager().get(EntityDriveAdd.COLOR_R);
        float g = entityDrive.getDataManager().get(EntityDriveAdd.COLOR_G);
        float b = entityDrive.getDataManager().get(EntityDriveAdd.COLOR_B);



        float lifetime = (float)entityDrive.getLifeTime();
        float ticks = (float)entityDrive.ticksExisted;
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

package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.EntitySummonedButterfly;
import mods.flammpfeil.slashblade.util.ResourceLocationRaw;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

public class RenderSummonedButterFly extends Render<EntitySummonedButterfly> {
    private static double[][] dVec = new double[][]{{1.05, 5.03, 22.63}, {2.36, 3.05, 8.4}, {3.83, 1.2, -5.34}, {5.4, -0.47, -18.09}, {7.13, -1.85, -29.36}, {9.07, -2.85, -38.66}, {12.52, -3.37, -47.44}, {21.05, -1.84, -51.88}, {30.57, 0.87, -50.69}, {39.45, 3.14, -51.22}, {42.36, 4.8, -45.76}, {47.93, 7.38, -39.0}, {55.35, 10.33, -32.93}, {59.25, 12.66, -24.99}, {59.61, 14.03, -17.16}, {58.07, 14.95, -9.08}, {55.23, 15.26, -2.54}, {51.3, 15.08, 2.69}, {46.49, 14.49, 6.9}, {41.03, 13.61, 10.33}, {35.13, 12.52, 13.24}, {29.02, 11.33, 15.89}, {35.81, 13.07, 15.54}, {45.37, 15.58, 15.38}, {55.78, 18.52, 16.53}, {61.65, 20.7, 20.35}, {63.76, 22.38, 27.2}, {66.06, 24.43, 36.03}, {68.45, 26.8, 46.65}, {70.12, 29.0, 57.46}, {70.21, 30.56, 66.88}, {65.14, 30.42, 74.24}, {54.81, 27.67, 74.13}, {46.95, 25.3, 72.35}, {38.75, 22.58, 68.98}, {30.12, 19.35, 63.18}, {21.03, 15.45, 54.08}, {11.39, 10.74, 40.85}, {-37.42, 3.13, -51.46}, {-28.53, 0.86, -50.94}, {-22.07, -1.09, -52.38}, {-15.97, -2.61, -51.86}, {-10.48, -3.39, -47.68}, {-7.03, -2.87, -38.9}, {-40.32, 4.79, -46.01}, {-5.09, -1.86, -29.6}, {-3.36, -0.48, -18.33}, {-45.89, 7.37, -39.24}, {-53.31, 10.32, -33.17}, {-57.22, 12.65, -25.24}, {-57.57, 14.02, -17.41}, {-56.03, 14.94, -9.32}, {-53.19, 15.25, -2.79}, {-49.26, 15.07, 2.45}, {-44.45, 14.48, 6.66}, {-38.99, 13.59, 10.08}, {-33.1, 12.51, 12.99}, {-1.79, 1.19, -5.58}, {-26.98, 11.32, 15.64}, {-0.32, 3.04, 8.16}, {-9.36, 10.73, 40.61}, {-33.77, 13.06, 15.29}, {-43.33, 15.57, 15.14}, {-53.75, 18.51, 16.29}, {-59.61, 20.69, 20.1}, {-61.72, 22.36, 26.95}, {-18.99, 15.44, 53.84}, {-64.02, 24.42, 35.79}, {-28.09, 19.34, 62.93}, {-66.42, 26.79, 46.41}, {-36.71, 22.57, 68.74}, {-44.92, 25.29, 72.11}, {-68.08, 28.99, 57.22}, {-52.77, 27.66, 73.88}, {-68.18, 30.55, 66.63}, {-63.1, 30.41, 74.0}, {0.98, 5.02, 22.39}};
    private static int[][] nVecPos = new int[][]{{8, 9, 10}, {7, 8, 10}, {6, 7, 10}, {10, 5, 6}, {11, 4, 5}, {11, 5, 10}, {11, 3, 4}, {12, 3, 11}, {14, 3, 12}, {14, 12, 13}, {16, 3, 14}, {16, 14, 15}, {18, 3, 16}, {18, 16, 17}, {20, 3, 18}, {20, 18, 19}, {21, 2, 3}, {21, 3, 20}, {0, 1, 2}, {0, 2, 21}, {22, 37, 0}, {22, 0, 21}, {23, 37, 22}, {37, 23, 24}, {37, 24, 25}, {36, 37, 25}, {36, 25, 26}, {35, 36, 26}, {35, 26, 27}, {34, 35, 27}, {34, 27, 28}, {29, 33, 34}, {29, 34, 28}, {30, 32, 33}, {30, 33, 29}, {31, 32, 30}, {44, 38, 39}, {39, 40, 44}, {44, 40, 41}, {41, 42, 44}, {44, 42, 43}, {44, 43, 45}, {44, 45, 47}, {45, 46, 47}, {48, 47, 46}, {48, 46, 50}, {48, 50, 49}, {51, 50, 46}, {51, 46, 52}, {52, 46, 54}, {52, 54, 53}, {55, 54, 46}, {55, 46, 56}, {46, 57, 58}, {46, 58, 56}, {57, 59, 76}, {57, 76, 58}, {58, 76, 60}, {58, 60, 61}, {62, 61, 60}, {63, 62, 60}, {64, 63, 60}, {65, 64, 60}, {65, 60, 66}, {67, 65, 66}, {67, 66, 68}, {69, 67, 68}, {69, 68, 70}, {69, 70, 71}, {69, 71, 72}, {72, 71, 73}, {72, 73, 74}, {73, 75, 74}, {10, 9, 8}, {8, 7, 10}, {7, 6, 10}, {10, 6, 5}, {5, 4, 11}, {5, 11, 10}, {4, 3, 11}, {11, 3, 12}, {13, 12, 3}, {13, 3, 14}, {14, 3, 16}, {14, 16, 15}, {16, 3, 18}, {16, 18, 17}, {19, 18, 3}, {19, 3, 20}, {3, 2, 21}, {3, 21, 20}, {2, 1, 0}, {2, 0, 21}, {21, 0, 37}, {21, 37, 22}, {23, 22, 37}, {24, 23, 37}, {25, 24, 37}, {25, 37, 36}, {25, 36, 26}, {27, 26, 36}, {27, 36, 35}, {28, 27, 35}, {28, 35, 34}, {28, 34, 33}, {28, 33, 29}, {33, 32, 30}, {33, 30, 29}, {30, 32, 31}, {39, 38, 44}, {40, 39, 44}, {41, 40, 44}, {42, 41, 44}, {43, 42, 44}, {47, 45, 43}, {47, 43, 44}, {47, 46, 45}, {48, 46, 47}, {50, 46, 48}, {50, 48, 49}, {46, 50, 51}, {46, 51, 52}, {54, 46, 52}, {54, 52, 53}, {56, 46, 54}, {56, 54, 55}, {58, 57, 46}, {58, 46, 56}, {76, 59, 57}, {76, 57, 58}, {60, 76, 58}, {60, 58, 61}, {60, 61, 62}, {63, 60, 62}, {64, 60, 63}, {60, 64, 65}, {60, 65, 66}, {68, 66, 65}, {68, 65, 67}, {70, 68, 67}, {70, 67, 69}, {72, 71, 70}, {72, 70, 69}, {73, 71, 72}, {73, 72, 74}, {75, 73, 74}};
    public RenderSummonedButterFly(RenderManager renderManager) {
        super(renderManager);
    }

    public void doRender(@Nonnull EntitySummonedButterfly entity, double d0, double d1, double d2, float f, float f1) {
        if (entity instanceof EntitySummonedButterfly) {
            this.doDriveRender(entity, d0, d1, d2, f1);
        }
    }

    protected ResourceLocationRaw getEntityTexture(EntitySummonedButterfly var1) {
        return null;
    }

    private void doDriveRender(EntitySummonedButterfly entityPhantomSword, double dX, double dY, double dZ, float f1) {
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

    float lerp(float start, float end, float percent) {
        return start + percent * (end - start);
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

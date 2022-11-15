package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.RainBow;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;

import java.util.Random;

public class RenderRainbow{ //extends Render {
    /*public void doRender(Entity entity, double x, double y, double z, float f, float f1) {
        Tessellator tessellator = Tessellator.getInstance();
        GL11.glDisable(3553);
        GL11.glDisable(2896);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 1);
        double[] adouble = new double[8];
        double[] adouble1 = new double[8];
        double d3 = 0.0D;
        double d4 = 0.0D;
        Random random = new Random(((RainBow)entity).boltVertex);

        int j;
        for(j = 7; j >= 0; --j) {
            adouble[j] = d3;
            adouble1[j] = d4;
            d3 += (double)(random.nextInt(11) - 5);
            d4 += (double)(random.nextInt(11) - 5);
        }

        for(j = 0; j < 4; ++j) {
            Random random1 = new Random(((RainBow)entity).boltVertex);

            for(int k = 0; k < 3; ++k) {
                int l = 7;
                int i1 = 0;
                if (k > 0) {
                    l = 7 - k;
                }

                if (k > 0) {
                    i1 = l - 2;
                }

                double d5 = adouble[l] - d3;
                double d6 = adouble1[l] - d4;

                for(int j1 = l; j1 >= i1; --j1) {
                    double d7 = d5;
                    double d8 = d6;
                    if (k == 0) {
                        d5 += (double)(random1.nextInt(11) - 5);
                        d6 += (double)(random1.nextInt(11) - 5);
                    } else {
                        d5 += (double)(random1.nextInt(31) - 15);
                        d6 += (double)(random1.nextInt(31) - 15);
                    }

                    tessellator.(5);
                    tessellator.(random1.nextFloat(), random1.nextFloat(), random1.nextFloat(), random1.nextFloat());
                    double d9 = 0.1D + (double)j * 0.2D;
                    if (k == 0) {
                        d9 *= (double)j1 * 0.1D + 1.0D;
                    }

                    double d10 = 0.1D + (double)j * 0.2D;
                    if (k == 0) {
                        d10 *= (double)(j1 - 1) * 0.1D + 1.0D;
                    }

                    for(int k1 = 0; k1 < 5; ++k1) {
                        double d11 = x + 0.5D - d9;
                        double d12 = z + 0.5D - d9;
                        if (k1 == 1 || k1 == 2) {
                            d11 += d9 * 2.0D;
                        }

                        if (k1 == 2 || k1 == 3) {
                            d12 += d9 * 2.0D;
                        }

                        double d13 = x + 0.5D - d10;
                        double d14 = z + 0.5D - d10;
                        if (k1 == 1 || k1 == 2) {
                            d13 += d10 * 2.0D;
                        }

                        if (k1 == 2 || k1 == 3) {
                            d14 += d10 * 2.0D;
                        }

                        tessellator.func_78377_a(d13 + d5, y + (double)(j1 * 16), d14 + d6);
                        tessellator.func_78377_a(d11 + d7, y + (double)((j1 + 1) * 16), d12 + d8);
                    }

                    tessellator.func_78381_a();
                }
            }
        }

        GL11.glDisable(3042);
        GL11.glEnable(2896);
        GL11.glEnable(3553);
    }

    protected ResourceLocation func_110775_a(Entity entity) {
        return null;
    }*/
}

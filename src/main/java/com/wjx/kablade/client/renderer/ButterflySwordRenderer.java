package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wjx.kablade.entity.ButterflySwordEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** 1.12.2 RenderSummonedButterFly's hard-coded butterfly sword mesh. */
public final class ButterflySwordRenderer extends EntityRenderer<ButterflySwordEntity> {

    private static final ResourceLocation EMPTY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("kablade", "textures/entity/empty.png");

    private static final float[][] VERTICES = {
            {1.05F, 5.03F, 22.63F}, {2.36F, 3.05F, 8.4F}, {3.83F, 1.2F, -5.34F},
            {5.4F, -0.47F, -18.09F}, {7.13F, -1.85F, -29.36F}, {9.07F, -2.85F, -38.66F},
            {12.52F, -3.37F, -47.44F}, {21.05F, -1.84F, -51.88F}, {30.57F, 0.87F, -50.69F},
            {39.45F, 3.14F, -51.22F}, {42.36F, 4.8F, -45.76F}, {47.93F, 7.38F, -39.0F},
            {55.35F, 10.33F, -32.93F}, {59.25F, 12.66F, -24.99F}, {59.61F, 14.03F, -17.16F},
            {58.07F, 14.95F, -9.08F}, {55.23F, 15.26F, -2.54F}, {51.3F, 15.08F, 2.69F},
            {46.49F, 14.49F, 6.9F}, {41.03F, 13.61F, 10.33F}, {35.13F, 12.52F, 13.24F},
            {29.02F, 11.33F, 15.89F}, {35.81F, 13.07F, 15.54F}, {45.37F, 15.58F, 15.38F},
            {55.78F, 18.52F, 16.53F}, {61.65F, 20.7F, 20.35F}, {63.76F, 22.38F, 27.2F},
            {66.06F, 24.43F, 36.03F}, {68.45F, 26.8F, 46.65F}, {70.12F, 29.0F, 57.46F},
            {70.21F, 30.56F, 66.88F}, {65.14F, 30.42F, 74.24F}, {54.81F, 27.67F, 74.13F},
            {46.95F, 25.3F, 72.35F}, {38.75F, 22.58F, 68.98F}, {30.12F, 19.35F, 63.18F},
            {21.03F, 15.45F, 54.08F}, {11.39F, 10.74F, 40.85F}, {-37.42F, 3.13F, -51.46F},
            {-28.53F, 0.86F, -50.94F}, {-22.07F, -1.09F, -52.38F}, {-15.97F, -2.61F, -51.86F},
            {-10.48F, -3.39F, -47.68F}, {-7.03F, -2.87F, -38.9F}, {-40.32F, 4.79F, -46.01F},
            {-5.09F, -1.86F, -29.6F}, {-3.36F, -0.48F, -18.33F}, {-45.89F, 7.37F, -39.24F},
            {-53.31F, 10.32F, -33.17F}, {-57.22F, 12.65F, -25.24F}, {-57.57F, 14.02F, -17.41F},
            {-56.03F, 14.94F, -9.32F}, {-53.19F, 15.25F, -2.79F}, {-49.26F, 15.07F, 2.45F},
            {-44.45F, 14.48F, 6.66F}, {-38.99F, 13.59F, 10.08F}, {-33.1F, 12.51F, 12.99F},
            {-1.79F, 1.19F, -5.58F}, {-26.98F, 11.32F, 15.64F}, {-0.32F, 3.04F, 8.16F},
            {-9.36F, 10.73F, 40.61F}, {-33.77F, 13.06F, 15.29F}, {-43.33F, 15.57F, 15.14F},
            {-53.75F, 18.51F, 16.29F}, {-59.61F, 20.69F, 20.1F}, {-61.72F, 22.36F, 26.95F},
            {-18.99F, 15.44F, 53.84F}, {-64.02F, 24.42F, 35.79F}, {-28.09F, 19.34F, 62.93F},
            {-66.42F, 26.79F, 46.41F}, {-36.71F, 22.57F, 68.74F}, {-44.92F, 25.29F, 72.11F},
            {-68.08F, 28.99F, 57.22F}, {-52.77F, 27.66F, 73.88F}, {-68.18F, 30.55F, 66.63F},
            {-63.1F, 30.41F, 74.0F}, {0.98F, 5.02F, 22.39F}
    };

    private static final int[][] FACES = {
            {8, 9, 10}, {7, 8, 10}, {6, 7, 10}, {10, 5, 6}, {11, 4, 5}, {11, 5, 10},
            {11, 3, 4}, {12, 3, 11}, {14, 3, 12}, {14, 12, 13}, {16, 3, 14}, {16, 14, 15},
            {18, 3, 16}, {18, 16, 17}, {20, 3, 18}, {20, 18, 19}, {21, 2, 3}, {21, 3, 20},
            {0, 1, 2}, {0, 2, 21}, {22, 37, 0}, {22, 0, 21}, {23, 37, 22}, {37, 23, 24},
            {37, 24, 25}, {36, 37, 25}, {36, 25, 26}, {35, 36, 26}, {35, 26, 27},
            {34, 35, 27}, {34, 27, 28}, {29, 33, 34}, {29, 34, 28}, {30, 32, 33},
            {30, 33, 29}, {31, 32, 30}, {44, 38, 39}, {39, 40, 44}, {44, 40, 41},
            {41, 42, 44}, {44, 42, 43}, {44, 43, 45}, {44, 45, 47}, {45, 46, 47},
            {48, 47, 46}, {48, 46, 50}, {48, 50, 49}, {51, 50, 46}, {51, 46, 52},
            {52, 46, 54}, {52, 54, 53}, {55, 54, 46}, {55, 46, 56}, {46, 57, 58},
            {46, 58, 56}, {57, 59, 76}, {57, 76, 58}, {58, 76, 60}, {58, 60, 61},
            {62, 61, 60}, {63, 62, 60}, {64, 63, 60}, {65, 64, 60}, {65, 60, 66},
            {67, 65, 66}, {67, 66, 68}, {69, 67, 68}, {69, 68, 70}, {69, 70, 71},
            {69, 71, 72}, {72, 71, 73}, {72, 73, 74}, {73, 75, 74}, {10, 9, 8},
            {8, 7, 10}, {7, 6, 10}, {10, 6, 5}, {5, 4, 11}, {5, 11, 10}, {4, 3, 11},
            {11, 3, 12}, {13, 12, 3}, {13, 3, 14}, {14, 3, 16}, {14, 16, 15},
            {16, 3, 18}, {16, 18, 17}, {19, 18, 3}, {19, 3, 20}, {3, 2, 21},
            {3, 21, 20}, {2, 1, 0}, {2, 0, 21}, {21, 0, 37}, {21, 37, 22},
            {23, 22, 37}, {24, 23, 37}, {25, 24, 37}, {25, 37, 36}, {25, 36, 26},
            {27, 26, 36}, {27, 36, 35}, {28, 27, 35}, {28, 35, 34}, {28, 34, 33},
            {28, 33, 29}, {33, 32, 30}, {33, 30, 29}, {30, 32, 31}, {39, 38, 44},
            {40, 39, 44}, {41, 40, 44}, {42, 41, 44}, {43, 42, 44}, {47, 45, 43},
            {47, 43, 44}, {47, 46, 45}, {48, 46, 47}, {50, 46, 48}, {50, 48, 49},
            {46, 50, 51}, {46, 51, 52}, {54, 46, 52}, {54, 52, 53}, {56, 46, 54},
            {56, 54, 55}, {58, 57, 46}, {58, 46, 56}, {76, 59, 57}, {76, 57, 58},
            {60, 76, 58}, {60, 58, 61}, {60, 61, 62}, {63, 60, 62}, {64, 60, 63},
            {60, 64, 65}, {60, 65, 66}, {68, 66, 65}, {68, 65, 67}, {70, 68, 67},
            {70, 67, 69}, {72, 71, 70}, {72, 70, 69}, {73, 71, 72}, {73, 72, 74},
            {75, 73, 74}
    };

    public ButterflySwordRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ButterflySwordEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        int color = Math.abs(entity.getColor());
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.5D, 0.0D);
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getRoll()));
        poseStack.scale(0.0045F, 0.0045F, 0.0045F);
        poseStack.scale(0.5F, 0.5F, 1.0F);

        Matrix4f mat = poseStack.last().pose();
        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());
        for (int[] face : FACES) {
            vertex(vc, mat, VERTICES[face[0]], r, g, b);
            vertex(vc, mat, VERTICES[face[1]], r, g, b);
            vertex(vc, mat, VERTICES[face[2]], r, g, b);
            vertex(vc, mat, VERTICES[face[2]], r, g, b);
        }
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat, float[] p, float r, float g, float b) {
        vc.vertex(mat, p[0], p[1], p[2]).color(r, g, b, 1.0F).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(ButterflySwordEntity entity) {
        return EMPTY_TEXTURE;
    }
}

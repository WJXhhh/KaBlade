package com.wjx.kablade.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wjx.kablade.Main;
import com.wjx.kablade.entity.RaikiriShieldEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * 雷切护盾渲染器 —— 复刻 1.12.2 的模型渲染（mdlRaikiriBlade + RenderWorldLastEvent），
 * 改为标准实体渲染，效果完全一致：
 * <ul>
 *   <li>十字星芒面板（additive 全亮度青白色）</li>
 *   <li>绕 Y 轴缓慢自转</li>
 *   <li>护盾耐久越低越暗</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class RaikiriShieldRenderer extends EntityRenderer<RaikiriShieldEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/effect/tex_raikiri_blade.png");

    public RaikiriShieldRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RaikiriShieldEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float blood = entity.getShieldBlood();
        if (blood <= 0.0F) return;

        // 耐久比例
        float h = Math.min(1.0F, blood / 10.0F);
        float alpha = 0.3F + 0.35F * h;
        float bright = 0.3F + 0.7F * h;

        // 颜色：青白 (0, 1, 1) 方向
        float r = 0.0F;
        float g = 1.0F * bright;
        float b = 1.0F * bright;
        int a = (int) (alpha * 255);

        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        poseStack.pushPose();

        // 抬高到胸部高度（复刻 1.12.2 prepareScale 的 translate(0,-1.501,0) + 模型偏移）
        poseStack.translate(0.0, 0.85, 0.0);
        // 绕 Y 轴自转（复刻 angleManager.getAngle()）
        float angle = (entity.tickCount + partialTick) * 0.08F;
        poseStack.mulPose(com.mojang.math.Axis.YN.rotationDegrees(angle * 360.0F));
        // 复刻 prepareScale 的 scale(-1, -1, 1)
        poseStack.scale(-1.0F, -1.0F, 1.0F);

        Matrix4f mat = poseStack.last().pose();

        // 尺寸：相对单位，1.0 = 1 格
        float armLen = 0.70F * (0.4F + 0.6F * h);
        float armW = 0.10F;
        float cxLen = 0.28F;

        // ── 六个面板（4 条长臂 + 2 对角线），复刻 mdlRaikiriBlade 的 6 个 ModelBox ──
        // 每个面板 = 2 个三角形，一共 12 个面板 = 24 个三角形
        float[][] panels = {
                // { 方向X, 方向Z, 长度, 宽度, 偏移X, 偏移Z }
                { 1, 0, armLen, armW, 0, 0 },        // X+ 臂
                { -1, 0, armLen, armW, 0, 0 },       // X- 臂
                { 0, 1, armLen, armW, 0, 0 },        // Z+ 臂
                { 0, -1, armLen, armW, 0, 0 },       // Z- 臂
                { 0.707F, 0.707F, armLen * 0.65F, armW * 0.8F, 0, 0 },  // 对角线
                { -0.707F, 0.707F, armLen * 0.65F, armW * 0.8F, 0, 0 }, // 对角线
        };

        for (float[] p : panels) {
            float dx = p[0], dz = p[1], len = p[2], wid = p[3], ox = p[4], oz = p[5];
            float nx = -dz, nz = dx; // 法线方向（垂直）
            float hw = wid * 0.5F;

            // 四个顶点：末端两个、中心两个
            float ex = ox + dx * len;
            float ez = oz + dz * len;
            float cx = ox - dx * cxLen;
            float cz = oz - dz * cxLen;

            // 三角形 1: 末端两个顶点 + 中心右侧
            consumer.vertex(mat, ox + nx * hw, 0, oz + nz * hw).color(r, g, b, a).endVertex();  // 中心右
            consumer.vertex(mat, ex + nx * hw, 0, ez + nz * hw).color(r, g, b, a).endVertex();  // 末端右
            consumer.vertex(mat, ex - nx * hw, 0, ez - nz * hw).color(r, g, b, a).endVertex();  // 末端左

            // 三角形 2: 末端左 + 中心左侧 + 中心右
            consumer.vertex(mat, ex - nx * hw, 0, ez - nz * hw).color(r, g, b, a).endVertex();
            consumer.vertex(mat, ox - nx * hw, 0, oz - nz * hw).color(r, g, b, a).endVertex();
            consumer.vertex(mat, ox + nx * hw, 0, oz + nz * hw).color(r, g, b, a).endVertex();
        }

        // ── 四根交叉斜撑（复刻 cube_r1 / cube_r2 的旋转效果）──
        for (int k = 0; k < 4; k++) {
            float rot = (float) (Math.PI * 0.5 * k + Math.PI * 0.25);
            float dx = (float) Math.cos(rot) * armLen * 0.55F;
            float dz = (float) Math.sin(rot) * armLen * 0.55F;
            float nx2 = -dz, nz2 = dx;
            float hw2 = armW * 0.5F;

            consumer.vertex(mat, nx2 * hw2, 0, nz2 * hw2).color(r, g, b, a).endVertex();
            consumer.vertex(mat, dx + nx2 * hw2, 0, dz + nz2 * hw2).color(r, g, b, a).endVertex();
            consumer.vertex(mat, dx - nx2 * hw2, 0, dz - nz2 * hw2).color(r, g, b, a).endVertex();

            consumer.vertex(mat, dx - nx2 * hw2, 0, dz - nz2 * hw2).color(r, g, b, a).endVertex();
            consumer.vertex(mat, -nx2 * hw2, 0, -nz2 * hw2).color(r, g, b, a).endVertex();
            consumer.vertex(mat, nx2 * hw2, 0, nz2 * hw2).color(r, g, b, a).endVertex();
        }

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(RaikiriShieldEntity entity) {
        return TEXTURE;
    }
}

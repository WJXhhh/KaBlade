package com.wjx.kablade.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wjx.kablade.Main;
import com.wjx.kablade.entity.RaikiriShieldEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * 雷切护盾模型 —— 从 1.12.2 {@code mdlRaikiriBlade} 移植。
 * <p>
 * 十字星芒，6 条扁平臂 + 2 条旋转臂，使用 {@link RenderType#lightning()} 全亮度 additive 渲染。
 */
public class RaikiriShieldModel extends EntityModel<RaikiriShieldEntity> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "raikiri_shield"), "main");
    private final ModelPart bb_main;

    public RaikiriShieldModel(ModelPart root) {
        super(tex -> RenderType.lightning());
        this.bb_main = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // bb_main 旋转原点 (0, 24, 0) — 对应 1.12.2 setRotationPoint(0, 24, 0)
        PartDefinition bb_main = root.addOrReplaceChild("bb_main",
                CubeListBuilder.create()
                        // 六条扁平臂（2 像素厚）
                        .addBox("z_plus", -1, -10, 8, 2, 2, 15)       // Z+ 臂
                        .addBox("z_minus", -1, -10, -24, 2, 2, 15)     // Z- 臂
                        .addBox("z_cross_plus", -3, -10, 12, 6, 2, 2)  // Z+ 横档
                        .addBox("z_cross_minus", -3, -10, -15, 6, 2, 2) // Z- 横档
                        .addBox("x_plus", 12, -10, -3, 2, 2, 6)        // X+ 臂
                        .addBox("x_minus", -14, -10, -3, 2, 2, 6),     // X- 臂
                PartPose.offset(0, 24, 0));

        // cube_r1: (-2, 0, 0) 绕 Y 旋转 -90°
        bb_main.addOrReplaceChild("cube_r1",
                CubeListBuilder.create()
                        .addBox("r1", -1, -10, 6, 2, 2, 15),
                PartPose.offsetAndRotation(-2, 0, 0, 0, -1.5708F, 0));

        // cube_r2: (2, 0, 0) 绕 Y 旋转 -90°
        bb_main.addOrReplaceChild("cube_r2",
                CubeListBuilder.create()
                        .addBox("r2", -1, -10, -21, 2, 2, 15),
                PartPose.offsetAndRotation(2, 0, 0, 0, -1.5708F, 0));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(RaikiriShieldEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // 无动画
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer,
                               int packedLight, int packedOverlay, float r, float g, float b, float a) {
        this.bb_main.render(poseStack, buffer, 240, packedOverlay, r, g, b, a);
    }
}

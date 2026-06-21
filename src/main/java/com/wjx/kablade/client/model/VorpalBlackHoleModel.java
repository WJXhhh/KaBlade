package com.wjx.kablade.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wjx.kablade.Main;
import com.wjx.kablade.entity.VorpalBlackHoleEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * 反力场黑洞模型 —— 一比一复刻 1.12.2 的 {@code ModelVorpalBlackHole}（Blockbench 导出）。
 * <p>
 * 中心 5×5×5 立方 + 四周面板，贴图 32×32。几何坐标、UV、各面旋转角与 1.12.2 完全一致。
 */
public class VorpalBlackHoleModel extends EntityModel<VorpalBlackHoleEntity> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "vorpal_black_hole"), "main");

    private static final float DEG90 = 1.5708F;
    private static final float DEG180 = 3.1416F;

    private final ModelPart root;

    public VorpalBlackHoleModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition bbMain = root.addOrReplaceChild("bb_main", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.5125F, -11.0F, -2.5125F, 5.0F, 5.0F, 5.0F)
                        .texOffs(0, 20).addBox(-3.5125F, -11.0F, -2.5125F, 1.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        bbMain.addOrReplaceChild("cube_r1", CubeListBuilder.create()
                        .texOffs(0, 10).addBox(-6.0F, -3.0F, -2.0F, 1.0F, 5.0F, 5.0F)
                        .texOffs(12, 10).addBox(-12.0F, -3.0F, -2.0F, 1.0F, 5.0F, 5.0F),
                PartPose.offsetAndRotation(-0.5125F, 0.0F, -0.5125F, 0.0F, 0.0F, DEG90));

        bbMain.addOrReplaceChild("cube_r2", CubeListBuilder.create()
                        .texOffs(19, 5).addBox(-4.0F, -11.0F, -2.0F, 1.0F, 5.0F, 5.0F),
                PartPose.offsetAndRotation(-0.5125F, 0.0F, -0.5125F, 0.0F, DEG90, 0.0F));

        bbMain.addOrReplaceChild("cube_r3", CubeListBuilder.create()
                        .texOffs(19, 15).addBox(-4.0F, -11.0F, -3.0F, 1.0F, 5.0F, 5.0F),
                PartPose.offsetAndRotation(-0.5125F, 0.0F, -0.5125F, 0.0F, DEG180, 0.0F));

        bbMain.addOrReplaceChild("cube_r4", CubeListBuilder.create()
                        .texOffs(12, 20).addBox(-3.0F, -11.0F, -3.0F, 1.0F, 5.0F, 5.0F),
                PartPose.offsetAndRotation(-0.5125F, 0.0F, -0.5125F, 0.0F, -DEG90, 0.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(VorpalBlackHoleEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // 1.12.2 原版模型为静态，无骨骼动画。
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float r, float g, float b, float a) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }
}

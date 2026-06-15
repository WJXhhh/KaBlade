package com.wjx.kablade.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wjx.kablade.Main;
import com.wjx.kablade.entity.RockSpikeEntity;
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
 * 岩刺（石矛）模型 —— 纯代码手搓，不依赖任何 .json/.obj 美术文件。
 * <p>
 * 由「逐段收窄的矛杆 + 旋转 45° 的菱形矛尖 + 两根侧倒钩」拼成，整体像一根从地里顶出来的石矛。
 * 几何沿 <b>−Y</b> 向上生长（矛根在 y=0、矛尖在 y=−32），配合渲染器里的 {@code scale(-,-,+)} 翻转，
 * 在世界里就是正立、约 2 格高（×体型）的尖刺。1 模型单位 = 1/16 格。
 */
public class RockSpikeModel extends EntityModel<RockSpikeEntity> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "rock_spike"), "main");

    private final ModelPart root;

    public RockSpikeModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 矛杆：四段逐渐收窄的方块，从地面（y=0）往上（−y）堆。
        root.addOrReplaceChild("shaft", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -7.0F, -3.0F, 6.0F, 7.0F, 6.0F)
                        .texOffs(0, 8).addBox(-2.5F, -13.0F, -2.5F, 5.0F, 6.0F, 5.0F)
                        .texOffs(8, 0).addBox(-2.0F, -18.0F, -2.0F, 4.0F, 5.0F, 4.0F)
                        .texOffs(8, 8).addBox(-1.5F, -23.0F, -1.5F, 3.0F, 5.0F, 3.0F),
                PartPose.ZERO);

        // 矛尖：绕 Y 转 45°，菱形横截面 → 棱角分明的尖。
        root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.5F, -33.0F, -1.5F, 3.0F, 10.0F, 3.0F),
                PartPose.rotation(0.0F, (float) (Math.PI / 4.0), 0.0F));

        // 两根侧倒钩：左右各一、向上外翻，像戟一样多点层次。
        root.addOrReplaceChild("barb_r", CubeListBuilder.create()
                        .texOffs(8, 0).addBox(0.0F, -3.0F, -1.25F, 5.0F, 3.0F, 2.5F),
                PartPose.offsetAndRotation(1.5F, -15.0F, 0.0F,
                        0.0F, 0.0F, (float) Math.toRadians(-42.0)));
        root.addOrReplaceChild("barb_l", CubeListBuilder.create()
                        .texOffs(8, 0).addBox(-5.0F, -3.0F, -1.25F, 5.0F, 3.0F, 2.5F),
                PartPose.offsetAndRotation(-1.5F, -17.0F, 0.0F,
                        0.0F, 0.0F, (float) Math.toRadians(42.0)));

        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void setupAnim(RockSpikeEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // 出土 / 缩回动画全部在渲染器里用 PoseStack 完成，模型本身无骨骼动画。
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float r, float g, float b, float a) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }
}

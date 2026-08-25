package com.wjx.kablade.client.renderer;

import mods.flammpfeil.slashblade.client.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.ResourceLocationRaw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

/** 用 1.20 bladeholder 的 hardpointA VMD 轨道替换 1.12 固定持刀层。 */
public final class AnimatedGreatswordRenderer {
    private static final FloatBuffer BLADE_MATRIX = BufferUtils.createFloatBuffer(16);
    /** 1.20 LayerMainBlade：骨架空间 1/8，刀模再乘 1/16，最终为 1/128。 */
    private static final float BONE_SPACE_SCALE = 0.125F;
    private static final float MODEL_SPACE_SCALE = 0.0625F;
    /** RenderLivingBase 的 Y 轴已翻转，负值会让画面中的核能震动整条刀轨上移。 */
    private static final float NUCLEAR_VERTICAL_OFFSET = -2.0F;
    /** 女武神冲击的大剑轨迹修正；渲染空间中 +Y 向下、-Z 为人物前方。 */
    private static final float VALKYRIE_VERTICAL_OFFSET = 0.5F;
    private static final float VALKYRIE_FORWARD_OFFSET = -1.0F;

    private AnimatedGreatswordRenderer() {}

    /** @return true 表示当前为 VMD 动作并已接管原 SlashBlade 刀层。 */
    public static boolean render(EntityLivingBase owner, float partialTicks) {
        GreatswordVmdAnimation.ActiveMotion active =
                GreatswordVmdAnimation.INSTANCE.findActive(owner, partialTicks);
        if (active == null) return false;
        ItemStack stack = owner.getHeldItemMainhand();
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemSlashBlade)) return false;

        ItemSlashBlade blade = (ItemSlashBlade) stack.getItem();
        WavefrontObject model = BladeModelManager.getInstance().getModel(blade.getModelLocation(stack));
        ResourceLocationRaw texture = blade.getModelTexture(stack);
        float lightX = OpenGlHelper.lastBrightnessX;
        float lightY = OpenGlHelper.lastBrightnessY;

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            GlStateManager.enableBlend();
            GlStateManager.disableCull();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.color(1, 1, 1, 1);

            // 两级缩放不能合并：前一级同时决定 Joint/hardpoint 的位移尺度。
            active.applyBladeUserPose();
            if (active.skill == GreatswordVmdAnimation.Skill.NUCLEAR) {
                // 统一修正整套动作高度，不介入 VMD 轨迹、握点或剑身旋转。
                GlStateManager.translate(0.0F, NUCLEAR_VERTICAL_OFFSET, 0.0F);
            } else if (active.skill == GreatswordVmdAnimation.Skill.VALKYRIE) {
                // 仅移动大剑整条轨迹：下移一格，并沿人物面朝方向前移一格。
                GlStateManager.translate(0.0F, VALKYRIE_VERTICAL_OFFSET,
                        VALKYRIE_FORWARD_OFFSET);
            }
            GlStateManager.scale(BONE_SPACE_SCALE, BONE_SPACE_SCALE, BONE_SPACE_SCALE);
            GlStateManager.rotate(180.0F, 0, 1, 0);
            // 核能震动：整条 hardpoint 运动路径绕人物 Y 轴翻转 180°。
            // 放在 VMD 矩阵之前，轨迹平移与旋转会一起翻转。
            if (active.skill == GreatswordVmdAnimation.Skill.NUCLEAR) {
                GlStateManager.rotate(180.0F, 0, 1, 0);
            }
            // 女武神冲击：整套 hardpoint 动作绕 X 轴俯仰翻转 180°，
            // 轨迹位置与剑身朝向共同旋转。
            if (active.skill == GreatswordVmdAnimation.Skill.VALKYRIE) {
                GlStateManager.rotate(180.0F, 1, 0, 0);
            }
            active.writeBladeMatrix(BLADE_MATRIX);
            // 回到尺寸修正后的基线：只保留 NyMmd 到 Minecraft 的 X 坐标换手。
            // 后续方向校准只增加旋转，不再改 hardpoint 平移和两级缩放。
            if (active.skill == GreatswordVmdAnimation.Skill.NUCLEAR) {
                // 围绕当前 hardpoint 握点，在动画空间预乘 180° 俯仰。
                // 这样只翻转剑身基向量，不会旋转或平移整条运动路径。
                float pivotX = -BLADE_MATRIX.get(12);
                float pivotY = BLADE_MATRIX.get(13);
                float pivotZ = BLADE_MATRIX.get(14);
                GlStateManager.translate(pivotX, pivotY, pivotZ);
                GlStateManager.rotate(180.0F, 1, 0, 0);
                GlStateManager.translate(-pivotX, -pivotY, -pivotZ);
            }
            GlStateManager.scale(-1, 1, 1);
            GL11.glMultMatrix(BLADE_MATRIX);
            GlStateManager.scale(-1, 1, 1);
            GlStateManager.scale(MODEL_SPACE_SCALE, MODEL_SPACE_SCALE, MODEL_SPACE_SCALE);

            Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
            model.renderPart("blade");
            // 这些崩坏大剑把握柄拆成独立 OBJ 组，动作中必须随 blade 一起走。
            model.renderPart("handle");

            ResourceLocation luminous = luminous(texture);
            if (luminous != null) {
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
                GlStateManager.depthMask(false);
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE,
                        GlStateManager.DestFactor.ZERO);
                Minecraft.getMinecraft().getTextureManager().bindTexture(luminous);
                GlStateManager.color(1.0F, 0.92F, 0.78F, 0.82F);
                model.renderPart("blade");
                model.renderPart("handle");
            }
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GlStateManager.color(1, 1, 1, 1);
            GL11.glPopAttrib();
            GL11.glPopMatrix();
            GlStateManager.enableTexture2D();
            GlStateManager.enableCull();
            GlStateManager.enableLighting();
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
        }
        return true;
    }

    private static ResourceLocation luminous(ResourceLocationRaw texture) {
        String path = texture.getPath();
        if (!path.endsWith(".png")) return null;
        return new ResourceLocationRaw(texture.getNamespace(),
                path.substring(0, path.length() - 4) + "_luminous.png");
    }
}

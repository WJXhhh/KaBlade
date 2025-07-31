package com.wjx.kablade.particle;

import mods.flammpfeil.slashblade.client.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.util.ResourceLocationRaw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

public class ParticlePetal extends Particle {




    private final float initialScale = 0.1F;
    private float rotationSpeed;
    private final float swayAmplitude;
    private final float swayFrequency;
    private byte ptex = 1;

    // 花瓣飘落的物理参数
    private float rotation = 0.0F;
    private float swayOffset;
    private double windStrength;
    private int swayDirection;

    private WavefrontObject model = new WavefrontObject(new ResourceLocationRaw("kablade:effects/rain/petal.obj"));

    public ParticlePetal(World worldIn, double x, double y, double z, int type) {
        super(worldIn, x, y, z);
        ptex = (byte)type;



        this.particleScale = this.initialScale;
        this.particleMaxAge = 300 + (int)(Math.random() * 200); // 15-25秒

        // 飘落物理参数
        this.rotationSpeed = (float)(Math.random() - 0.5) * 2.0F; // 旋转速度
        this.swayAmplitude = 0.3F + (float)(Math.random() * 0.4F); // 摆动幅度
        this.swayFrequency = 0.5F + (float)(Math.random() * 1.0F); // 摆动频率
        this.swayOffset = (float)(Math.random() * Math.PI * 2); // 摆动相位偏移
        this.swayDirection = Math.random() > 0.5 ? 1 : -1;

        // 初始运动
        this.motionX = (Math.random() - 0.5D) * 0.05D;
        this.motionY = -0.01D - Math.random() * 0.02D; // 缓慢下落
        this.motionZ = (Math.random() - 0.5D) * 0.05D;

        // 风力强度
        this.windStrength = 0.3D + Math.random() * 0.4D;

        this.particleGravity = 0.05F; // 很轻的重力
        this.canCollide = true;
    }

    public ParticlePetal(World worldIn, double x, double y, double z,
                         double motionX, double motionY, double motionZ, int type) {
        this(worldIn, x, y, z, type);
        // 保留一些随机性，但应用传入的运动
        this.motionX = motionX + (Math.random() - 0.5D) * 0.02D;
        this.motionY = motionY;
        this.motionZ = motionZ + (Math.random() - 0.5D) * 0.02D;
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.particleAge++ >= this.particleMaxAge) {
            this.setExpired();
            return;
        }

        // 更新花瓣飘落效果
        updatePetalMotion();
        updatePetalVisuals();

        // 移动粒子
        this.move(this.motionX, this.motionY, this.motionZ);

        // 地面碰撞处理
        handleGroundCollision();
    }

    private void updatePetalMotion() {
        float time = (float)this.particleAge * 0.05F;

        // 1. 重力下落
        this.motionY -= 0.001D * this.particleGravity;

        // 2. 左右摆动（模拟空气阻力造成的摆动）
        double swayX = Math.sin(time * swayFrequency + swayOffset) * swayAmplitude * 0.01D;
        double swayZ = Math.cos(time * swayFrequency * 0.7F + swayOffset) * swayAmplitude * 0.008D;

        this.motionX += swayX * swayDirection;
        this.motionZ += swayZ * swayDirection;

        // 3. 风力效果（随机方向的轻微推力）
        if (this.particleAge % 20 == 0) { // 每秒更新一次风向
            this.motionX += (Math.random() - 0.5D) * windStrength * 0.01D;
            this.motionZ += (Math.random() - 0.5D) * windStrength * 0.01D;
        }

        // 4. 空气阻力
        this.motionX *= 0.995D;
        this.motionY *= 0.998D;
        this.motionZ *= 0.995D;

        // 5. 旋转更新
        this.rotation += this.rotationSpeed;
        if (this.rotation > 360.0F) this.rotation -= 360.0F;
        if (this.rotation < 0.0F) this.rotation += 360.0F;

        // 6. 限制最大下落速度
        if (this.motionY < -0.05D) {
            this.motionY = -0.05D;
        }
    }

    private void updatePetalVisuals() {
        float ageRatio = (float)this.particleAge / (float)this.particleMaxAge;

        // 透明度渐变 - 先保持，后期逐渐消失
        if (ageRatio < 0.7F) {
            this.particleAlpha = 0.9F;
        } else {
            float fadeRatio = (ageRatio - 0.7F) / 0.3F;
            this.particleAlpha = 0.9F * (1.0F - fadeRatio);
        }

        // 尺寸变化 - 轻微的呼吸效果
        float breathe = (float)Math.sin(this.particleAge * 0.1F) * 0.05F;
        this.particleScale = this.initialScale + breathe;

        // 颜色轻微变化 - 模拟光照变化

    }

    private void handleGroundCollision() {
        if (this.onGround || this.canCollide) {
            // 花瓣落地后轻微弹跳
            this.motionY = Math.abs(this.motionY) * 0.3D;
            this.motionX *= 0.8D;
            this.motionZ *= 0.8D;

            // 减少旋转速度
            this.rotationSpeed *= 0.9F;

            // 如果几乎静止，加速消失
            if (Math.abs(this.motionX) + Math.abs(this.motionZ) < 0.001D) {
                this.particleMaxAge = Math.min(this.particleMaxAge, this.particleAge + 40);
            }
        }
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entityIn,
                               float partialTicks, float rotationX, float rotationZ,
                               float rotationYZ, float rotationXY, float rotationXZ) {

        // 绑定花瓣纹理
        Minecraft.getMinecraft().getTextureManager()
                .bindTexture(new ResourceLocation(String.format("kablade:effects/rain/%d.png",ptex)));

        // 保存当前OpenGL状态
        GlStateManager.pushMatrix();

        // 启用混合和透明度
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.003921569F);

        // 计算插值位置
        float f = (float)(this.prevPosX + (this.posX - this.prevPosX) * (double)partialTicks);
        float f1 = (float)(this.prevPosY + (this.posY - this.prevPosY) * (double)partialTicks);
        float f2 = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * (double)partialTicks);

        float f3 = ( float)(entityIn.prevPosX+(entityIn.posX-entityIn.prevPosX)*partialTicks);
        float f4 = ( float)(entityIn.prevPosY+(entityIn.posY-entityIn.prevPosY)*partialTicks);
        float f5 = ( float)(entityIn.prevPosZ+(entityIn.posZ-entityIn.prevPosZ)*partialTicks);

        // 移动到粒子位置
        GlStateManager.translate(f - f3, f1 - f4, f2 - f5);

        // 应用旋转
        GlStateManager.rotate(this.rotation, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(this.rotation * 0.5F, 1.0F, 0.0F, 0.0F);

        // 应用缩放
        GlStateManager.scale(this.particleScale, this.particleScale, this.particleScale);




        // 渲染3D花瓣（这里你需要调用你的3D模型渲染代码）
        model.renderAll();

        // 恢复OpenGL状态
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }



    @Override
    public int getFXLayer() {
        return 3; // 使用自定义渲染层
    }

    @Override
    public boolean shouldDisableDepth() {
        return false; // 启用深度测试，让花瓣有正确的前后关系
    }
}

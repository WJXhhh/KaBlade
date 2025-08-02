package com.wjx.kablade.particle;

import mods.flammpfeil.slashblade.client.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.util.ResourceLocationRaw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

public class ParticlePetal extends Particle {





    private float rotationSpeedX=1;
    private float rotationSpeedY=1;
    private float rotationSpeedZ=1;
    private float smallspe = 0.01f;

    private byte ptex = 1;

    // 花瓣飘落的物理参数
    private float rotationX = 0;
    private float rotationY = 0;
    private float rotationZ = 0;


    private static final WavefrontObject model = new WavefrontObject(new ResourceLocationRaw("kablade:effects/rain/petal.obj"));

    public ParticlePetal(World worldIn, double x, double y, double z, int type) {
        super(worldIn, x, y, z);
        ptex = (byte)type;

        // 设置粒子的基本属性
        this.motionX = (Math.random() - 0.5D) * 0.1D;
        this.motionY = (Math.random() -0.5D) * 0.1D;
        this.motionZ = (Math.random() - 0.5D) * 0.1D;

        // 设置粒子颜色 (红色示例)
        this.particleRed = 1.0F;
        this.particleGreen = 0.2F;
        this.particleBlue = 0.2F;

        // 设置粒子大小和生命周期
        this.particleScale = 0.1F;
        this.particleMaxAge = 40; // 3秒 (20 ticks/秒)
        this.canCollide = false;
    }

    public ParticlePetal(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn,int type) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
        ptex = (byte)type;
        this.motionX = xSpeedIn;
        this.motionY = ySpeedIn;
        this.motionZ = zSpeedIn;

        this.particleGravity = 0.1F;

        // 设置粒子颜色 (红色示例)
        this.particleRed = 1.0F;
        this.particleGreen = 1.0F;
        this.particleBlue = 1.0F;

        this.rotationSpeedX = rand.nextFloat()*10f/2-5;
        this.rotationSpeedZ = rand.nextFloat()*10f/2-5;
        this.rotationSpeedY = rand.nextFloat()*10f/2-5;

        // 设置粒子大小和生命周期
        this.particleScale = 0.05F;
        smallspe = 0.001f+(rand.nextInt(10)/4000f);
        this.particleMaxAge = 80; // 3秒 (20 ticks/秒)
        this.canCollide = false;
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
        //handleGroundCollision();
    }

    private void updatePetalMotion() {
        float time = (float)this.particleAge * 0.05F;

        // 1. 重力下落
        this.motionY -= 0.001D * this.particleGravity;

        // 2. 左右摆动（模拟空气阻力造成的摆动）


        // 3. 风力效果（随机方向的轻微推力）


        // 4. 空气阻力
        this.motionX *= 0.98D;
        this.motionY *= 0.98D;
        this.motionZ *= 0.98D;

        // 5. 旋转更新
        this.rotationX += this.rotationSpeedX;
        if (this.rotationX > 360.0F) this.rotationX -= 360.0F;
        if (this.rotationX < 0.0F) this.rotationX += 360.0F;

        this.rotationY += this.rotationSpeedY;
        if (this.rotationY > 360.0F) this.rotationY -= 360.0F;
        if (this.rotationY < 0.0F) this.rotationY += 360.0F;

        this.rotationZ += this.rotationSpeedZ;
        if (this.rotationZ > 360.0F) this.rotationZ -= 360.0F;
        if (this.rotationZ < 0.0F) this.rotationZ += 360.0F;


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

        this.particleScale = this.particleScale-smallspe;
        if(this.particleScale<0){
            this.setExpired();
        }

        // 颜色轻微变化 - 模拟光照变化

    }

    private void handleGroundCollision() {
        if (this.onGround || this.canCollide) {
            // 花瓣落地后轻微弹跳
            this.motionY = Math.abs(this.motionY) * 0.3D;
            this.motionX *= 0.8D;
            this.motionZ *= 0.8D;

            // 减少旋转速度
            this.rotationSpeedX *= 0.95F;
            this.rotationSpeedY *= 0.95F;
            this.rotationSpeedZ *= 0.95F;

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
        float lastx = OpenGlHelper.lastBrightnessX;
        float lasty = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

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
        GlStateManager.rotate(this.rotationY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(this.rotationX, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(this.rotationZ, 0F, 0.0F, 1F);

        // 应用缩放
        GlStateManager.scale(this.particleScale, this.particleScale, this.particleScale);




        // 渲染3D花瓣（这里你需要调用你的3D模型渲染代码）
        model.renderAll();

        // 恢复OpenGL状态
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastx, lasty);
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

package com.wjx.kablade.particle;

import com.wjx.kablade.Main;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.World;

public class ParticleDust extends Particle {
    public ParticleDust(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
        this.motionX = xSpeedIn;
        this.motionY = ySpeedIn;
        this.motionZ = zSpeedIn;

        // 设置粒子颜色 (红色示例)
        this.particleRed = 1.0F;
        this.particleGreen = 1.0F;
        this.particleBlue = 1.0F;

        // 设置粒子大小和生命周期
        this.particleScale = 0.3F;
        this.particleMaxAge = 60; // 3秒 (20 ticks/秒)
        this.canCollide = false;

    }

    public ParticleDust(World worldIn, double posXIn, double posYIn, double posZIn) {
        super(worldIn, posXIn, posYIn, posZIn);
        // 设置粒子的基本属性
        this.motionX = (Math.random() - 0.5D) * 0.1D;
        this.motionY = (Math.random() -0.5D) * 0.1D;
        this.motionZ = (Math.random() - 0.5D) * 0.1D;

        // 设置粒子颜色 (红色示例)
        this.particleRed = 1.0F;
        this.particleGreen = 0.2F;
        this.particleBlue = 0.2F;

        // 设置粒子大小和生命周期
        this.particleScale = 1F;
        this.particleMaxAge = 60; // 3秒 (20 ticks/秒)
        this.canCollide = false;
    }
    @Override
    public void onUpdate() {
        //Main.logger.info("ParticleDust:"+this.motionX+":"+this.motionY+":"+this.motionZ);
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.particleAge++ >= this.particleMaxAge) {
            this.setExpired();
        }

        // 重力效果
        this.motionY -= 0.04D * (double)this.particleGravity;

        // 移动粒子
        this.move(this.motionX, this.motionY, this.motionZ);

        // 减速效果
        this.motionX *= 0.98D;
        this.motionY *= 0.98D;
        this.motionZ *= 0.98D;

        // 渐变效果
        float fadeRatio = (float)this.particleAge / (float)this.particleMaxAge;
        this.particleAlpha = 1.0F - fadeRatio;
    }

    @Override
    public int getFXLayer() {
        return 0; // 使用默认纹理层
    }


}

package com.wjx.kablade.particle;

import com.sun.javafx.geom.Vec3f;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ParticleColorfulSmoke extends Particle {
    public float disRate;
    public ParticleColorfulSmoke(World worldIn, double posXIn, double posYIn, double posZIn, Vec3f rgb, int life) {
        super(worldIn, posXIn, posYIn, posZIn);
        this.particleRed = rgb.x;
        this.particleGreen = rgb.y;
        this.particleBlue = rgb.z;
        this.particleAlpha = 0.8F;
        this.setParticleTextureIndex(147);
        this.setSize(0.02F, 0.02F);
        this.particleScale = 1.0F;
        this.particleMaxAge = life * 20;
        this.canCollide = false;
        disRate = this.particleAlpha/life;
        this.motionX = (300 - worldIn.rand.nextInt(600)) /1000d;
        this.motionY = (300 - worldIn.rand.nextInt(600)) /1000d;
        this.motionZ = (300 - worldIn.rand.nextInt(600)) /1000d;

    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.posX += this.motionX;
        this.posY += this.motionY;
        this.posZ += this.motionZ;
        this.particleAlpha-=disRate;
        if (this.particleMaxAge-- <= 0) {
            this.setExpired();
        }
    }
}

package com.wjx.kablade.particle.factory;

import com.wjx.kablade.particle.ParticleDust;
import com.wjx.kablade.particle.ParticleSmallLigh;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class ParticleSmallLighFactory implements IParticleFactory {
    @Nullable
    @Override
    public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn, int... p_178902_15_) {
        return new ParticleSmallLigh(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
    }
}

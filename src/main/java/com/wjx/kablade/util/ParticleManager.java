package com.wjx.kablade.util;

import com.wjx.kablade.particle.factory.ParticleDustFactory;
import com.wjx.kablade.particle.factory.ParticlePetalFactory;
import com.wjx.kablade.particle.manager.ParticleIDManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumParticleTypes;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


public class ParticleManager {
    public static EnumParticleTypes DUST_PARTICLE;
    public static EnumParticleTypes PETAL_PARTICLE;
   // private static final int BASE_PARTICLE_ID = 1000;
    public static void registerParticles() {
        try {
            // 使用 EnumHelper 添加新的粒子类型，避免ID冲突
            DUST_PARTICLE = EnumHelper.addEnum(
                    EnumParticleTypes.class,
                    "DUST_PARTICLE",
                    new Class[] { String.class, int.class, boolean.class, int.class },
                    "cdust",
                    ParticleIDManager.getNextAvailableID(),
                    false,
                    0
            );

            if (DUST_PARTICLE != null) {
                // 注册粒子工厂到效果渲染器
                Minecraft.getMinecraft().effectRenderer.registerParticle(
                        DUST_PARTICLE.getParticleID(),
                        new ParticleDustFactory()
                );

                System.out.println("自定义粒子注册成功！ID: " + DUST_PARTICLE.getParticleID());
            } else {
                System.err.println("自定义粒子注册失败！EnumHelper 返回 null");
            }

        } catch (Exception e) {
            System.err.println("粒子注册过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        try {
            // 使用 EnumHelper 添加新的粒子类型，避免ID冲突
            PETAL_PARTICLE = EnumHelper.addEnum(
                    EnumParticleTypes.class,
                    "PETAL_PARTICLE",
                    new Class[] { String.class, int.class, boolean.class, int.class },
                    "cdust",
                    ParticleIDManager.getNextAvailableID(),
                    false,
                    1
            );

            if (PETAL_PARTICLE != null) {
                // 注册粒子工厂到效果渲染器
                Minecraft.getMinecraft().effectRenderer.registerParticle(
                        PETAL_PARTICLE.getParticleID(),
                        new ParticlePetalFactory()
                );

                System.out.println("自定义粒子注册成功！ID: " + PETAL_PARTICLE.getParticleID());
            } else {
                System.err.println("自定义粒子注册失败！EnumHelper 返回 null");
            }

        } catch (Exception e) {
            System.err.println("粒子注册过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }

    }

    // 便捷方法：生成粒子
    public static void spawnCustomParticle(double x, double y, double z,
                                           double motionX, double motionY, double motionZ) {
        if (DUST_PARTICLE != null && Minecraft.getMinecraft().world != null) {
            Minecraft.getMinecraft().world.spawnParticle(
                    DUST_PARTICLE,
                    x, y, z,
                    motionX, motionY, motionZ
            );
        }
    }

    public static void spawnPetalParticle(double x, double y, double z,
                                           double motionX, double motionY, double motionZ,int type) {
        if (PETAL_PARTICLE != null && Minecraft.getMinecraft().world != null) {
            Minecraft.getMinecraft().world.spawnParticle(
                    PETAL_PARTICLE,
                    x, y, z,
                    motionX, motionY, motionZ, type
            );
        }
    }
}

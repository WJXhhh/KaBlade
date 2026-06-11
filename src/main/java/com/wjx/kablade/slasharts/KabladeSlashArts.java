package com.wjx.kablade.slasharts;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/** Fires three blue drives when the blade's SA is activated. */
public final class KabladeSlashArts extends SlashArts {

    public KabladeSlashArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (!user.level().isClientSide()) {
            for (int i = -1; i <= 1; i++) {
                EntityDrive drive = new EntityDrive(SlashBlade.RegistryEvents.Drive, user.level());
                drive.setPos(user.getX(), user.getEyeY() - 0.25D, user.getZ());
                drive.setShooter(user);
                Vec3 direction = user.getLookAngle().yRot((float) Math.toRadians(i * 10.0F));
                drive.shoot(direction.x, direction.y, direction.z, 1.5F, 0.0F);
                drive.setDamage(4.0D);
                drive.setColor(0x66CCFF);
                user.level().addFreshEntity(drive);
            }
        }
        return super.doArts(type, user);
    }
}

package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** The short-lived light hedra summoned by Pledge of Rain. */
public class SummonedHedraEntity extends PhantomSwordExEntity {

    private static final int HEDRA_COLOR = 0x3355647;

    public SummonedHedraEntity(EntityType<? extends SummonedHedraEntity> type, Level level) {
        super(type, level);
    }

    public static SummonedHedraEntity spawn(ServerLevel level, LivingEntity owner,
                                            double x, double y, double z,
                                            float damage, float pitch, float yaw) {
        SummonedHedraEntity entity = new SummonedHedraEntity(ModEntities.SUMMONED_HEDRA.get(), level);
        entity.setPos(x, y, z);
        entity.setYRot(yaw);
        entity.setXRot(pitch);
        entity.setThrower(owner);
        entity.attackDamage = Math.max(1.0F, damage);
        entity.blade = owner == null ? net.minecraft.world.item.ItemStack.EMPTY : owner.getMainHandItem();
        entity.setLifetime(20);
        entity.setInterval(0);
        entity.setColor(HEDRA_COLOR);
        entity.setRoll(0.0F);

        float yawRad = yaw * ((float) Math.PI / 180.0F);
        float pitchRad = pitch * ((float) Math.PI / 180.0F);
        Vec3 direction = new Vec3(
                -Mth.sin(yawRad) * Mth.cos(pitchRad),
                -Mth.sin(pitchRad),
                Mth.cos(yawRad) * Mth.cos(pitchRad));
        entity.initializeTrajectory(yaw, pitch, direction);
        if (owner != null) {
            entity.alreadyHit.add(owner.getUUID());
        }
        level.addFreshEntity(entity);
        return entity;
    }
}

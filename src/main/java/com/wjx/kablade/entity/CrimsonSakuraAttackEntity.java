package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public class CrimsonSakuraAttackEntity extends Entity {

    private static final int LIFETIME = 22;
    private static final float MIN_INNER_RADIUS = 2.30F;

    public CrimsonSakuraAttackEntity(EntityType<? extends CrimsonSakuraAttackEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static CrimsonSakuraAttackEntity spawn(Level level, LivingEntity owner) {
        CrimsonSakuraAttackEntity entity = new CrimsonSakuraAttackEntity(ModEntities.CRIMSON_SAKURA.get(), level);
        entity.setPos(owner.getX(), owner.getY(), owner.getZ());
        entity.setYRot(owner.getYRot());
        level.addFreshEntity(entity);
        return entity;
    }

    public int getLifetime() {
        return LIFETIME;
    }

    public float getAlpha(float partialTick) {
        float age = getRenderAge(partialTick);
        if (age <= 20.0F) {
            return 1.0F;
        }
        return Math.max(0.0F, 1.0F - (age - 20.0F) / (LIFETIME - 20.0F));
    }

    public float getAngle(float partialTick) {
        float age = Math.min(getRenderAge(partialTick), 20.0F);
        return 15.0F - 30.0F / 20.0F * age;
    }

    public int getVisibleSegments(float partialTick) {
        float age = getRenderAge(partialTick);
        if (age < 10.0F) {
            return Math.max(1, Math.round(60.0F / 10.0F * age));
        }
        return 60;
    }

    public float getInnerRadius(float partialTick) {
        float age = getRenderAge(partialTick);
        if (age < 10.0F) {
            return 1.75F + 1.25F / 10.0F * age;
        }
        if (age > 15.0F) {
            return Math.max(MIN_INNER_RADIUS, 3.0F - (3.0F - MIN_INNER_RADIUS) / 7.0F * (age - 15.0F));
        }
        return 3.0F;
    }

    private float getRenderAge(float partialTick) {
        return Math.max(0.0F, Math.min(this.tickCount + partialTick, LIFETIME));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount >= LIFETIME) {
            this.discard();
        }
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

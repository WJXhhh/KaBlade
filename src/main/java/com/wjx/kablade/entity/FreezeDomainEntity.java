package com.wjx.kablade.entity;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.util.SaTargeting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

/** Visible freeze domain for Ice Epiphyllum's Snow Dance, ported from 1.12.2 EntityFreezeDomain. */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FreezeDomainEntity extends Entity {

    private static final EntityDataAccessor<Integer> RENDER_TICK =
            SynchedEntityData.defineId(FreezeDomainEntity.class, EntityDataSerializers.INT);

    public static final String BOOSTER_TAG = Main.MODID + ".freeze_domain_damage_booster";
    private static final int LIFETIME = 100;
    private static final int BOOSTER_TICKS = 40;
    private static final float DAMAGE_BOOST = 1.4F;
    private static final double RANGE_XZ = 8.0D;
    private static final double RANGE_UP = 4.0D;
    private LivingEntity owner;

    public FreezeDomainEntity(EntityType<? extends FreezeDomainEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public FreezeDomainEntity(Level level, LivingEntity owner) {
        this(ModEntities.FREEZE_DOMAIN.get(), level);
        this.owner = owner;
        this.setPos(owner.getX(), owner.getY(), owner.getZ());
    }

    public static FreezeDomainEntity spawn(Level level, LivingEntity owner) {
        FreezeDomainEntity domain = new FreezeDomainEntity(level, owner);
        level.addFreshEntity(domain);
        return domain;
    }

    public int getRenderTick() {
        return this.entityData.get(RENDER_TICK);
    }

    private void setRenderTick(int renderTick) {
        this.entityData.set(RENDER_TICK, renderTick);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(RENDER_TICK, 0);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            applyDomain((ServerLevel) this.level());
            setRenderTick(getRenderTick() >= 5 ? 0 : getRenderTick() + 1);
            if (this.tickCount > LIFETIME) {
                this.discard();
            }
        }
    }

    private void applyDomain(ServerLevel level) {
        AABB box = new AABB(
                this.getX() - RANGE_XZ, this.getY(), this.getZ() - RANGE_XZ,
                this.getX() + RANGE_XZ, this.getY() + RANGE_UP, this.getZ() + RANGE_XZ);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box, this::canAffect);
        for (LivingEntity target : targets) {
            target.getPersistentData().putInt(BOOSTER_TAG, BOOSTER_TICKS);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    BOOSTER_TICKS, 3, false, true));
        }
    }

    private boolean canAffect(LivingEntity target) {
        return SaTargeting.canDamage(this.owner, target);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        CompoundTag tag = entity.getPersistentData();
        if (!tag.contains(BOOSTER_TAG)) {
            return;
        }
        int ticks = tag.getInt(BOOSTER_TAG) - 1;
        if (ticks > 0) {
            tag.putInt(BOOSTER_TAG, ticks);
        } else {
            tag.remove(BOOSTER_TAG);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || event.getAmount() <= 0.0F) {
            return;
        }
        if (event.getSource().getEntity() instanceof LivingEntity attacker
                && !SaTargeting.canDamage(attacker, entity)) {
            return;
        }
        if (entity.getPersistentData().getInt(BOOSTER_TAG) > 0) {
            event.setAmount(event.getAmount() * DAMAGE_BOOST);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("RenderTick")) {
            setRenderTick(tag.getInt("RenderTick"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("RenderTick", getRenderTick());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return new AABB(
                this.getX() - RANGE_XZ, this.getY() - 0.5D, this.getZ() - RANGE_XZ,
                this.getX() + RANGE_XZ, this.getY() + RANGE_UP, this.getZ() + RANGE_XZ);
    }
}

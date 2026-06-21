package com.wjx.kablade.entity;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModMobEffects;
import com.wjx.kablade.init.ModEntities;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.UUID;

/**
 * 高频坍缩力场实体 —— 从 1.12.2 {@code EntityConfinementForceField} 移植而来。
 * <p>
 * 在目标位置生成一个持续约 5 秒的立场，对范围内可攻击实体持续造成伤害、
 * 减速，并附加「禁锢」标记；标记期间受击者受到的伤害会放大 3.2 倍。
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ConfinementForceFieldEntity extends Entity {

    public static final EntityDataAccessor<Integer> OWNER_ID =
            SynchedEntityData.defineId(ConfinementForceFieldEntity.class, EntityDataSerializers.INT);

    private static final int LIFETIME = 100;
    private static final float FIELD_DAMAGE = 4.0F;
    private static final double FIELD_RADIUS = 6.0;
    private static final float DAMAGE_BOOST = 3.2F;

    private LivingEntity owner;
    private UUID ownerUUID;

    public ConfinementForceFieldEntity(EntityType<? extends ConfinementForceFieldEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public ConfinementForceFieldEntity(Level level, LivingEntity owner) {
        this(ModEntities.CONFINEMENT_FORCE_FIELD.get(), level);
        this.owner = owner;
        this.ownerUUID = owner.getUUID();
        this.setPos(owner.getX(), owner.getY(), owner.getZ());
        this.setYRot(owner.getYRot());
    }

    public static ConfinementForceFieldEntity spawn(Level level, LivingEntity owner) {
        ConfinementForceFieldEntity field = new ConfinementForceFieldEntity(level, owner);
        level.addFreshEntity(field);
        return field;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            return;
        }

        if (this.tickCount > LIFETIME) {
            this.discard();
            return;
        }

        if (this.owner == null && this.ownerUUID != null) {
            if (this.level() instanceof ServerLevel serverLevel) {
                this.owner = (LivingEntity) serverLevel.getEntity(this.ownerUUID);
            }
        }

        if (this.owner != null) {
            int id = this.owner.getId();
            if (this.entityData.get(OWNER_ID) != id) {
                this.entityData.set(OWNER_ID, id);
            }
        }

        ServerLevel serverLevel = (ServerLevel) this.level();
        AABB box = this.getBoundingBox()
                .inflate(FIELD_RADIUS, 2.0, FIELD_RADIUS);

        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, box, this::canAffect);

        for (LivingEntity target : targets) {
            // 禁锢标记：纯内部标记（隐藏粒子与图标），仅供 onLivingHurt 放大伤害用
            target.addEffect(new MobEffectInstance(ModMobEffects.CONFINEMENT.get(),
                    5, 0, false, false, false));
            // 减速
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    5, 3, false, true));

            if (this.owner instanceof Player player) {
                target.hurt(serverLevel.damageSources().playerAttack(player), FIELD_DAMAGE);
            } else if (this.owner != null) {
                target.hurt(serverLevel.damageSources().mobAttack(this.owner), FIELD_DAMAGE);
            } else {
                target.hurt(serverLevel.damageSources().magic(), FIELD_DAMAGE);
            }
        }
    }

    private boolean canAffect(LivingEntity e) {
        if (!e.isAlive() || e == this.owner) {
            return false;
        }
        if (e instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUUID != null) {
            tag.putUUID("OwnerUUID", this.ownerUUID);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    /**
     * 禁锢期间受击者伤害放大 3.2 倍（同 1.12.2 {@code EntityConfinementForceField.onLivingHurt}）。
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        if (entity.hasEffect(ModMobEffects.CONFINEMENT.get())) {
            event.setAmount(event.getAmount() * DAMAGE_BOOST);
        }
    }
}

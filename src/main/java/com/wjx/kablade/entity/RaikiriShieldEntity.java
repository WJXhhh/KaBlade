package com.wjx.kablade.entity;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.KabladeCapabilities;
import com.wjx.kablade.init.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
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

/**
 * 雷切护盾实体 —— 「雷切」SA 召唤的环绕护盾。
 * <p>
 * 从 1.12.2 {@code EntityRaikiriBlade} 移植而来：
 * 生成后跟随施法者 10 秒（200 tick），紧贴施法者并对周围敌人造成 2 点伤害；
 * 拥有 10 点护盾耐久：持有者受到的伤害优先由护盾吸收（{@link #onThrowerHurt}），
 * 耐久归零后向持有者发送提示并消失。
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RaikiriShieldEntity extends Entity {

    public static final EntityDataAccessor<Integer> THROWER_ID =
            SynchedEntityData.defineId(RaikiriShieldEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> SHIELD_BLOOD =
            SynchedEntityData.defineId(RaikiriShieldEntity.class, EntityDataSerializers.FLOAT);

    private static final int LIFETIME = 200;
    private static final float DEFAULT_SHIELD_BLOOD = 10.0F;
    private static final float CONTACT_DAMAGE = 2.0F;
    private static final double CONTACT_RADIUS = 1.0;

    private LivingEntity thrower;
    /** 客户端上一帧位置，手动跟踪用于 partialTick 插值 */
    private double prevX, prevY, prevZ;

    public RaikiriShieldEntity(EntityType<? extends RaikiriShieldEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public RaikiriShieldEntity(Level level, LivingEntity thrower) {
        this(ModEntities.RAIKIRI_SHIELD.get(), level);
        this.thrower = thrower;
        this.setPos(thrower.getX(), thrower.getY(), thrower.getZ());
        this.xOld = this.getX();
        this.yOld = this.getY();
        this.zOld = this.getZ();
        this.entityData.set(THROWER_ID, thrower.getId());
    }

    public static RaikiriShieldEntity spawn(Level level, LivingEntity thrower) {
        RaikiriShieldEntity shield = new RaikiriShieldEntity(level, thrower);
        level.addFreshEntity(shield);
        return shield;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(THROWER_ID, -1);
        this.entityData.define(SHIELD_BLOOD, DEFAULT_SHIELD_BLOOD);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            // 客户端直接跟随 thrower 位置，消除网络同步延迟
            int tid = this.entityData.get(THROWER_ID);
            if (tid != -1) {
                Entity follow = this.level().getEntity(tid);
                if (follow != null) {
                    // 手动记录上一帧位置供 partialTick 插值
                    this.prevX = this.getX();
                    this.prevY = this.getY();
                    this.prevZ = this.getZ();
                    this.setPos(follow.getX(), follow.getY(), follow.getZ());
                }
            }
            return;
        }

        if (this.thrower == null || !this.thrower.isAlive() || this.tickCount > LIFETIME) {
            clearHud();
            this.discard();
            return;
        }

        // 护盾耐久归零时消失
        if (this.getShieldBlood() <= 0.0F) {
            clearHud();
            this.discard();
            return;
        }

        // 同步 throwerID 供客户端渲染器使用
        int id = this.thrower.getId();
        if (this.entityData.get(THROWER_ID) != id) {
            this.entityData.set(THROWER_ID, id);
        }

        // 跟随施法者位置
        this.setPos(this.thrower.getX(), this.thrower.getY(), this.thrower.getZ());

        // 更新 HUD：写入护盾耐久
        updateHud();

        // 对贴近的敌人造成伤害
        AABB box = this.getBoundingBox()
                .inflate(CONTACT_RADIUS, 0.0, CONTACT_RADIUS);
        DamageSource src = this.level().damageSources().mobAttack(this.thrower);
        for (Entity e : this.level().getEntities(this, box, this::canHurt)) {
            e.hurt(src, CONTACT_DAMAGE);
        }
    }

    private boolean canHurt(Entity e) {
        if (e == this || e == this.thrower) {
            return false;
        }
        if (e instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        return e instanceof LivingEntity;
    }

    public LivingEntity getThrower() {
        return this.thrower;
    }

    public Entity getOwner() {
        int id = this.entityData.get(THROWER_ID);
        return id >= 0 ? this.level().getEntity(id) : null;
    }

    /** 客户端上一帧 X（用于渲染插值） */
    public double getPrevX() { return this.prevX; }
    /** 客户端上一帧 Y（用于渲染插值） */
    public double getPrevY() { return this.prevY; }
    /** 客户端上一帧 Z（用于渲染插值） */
    public double getPrevZ() { return this.prevZ; }

    public float getShieldBlood() {
        return this.entityData.get(SHIELD_BLOOD);
    }

    public void setShieldBlood(float blood) {
        this.entityData.set(SHIELD_BLOOD, blood);
        // 复刻 1.12.2：向持有者用动作栏播报剩余护盾 / 破盾提示。
        if (!this.level().isClientSide() && this.thrower instanceof Player player) {
            if (blood <= 0.0F) {
                player.displayClientMessage(
                        Component.translatable("msg.kablade.raikiri_shield_crash").withStyle(ChatFormatting.AQUA), true);
            } else {
                player.displayClientMessage(
                        Component.translatable("msg.kablade.raikiri_shield_blood", (int) blood).withStyle(ChatFormatting.AQUA), true);
            }
        }
    }

    /** 将当前护盾耐久写入玩家 HUD capability。 */
    private void updateHud() {
        if (this.thrower instanceof Player player) {
            int blood = (int) this.getShieldBlood();
            player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                    .ifPresent(cap -> cap.set("raikiri_shield_blood", blood));
        }
    }

    /** 护盾消失时清除 HUD 显示。 */
    private void clearHud() {
        if (this.thrower instanceof Player player) {
            player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                    .ifPresent(cap -> cap.set("raikiri_shield_blood", 0));
        }
    }

    /**
     * 护盾抵伤 —— 一比一复刻 1.12.2 {@code WorldEvent#onLivingHurt} 的护盾逻辑：
     * 持有者受到的伤害优先由其护盾的耐久吸收，耗尽则把剩余伤害透传。
     */
    @SubscribeEvent
    public static void onThrowerHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        List<RaikiriShieldEntity> shields = player.level().getEntitiesOfClass(
                RaikiriShieldEntity.class, player.getBoundingBox().inflate(2.0),
                s -> s.isAlive() && s.thrower == player);
        if (shields.isEmpty()) {
            return;
        }
        float damage = event.getAmount();
        for (RaikiriShieldEntity shield : shields) {
            if (damage <= 0.0F) {
                break;
            }
            float blood = shield.getShieldBlood();
            if (blood > damage) {
                shield.setShieldBlood(blood - damage);
                damage = 0.0F;
            } else {
                shield.setShieldBlood(0.0F);
                damage -= blood;
            }
        }
        event.setAmount(damage);
    }

    @Override
    public boolean isPickable() {
        return false;
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

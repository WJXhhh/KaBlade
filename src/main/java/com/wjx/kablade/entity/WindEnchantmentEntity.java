package com.wjx.kablade.entity;

import com.wjx.kablade.init.KabladeCapabilities;
import com.wjx.kablade.init.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * 风之结界 AOE 光环实体，对应 1.12.2 的 {@code EntityWindEnchantment}。
 * <p>
 * - 生成在玩家位置
 * - 100 tick 后消失
 * - 每 tick 扫描 10 格半径内的玩家，对其 capability 写入 {@code wind_enchantment_boost = 5}
 * - 持有 renderTick（0-100）供渲染器驱动 4 层纹理旋转动画
 */
public final class WindEnchantmentEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_RENDER_TICK =
            SynchedEntityData.defineId(WindEnchantmentEntity.class, EntityDataSerializers.INT);

    private static final int LIFETIME = 100;
    private static final double SCAN_RADIUS = 10.0;
    private static final int BOOST_TICKS = 5;

    /** 随机旋转速率，对应 1.12.2 instance initializer 中的 getRate。 */
    public final HashMap<String, Float> rates = new HashMap<>();

    public WindEnchantmentEntity(EntityType<? extends WindEnchantmentEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        initRates();
    }

    /** 方便从 SA 中生成。 */
    public static WindEnchantmentEntity spawn(Level level, double x, double y, double z) {
        WindEnchantmentEntity e = new WindEnchantmentEntity(ModEntities.WIND_ENCHANTMENT.get(), level);
        e.setPos(x, y, z);
        level.addFreshEntity(e);
        return e;
    }

    /** 对应 1.12.2 的 instance initializer 随机生成 4 组旋转速率。 */
    private void initRates() {
        Random r = new Random();
        rates.put("effect1", randRate(r, 3) * 3.6f);
        rates.put("effect2", randRate(r, 2) * 3.6f);
        rates.put("effect3", randRate(r, 1) * 3.6f);
        rates.put("effect4", randRate(r, 4) * 3.6f);
    }

    private static int randRate(Random r, int base) {
        return (r.nextBoolean() ? -1 : 1) * (base + r.nextInt(3));
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_RENDER_TICK, 0);
    }

    public int getRenderTick() {
        return this.entityData.get(DATA_RENDER_TICK);
    }

    /**
     * 可见纹理的宽度为 20 格，而实体碰撞箱只有 0.5 格。
     * 使用默认剪裁箱时，第一人称镜头很容易把脚下的实体原点判为不可见，
     * 导致整个大型法阵都不进入渲染器。
     */
    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(10.0, 1.0, 10.0);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // 扫描附近玩家，写入 wind_enchantment_boost
            List<Player> players = this.level().getEntitiesOfClass(Player.class,
                    this.getBoundingBox().inflate(SCAN_RADIUS, 0, SCAN_RADIUS).expandTowards(0, 4, 0));
            for (Player p : players) {
                p.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                        .ifPresent(cap -> cap.set("wind_enchantment_boost", BOOST_TICKS));
            }

            // 生命周期
            if (this.tickCount >= LIFETIME) {
                this.discard();
                return;
            }

            // 更新渲染 tick
            int rt = getRenderTick();
            if (rt < LIFETIME) {
                this.entityData.set(DATA_RENDER_TICK, rt + 1);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

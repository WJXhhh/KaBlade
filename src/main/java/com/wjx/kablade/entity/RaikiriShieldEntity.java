package com.wjx.kablade.entity;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.KabladeCapabilities;
import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaTargeting;
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
 * 闆峰垏鎶ょ浘瀹炰綋 鈥斺€?銆岄浄鍒囥€峉A 鍙敜鐨勭幆缁曟姢鐩俱€? * <p>
 * 浠?1.12.2 {@code EntityRaikiriBlade} 绉绘鑰屾潵锛? * 鐢熸垚鍚庤窡闅忔柦娉曡€?10 绉掞紙200 tick锛夛紝绱ц创鏂芥硶鑰呭苟瀵瑰懆鍥存晫浜洪€犳垚浼ゅ锛? * 鎶ょ浘鑰愪箙鍜屾帴瑙︿激瀹虫牴鎹噴鏀炬椂鐨勬嫈鍒€鍓?baseAttackModifier 鍔ㄦ€佽绠楋細
 * <pre>
 *   鎶ょ浘鑰愪箙 = baseAttackModifier &times; 0.75 + amplifierCalc(baseAttackModifier, factor)
 *   鎺ヨЕ浼ゅ = (baseAttackModifier + amplifierCalc(baseAttackModifier, factor)) &times; 0.25
 * </pre>
 * 鎸佹湁鑰呭彈鍒扮殑浼ゅ浼樺厛鐢辨姢鐩惧惛鏀讹紙{@link #onThrowerHurt}锛夛紝鑰愪箙褰掗浂鍚庢秷澶便€? */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RaikiriShieldEntity extends Entity {

    public static final EntityDataAccessor<Integer> THROWER_ID =
            SynchedEntityData.defineId(RaikiriShieldEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> SHIELD_BLOOD =
            SynchedEntityData.defineId(RaikiriShieldEntity.class, EntityDataSerializers.FLOAT);

    private static final int LIFETIME = 200;
    private static final double CONTACT_RADIUS = 1.0;
    /** amplifierCalc 琛ユ绯绘暟锛堣€愪箙鍜屼激瀹冲叡鐢ㄥ悓涓€涓斁澶у櫒鍊硷級銆?*/
    private static final float AMP_FACTOR = 1.0F;

    private LivingEntity thrower;
    /** 鍙敜鏃舵柦娉曡€呮嫈鍒€鍓戠殑 baseAttackModifier銆?*/
    private float bladeAttack;
    /** 瀹㈡埛绔笂涓€甯т綅缃紝鎵嬪姩璺熻釜鐢ㄤ簬 partialTick 鎻掑€?*/
    private double prevX, prevY, prevZ;

    public RaikiriShieldEntity(EntityType<? extends RaikiriShieldEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public RaikiriShieldEntity(Level level, LivingEntity thrower, float bladeAttack) {
        this(ModEntities.RAIKIRI_SHIELD.get(), level);
        this.thrower = thrower;
        this.bladeAttack = bladeAttack;
        this.setPos(thrower.getX(), thrower.getY(), thrower.getZ());
        this.xOld = this.getX();
        this.yOld = this.getY();
        this.zOld = this.getZ();
        this.entityData.set(THROWER_ID, thrower.getId());
        // 鏍规嵁鍒€鐨勬敾鍑诲姏璁＄畻鍒濆鎶ょ浘鑰愪箙
        this.entityData.set(SHIELD_BLOOD, calcShieldBlood(bladeAttack));
    }

    public static RaikiriShieldEntity spawn(Level level, LivingEntity thrower, float bladeAttack) {
        RaikiriShieldEntity shield = new RaikiriShieldEntity(level, thrower, bladeAttack);
        level.addFreshEntity(shield);
        return shield;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(THROWER_ID, -1);
        this.entityData.define(SHIELD_BLOOD, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            int tid = this.entityData.get(THROWER_ID);
            if (tid != -1) {
                Entity follow = this.level().getEntity(tid);
                if (follow != null) {
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

        if (this.getShieldBlood() <= 0.0F) {
            clearHud();
            this.discard();
            return;
        }

        int id = this.thrower.getId();
        if (this.entityData.get(THROWER_ID) != id) {
            this.entityData.set(THROWER_ID, id);
        }

        this.setPos(this.thrower.getX(), this.thrower.getY(), this.thrower.getZ());

        // 鏇存柊 HUD锛氬啓鍏ユ姢鐩捐€愪箙
        updateHud();

        // 瀵硅创杩戠殑鍙敾鍑荤洰鏍囬€犳垚浼ゅ锛堟寜鍒€鐨勬敾鍑诲姏鍔ㄦ€佽绠楋級
        float contactDamage = calcContactDamage(this.bladeAttack);
        AABB box = this.getBoundingBox()
                .inflate(CONTACT_RADIUS, 0.0, CONTACT_RADIUS);
        DamageSource src = this.level().damageSources().mobAttack(this.thrower);
        for (Entity e : this.level().getEntities(this, box,
                e -> e != this && e instanceof LivingEntity living
                        && SaTargeting.canDamageAttackable(this.thrower, living))) {
            e.hurt(src, contactDamage);
        }
    }

    /** 鎶ょ浘鑰愪箙璁＄畻鍏紡銆?*/
    public static float calcShieldBlood(float attack) {
        float amp = MathFunc.amplifierCalc(attack, AMP_FACTOR);
        return attack * 0.75F + amp;
    }

    /** 鎺ヨЕ浼ゅ璁＄畻鍏紡锛堝熀纭€ 脳0.5锛岀炕鍊嶄互鍖归厤闆峰垏鐨勯珮鏀诲畾浣嶏級銆?*/
    public static float calcContactDamage(float attack) {
        float amp = MathFunc.amplifierCalc(attack, AMP_FACTOR);
        return (attack + amp) * 0.5F;
    }

    public LivingEntity getThrower() {
        return this.thrower;
    }

    public Entity getOwner() {
        int id = this.entityData.get(THROWER_ID);
        return id >= 0 ? this.level().getEntity(id) : null;
    }

    /** 瀹㈡埛绔笂涓€甯?X锛堢敤浜庢覆鏌撴彃鍊硷級 */
    public double getPrevX() { return this.prevX; }
    /** 瀹㈡埛绔笂涓€甯?Y锛堢敤浜庢覆鏌撴彃鍊硷級 */
    public double getPrevY() { return this.prevY; }
    /** 瀹㈡埛绔笂涓€甯?Z锛堢敤浜庢覆鏌撴彃鍊硷級 */
    public double getPrevZ() { return this.prevZ; }

    public float getShieldBlood() {
        return this.entityData.get(SHIELD_BLOOD);
    }

    public void setShieldBlood(float blood) {
        this.entityData.set(SHIELD_BLOOD, blood);
    }

    /** 灏嗗綋鍓嶆姢鐩捐€愪箙鍐欏叆鐜╁ HUD capability銆?*/
    private void updateHud() {
        if (this.thrower instanceof Player player) {
            int blood = Math.round(this.getShieldBlood());
            player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                    .ifPresent(cap -> cap.set("raikiri_shield_blood", blood));
        }
    }

    /** 鎶ょ浘娑堝け鏃舵竻闄?HUD 鏄剧ず銆?*/
    private void clearHud() {
        if (this.thrower instanceof Player player) {
            player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                    .ifPresent(cap -> cap.set("raikiri_shield_blood", 0));
        }
    }

    /**
     * 鎶ょ浘鎶典激 鈥斺€?涓€姣斾竴澶嶅埢 1.12.2 {@code WorldEvent#onLivingHurt} 鐨勬姢鐩鹃€昏緫锛?     * 鎸佹湁鑰呭彈鍒扮殑浼ゅ浼樺厛鐢卞叾鎶ょ浘鐨勮€愪箙鍚告敹锛岃€楀敖鍒欐妸鍓╀綑浼ゅ閫忎紶銆?     */
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
        if (tag.contains("bladeAttack")) {
            this.bladeAttack = tag.getFloat("bladeAttack");
        }
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        tag.putFloat("bladeAttack", this.bladeAttack);
        tag.putFloat("shieldBlood", this.getShieldBlood());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

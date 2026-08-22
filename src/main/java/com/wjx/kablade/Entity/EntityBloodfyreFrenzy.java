package com.wjx.kablade.Entity;

import com.wjx.kablade.util.TargetingUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 魂妖刀「血樱寂灭」SA 的同步锚点、时间轴与服务端伤害逻辑。 */
public class EntityBloodfyreFrenzy extends Entity {
    public static final int LIFETIME_TICKS = 52;

    private static final DataParameter<Integer> OWNER_ID =
            EntityDataManager.createKey(EntityBloodfyreFrenzy.class, DataSerializers.VARINT);
    private static final DataParameter<Float> BASE_DAMAGE =
            EntityDataManager.createKey(EntityBloodfyreFrenzy.class, DataSerializers.FLOAT);
    private static final double SCAN_RADIUS = 13.0D;
    /** 逻辑本体 ID -> 实际伤害接收部位 ID，避免 multipart Boss 的持续伤害改打本体。 */
    private final Map<Integer, Integer> burningTargets = new HashMap<>();

    public EntityBloodfyreFrenzy(World worldIn) {
        super(worldIn);
        this.setSize(0.2F, 0.2F);
        this.noClip = true;
        this.setNoGravity(true);
        this.isImmuneToFire = true;
        this.ignoreFrustumCheck = true;
    }

    public static EntityBloodfyreFrenzy spawn(World world, EntityLivingBase owner, float damage) {
        EntityBloodfyreFrenzy frenzy = new EntityBloodfyreFrenzy(world);
        frenzy.dataManager.set(OWNER_ID, owner.getEntityId());
        frenzy.dataManager.set(BASE_DAMAGE, Math.max(0.0F, damage));
        frenzy.rotationYaw = owner.rotationYaw;
        frenzy.prevRotationYaw = owner.prevRotationYaw;
        frenzy.setPosition(owner.posX, owner.posY, owner.posZ);
        world.spawnEntity(frenzy);
        return frenzy;
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(OWNER_ID, -1);
        this.dataManager.register(BASE_DAMAGE, 1.0F);
    }

    public EntityLivingBase getOwner() {
        Entity entity = this.world.getEntityByID(this.dataManager.get(OWNER_ID));
        return entity instanceof EntityLivingBase ? (EntityLivingBase) entity : null;
    }

    public float getBaseDamage() {
        return this.dataManager.get(BASE_DAMAGE);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.world.isRemote) return;

        EntityLivingBase owner = getOwner();
        if (owner == null || !owner.isEntityAlive()) {
            this.setDead();
            return;
        }
        // 两段回旋斩跟随施术者；终结斩开始后固定落点。
        if (this.ticksExisted <= 8) {
            this.setPosition(owner.posX, owner.posY, owner.posZ);
        }

        WorldServer server = (WorldServer) this.world;
        playTimelineSounds(server);
        applyTimelineDamage(owner);
        if (this.ticksExisted >= LIFETIME_TICKS) this.setDead();
    }

    private void playTimelineSounds(WorldServer server) {
        if (this.ticksExisted == 4 || this.ticksExisted == 7) {
            float pitch = this.ticksExisted == 4 ? 1.18F : 0.92F;
            server.playSound(null, this.posX, this.posY + 1.1D, this.posZ,
                    SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0F, pitch);
            server.playSound(null, this.posX, this.posY + 1.1D, this.posZ,
                    SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.62F, pitch + 0.32F);
        } else if (this.ticksExisted == 9) {
            server.playSound(null, this.posX, this.posY + 1.0D, this.posZ,
                    SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.15F, 0.68F);
        } else if (this.ticksExisted == 11) {
            Vec3d center = getPositionVector().add(flatForward().scale(5.0D));
            server.playSound(null, center.x, center.y + 1.0D, center.z,
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.55F, 0.72F);
            server.playSound(null, center.x, center.y + 1.0D, center.z,
                    SoundEvents.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 0.88F, 1.42F);
        } else if (this.ticksExisted == 40) {
            server.playSound(null, this.posX, this.posY + 0.8D, this.posZ,
                    SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.34F, 0.58F);
        }
    }

    private void applyTimelineDamage(EntityLivingBase owner) {
        if (this.ticksExisted == 4) {
            hitSpin(owner, 3.2D, getBaseDamage() * 0.12F, 0.08D);
        } else if (this.ticksExisted == 7) {
            hitSpin(owner, 4.4D, getBaseDamage() * 0.14F, 0.13D);
        } else if (this.ticksExisted == 9) {
            hitForward(owner, getBaseDamage() * 0.20F, false);
        } else if (this.ticksExisted == 11) {
            hitForward(owner, getBaseDamage() * 0.38F, true);
        } else if (this.ticksExisted == 15 || this.ticksExisted == 19 || this.ticksExisted == 23) {
            hitBurning(owner, getBaseDamage() * 0.053F);
        }
    }

    private void hitSpin(EntityLivingBase owner, double radius, float damage, double lift) {
        Vec3d origin = getPositionVector().add(0.0D, 1.0D, 0.0D);
        for (Entity receiver : nearbyTargets(owner)) {
            EntityLivingBase target = TargetingUtil.getSelectionTarget(receiver);
            if (target == null) continue;
            Vec3d hitPoint = TargetingUtil.getClosestPointOnDamageBounds(receiver, origin);
            Vec3d offset = hitPoint.subtract(origin);
            if (Math.abs(offset.y) > 2.7D || offset.x * offset.x + offset.z * offset.z > radius * radius) continue;
            if (hurt(receiver, owner, damage)) {
                Vec3d pull = origin.subtract(hitPoint);
                pull = pull.x * pull.x + pull.z * pull.z > 1.0E-5D
                        ? new Vec3d(pull.x, 0.0D, pull.z).normalize().scale(0.10D) : Vec3d.ZERO;
                target.motionX = target.motionX * 0.60D + pull.x;
                target.motionY = target.motionY * 0.60D + pull.y + lift;
                target.motionZ = target.motionZ * 0.60D + pull.z;
                target.velocityChanged = true;
            }
        }
    }

    private void hitForward(EntityLivingBase owner, float damage, boolean finisher) {
        Vec3d origin = getPositionVector().add(0.0D, 1.0D, 0.0D);
        Vec3d forward = flatForward();
        Vec3d right = new Vec3d(-forward.z, 0.0D, forward.x);
        for (Entity receiver : nearbyTargets(owner)) {
            EntityLivingBase target = TargetingUtil.getSelectionTarget(receiver);
            if (target == null) continue;
            // 范围判断必须针对实际命中的部位；九头蛇头部等 multipart 会折算到远处的本体。
            Vec3d offset = TargetingUtil.getClosestPointOnDamageBounds(receiver, origin).subtract(origin);
            double ahead = offset.dotProduct(forward);
            double side = Math.abs(offset.dotProduct(right));
            double width = finisher ? 2.6D + Math.max(0.0D, ahead) * 0.48D
                    : 1.35D + Math.max(0.0D, ahead) * 0.32D;
            double reach = finisher ? 11.5D : 9.0D;
            if (ahead < -0.8D || ahead > reach || side > width || offset.y < -1.25D || offset.y > 4.5D) continue;
            float falloff = MathHelper.clamp((float) (1.0D - Math.max(0.0D, ahead - 2.0D) / 22.0D), 0.62F, 1.0F);
            if (hurt(receiver, owner, damage * falloff)) {
                this.burningTargets.put(target.getEntityId(), receiver.getEntityId());
                double push = finisher ? 0.72D : 0.28D;
                target.motionX = target.motionX * (finisher ? 0.38D : 0.55D) + forward.x * push;
                target.motionY = target.motionY * (finisher ? 0.38D : 0.55D) + (finisher ? 0.24D : 0.12D);
                target.motionZ = target.motionZ * (finisher ? 0.38D : 0.55D) + forward.z * push;
                target.velocityChanged = true;
            }
        }
    }

    private void hitBurning(EntityLivingBase owner, float damage) {
        for (Map.Entry<Integer, Integer> entry : new HashMap<>(this.burningTargets).entrySet()) {
            Entity receiver = this.world.getEntityByID(entry.getValue());
            EntityLivingBase logical = TargetingUtil.getSelectionTarget(receiver);
            if (receiver == null || logical == null || !logical.isEntityAlive()
                    || logical.getEntityId() != entry.getKey()
                    || getDistanceSq(receiver) > SCAN_RADIUS * SCAN_RADIUS * 2.0D
                    || !TargetingUtil.canSelectForDamage(owner, receiver)) {
                this.burningTargets.remove(entry.getKey());
                continue;
            }
            hurt(receiver, owner, damage);
            logical.setFire(2);
        }
    }

    private List<Entity> nearbyTargets(EntityLivingBase owner) {
        AxisAlignedBB area = new AxisAlignedBB(this.posX - SCAN_RADIUS, this.posY - 1.25D,
                this.posZ - SCAN_RADIUS, this.posX + SCAN_RADIUS, this.posY + 4.5D,
                this.posZ + SCAN_RADIUS);
        List<Entity> found = this.world.getEntitiesInAABBexcluding(this, area,
                entity -> TargetingUtil.canSelectForDamage(owner, entity));
        return TargetingUtil.getDistinctDamageTargets(found);
    }

    private static boolean hurt(Entity receiver, EntityLivingBase owner, float damage) {
        DamageSource source = owner instanceof EntityPlayer
                ? DamageSource.causePlayerDamage((EntityPlayer) owner) : DamageSource.causeMobDamage(owner);
        EntityLivingBase logical = TargetingUtil.getSelectionTarget(receiver);
        if (logical != null) logical.hurtResistantTime = 0;
        return receiver.attackEntityFrom(source, damage);
    }

    private Vec3d flatForward() {
        float yaw = this.rotationYaw * 0.017453292F;
        return new Vec3d(-MathHelper.sin(yaw), 0.0D, MathHelper.cos(yaw)).normalize();
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return this.getEntityBoundingBox().grow(18.0D, 8.0D, 18.0D);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        this.dataManager.set(OWNER_ID, tag.getInteger("OwnerId"));
        this.dataManager.set(BASE_DAMAGE, tag.getFloat("BaseDamage"));
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        tag.setInteger("OwnerId", this.dataManager.get(OWNER_ID));
        tag.setFloat("BaseDamage", getBaseDamage());
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }
}

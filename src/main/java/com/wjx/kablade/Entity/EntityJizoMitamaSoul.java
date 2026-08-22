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
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 地藏御魂的 66 tick 服务端时间轴与客户端渲染锚点。 */
public class EntityJizoMitamaSoul extends Entity {
    public static final int LIFETIME = 66;
    public static final int ASCEND_TICK = 36;
    public static final int SLAM_TICK = 46;
    public static final int IMPACT_TICK = 58;

    private static final double ATTACK_REACH = 16.0D;
    private static final double ATTACK_BASE_HALF_WIDTH = 4.8D;
    private static final double ATTACK_DOWN_REACH = 3.0D;
    private static final double ATTACK_UP_REACH = 7.0D;
    private static final double IMPACT_DISTANCE = 10.0D;

    private static final DataParameter<Integer> OWNER_ID = EntityDataManager.createKey(
            EntityJizoMitamaSoul.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> START_LOW = EntityDataManager.createKey(
            EntityJizoMitamaSoul.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> START_HIGH = EntityDataManager.createKey(
            EntityJizoMitamaSoul.class, DataSerializers.VARINT);
    private static final Set<UUID> ACTIVE_CASTERS = Collections.newSetFromMap(
            new ConcurrentHashMap<UUID, Boolean>());

    private UUID ownerUuid;
    private EntityPlayer owner;
    private float damage;
    private boolean impacted;
    private boolean releasedCaster;

    public EntityJizoMitamaSoul(World worldIn) {
        super(worldIn);
        this.noClip = true;
        this.setNoGravity(true);
        this.setSize(1.0F, 1.0F);
    }

    public static EntityJizoMitamaSoul spawn(World world, EntityPlayer owner, float damage) {
        if (world.isRemote || !ACTIVE_CASTERS.add(owner.getUniqueID())) {
            return null;
        }

        Vec3d forward = flatForward(owner.rotationYaw);
        Vec3d origin = owner.getPositionVector().subtract(forward.scale(1.25D));
        EntityJizoMitamaSoul entity = new EntityJizoMitamaSoul(world);
        entity.owner = owner;
        entity.ownerUuid = owner.getUniqueID();
        entity.damage = Math.max(1.0F, damage);
        entity.dataManager.set(OWNER_ID, owner.getEntityId());
        entity.setStartWorldTime(world.getTotalWorldTime());
        entity.setPosition(origin.x, origin.y, origin.z);
        entity.rotationYaw = owner.rotationYaw;
        entity.prevRotationYaw = owner.rotationYaw;
        if (!world.spawnEntity(entity)) {
            ACTIVE_CASTERS.remove(owner.getUniqueID());
            return null;
        }
        return entity;
    }

    public static boolean isCasting(EntityPlayer player) {
        return player != null && ACTIVE_CASTERS.contains(player.getUniqueID());
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(OWNER_ID, -1);
        this.dataManager.register(START_LOW, 0);
        this.dataManager.register(START_HIGH, 0);
    }

    private void setStartWorldTime(long time) {
        this.dataManager.set(START_LOW, (int) time);
        this.dataManager.set(START_HIGH, (int) (time >>> 32));
    }

    private long getStartWorldTime() {
        return ((long) this.dataManager.get(START_HIGH) << 32)
                | (this.dataManager.get(START_LOW) & 0xFFFFFFFFL);
    }

    public float getRenderAge(float partialTicks) {
        long start = getStartWorldTime();
        float age = start == 0L
                ? this.ticksExisted + partialTicks
                : (float) (this.world.getTotalWorldTime() + partialTicks - start);
        return MathHelper.clamp(age, 0.0F, LIFETIME);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        this.motionX = 0.0D;
        this.motionY = 0.0D;
        this.motionZ = 0.0D;
        if (this.world.isRemote) {
            return;
        }

        EntityPlayer caster = resolveOwner();
        if (caster == null || !caster.isEntityAlive()) {
            this.setDead();
            return;
        }

        WorldServer server = (WorldServer) this.world;
        playTimelineEffects(server);
        if (!this.impacted && this.ticksExisted >= IMPACT_TICK) {
            this.impacted = true;
            resolveSlam(server, caster);
        }
        if (this.ticksExisted >= LIFETIME) {
            this.setDead();
        }
    }

    private EntityPlayer resolveOwner() {
        if (this.owner != null && this.owner.isEntityAlive()) {
            return this.owner;
        }
        Entity byId = this.world.getEntityByID(this.dataManager.get(OWNER_ID));
        if (byId instanceof EntityPlayer
                && (this.ownerUuid == null || this.ownerUuid.equals(byId.getUniqueID()))) {
            this.owner = (EntityPlayer) byId;
            this.ownerUuid = byId.getUniqueID();
            return this.owner;
        }
        if (this.world instanceof WorldServer && this.ownerUuid != null) {
            Entity byUuid = ((WorldServer) this.world).getEntityFromUuid(this.ownerUuid);
            if (byUuid instanceof EntityPlayer) {
                this.owner = (EntityPlayer) byUuid;
                this.dataManager.set(OWNER_ID, byUuid.getEntityId());
                return this.owner;
            }
        }
        return null;
    }

    private void resolveSlam(WorldServer server, EntityPlayer caster) {
        Vec3d forward = flatForward(this.rotationYaw);
        Vec3d right = new Vec3d(-forward.z, 0.0D, forward.x);
        Vec3d origin = this.getPositionVector().add(0.0D, 1.0D, 0.0D);
        AxisAlignedBB scan = this.getEntityBoundingBox().grow(18.0D, 7.0D, 18.0D);
        List<Entity> rawTargets = this.world.getEntitiesInAABBexcluding(
                this, scan, target -> TargetingUtil.canSelectForDamage(caster, target));

        for (Entity receiver : TargetingUtil.getDistinctDamageTargets(rawTargets)) {
            EntityLivingBase target = TargetingUtil.getSelectionTarget(receiver);
            if (target == null) {
                continue;
            }
            Vec3d center = receiver.getEntityBoundingBox().getCenter();
            Vec3d offset = center.subtract(origin);
            double ahead = offset.dotProduct(forward);
            double side = Math.abs(offset.dotProduct(right));
            if (ahead < -1.0D || ahead > ATTACK_REACH
                    || side > ATTACK_BASE_HALF_WIDTH + ahead * 0.18D
                    || offset.y < -ATTACK_DOWN_REACH || offset.y > ATTACK_UP_REACH) {
                continue;
            }

            target.hurtTime = 0;
            target.hurtResistantTime = 0;
            receiver.hurtResistantTime = 0;
            boolean hurt = receiver.attackEntityFrom(
                    DamageSource.causePlayerDamage(caster).setDamageBypassesArmor(), this.damage);
            target.hurtTime = 0;
            target.hurtResistantTime = 0;
            receiver.hurtResistantTime = 0;
            if (!hurt) {
                continue;
            }
            target.motionX = target.motionX * 0.35D + forward.x * 0.55D;
            target.motionY = target.motionY * 0.35D - 0.18D;
            target.motionZ = target.motionZ * 0.35D + forward.z * 0.55D;
            target.velocityChanged = true;
        }

        Vec3d impact = this.getPositionVector().add(forward.scale(IMPACT_DISTANCE))
                .add(0.0D, 0.35D, 0.0D);
        server.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE,
                impact.x, impact.y + 0.7D, impact.z,
                2, 0.25D, 0.25D, 0.25D, 0.0D);
        server.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                impact.x, impact.y, impact.z,
                8, 2.4D, 0.35D, 2.4D, 0.08D);
        server.spawnParticle(EnumParticleTypes.REDSTONE,
                impact.x, impact.y, impact.z,
                110, 4.4D, 0.65D, 4.4D, 0.12D);
        server.spawnParticle(EnumParticleTypes.FLAME,
                impact.x, impact.y, impact.z,
                68, 4.0D, 0.55D, 4.0D, 0.10D);
    }

    private void playTimelineEffects(WorldServer server) {
        if (this.ticksExisted == 1) {
            // 1.12.2 没有 Soul Escape 与重生锚充能声，使用同生态中音色最接近的传送/末地充能声。
            this.world.playSound(null, this.posX, this.posY + 1.2D, this.posZ,
                    SoundEvents.ENTITY_ENDERMEN_TELEPORT,
                    SoundCategory.PLAYERS, 1.5F, 0.58F);
            this.world.playSound(null, this.posX, this.posY + 1.2D, this.posZ,
                    SoundEvents.BLOCK_END_PORTAL_FRAME_FILL,
                    SoundCategory.PLAYERS, 1.0F, 0.72F);
            server.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    this.posX, this.posY + 1.2D, this.posZ,
                    38, 1.3D, 1.6D, 1.3D, 0.025D);
            server.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                    this.posX, this.posY + 1.0D, this.posZ,
                    26, 1.15D, 1.35D, 1.15D, 0.045D);
            server.spawnParticle(EnumParticleTypes.REDSTONE,
                    this.posX, this.posY + 1.2D, this.posZ,
                    44, 1.3D, 1.6D, 1.3D, 0.04D);
        } else if (this.ticksExisted == ASCEND_TICK) {
            this.world.playSound(null, this.posX, this.posY + 2.0D, this.posZ,
                    SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                    SoundCategory.PLAYERS, 0.9F, 0.55F);
        } else if (this.ticksExisted == SLAM_TICK) {
            this.world.playSound(null, this.posX, this.posY + 2.2D, this.posZ,
                    SoundEvents.ENTITY_WITHER_SHOOT,
                    SoundCategory.PLAYERS, 1.15F, 0.52F);
            // 1.12.2 没有激流 III 声效，以低沉的龙翼掠空保留挥砍的风压层。
            this.world.playSound(null, this.posX, this.posY + 2.2D, this.posZ,
                    SoundEvents.ENTITY_ENDERDRAGON_FLAP,
                    SoundCategory.PLAYERS, 1.0F, 0.62F);
        } else if (this.ticksExisted == IMPACT_TICK) {
            Vec3d impact = this.getPositionVector()
                    .add(flatForward(this.rotationYaw).scale(IMPACT_DISTANCE));
            this.world.playSound(null, impact.x, impact.y, impact.z,
                    SoundEvents.ENTITY_GENERIC_EXPLODE,
                    SoundCategory.PLAYERS, 1.8F, 0.62F);
            this.world.playSound(null, impact.x, impact.y, impact.z,
                    SoundEvents.ENTITY_WITHER_BREAK_BLOCK,
                    SoundCategory.PLAYERS, 1.0F, 0.72F);
        }
    }

    private static Vec3d flatForward(float yaw) {
        float radians = yaw * 0.017453292F;
        return new Vec3d(-MathHelper.sin(radians), 0.0D, MathHelper.cos(radians)).normalize();
    }

    @Override
    public void setDead() {
        releaseCaster();
        super.setDead();
    }

    private void releaseCaster() {
        if (!this.releasedCaster && this.ownerUuid != null) {
            ACTIVE_CASTERS.remove(this.ownerUuid);
            this.releasedCaster = true;
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        if (compound.hasUniqueId("Owner")) {
            this.ownerUuid = compound.getUniqueId("Owner");
            if (!this.world.isRemote) {
                ACTIVE_CASTERS.add(this.ownerUuid);
            }
        }
        this.damage = compound.getFloat("Damage");
        this.impacted = compound.getBoolean("Impacted");
        if (compound.hasKey("StartTime")) {
            setStartWorldTime(compound.getLong("StartTime"));
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        if (this.ownerUuid != null) {
            compound.setUniqueId("Owner", this.ownerUuid);
        }
        compound.setFloat("Damage", this.damage);
        compound.setBoolean("Impacted", this.impacted);
        compound.setLong("StartTime", getStartWorldTime());
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return this.getEntityBoundingBox().grow(24.0D, 8.0D, 24.0D);
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }
}

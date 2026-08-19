package com.wjx.kablade.Entity;

import com.wjx.kablade.util.TargetingUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.registry.IThrowableEntity;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 时空黑洞实体（Vorpal Hole）。
 * <p>
 * 战斗与渲染双重锚点：
 * 前 56 tick 持续平滑吸引周围敌人，并在 14/19/24 tick 分阶段射出交错斩击飞刃；
 * 第 16 tick 触发 80% 开场斩与上挑击飞；
 * 第 24~44 tick 连续释放 6 次 20% 能量脉冲；
 * 第 56 tick 坍缩爆发，视觉余韵结束后销毁。
 */
public class EntityVorpalBlackHole extends Entity implements IThrowableEntity {

    private static final DataParameter<Integer> DATA_LIFETIME =
            EntityDataManager.createKey(EntityVorpalBlackHole.class, DataSerializers.VARINT);
    private static final DataParameter<Float> DATA_OPENING_DAMAGE =
            EntityDataManager.createKey(EntityVorpalBlackHole.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> DATA_PULSE_DAMAGE =
            EntityDataManager.createKey(EntityVorpalBlackHole.class, DataSerializers.FLOAT);
    private static final DataParameter<Integer> DATA_OWNER_ID =
            EntityDataManager.createKey(EntityVorpalBlackHole.class, DataSerializers.VARINT);

    private static final int OPENING_TICK = 16;
    private static final int FIRST_PULSE_TICK = 24;
    private static final int PULSE_INTERVAL = 4;
    private static final int PULSE_COUNT = 6;
    private static final int VISUAL_AFTER_TICKS = 26;

    private static final double PULL_RADIUS = 14.5;
    private static final double PULL_VERTICAL_RADIUS = 8.0;
    private static final double DAMAGE_RADIUS = 6.25;
    private static final double DAMAGE_VERTICAL_RADIUS = 3.4;

    private EntityLivingBase owner;
    private UUID ownerUUID;
    private final Set<Integer> openingHit = new HashSet<>();

    public EntityVorpalBlackHole(World worldIn) {
        super(worldIn);
        this.noClip = true;
        this.setSize(0.5F, 0.5F);
    }

    public EntityVorpalBlackHole(World worldIn, EntityLivingBase owner, double x, double y, double z,
                                 int lifetime, float openingDamage, float pulseDamage) {
        this(worldIn);
        this.owner = owner;
        this.ownerUUID = owner.getUniqueID();
        this.dataManager.set(DATA_OWNER_ID, owner.getEntityId());
        this.setPosition(x, y, z);
        this.prevRotationYaw = this.rotationYaw = owner.rotationYaw;
        this.prevRotationPitch = this.rotationPitch = owner.rotationPitch;
        this.setLifetime(lifetime);
        this.setOpeningDamage(openingDamage);
        this.setPulseDamage(pulseDamage);
    }

    public static EntityVorpalBlackHole spawn(World world, EntityLivingBase owner, Vec3d pos,
                                              int lifetime, float openingDamage, float pulseDamage) {
        EntityVorpalBlackHole hole = new EntityVorpalBlackHole(world, owner, pos.x, pos.y, pos.z,
                lifetime, openingDamage, pulseDamage);
        world.spawnEntity(hole);
        return hole;
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(DATA_LIFETIME, 56);
        this.dataManager.register(DATA_OPENING_DAMAGE, 0.0F);
        this.dataManager.register(DATA_PULSE_DAMAGE, 0.0F);
        this.dataManager.register(DATA_OWNER_ID, -1);
    }

    public int getLifetime() {
        return this.dataManager.get(DATA_LIFETIME);
    }

    public int getVisualLifetime() {
        return this.getLifetime() + VISUAL_AFTER_TICKS;
    }

    public void setLifetime(int lifetime) {
        this.dataManager.set(DATA_LIFETIME, Math.max(1, lifetime));
    }

    public float getOpeningDamage() {
        return this.dataManager.get(DATA_OPENING_DAMAGE);
    }

    public void setOpeningDamage(float damage) {
        this.dataManager.set(DATA_OPENING_DAMAGE, Math.max(0.0F, damage));
    }

    public float getPulseDamage() {
        return this.dataManager.get(DATA_PULSE_DAMAGE);
    }

    public void setPulseDamage(float damage) {
        this.dataManager.set(DATA_PULSE_DAMAGE, Math.max(0.0F, damage));
    }

    @Nullable
    public EntityLivingBase getOwnerEntity() {
        if (this.owner != null && !this.owner.isDead) {
            return this.owner;
        }
        int id = this.dataManager.get(DATA_OWNER_ID);
        if (id != -1) {
            Entity entity = this.world.getEntityByID(id);
            if (entity instanceof EntityLivingBase) {
                this.owner = (EntityLivingBase) entity;
                return this.owner;
            }
        }
        return null;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (this.world.isRemote) {
            if (this.ticksExisted >= this.getVisualLifetime()) {
                this.setDead();
            }
            return;
        }

        resolveOwner();

        if (this.ticksExisted < this.getLifetime()) {
            pullNearbyEntities();
        }

        // 1.20 原版分阶段飞刃发射演出（14, 19, 24 tick）
        if (this.ticksExisted == 14) {
            spawnCutDrive(0);
        } else if (this.ticksExisted == 19) {
            spawnCutDrive(1);
        } else if (this.ticksExisted == 24) {
            spawnCutDrive(2);
        }

        if (this.ticksExisted == OPENING_TICK) {
            openingCut();
        }

        int pulseIndex = (this.ticksExisted - FIRST_PULSE_TICK) / PULSE_INTERVAL;
        if (pulseIndex >= 0
                && pulseIndex < PULSE_COUNT
                && (this.ticksExisted - FIRST_PULSE_TICK) % PULSE_INTERVAL == 0) {
            energyPulse(pulseIndex);
        }

        if (this.ticksExisted == this.getLifetime()) {
            collapseFx();
        }

        if (this.ticksExisted >= this.getVisualLifetime()) {
            this.setDead();
        }
    }

    private void resolveOwner() {
        if (this.owner == null && this.ownerUUID != null && this.world instanceof WorldServer) {
            Entity entity = ((WorldServer) this.world).getEntityFromUuid(this.ownerUUID);
            if (entity instanceof EntityLivingBase) {
                this.owner = (EntityLivingBase) entity;
                this.dataManager.set(DATA_OWNER_ID, entity.getEntityId());
            }
        }
    }

    private void spawnCutDrive(int index) {
        if (this.owner == null) return;
        Vec3d look = this.owner.getLookVec();
        Vec3d flatLook = new Vec3d(look.x, 0.0, look.z);
        if (flatLook.lengthSquared() < 1.0e-6) {
            flatLook = new Vec3d(0.0, 0.0, 1.0);
        } else {
            flatLook = flatLook.normalize();
        }

        Vec3d side = new Vec3d(-flatLook.z, 0.0, flatLook.x);
        Vec3d base = new Vec3d(this.posX, this.posY - 0.15, this.posZ);
        Vec3d dir = side.scale(index == 1 ? -1.0 : 1.0).add(flatLook.scale(0.18)).normalize();
        Vec3d spawnPos = base.add(flatLook.scale(0.35 * index)).add(new Vec3d(0.0, (index - 1) * 0.18, 0.0));

        EntityDriveAdd drive = new EntityDriveAdd(this.world, this.owner, this.getOpeningDamage() * 0.28F, false, index == 1 ? -24.0F : 18.0F);
        drive.setPosition(spawnPos.x, spawnPos.y, spawnPos.z);
        drive.setInitialSpeed(0.72F);
        drive.setColor(0xFF1E24);
        drive.setLifeTime(11);
        this.world.spawnEntity(drive);

        if (this.owner instanceof EntityPlayer) {
            ((EntityPlayer) this.owner).swingArm(EnumHand.MAIN_HAND);
        }
    }

    private void pullNearbyEntities() {
        AxisAlignedBB bounds = new AxisAlignedBB(
                this.posX - PULL_RADIUS, this.posY - PULL_VERTICAL_RADIUS, this.posZ - PULL_RADIUS,
                this.posX + PULL_RADIUS, this.posY + PULL_VERTICAL_RADIUS, this.posZ + PULL_RADIUS);
        List<Entity> list = this.world.getEntitiesWithinAABB(Entity.class, bounds, this::canPull);

        for (Entity e : TargetingUtil.getDistinctDamageTargets(list)) {
            EntityLivingBase selection = TargetingUtil.getSelectionTarget(e);
            Entity target = selection != null ? selection : e;
            Vec3d targetCenter = new Vec3d(target.posX, target.posY + target.height * 0.52, target.posZ);
            double dx = this.posX - targetCenter.x;
            double dy = this.posY - targetCenter.y;
            double dz = this.posZ - targetCenter.z;
            double distanceSq = dx * dx + dy * dy + dz * dz;
            double distance = Math.max(0.35, Math.sqrt(distanceSq));
            if (distance > PULL_RADIUS) {
                continue;
            }

            double ease = 1.0 - MathHelper.clamp((float) (distance / PULL_RADIUS), 0.0F, 1.0F);
            double strength = 0.16 + ease * 0.46;
            double invDist = 1.0 / distance;
            double px = dx * invDist * strength;
            double py = dy * invDist * strength + (target instanceof EntityLivingBase ? 0.025 + ease * 0.035 : 0.0);
            double pz = dz * invDist * strength;

            target.motionX = target.motionX * 0.28 + px;
            target.motionY = target.motionY * 0.28 + py;
            target.motionZ = target.motionZ * 0.28 + pz;
            target.velocityChanged = true;
            syncVelocity(target);
        }
    }

    private void openingCut() {
        DamageSource source = damageSource();
        AxisAlignedBB bounds = new AxisAlignedBB(
                this.posX - DAMAGE_RADIUS, this.posY - DAMAGE_VERTICAL_RADIUS, this.posZ - DAMAGE_RADIUS,
                this.posX + DAMAGE_RADIUS, this.posY + DAMAGE_VERTICAL_RADIUS, this.posZ + DAMAGE_RADIUS);
        List<Entity> list = this.world.getEntitiesWithinAABB(Entity.class, bounds, this::canDamage);

        for (Entity e : TargetingUtil.getDistinctDamageTargets(list)) {
            EntityLivingBase selection = TargetingUtil.getSelectionTarget(e);
            if (selection != null) {
                if (!this.openingHit.add(selection.getEntityId())) {
                    continue;
                }
                selection.hurtResistantTime = 0;
                if (selection.attackEntityFrom(source, this.getOpeningDamage())) {
                    double pullX = (this.posX - selection.posX) * 0.08;
                    double pullZ = (this.posZ - selection.posZ) * 0.08;
                    selection.motionX = pullX;
                    selection.motionY = 0.28;
                    selection.motionZ = pullZ;
                    selection.velocityChanged = true;
                    syncVelocity(selection);
                }
            }
        }

        pulseFx(0, 1.0F);
        this.world.playSound(null, this.posX, this.posY, this.posZ,
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.35F, 0.58F);
        this.world.playSound(null, this.posX, this.posY, this.posZ,
                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.52F, 0.82F);
    }

    private void energyPulse(int pulseIndex) {
        DamageSource source = damageSource();
        double radius = DAMAGE_RADIUS * (0.88 + pulseIndex * 0.025);
        AxisAlignedBB bounds = new AxisAlignedBB(
                this.posX - radius, this.posY - DAMAGE_VERTICAL_RADIUS, this.posZ - radius,
                this.posX + radius, this.posY + DAMAGE_VERTICAL_RADIUS, this.posZ + radius);
        List<Entity> list = this.world.getEntitiesWithinAABB(Entity.class, bounds, this::canDamage);

        for (Entity e : TargetingUtil.getDistinctDamageTargets(list)) {
            EntityLivingBase selection = TargetingUtil.getSelectionTarget(e);
            if (selection != null) {
                selection.hurtResistantTime = 0;
                selection.attackEntityFrom(source, this.getPulseDamage());
                double dx = this.posX - selection.posX;
                double dy = this.posY - selection.posY;
                double dz = this.posZ - selection.posZ;
                double distSq = dx * dx + dy * dy + dz * dz;
                if (distSq > 1.0E-5) {
                    double dist = Math.sqrt(distSq);
                    double invDist = 1.0 / dist;
                    double strength = 0.16 + pulseIndex * 0.018;
                    selection.motionX = selection.motionX * 0.18 + dx * invDist * strength;
                    selection.motionY = selection.motionY * 0.18 + dy * invDist * strength;
                    selection.motionZ = selection.motionZ * 0.18 + dz * invDist * strength;
                    selection.velocityChanged = true;
                    syncVelocity(selection);
                }
            }
        }

        pulseFx(pulseIndex, 0.72F);
        this.world.playSound(null, this.posX, this.posY, this.posZ,
                SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS,
                0.85F, 0.72F + pulseIndex * 0.08F);
        this.world.playSound(null, this.posX, this.posY, this.posZ,
                SoundEvents.ENTITY_ENDEREYE_DEATH, SoundCategory.PLAYERS,
                0.55F, 0.95F + pulseIndex * 0.10F);
    }

    private void syncVelocity(Entity entity) {
        if (entity instanceof EntityPlayerMP) {
            ((EntityPlayerMP) entity).connection.sendPacket(new SPacketEntityVelocity(entity));
        }
    }

    private boolean canPull(Entity entity) {
        if (entity == this || entity == this.owner) {
            return false;
        }
        if (entity instanceof IThrowableEntity && ((IThrowableEntity) entity).getThrower() == this.owner) {
            return false;
        }
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            return !player.isCreative() && !player.isSpectator();
        }
        if (entity instanceof EntityLivingBase) {
            return TargetingUtil.canSelectForDamage(this.owner, entity);
        }
        return entity instanceof IThrowableEntity;
    }

    private boolean canDamage(Entity entity) {
        return TargetingUtil.canSelectForDamage(this.owner, entity);
    }

    private DamageSource damageSource() {
        if (this.owner instanceof EntityPlayer) {
            return DamageSource.causePlayerDamage((EntityPlayer) this.owner).setDamageBypassesArmor();
        }
        if (this.owner != null) {
            return DamageSource.causeMobDamage(this.owner).setDamageBypassesArmor();
        }
        return DamageSource.MAGIC.setDamageBypassesArmor();
    }

    private void pulseFx(int pulseIndex, float intensity) {
        if (this.world instanceof WorldServer) {
            ((WorldServer) this.world).spawnParticle(
                    EnumParticleTypes.PORTAL,
                    this.posX, this.posY, this.posZ,
                    (int) (12 * intensity), 0.25, 0.25, 0.25, 0.05);
        }
    }

    private void collapseFx() {
        if (this.world instanceof WorldServer) {
            ((WorldServer) this.world).spawnParticle(
                    EnumParticleTypes.EXPLOSION_LARGE,
                    this.posX, this.posY, this.posZ,
                    2, 0.1, 0.1, 0.1, 0.0);
            ((WorldServer) this.world).spawnParticle(
                    EnumParticleTypes.DRAGON_BREATH,
                    this.posX, this.posY, this.posZ,
                    20, 0.3, 0.3, 0.3, 0.08);
        }
        this.world.playSound(null, this.posX, this.posY, this.posZ,
                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.85F, 1.38F);
        this.world.playSound(null, this.posX, this.posY, this.posZ,
                SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 0.85F, 1.38F);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        if (tag.hasUniqueId("OwnerUUID")) {
            this.ownerUUID = tag.getUniqueId("OwnerUUID");
        }
        if (tag.hasKey("Lifetime")) {
            this.setLifetime(tag.getInteger("Lifetime"));
        }
        if (tag.hasKey("OpeningDamage")) {
            this.setOpeningDamage(tag.getFloat("OpeningDamage"));
        }
        if (tag.hasKey("PulseDamage")) {
            this.setPulseDamage(tag.getFloat("PulseDamage"));
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        if (this.ownerUUID != null) {
            tag.setUniqueId("OwnerUUID", this.ownerUUID);
        }
        tag.setInteger("Lifetime", this.getLifetime());
        tag.setFloat("OpeningDamage", this.getOpeningDamage());
        tag.setFloat("PulseDamage", this.getPulseDamage());
    }

    @Override
    public void setItemStackToSlot(EntityEquipmentSlot slotIn, ItemStack stack) {
    }

    @Override
    public Entity getThrower() {
        return this.owner;
    }

    @Override
    public void setThrower(Entity entity) {
        if (entity instanceof EntityLivingBase) {
            this.owner = (EntityLivingBase) entity;
            this.ownerUUID = entity.getUniqueID();
            this.dataManager.set(DATA_OWNER_ID, entity.getEntityId());
        }
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }
}

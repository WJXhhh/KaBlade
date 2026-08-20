package com.wjx.kablade.Entity;

import com.wjx.kablade.util.TargetingUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;

/** 御灵刀「寒狱冰天」SA 的同步锚点、时间轴与服务端伤害逻辑。 */
public class EntityUtpalaAura extends Entity {
    public static final int LIFETIME_TICKS = 78;

    private static final DataParameter<Integer> OWNER_ID =
            EntityDataManager.createKey(EntityUtpalaAura.class, DataSerializers.VARINT);
    private static final DataParameter<Float> BASE_DAMAGE =
            EntityDataManager.createKey(EntityUtpalaAura.class, DataSerializers.FLOAT);
    private static final DataParameter<Integer> LIFETIME =
            EntityDataManager.createKey(EntityUtpalaAura.class, DataSerializers.VARINT);

    public EntityUtpalaAura(World worldIn) {
        super(worldIn);
        this.setSize(0.2F, 0.2F);
        this.noClip = true;
        this.setNoGravity(true);
        this.isImmuneToFire = true;
        this.ignoreFrustumCheck = true;
    }

    public static EntityUtpalaAura spawn(World world, EntityLivingBase owner, float damage) {
        EntityUtpalaAura aura = new EntityUtpalaAura(world);
        aura.dataManager.set(OWNER_ID, owner.getEntityId());
        aura.dataManager.set(BASE_DAMAGE, Math.max(0.0F, damage));
        aura.dataManager.set(LIFETIME, LIFETIME_TICKS);
        aura.rotationYaw = owner.rotationYaw;
        aura.prevRotationYaw = aura.rotationYaw;
        aura.setPosition(owner.posX, owner.posY, owner.posZ);
        world.spawnEntity(aura);
        return aura;
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(OWNER_ID, -1);
        this.dataManager.register(BASE_DAMAGE, 1.0F);
        this.dataManager.register(LIFETIME, LIFETIME_TICKS);
    }

    public EntityLivingBase getOwner() {
        Entity owner = this.world.getEntityByID(this.dataManager.get(OWNER_ID));
        return owner instanceof EntityLivingBase ? (EntityLivingBase) owner : null;
    }

    public float getBaseDamage() {
        return this.dataManager.get(BASE_DAMAGE);
    }

    public int getLifetime() {
        return this.dataManager.get(LIFETIME);
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
        if (this.ticksExisted <= 34) {
            this.setPosition(owner.posX, owner.posY, owner.posZ);
            this.prevRotationYaw = this.rotationYaw;
            this.rotationYaw = owner.rotationYaw;
        }

        WorldServer server = (WorldServer) this.world;
        playTimelineSounds(server);
        spawnTimelineParticles(server);
        applyTimelineHits(owner);
        if (this.ticksExisted >= getLifetime()) this.setDead();
    }

    private void playTimelineSounds(WorldServer server) {
        if (this.ticksExisted == 6) {
            server.playSound(null, this.posX, this.posY, this.posZ,
                    SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.75F, 1.65F);
        } else if (this.ticksExisted == 18) {
            server.playSound(null, this.posX, this.posY, this.posZ,
                    SoundEvents.ENTITY_ENDERDRAGON_FLAP, SoundCategory.PLAYERS, 0.72F, 1.48F);
        } else if (this.ticksExisted == 30) {
            server.playSound(null, this.posX, this.posY + 0.8D, this.posZ,
                    SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.08F, 0.82F);
        } else if (this.ticksExisted == 38) {
            server.playSound(null, this.posX, this.posY + 1.0D, this.posZ,
                    SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.08F, 0.76F);
        }
    }

    private void spawnTimelineParticles(WorldServer server) {
        if (this.ticksExisted >= 4 && this.ticksExisted <= 34) {
            float grow = smootherStep(MathHelper.clamp((this.ticksExisted - 4.0F) / 20.0F, 0.0F, 1.0F));
            int count = 5 + MathHelper.floor(grow * 13.0F);
            for (int i = 0; i < count; i++) {
                double angle = this.rand.nextDouble() * Math.PI * 2.0D + this.ticksExisted * 0.34D;
                double radius = 0.55D + grow * 4.25D + this.rand.nextDouble() * 0.35D;
                double y = 0.14D + this.rand.nextDouble() * (0.34D + grow * 2.15D);
                server.spawnParticle(EnumParticleTypes.SPELL_MOB,
                        this.posX + Math.cos(angle) * radius, this.posY + y,
                        this.posZ + Math.sin(angle) * radius, 0,
                        0.12D, 0.72D, 1.0D, 1.0D);
            }
        }
        if (this.ticksExisted >= 24 && this.ticksExisted <= 70 && (this.ticksExisted & 1) == 0) {
            Vec3d forward = flatForward();
            Vec3d right = new Vec3d(-forward.z, 0.0D, forward.x);
            for (int i = 0; i < 3; i++) {
                double ahead = 1.0D + this.rand.nextDouble() * 9.0D;
                double side = (this.rand.nextDouble() - 0.5D) * 5.8D;
                Vec3d pos = getPositionVector().add(forward.scale(ahead)).add(right.scale(side))
                        .add(0.0D, 0.45D + this.rand.nextDouble() * 2.2D, 0.0D);
                server.spawnParticle(EnumParticleTypes.SNOW_SHOVEL, pos.x, pos.y, pos.z,
                        1, 0.02D, 0.04D, 0.02D, 0.025D);
            }
        }
        if (this.ticksExisted == 30) {
            Vec3d burst = getPositionVector().add(flatForward().scale(4.6D)).add(0.0D, 0.6D, 0.0D);
            server.spawnParticle(EnumParticleTypes.SNOWBALL, burst.x, burst.y, burst.z,
                    60, 1.9D, 1.1D, 1.9D, 0.18D);
            server.spawnParticle(EnumParticleTypes.END_ROD, burst.x, burst.y, burst.z,
                    32, 1.35D, 0.8D, 1.35D, 0.10D);
        }
        if (this.ticksExisted == 38 || this.ticksExisted == 42) {
            Vec3d origin = getPositionVector().add(0.0D, 1.1D, 0.0D);
            Vec3d forward = flatForward();
            for (int i = 0; i < 9; i++) {
                Vec3d pos = origin.add(forward.scale(1.3D + i * 1.15D));
                server.spawnParticle(EnumParticleTypes.END_ROD, pos.x, pos.y, pos.z,
                        4, 0.42D + i * 0.04D, 0.18D, 0.42D + i * 0.04D, 0.08D);
            }
        }
    }

    private void applyTimelineHits(EntityLivingBase owner) {
        if (this.ticksExisted == 10 || this.ticksExisted == 16
                || this.ticksExisted == 22 || this.ticksExisted == 28) {
            vortexPulse(owner);
        } else if (this.ticksExisted == 30) {
            forwardHit(owner, 7.6D, 2.8D, getBaseDamage() * 0.74F, 0.36D, 82, 1);
        } else if (this.ticksExisted == 38) {
            forwardHit(owner, 12.5D, 3.7D, getBaseDamage() * 1.06F, 0.62D, 96, 1);
        } else if (this.ticksExisted == 42) {
            forwardHit(owner, 12.5D, 4.4D, getBaseDamage() * 0.42F, 0.34D, 70, 0);
        }
    }

    private void vortexPulse(EntityLivingBase owner) {
        AxisAlignedBB area = new AxisAlignedBB(this.posX - 5.2D, this.posY - 0.2D, this.posZ - 5.2D,
                this.posX + 5.2D, this.posY + 2.8D, this.posZ + 5.2D);
        List<Entity> found = this.world.getEntitiesInAABBexcluding(this, area,
                entity -> TargetingUtil.canSelectForDamage(owner, entity));
        for (Entity receiver : TargetingUtil.getDistinctDamageTargets(found)) {
            EntityLivingBase target = TargetingUtil.getSelectionTarget(receiver);
            if (target == null) continue;
            double dx = target.posX - this.posX;
            double dz = target.posZ - this.posZ;
            if (dx * dx + dz * dz > 27.04D) continue;
            hurt(receiver, owner, getBaseDamage() * 0.16F);
            target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 45, 2));
            Vec3d pull = new Vec3d(this.posX - target.posX, 0.0D, this.posZ - target.posZ);
            if (pull.lengthSquared() > 1.0E-6D) pull = pull.normalize().scale(0.14D);
            target.motionX = target.motionX * 0.58D + pull.x;
            target.motionY = target.motionY * 0.58D + 0.055D;
            target.motionZ = target.motionZ * 0.58D + pull.z;
            target.velocityChanged = true;
        }
    }

    private void forwardHit(EntityLivingBase owner, double range, double halfWidth,
                            float damage, double knockback, int slowTicks, int slowAmplifier) {
        Vec3d forward = flatForward();
        Vec3d right = new Vec3d(-forward.z, 0.0D, forward.x);
        Vec3d origin = getPositionVector().add(0.0D, 1.0D, 0.0D);
        AxisAlignedBB area = new AxisAlignedBB(this.posX - range, this.posY - 1.0D, this.posZ - range,
                this.posX + range, this.posY + 4.0D, this.posZ + range);
        List<Entity> found = this.world.getEntitiesInAABBexcluding(this, area,
                entity -> TargetingUtil.canSelectForDamage(owner, entity));
        for (Entity receiver : TargetingUtil.getDistinctDamageTargets(found)) {
            EntityLivingBase target = TargetingUtil.getSelectionTarget(receiver);
            if (target == null) continue;
            Vec3d rel = target.getPositionVector().add(0.0D, target.height * 0.5D, 0.0D).subtract(origin);
            double ahead = rel.dotProduct(forward);
            if (ahead < 0.25D || ahead > range || Math.abs(rel.dotProduct(right)) > halfWidth
                    || Math.abs(rel.y) > 3.0D) continue;
            hurt(receiver, owner, damage);
            target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, slowTicks,
                    Math.max(1, slowAmplifier + 2)));
            target.motionX = target.motionX * 0.35D + forward.x * knockback;
            target.motionY = target.motionY * 0.35D + 0.10D;
            target.motionZ = target.motionZ * 0.35D + forward.z * knockback;
            target.velocityChanged = true;
        }
    }

    private static void hurt(Entity receiver, EntityLivingBase owner, float damage) {
        DamageSource source = owner instanceof EntityPlayer
                ? DamageSource.causePlayerDamage((EntityPlayer) owner)
                : DamageSource.causeMobDamage(owner);
        EntityLivingBase logical = TargetingUtil.getSelectionTarget(receiver);
        if (logical != null) logical.hurtResistantTime = 0;
        receiver.attackEntityFrom(source, damage);
    }

    private Vec3d flatForward() {
        float yaw = this.rotationYaw * 0.017453292F;
        return new Vec3d(-MathHelper.sin(yaw), 0.0D, MathHelper.cos(yaw)).normalize();
    }

    private static float smootherStep(float value) {
        value = MathHelper.clamp(value, 0.0F, 1.0F);
        return value * value * value * (value * (value * 6.0F - 15.0F) + 10.0F);
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return this.getEntityBoundingBox().grow(14.0D, 6.0D, 14.0D);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        this.dataManager.set(OWNER_ID, tag.getInteger("OwnerId"));
        this.dataManager.set(BASE_DAMAGE, tag.getFloat("BaseDamage"));
        this.dataManager.set(LIFETIME, Math.max(1, tag.getInteger("Lifetime")));
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        tag.setInteger("OwnerId", this.dataManager.get(OWNER_ID));
        tag.setFloat("BaseDamage", getBaseDamage());
        tag.setInteger("Lifetime", getLifetime());
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }
}

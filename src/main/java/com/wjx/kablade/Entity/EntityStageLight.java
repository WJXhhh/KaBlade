package com.wjx.kablade.Entity;

import com.wjx.kablade.util.KaBladePlayerProp;
import com.wjx.kablade.util.TargetingUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.init.SoundEvents;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;
import java.util.UUID;

/**
 * 「聚光舞台」（Lights on Stage）的 1.12.2 同步锚点。
 * <p>
 * 伤害与范围增益都在服务端结算；该实体只负责把固定落点、朝向和演出时长同步到客户端。
 * 客户端渲染走 1.12.2 固定管线（Tessellator / BufferBuilder / POSITION_COLOR），
 * 不使用 1.20 的 RenderType 与 core shader。
 */
public class EntityStageLight extends Entity {

    public static DataParameter<Integer> LIFETIME =
            EntityDataManager.createKey(EntityStageLight.class, DataSerializers.VARINT);

    private static final double BUFF_RANGE = 6.25D;
    private static final double BUFF_VERTICAL_RANGE = 3.0D;
    private static final int BUFF_TICKS = 10;

    private static final int HIT_DELAY = 5;
    private static final double RANGE = 6.25D;
    private static final double VERTICAL_RANGE = 3.0D;

    private EntityPlayer owner;
    private UUID ownerId;
    private float damage;

    public EntityStageLight(World worldIn) {
        super(worldIn);
        this.setSize(0.2F, 0.2F);
        this.noClip = true;
        this.setNoGravity(true);
        this.isImmuneToFire = true;
        this.ignoreFrustumCheck = true;
    }

    public EntityStageLight(World worldIn, EntityPlayer owner, float damage, int lifetime) {
        this(worldIn);
        this.owner = owner;
        this.ownerId = owner.getUniqueID();
        this.damage = damage;
        this.setLifetime(lifetime);
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(LIFETIME, 80);
    }

    public int getLifetime() {
        return this.dataManager.get(LIFETIME);
    }

    public void setLifetime(int lifetime) {
        this.dataManager.set(LIFETIME, lifetime);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.world.isRemote) {
            return;
        }

        refreshPlayerBoost();

        if (this.ticksExisted == HIT_DELAY) {
            this.strikeStage();
        }
        if (this.ticksExisted >= this.getLifetime()) {
            this.setDead();
        }
    }

    private void refreshPlayerBoost() {
        AxisAlignedBB bounds = this.getEntityBoundingBox()
                .grow(BUFF_RANGE, BUFF_VERTICAL_RANGE, BUFF_RANGE);
        List<EntityPlayer> players = this.world.getEntitiesWithinAABB(EntityPlayer.class, bounds);
        for (EntityPlayer player : players) {
            if (player == null || !player.isEntityAlive()) {
                continue;
            }
            double dx = player.posX - this.posX;
            double dz = player.posZ - this.posZ;
            if (dx * dx + dz * dz <= BUFF_RANGE * BUFF_RANGE) {
                NBTTagCompound properties = KaBladePlayerProp.getPropCompound(player);
                boolean enteringStage = properties.getInteger(KaBladePlayerProp.STAGE_LIGHT) <= 0;
                properties.setInteger(KaBladePlayerProp.STAGE_LIGHT, BUFF_TICKS);
                if (enteringStage) {
                    KaBladePlayerProp.updateNBTForClient(player);
                }
            }
        }
    }

    private void strikeStage() {
        if (this.owner == null && this.ownerId != null) {
            this.owner = this.world.getPlayerEntityByUUID(this.ownerId);
        }
        if (this.owner == null || this.owner.isDead || this.owner.world != this.world) {
            return;
        }

        double centerY = this.posY + 1.0D;
        AxisAlignedBB bounds = new AxisAlignedBB(
                this.posX - RANGE, centerY - VERTICAL_RANGE, this.posZ - RANGE,
                this.posX + RANGE, centerY + VERTICAL_RANGE, this.posZ + RANGE);
        List<Entity> candidates = this.world.getEntitiesInAABBexcluding(this, bounds,
                input -> TargetingUtil.canSelectForDamage(this.owner, input));

        DamageSource source = DamageSource.causePlayerDamage(this.owner);
        Vec3d stageCenter = new Vec3d(this.posX, centerY, this.posZ);
        for (Entity raw : TargetingUtil.getDistinctDamageTargets(candidates)) {
            EntityLivingBase target = TargetingUtil.getSelectionTarget(raw);
            if (target == null) {
                continue;
            }
            Vec3d hitPoint = TargetingUtil.getClosestPointOnDamageBounds(raw, stageCenter);
            double dx = hitPoint.x - this.posX;
            double dz = hitPoint.z - this.posZ;
            if (dx * dx + dz * dz > RANGE * RANGE) {
                continue;
            }

            raw.hurtResistantTime = 0;
            raw.attackEntityFrom(source, this.damage);
            target.hurtResistantTime = 0;
            target.knockBack(this.owner, 0.55F, this.posX - target.posX, this.posZ - target.posZ);

            for (int i = 0; i < 12; i++) {
                double ox = (this.world.rand.nextDouble() * 2.0D - 1.0D) * target.width * 0.45D;
                double oy = (this.world.rand.nextDouble() * 2.0D - 1.0D) * target.height * 0.35D;
                double oz = (this.world.rand.nextDouble() * 2.0D - 1.0D) * target.width * 0.45D;
                ((WorldServer) this.world).spawnParticle(EnumParticleTypes.END_ROD,
                        target.posX + ox, target.posY + target.height * 0.55D + oy, target.posZ + oz,
                        1, 0.08D, 0.08D, 0.08D, 0.0D);
            }
        }

        for (int i = 0; i < 42; i++) {
            double angle = Math.PI * 2.0D * i / 42.0D;
            double radius = RANGE * (0.92D + this.world.rand.nextDouble() * 0.08D);
            double py = this.posY + 0.16D + this.world.rand.nextDouble() * 0.3D;
            ((WorldServer) this.world).spawnParticle(EnumParticleTypes.END_ROD,
                    this.posX + Math.cos(angle) * radius, py, this.posZ + Math.sin(angle) * radius,
                    1, 0.02D, 0.06D, 0.02D, 0.0D);
        }
        this.world.playSound(null, this.posX, this.posY, this.posZ,
                SoundEvents.BLOCK_NOTE_BELL, SoundCategory.PLAYERS, 1.45F, 1.35F);
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return this.getEntityBoundingBox().grow(7.0D, 3.0D, 7.0D);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        this.setLifetime(compound.getInteger("Lifetime"));
        this.damage = compound.getFloat("Damage");
        if (compound.hasUniqueId("Owner")) {
            this.ownerId = compound.getUniqueId("Owner");
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        compound.setInteger("Lifetime", this.getLifetime());
        compound.setFloat("Damage", this.damage);
        if (this.ownerId != null) {
            compound.setUniqueId("Owner", this.ownerId);
        }
    }
}

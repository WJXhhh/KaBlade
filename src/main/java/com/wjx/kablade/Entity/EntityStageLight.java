package com.wjx.kablade.Entity;

import com.wjx.kablade.util.KaBladePlayerProp;
import com.wjx.kablade.util.TargetingUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.registry.IThrowableEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Server-synchronised anchor for the Lights on Stage slash art.
 * The entity keeps the stage fixed in place, performs the delayed hit and
 * refreshes the short-lived player buff while clients render the show.
 */
public class EntityStageLight extends Entity implements IThrowableEntity {

    public static final int LIFETIME = 80;
    public static final int HIT_DELAY = 5;
    public static final int BUFF_TICKS = 10;
    public static final double RANGE = 6.25D;

    private static final double RANGE_SQ = RANGE * RANGE;
    private static final double VERTICAL_RANGE = 3.0D;
    private static final double RENDER_Y_OFFSET = 0.06D;

    private EntityLivingBase owner;
    private UUID ownerId;
    private float damage;
    private boolean struck;

    public EntityStageLight(World worldIn) {
        super(worldIn);
        this.setSize(0.1F, 0.1F);
        this.noClip = true;
        this.ignoreFrustumCheck = true;
    }

    public EntityStageLight(World worldIn, EntityPlayer ownerIn, float damageIn) {
        this(worldIn);
        this.setThrower(ownerIn);
        this.damage = damageIn;
        this.setLocationAndAngles(ownerIn.posX, ownerIn.posY + RENDER_Y_OFFSET,
                ownerIn.posZ, ownerIn.rotationYaw, 0.0F);
    }

    @Override
    protected void entityInit() {
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

        refreshStageBuff();
        if (!this.struck && this.ticksExisted >= HIT_DELAY) {
            this.struck = true;
            EntityLivingBase currentOwner = resolveOwner();
            if (currentOwner instanceof EntityPlayer
                    && currentOwner.isEntityAlive()
                    && currentOwner.world == this.world) {
                strike((EntityPlayer) currentOwner);
            }
        }

        if (this.ticksExisted >= LIFETIME) {
            this.setDead();
        }
    }

    private void refreshStageBuff() {
        AxisAlignedBB bounds = new AxisAlignedBB(
                this.posX - RANGE, this.posY - RANGE, this.posZ - RANGE,
                this.posX + RANGE, this.posY + RANGE, this.posZ + RANGE);
        List<EntityPlayer> players = this.world.getEntitiesWithinAABB(EntityPlayer.class, bounds,
                player -> player.isEntityAlive() && player.getDistanceSq(this) <= RANGE_SQ);
        for (EntityPlayer player : players) {
            KaBladePlayerProp.getPropCompound(player)
                    .setInteger(KaBladePlayerProp.STAGE_LIGHT, BUFF_TICKS);
        }
    }

    private void strike(EntityPlayer player) {
        final double originY = this.posY - RENDER_Y_OFFSET;
        AxisAlignedBB bounds = new AxisAlignedBB(
                this.posX - RANGE, originY + 1.0D - VERTICAL_RANGE, this.posZ - RANGE,
                this.posX + RANGE, originY + 1.0D + VERTICAL_RANGE, this.posZ + RANGE);
        List<EntityLivingBase> targets = this.world.getEntitiesWithinAABB(EntityLivingBase.class, bounds,
                target -> {
                    if (!TargetingUtil.canDamage(player, target)) {
                        return false;
                    }
                    double dx = target.posX - this.posX;
                    double dz = target.posZ - this.posZ;
                    return dx * dx + dz * dz <= RANGE_SQ;
                });

        DamageSource source = DamageSource.causePlayerDamage(player).setDamageBypassesArmor();
        WorldServer serverWorld = this.world instanceof WorldServer ? (WorldServer) this.world : null;
        for (EntityLivingBase target : targets) {
            target.hurtTime = 0;
            target.hurtResistantTime = 0;
            target.attackEntityFrom(source, this.damage);
            target.hurtResistantTime = 0;
            target.knockBack(player, 0.55F,
                    this.posX - target.posX, this.posZ - target.posZ);

            if (serverWorld != null) {
                serverWorld.spawnParticle(EnumParticleTypes.END_ROD,
                        target.posX, target.posY + target.height * 0.55D, target.posZ,
                        12, target.width * 0.45D, target.height * 0.35D,
                        target.width * 0.45D, 0.08D);
            }
        }

        if (serverWorld != null) {
            for (int i = 0; i < 42; i++) {
                double angle = Math.PI * 2.0D * i / 42.0D;
                double radius = RANGE * (0.92D + this.rand.nextDouble() * 0.08D);
                serverWorld.spawnParticle(EnumParticleTypes.END_ROD,
                        this.posX + Math.cos(angle) * radius,
                        originY + 0.16D + this.rand.nextDouble() * 0.3D,
                        this.posZ + Math.sin(angle) * radius,
                        1, 0.02D, 0.06D, 0.02D, 0.0D);
            }
        }
        this.world.playSound(null, this.posX, originY, this.posZ,
                SoundEvents.BLOCK_NOTE_CHIME, SoundCategory.PLAYERS, 1.45F, 1.35F);
    }

    @Nullable
    private EntityLivingBase resolveOwner() {
        if (this.ownerId != null) {
            EntityPlayer player = this.world.getPlayerEntityByUUID(this.ownerId);
            if (player != null) {
                this.owner = player;
                return player;
            }
            if (this.owner instanceof EntityPlayer) {
                this.owner = null;
                return null;
            }
        }
        if (this.owner != null && !this.owner.isDead) {
            return this.owner;
        }
        return null;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        if (compound.hasUniqueId("Owner")) {
            this.ownerId = compound.getUniqueId("Owner");
        }
        this.damage = compound.getFloat("Damage");
        this.struck = compound.getBoolean("Struck");
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        if (this.ownerId != null) {
            compound.setUniqueId("Owner", this.ownerId);
        }
        compound.setFloat("Damage", this.damage);
        compound.setBoolean("Struck", this.struck);
    }

    @Override
    public Entity getThrower() {
        return resolveOwner();
    }

    @Override
    public void setThrower(Entity entity) {
        if (entity instanceof EntityLivingBase) {
            this.owner = (EntityLivingBase) entity;
            this.ownerId = entity.getUniqueID();
        } else {
            this.owner = null;
            this.ownerId = null;
        }
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return distance < 16384.0D;
    }
}

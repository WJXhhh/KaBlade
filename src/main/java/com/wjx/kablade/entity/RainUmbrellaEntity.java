package com.wjx.kablade.entity;

import com.wjx.kablade.init.ModEntities;
import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

/** Rain umbrella anchor for Pledge of Rain's Love is War SA. */
public class RainUmbrellaEntity extends Entity {
    private static final int VISUAL_LIFETIME = 160;

    private UUID ownerUUID;
    private LivingEntity owner;
    private float hedraDamage;

    public RainUmbrellaEntity(EntityType<? extends RainUmbrellaEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public RainUmbrellaEntity(Level level, LivingEntity owner, float hedraDamage) {
        this(ModEntities.RAIN_UMBRELLA.get(), level);
        this.owner = owner;
        this.ownerUUID = owner.getUUID();
        this.hedraDamage = hedraDamage;
        this.setYRot(owner.getYRot());
    }

    public static RainUmbrellaEntity spawn(ServerLevel level, LivingEntity owner,
                                           double x, double y, double z) {
        ItemStack blade = owner.getMainHandItem();
        float baseAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float extraDamage = MathFunc.amplifierCalc(baseAttack, 1.25F);
        RainUmbrellaEntity entity = new RainUmbrellaEntity(level, owner, (2.0F + extraDamage) / 3.0F);
        entity.setPos(x, y, z);
        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }

        resolveOwner();
        if (this.tickCount > 5 && this.tickCount < 70 && this.owner != null) {
            ServerLevel level = (ServerLevel) this.level();
            for (int i = 0; i < 4; i++) {
                SummonedHedraEntity.spawn(level, this.owner, this.getX(), this.getY() + 1.0D, this.getZ(),
                        this.hedraDamage, 0.0F, level.random.nextFloat() * 360.0F);
            }
        }

        if (this.tickCount > VISUAL_LIFETIME) {
            this.discard();
        }
    }

    private void resolveOwner() {
        if (this.owner == null && this.ownerUUID != null && this.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.ownerUUID);
            if (entity instanceof LivingEntity living) {
                this.owner = living;
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
        if (tag.contains("HedraDamage")) {
            this.hedraDamage = tag.getFloat("HedraDamage");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUUID != null) {
            tag.putUUID("OwnerUUID", this.ownerUUID);
        }
        tag.putFloat("HedraDamage", this.hedraDamage);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}

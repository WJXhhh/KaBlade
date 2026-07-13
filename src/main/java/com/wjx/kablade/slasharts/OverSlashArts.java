package com.wjx.kablade.slasharts;

import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Function;

/**
 * 榫欎竴鏂囧瓧绾?SA銆岃秴瓒婃柀銆嶁€斺€?1.12.2 {@code OverSlash} 瀹屾暣绉绘銆? * <p>
 * 娑堣€?40 榄傚洿缁曠帺瀹惰扛鍙戜竴娆?5 鏍艰寖鍥寸殑鍐插嚮娉?+ 6 閬撶传鑹查鏂┿€? */
public final class OverSlashArts extends SlashArts {

    private static final int SOUL_COST = 40;
    private static final float AOE_RADIUS = 5.0F;
    private static final int DRIVE_COUNT = 6;
    private static final int DRIVE_COLOR = 0x9932CC;

    public OverSlashArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        ItemStack blade = user.getMainHandItem();
        if (!(blade.getItem() instanceof ItemSlashBlade)) {
            return super.doArts(type, user);
        }

        // 濂冲帆绮掑瓙 (20 涓?
        for (int i = 0; i < 20; i++) {
            double d0 = user.getRandom().nextGaussian() * 0.02;
            double d1 = user.getRandom().nextGaussian() * 0.02;
            double d2 = user.getRandom().nextGaussian() * 0.02;
            double ox = (user.getRandom().nextFloat() * user.getBbWidth() * 2.0F) - user.getBbWidth();
            double oz = (user.getRandom().nextFloat() * user.getBbWidth() * 2.0F) - user.getBbWidth();
            level.sendParticles(ParticleTypes.WITCH,
                    user.getX() + ox, user.getY(), user.getZ() + oz,
                    1, d0, d1, d2, 0.0);
        }

        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F,
                (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);

        boolean paid = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> {
                    if (state.getProudSoulCount() >= SOUL_COST) {
                        state.setProudSoulCount(state.getProudSoulCount() - SOUL_COST);
                        return true;
                    }
                    return false;
                }).orElse(false);
        if (!paid) {
            blade.hurtAndBreak(10, user, e -> {});
        }

        float baseAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float magicDamage = baseAttack / 2.0F;
        AABB box = user.getBoundingBox().inflate(AOE_RADIUS, 0.25, AOE_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> SaTargeting.canDamageAttackable(user, e));
        for (LivingEntity target : targets) {
            com.wjx.kablade.util.SaDamage.hurtSlashArtNoIFrame(target, level, user, magicDamage);
        }
        // 鏆村嚮鏁堟灉锛堢矑瀛愮敱瀹㈡埛绔鐞嗭級

        // 闆峰０
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 0.4F,
                (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);

        Vec3 center = new Vec3(user.getX(), user.getY() + user.getEyeHeight() / 2.0, user.getZ());
        for (int i = 0; i < DRIVE_COUNT; i++) {
            float yaw = user.getYRot() + (60 * i) + (level.random.nextFloat() - 0.5F) * 60.0F;
            float pitch = (level.random.nextFloat() - 0.5F) * 60.0F;
            float yawRad = yaw * ((float) Math.PI / 180.0F);
            float pitchRad = pitch * ((float) Math.PI / 180.0F);

            Vec3 dir = new Vec3(
                    -Math.sin(yawRad) * Math.cos(pitchRad),
                    -Math.sin(pitchRad),
                    Math.cos(yawRad) * Math.cos(pitchRad)
            );

            EntityDrive drive = new EntityDrive(SlashBlade.RegistryEvents.Drive, level);
            drive.setPos(center.x, center.y, center.z);
            drive.setShooter(user);
            drive.setDamage(magicDamage * 2.0F);
            drive.setColor(DRIVE_COLOR);
            drive.setLifetime(10);
            drive.shoot(dir.x, dir.y, dir.z, 0.5F, 0.0F);
            level.addFreshEntity(drive);
        }

        return super.doArts(type, user);
    }
}

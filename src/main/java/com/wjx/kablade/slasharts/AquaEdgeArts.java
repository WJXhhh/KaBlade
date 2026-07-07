package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.AquaEdgeEntity;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Function;

/**
 * 榫欎竴鏂囧瓧绾?SA銆岃媿娴佸垉銆嶁€斺€?1.12.2 {@code AquaEdgeEx} 瀹屾暣绉绘銆? * <p>
 * 鎵戠伃鑷韩鐏劙锛屽鍛ㄥ洿 5 鏍兼晫浜洪€犳垚 AOE 鏂╁嚮锛屽苟鍚戠帺瀹跺墠鏂规墖褰㈠皠鍑?3脳N 鍒楁按娴侀鍒冿紝
 * 姘存祦椋炲垉鍛戒腑鏃堕€犳垚婧烘按浼ゅ + 鐏伀銆? */
public final class AquaEdgeArts extends SlashArts {

    private static final float AOE_RADIUS = 5.0F;
    private static final int DRIVE_COLOR = 0x0000FF; // 255

    public AquaEdgeArts(Function<LivingEntity, ResourceLocation> state) {
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

        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_SWIM, SoundSource.PLAYERS, 1.0F, 1.5F);

        for (int i = 0; i < 100; i++) {
            double d0 = user.getRandom().nextGaussian() * 0.02;
            double d2 = user.getRandom().nextGaussian() * 0.02;
            double d3 = user.getRandom().nextGaussian() * 0.02;
            double ox = (user.getRandom().nextFloat() * user.getBbWidth() * 2.0F - user.getBbWidth() - d0 * 10.0) * 5.0;
            double oz = (user.getRandom().nextFloat() * user.getBbWidth() * 2.0F - user.getBbWidth() - d3 * 10.0) * 5.0;
            level.sendParticles(ParticleTypes.SPLASH,
                    user.getX() + ox, user.getY(), user.getZ() + oz,
                    1, d0, d2, d3, 0.0);
        }

        // 鎵戠伃鑷韩鐏劙
        if (user.isOnFire()) {
            user.clearFire();
            level.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 0.7F,
                    1.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.4F);
        }

        // 鈹€鈹€ 鏈嶅姟绔€昏緫 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€
        float baseAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float magicDamage = baseAttack * 0.27F;

        // AOE 鏂╁嚮
        AABB box = user.getBoundingBox().inflate(AOE_RADIUS, 0.25, AOE_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> SaTargeting.canDamageAttackable(user, e));
        for (LivingEntity target : targets) {
            com.wjx.kablade.util.SaDamage.hurtNoIFrame(target,
                    level.damageSources().mobAttack(user), baseAttack);
        }

        // 鎵囧舰灏勫嚭姘存祦椋炲垉
        int maxCol = 3;
        int maxCount = 3;  // rank=0 鏃讹紝1.12.2 榛樿 3
        int halfCount = (int) Math.floor((double) maxCount / 2);

        for (int j = 0; j < maxCol; j++) {
            for (int i = 0; i < maxCount; i++) {
                double posY = user.getY() + user.getEyeHeight() / 2.0;

                // 1.12.2 瀹氫綅: player.rotationYaw + 15 * (floor(maxCount/2)+1 - i)
                float yawOffset = (halfCount + 1 - i) * 15.0F;
                float spawnYaw = user.getYRot() + yawOffset;
                float yawRad = spawnYaw * ((float) Math.PI / 180.0F);

                Vec3 dir = user.getLookAngle();

                double px = user.getX() - Math.sin(yawRad);
                double pz = user.getZ() + Math.cos(yawRad);

                AquaEdgeEntity aqua = AquaEdgeEntity.spawn(level,
                        user,
                        new Vec3(px, posY, pz),
                        dir,
                        magicDamage,
                        DRIVE_COLOR,
                        30 + 5 * j + i,  // lifetime
                        (j - 1) * 3.0F,
                        true              // multiHit
                );
                aqua.setInitialSpeed(0.1F);
                aqua.setNextSpeed(1.05F);
                aqua.setChangeTime(5 + 2 * j + i);
                aqua.setParticleEnabled(true);
                aqua.setParticleStyle("WATER_SPLASH");
                aqua.setSoundName("minecraft:entity.player.swim");
            }
        }

        return super.doArts(type, user);
    }
}

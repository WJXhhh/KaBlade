package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.ThunderEdgeAttackEntity;
import com.wjx.kablade.init.ModMobEffects;
import com.wjx.kablade.specialeffect.ThunderBlitz;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Function;

/** Thunder Edge, ported from 1.12.2 {@code HonkaiThunderEdge}. */
public final class ThunderEdgeArts extends SlashArts {

    private static final int VISUAL_LIFETIME = 30;
    private static final double RANGE = 6.5D;
    private static final double HORIZONTAL_PADDING = 3.0D;
    private static final double VERTICAL_PADDING = 1.3D;
    private static final float BASE_DAMAGE = 50.0F;
    private static final float DAMAGE_MULTIPLIER = 1.4F;
    private static final float ATTACK_FACTOR = 12.0F;
    private static final int PARALYSIS_DURATION = 100;
    private static final int PARALYSIS_AMPLIFIER = 5;
    private static final int THUNDER_MARK_DURATION = 300;

    public ThunderEdgeArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        ItemStack blade = user.getMainHandItem();
        ThunderEdgeAttackEntity.spawn(level, user, VISUAL_LIFETIME);

        Vec3 look = user.getLookAngle().normalize();
        AABB area = user.getBoundingBox()
                .expandTowards(look.scale(RANGE))
                .inflate(HORIZONTAL_PADDING, VERTICAL_PADDING, HORIZONTAL_PADDING);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                target -> SaTargeting.canDamageAttackable(user, target));

        float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float damage = (BASE_DAMAGE + MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR)) * DAMAGE_MULTIPLIER;

        for (LivingEntity target : targets) {
            com.wjx.kablade.util.SaDamage.hurtNoIFrame(target,
                    level.damageSources().mobAttack(user), damage);
            target.addEffect(new MobEffectInstance(ModMobEffects.PARALYSIS.get(),
                    PARALYSIS_DURATION, PARALYSIS_AMPLIFIER));
            target.getPersistentData().putInt(ThunderBlitz.THUNDER_MARK_TAG, THUNDER_MARK_DURATION);
            if (user instanceof Player player) {
                player.crit(target);
            }
        }

        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.0F, 1.45F);
        level.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.55F, 1.65F);

        return super.doArts(type, user);
    }
}

package com.wjx.kablade.slasharts;

import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Function;

/**
 * 闇滃喕褰楁槦 鈥斺€?缁撴櫠閫嗗垉鍒€涓撳睘 SA銆? * 浠?1.12.2 {@code HonKaiFrostComet} 绉绘骞剁畝鍖栬€屾潵銆? * <p>
 * 瀵瑰懆鍥?5脳1脳5 鑼冨洿鍐呯殑鎵€鏈夌敓鐗╅€犳垚浼ゅ锛屽苟鍦ㄧ洰鏍囪剼涓嬫斁缃诞鍐般€佹竻闆跺叾杩愬姩鍚戦噺锛堝畾韬級銆? */
public final class FrostCometArts extends SlashArts {

    private static final float BASE_DAMAGE = 1.0F;
    /** 鏀诲嚮鍔涜ˉ姝ｇ郴鏁帮紝缈诲€嶄互鍖归厤楂樻敾姝﹀櫒鐨勬湡鏈涗激瀹炽€?*/
    private static final float ATTACK_FACTOR = 2.0F;
    private static final float DAMAGE_MULTIPLIER = 2.2F;
    private static final double RANGE_XZ = 5.0;
    private static final double RANGE_Y = 1.0;

    public FrostCometArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        ItemStack blade = user.getMainHandItem();
        float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float damage = (BASE_DAMAGE + MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR)) * DAMAGE_MULTIPLIER;

        Vec3 pos = user.position();
        AABB box = new AABB(
                pos.x - RANGE_XZ, pos.y - RANGE_Y, pos.z - RANGE_XZ,
                pos.x + RANGE_XZ, pos.y + RANGE_Y, pos.z + RANGE_XZ);

        DamageSource src = user instanceof Player player
                ? level.damageSources().playerAttack(player)
                : level.damageSources().mobAttack(user);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> SaTargeting.canDamageAttackable(user, e));

        for (LivingEntity target : targets) {
            com.wjx.kablade.util.SaDamage.hurtNoIFrame(target, src, damage);
            // 鑴氫笅鏀炬诞鍐帮紙鐢?blockPosition 鍚戜笅鍙栨暣锛岃礋鍧愭爣涔熸纭級
            BlockPos feet = target.blockPosition();
            level.setBlock(feet, Blocks.PACKED_ICE.defaultBlockState(), 3);
            level.setBlock(feet.above(), Blocks.PACKED_ICE.defaultBlockState(), 3);
            // 瀹氳韩
            target.setDeltaMovement(0, 0, 0);
            target.hurtMarked = true;
        }

        if (!targets.isEmpty()) {
            blade.hurtAndBreak(1, user, e -> e.broadcastBreakEvent(user.getUsedItemHand()));
        }

        level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y + 1.0, pos.z,
                30, RANGE_XZ / 2, RANGE_Y, RANGE_XZ / 2, 0.05);
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);

        return super.doArts(type, user);
    }
}

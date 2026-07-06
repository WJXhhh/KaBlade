package com.wjx.kablade.slasharts;

import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Function;

/**
 * 閿嬪垁鎶氭煶 鈥斺€?澶嶅悎鍒€路鏌冲彾涓撳睘 SA銆? * 浠?1.12.2 {@code HonKaiChopWillow} 绉绘骞剁畝鍖栬€屾潵銆? * <p>
 * 鍘熺増瀹炵幇锛歋A 缁欑帺瀹?Y 閫熷害 +1.1 寮硅捣锛屽啀閫氳繃 entityData 鏍囧織浣? * 锛坽@code to_chop_willow}/{@code start_chop_willow}/{@code chop_willow_retry_count}锛? * 鍦?{@code WorldEvent} 姣?tick 杞锛屽€掕鏃?10 tick 鍚庡鍛ㄥ洿 4脳2脳4 鑼冨洿缁撶畻浼ゅ锛? * 鑻ユ湭鍛戒腑鍒欐瘡 tick 閲嶈瘯锛屾渶澶?40 娆°€? * <p>
 * 1.20.1 绠€鍖栵細鎶婅法 tick 鐘舵€佹満鏀舵暃杩?SA 绫诲唴閮紝鐢?{@link TickTask} 璋冨害锛? * 涓嶅啀渚濊禆 entityData 鏍囧織浣嶆垨鍏ㄥ眬浜嬩欢鐩戝惉銆傝涓虹瓑浠凤細
 * <ol>
 *   <li>鐜╁寮硅捣锛圷 閫熷害 +1.1锛?/li>
 *   <li>10 tick 鍚?AABB 鎵弿鍛ㄥ洿 4脳2脳4 鑼冨洿锛屽姣忎釜鐢熺墿閫犳垚 {@code 4 + min(attack, 4)} 浼ゅ</li>
 *   <li>鑻ユ湭鍛戒腑涓旈噸璇曟鏁?&lt; 40锛屽垯涓嬩竴 tick 鍐嶇粨绠楋紱鍚﹀垯缁撴潫</li>
 * </ol>
 */
public final class ChopWillowArts extends SlashArts {

    /** 寮硅捣 Y 閫熷害锛?.12.2 鍘熷€硷級銆?*/
    private static final double LAUNCH_Y = 1.1;
    /** 鍊掕鏃?tick 鏁帮紙1.12.2 鍘熷€?10锛夈€?*/
    private static final int DELAY_TICKS = 10;
    /** 鑼冨洿浼ゅ鍩虹鍊笺€?*/
    private static final float BASE_DAMAGE = 4.0F;
    /** 鏀诲嚮鍔涜ˉ姝ｇ郴鏁帮細extraDamage = amplifierCalc(attack, 4)锛屽鏁拌ˉ姝ｃ€?*/
    private static final float ATTACK_FACTOR = 4.0F;
    /** AABB 鑼冨洿锛堟牸锛夈€?*/
    private static final double RANGE_XZ = 4.0;
    private static final double RANGE_Y = 2.0;
    /** 鏈€澶ч噸璇曟鏁帮紙1.12.2 鍘熷€?40锛夈€?*/
    private static final int MAX_RETRY = 40;

    public ChopWillowArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        // 寮硅捣
        user.setDeltaMovement(user.getDeltaMovement().x, LAUNCH_Y, user.getDeltaMovement().z);
        user.hurtMarked = true;

        final ServerLevel level = (ServerLevel) user.level();
        final MinecraftServer server = level.getServer();
        if (server == null) {
            return super.doArts(type, user);
        }

        server.tell(new TickTask(server.getTickCount() + DELAY_TICKS,
                () -> chopWillowTask(server, level, user, 0)));

        return super.doArts(type, user);
    }

    /**
     * 鑼冨洿鏂╁嚮缁撶畻浠诲姟銆?     * 鍛戒腑鏁屼汉鍒欎竴娆＄粨绠楀畬姣曪紱鏈懡涓垯涓嬩竴 tick 閲嶈瘯锛屾渶澶?{@link #MAX_RETRY} 娆°€?     */
    private static void chopWillowTask(MinecraftServer server, ServerLevel level, LivingEntity user, int retry) {
        if (!user.isAlive() || retry > MAX_RETRY) {
            return;
        }

        ItemStack blade = user.getMainHandItem();
        float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float extraDamage = MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR);
        float damage = BASE_DAMAGE + extraDamage;

        Vec3 pos = user.position();
        AABB box = new AABB(
                pos.x - RANGE_XZ, pos.y - RANGE_Y, pos.z - RANGE_XZ,
                pos.x + RANGE_XZ, pos.y + RANGE_Y, pos.z + RANGE_XZ);

        DamageSource src = user instanceof Player player
                ? level.damageSources().playerAttack(player)
                : level.damageSources().mobAttack(user);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> SaTargeting.canDamageAttackable(user, e));

        if (targets.isEmpty() && retry < MAX_RETRY) {
            // 鏈懡涓紝涓嬩竴 tick 閲嶈瘯
            server.tell(new TickTask(server.getTickCount() + 1,
                    () -> chopWillowTask(server, level, user, retry + 1)));
            return;
        }

        // 缁撶畻浼ゅ
        for (LivingEntity target : targets) {
            target.hurt(src, damage);
        }
        if (!targets.isEmpty()) {
            blade.hurtAndBreak(1, user, e -> e.broadcastBreakEvent(user.getUsedItemHand()));
        }

        // 钀藉湴鐖嗚绮掑瓙
        for (int i = 0; i < 10; i++) {
            double ox = (level.random.nextBoolean() ? 1 : -1) * level.random.nextDouble();
            double oz = (level.random.nextBoolean() ? 1 : -1) * level.random.nextDouble();
            level.sendParticles(ParticleTypes.POOF,
                    pos.x + ox, pos.y + level.random.nextDouble() * user.getBbHeight() / 2.0, pos.z + oz,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.8F);
    }
}

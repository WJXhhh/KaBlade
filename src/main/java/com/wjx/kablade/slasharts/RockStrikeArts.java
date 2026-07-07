package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.RockSpikeEntity;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;


/**
 * 宀╃煶鎾煎嚮 鈥?鍗冨博涔嬮攱涓撳睘 SA銆? * <p>
 * 浠ュ垁椤垮湴锛屼竴閬撶幆褰㈠啿鍑绘尝鐢辫繎鍙婅繙鍚戝鎵╂暎锛屾部閫斾粠鍦伴噷椤跺嚭涓€鍦堝湀<b>鐪熸鐨?3D 鐭崇煕</b>
 * 锛坽@link RockSpikeEntity}锛屾ā鍨?鍔ㄧ敾鍦ㄥ鎴风鎵嬫悡锛屼細鍑哄湡銆佸仠鐣欍€佸啀缂╁洖锛夈€傝娉㈠強鐨勬晫浜轰細鍏? * 鍚冩弧浼ゅ銆佸啀琚暣涓巰椋炲埌绌轰腑鈥斺€斿厛浼ゅ鍚庡嚮椋烇紝鏉滅粷銆屼汉鍏堥璧般€佹敾鍑诲嵈杩戒笉涓娿€嶃€? * <p>
 * 瀹炵幇瑕佺偣锛? * <ul>
 *   <li><b>浼ゅ</b>鐢?AABB 鐩存帴鎵弿锛屼笌琛ㄧ幇瀹炰綋瑙ｈ€︼細鑼冨洿鍙帶銆佷竴瀹氬厛浜庡嚮椋炵粨绠楋紱姣忎釜鏁屼汉鍙
 *       鏈€鍏堝埌杈惧畠鐨勯偅涓€鍦堢粨绠椾竴娆★紙{@code damaged} 鍘婚噸锛夈€?/li>
 *   <li><b>琛ㄧ幇</b>鏄垚鐗囩殑宀╁埡瀹炰綋 + 鍒烘牴鎵捣鐨勫皯閲忕宀╋紝涓嶅啀鏄弧鍦颁贡椋炵殑鏂瑰潡纰庡睉鍜岀櫧鐑熴€?/li>
 *   <li><b>鑺傚</b>鐢?{@link TickTask} 鎶婃瘡涓€鍦堥敊寮€鍑?tick 瑙﹀彂锛屽舰鎴愬悜澶栬崱寮€鐨勬稛婕€?/li>
 * </ul>
 * <b>寮哄害锛?/b>
 * <ul>
 *   <li>{@code Success}锛堟櫘閫氳搫鍔涳級鈥?2 鍦?/li>
 *   <li>{@code Jackpot}/{@code Super}锛圝ust 鏃舵満锛夆€?3 鍦堬紝浼ゅ 脳1.3銆佹巰椋炴洿楂?/li>
 *   <li>绮剧偧搴︽瘡 10 绾?+1 鍦堬紙涓婇檺 +2锛夛紝绮剧偧 20+ 杩藉姞鐖嗙偢杞伴福</li>
 * </ul>
 * 浼ゅ = amplifierCalc(鍒€褰撳墠鏀诲嚮鍔? 绯绘暟) 脳 鐜郴鏁?脳 鏃舵満鍊嶇巼锛堝鏁拌ˉ姝ｏ紝涓庡叾浣?SA 缁熶竴锛夛紱
 * config 鐨?attack_multiplier 宸茬儰鍏ュ垁 NBT锛岀粡 {@link ISlashBladeState#getBaseAttackModifier()} 璇诲彇鍚庤嚜鍔ㄧ敓鏁堛€? */
public final class RockStrikeArts extends SlashArts {

    /** 鏈€鍐呭湀鍗婂緞锛堟牸锛夈€?*/
    private static final double INNER_RADIUS = 2.0;
    /** 鐩搁偦涓ゅ湀鐨勫崐寰勫樊锛堟牸锛夈€?*/
    private static final double RING_STEP = 2.2;
    /** 鍒ゅ畾甯﹀锛氭晫浜虹鍐插嚮娉㈣竟缂樿繖涔堣繎灏辩畻琚壂鍒帮紝鐣ュぇ浜庢闀夸互鍏嶆紡鍒ゃ€?*/
    private static final double HIT_BAND = 1.6;
    /** 鐩搁偦涓ゅ湀瑙﹀彂鐨勯棿闅旓紙tick锛夛紝鍒堕€犲悜澶栨墿鏁ｇ殑娑熸吉銆?*/
    private static final int RING_DELAY = 2;

    /** 鏀诲嚮鍔涜ˉ姝ｇ郴鏁帮細鍗曞湀鍩虹浼ゅ = amplifierCalc(bladeAttack, ATTACK_FACTOR)锛屽鏁拌ˉ姝ｃ€?*/
    private static final float ATTACK_FACTOR = 4.0F;

    /** 鍚勫湀浼ゅ绯绘暟锛堣秺澶栧湀浣欐尝瓒婂急锛夈€傛暟缁勮鐩栨渶澶у湀鏁帮紙2 鍩虹 + 2 绮剧偧 + 1 浣欓噺锛夈€?*/
    private static final float[] RING_DAMAGE_FACTOR = {1.1F, 0.95F, 0.8F, 0.65F, 0.5F};

    /** 鍒烘牴鎵皹鐢ㄧ殑纰庡博璐村浘銆?*/
    private static final BlockState DUST_ROCK = Blocks.STONE.defaultBlockState();

    public RockStrikeArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    private static float getBladeAttack(LivingEntity user) {
        ItemStack stack = user.getMainHandItem();
        return stack.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
    }

    private static int getRefine(LivingEntity user) {
        ItemStack stack = user.getMainHandItem();
        return stack.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getRefine)
                .orElse(0);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide()) {
            return super.doArts(type, user);
        }

        final ServerLevel level = (ServerLevel) user.level();
        final MinecraftServer server = level.getServer();
        final float bladeAttack = getBladeAttack(user);
        final int refine = getRefine(user);

        final boolean empowered = type == ArtsType.Jackpot || type == ArtsType.Super;
        final float tierMultiplier = empowered ? 1.3F : 1.0F;
        final double launchUp = empowered ? 1.15 : 0.85;   // 鎺€椋為珮搴︼紙鍨傜洿鍒濋€燂級
        final boolean hasExplosion = refine >= 20;

        final int refineBonus = Math.min(refine / 10, 2);
        final int rings = (empowered ? 3 : 2) + refineBonus;

        final Vec3 origin = user.position();
        final DamageSource source = user instanceof Player player
                ? level.damageSources().playerAttack(player)
                : level.damageSources().mobAttack(user);

        final Set<Entity> damaged = new HashSet<>();

        // 鈹€鈹€ 椤垮湴鐬棿锛氫腑蹇冧竴鏍瑰ぇ鐭崇煕 + 纰庡博鐖嗚 鈹€鈹€
        impactBurst(level, origin);
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS,
                1.6F, 0.5F + level.getRandom().nextFloat() * 0.15F);
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.9F, 0.45F);
        if (hasExplosion) {
            level.playSound(null, origin.x, origin.y, origin.z,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.6F, 0.6F);
        }

        // 鈹€鈹€ 涓€鍦堝湀鍚戝鑽″紑 鈹€鈹€
        for (int ring = 0; ring < rings; ring++) {
            final int r = ring;
            final double radius = INNER_RADIUS + ring * RING_STEP;
            final float factor = RING_DAMAGE_FACTOR[Math.min(ring, RING_DAMAGE_FACTOR.length - 1)];
            final double damage = MathFunc.amplifierCalc(bladeAttack, ATTACK_FACTOR) * factor * tierMultiplier;

            final Runnable wave = () -> {
                if (!user.isAlive()) {
                    return;
                }
                spawnWave(level, user, origin, radius, r, damage, source, launchUp, damaged);
            };

            int delay = ring * RING_DELAY;
            if (delay == 0) {
                wave.run();
            } else {
                server.tell(new TickTask(server.getTickCount() + delay, wave));
            }
        }

        return super.doArts(type, user);
    }

    /** 瑙﹀彂涓€鏁村湀锛氬厛娌垮渾鍛ㄩ《鍑轰竴鍦堢煶鐭涳紝鍐嶆妸娉㈠強鑼冨洿鍐呯殑鏁屼汉鎺€椋炪€?*/
    private static void spawnWave(ServerLevel level, LivingEntity user, Vec3 origin,
                                  double radius, int ring, double damage,
                                  DamageSource source, double launchUp, Set<Entity> damaged) {

        RandomSource rng = level.getRandom();

        int spikes = 4 + ring * 2;
        double groundY = origin.y;
        for (int i = 0; i < spikes; i++) {
            double angle = 2.0 * Math.PI * i / spikes + ring * 0.4;
            double x = origin.x + Math.cos(angle) * radius;
            double z = origin.z + Math.sin(angle) * radius;
            float scale = 0.85F + rng.nextFloat() * 0.3F;
            spawnSpike(level, x, groundY, z, scale, rng);
        }

        shockwaveRing(level, origin, radius);

        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.STONE_BREAK, SoundSource.PLAYERS,
                1.4F, 0.55F + ring * 0.1F);

        // Damage and launch targets in a circle centered at origin.
        double reach = radius + HIT_BAND;
        AABB box = new AABB(
                origin.x - reach, origin.y - 1.0, origin.z - reach,
                origin.x + reach, origin.y + 3.0, origin.z + reach);
        double reachSq = reach * reach;

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != user && !damaged.contains(e)
                        && SaTargeting.canDamageAttackable(user, e))) {
            double dx = target.getX() - origin.x;
            double dz = target.getZ() - origin.z;
            if (dx * dx + dz * dz > reachSq) {
                continue;
                }
            damaged.add(target);
            com.wjx.kablade.util.SaDamage.hurtNoIFrame(target, source, (float) damage);
            launchUp(target, origin, launchUp);
        }
    }

    /** 鍦ㄤ竴鐐归《鍑轰竴鏍瑰博鍒哄疄浣擄紝骞舵壃璧峰皯閲忕宀┿€?*/
    private static void spawnSpike(ServerLevel level, double x, double y, double z,
                                   float scale, RandomSource rng) {
        float yaw = rng.nextFloat() * 360.0F;
        int lifetime = 16 + rng.nextInt(5);
        RockSpikeEntity.spawn(level, x, y, z, yaw, scale, lifetime, rng.nextInt());
        spikeFx(level, x, y, z);
    }

    /** 鍒烘牴鐗规晥锛氬穿璧风殑纰庡博 + 鍠锋簠鐨勭伀鏄熶綑鐑?+ 缂撶紦涓婂崌鐨勫皹鐑熴€?*/
    private static void spikeFx(ServerLevel level, double x, double y, double z) {
        BlockParticleOption rock = new BlockParticleOption(ParticleTypes.BLOCK, DUST_ROCK);
        // 绱у噾宕╄捣鐨勭宀╋紙涓嶅洓鏁ｄ贡椋烇級
        level.sendParticles(rock, x, y + 0.1, z, 4, 0.12, 0.05, 0.12, 0.05);
        level.sendParticles(rock, x, y + 0.1, z, 0, 0.0, 1.0, 0.0, 0.4);
        level.sendParticles(rock, x, y + 0.1, z, 0, 0.15, 0.9, 0.1, 0.35);
        // 鐔斿博鐏槦锛氶殢宀╃煕鍠峰嚭銆佷笂鎶涘悗鍥炶惤
        level.sendParticles(ParticleTypes.LAVA, x, y + 0.25, z, 2, 0.08, 0.1, 0.08, 0.0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.15, z, 1, 0.08, 0.0, 0.08, 0.015);
    }

    /** 娌垮崐寰勭敾涓€閬撹创鍦扮殑灏樼幆锛屾妸鏁村湀宀╁埡杩炴垚涓€閬撳啿鍑绘尝銆?*/
    private static void shockwaveRing(ServerLevel level, Vec3 origin, double radius) {
        BlockParticleOption rock = new BlockParticleOption(ParticleTypes.BLOCK, DUST_ROCK);
        int points = Math.min(40, Math.max(16, (int) (radius * 6.0)));
        for (int i = 0; i < points; i++) {
            double a = 2.0 * Math.PI * i / points;
            double x = origin.x + Math.cos(a) * radius;
            double z = origin.z + Math.sin(a) * radius;
            level.sendParticles(rock, x, origin.y + 0.05, z, 1, 0.04, 0.01, 0.04, 0.015);
        }
    }

    /** 灏嗙洰鏍囨巰椋烇細鍨傜洿涓轰富銆佸甫涓€鐐瑰悜澶栫殑姘村钩鍒嗛噺銆?*/
    private static void launchUp(LivingEntity target, Vec3 origin, double up) {
        double dx = target.getX() - origin.x;
        double dz = target.getZ() - origin.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        double outward = 0.2;
        double ox = len < 1.0e-4 ? 0.0 : dx / len * outward;
        double oz = len < 1.0e-4 ? 0.0 : dz / len * outward;
        target.setDeltaMovement(ox, up, oz);
        target.hurtMarked = true;   // 寮哄埗鎶婃柊閫熷害鍚屾鍒板鎴风
    }

    /** 涓績椤垮湴鐗规晥锛氫竴鏍规洿澶х殑鐭崇煕 + 纰庡博鐖嗚 + 鐏槦鍠锋硥 + 鍗囪吘灏樻煴锛堟棤鐧借壊鐖嗙偢鐞冿級銆?*/
    private static void impactBurst(ServerLevel level, Vec3 origin) {
        RandomSource rng = level.getRandom();
        RockSpikeEntity.spawn(level, origin.x, origin.y, origin.z,
                rng.nextFloat() * 360.0F, 1.7F, 22, rng.nextInt());

        BlockParticleOption rock = new BlockParticleOption(ParticleTypes.BLOCK, DUST_ROCK);
        level.sendParticles(rock, origin.x, origin.y + 0.4, origin.z, 30, 0.3, 0.3, 0.3, 0.35);
        // 鐏槦鍠锋硥
        level.sendParticles(ParticleTypes.LAVA, origin.x, origin.y + 0.3, origin.z, 12, 0.3, 0.25, 0.3, 0.0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, origin.x, origin.y + 0.2, origin.z, 8, 0.25, 0.1, 0.25, 0.03);
    }
}

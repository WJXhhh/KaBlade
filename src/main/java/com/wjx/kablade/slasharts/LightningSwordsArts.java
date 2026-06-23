package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.LightningSwordEntity;
import com.wjx.kablade.entity.PhantomSwordExEntity;
import com.wjx.kablade.util.SATool;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.capability.concentrationrank.CapabilityConcentrationRank;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/**
 * 龙一文字线 SA「云轮风暴」—— 1.12.2 {@code LightningSwordsEx} 行为移植。
 *
 * <p>演出流程（4波，rank=0）：
 * <ol>
 *   <li>起手暴击斩 + 定身
 *   <li>① PhantomSwordEx×3 紫剑 目标周围R2 悬停7t→追踪骑乘
 *   <li>② LightningSword×2 金剑 目标周围R3 Z+2 悬停22t→追踪落雷
 *   <li>③ LightningSword×2 金剑 <b>玩家位置</b> 悬停7t→飞出落雷
 *   <li>④ LightningSword×1 金剑 <b>目标头顶Y+4</b> 悬停18t→垂直下落落雷
 * </ol>
 */
public final class LightningSwordsArts extends SlashArts {

    private static final int PHANTOM_COLOR = 0x7060E8;   // 7364008 紫
    private static final int LIGHTNING_COLOR = 0xFFD700; // 16766720 金

    public LightningSwordsArts(Function<LivingEntity, ResourceLocation> state) {
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

        // Prefer SlashBlade's lock target, then fall back to the entity under the crosshair.
        Entity target = blade.getCapability(ItemSlashBlade.BLADESTATE).resolve()
                .map(state -> state.getTargetEntity(level))
                .orElse(null);
        if (target == null || !target.isAlive() || target.distanceTo(user) >= 30.0F) {
            target = SATool.getEntityToWatch(user);
        }
        if (!(target instanceof LivingEntity livingTarget) || !target.isAlive()) {
            return super.doArts(type, user);
        }

        // 魂耗尽力扣
        blade.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            if (state.getProudSoulCount() >= 100) {
                state.setProudSoulCount(state.getProudSoulCount() - 100);
            }
        });

        float baseAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);

        int rank = user.getCapability(CapabilityConcentrationRank.RANK_POINT)
                .map(cap -> cap.getRank(level.getGameTime()).level)
                .orElse(0);
        int powerLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, blade);
        float magicDamage = (1.0F + baseAttack * 0.5F
                * ((float) powerLevel / 5.0F)
                * (1.5F + 0.2F * rank)) * 0.5F;

        // Opening hit: use SlashBlade's managed melee path and reproduce the old critical burst.
        AttackManager.doMeleeAttack(user, livingTarget, true, true);
        level.sendParticles(ParticleTypes.CRIT,
                livingTarget.getX(), livingTarget.getY(0.5D), livingTarget.getZ(),
                18, livingTarget.getBbWidth() * 0.5D, livingTarget.getBbHeight() * 0.35D,
                livingTarget.getBbWidth() * 0.5D, 0.25D);
        StunManager.setStun(livingTarget);
        livingTarget.setDeltaMovement(0.0, 0.0, 0.0);
        livingTarget.hurtMarked = true;

        Vec3 tpos = livingTarget.position();

        // ═══════════════════════════════════════════════════════
        // 第①波：PhantomSwordEx × 3（紫，目标周围R2）
        // rank=0 → count=3, rad=2PI/3
        // ═══════════════════════════════════════════════════════
        int count1 = 3 + rank;
        double rad1 = Math.PI * 2 / count1;
        for (int i = 0; i < count1; i++) {
            double ran = rad1 * i;
            double dist = 2.0;
            double sx = tpos.x + Math.sin(ran) * dist;
            double sy = tpos.y + (1.0 + level.random.nextDouble()) * dist;
            double sz = tpos.z + Math.cos(ran) * dist;
            int dir = -(int) Math.toDegrees(Math.PI + ran);

            PhantomSwordExEntity ph = PhantomSwordExEntity.spawn(level, user,
                    new Vec3(sx, sy, sz), Vec3.ZERO,
                    magicDamage, PHANTOM_COLOR,
                    30,                   // lifetime
                    7 + i / 2,            // interval
                    dir,                  // iniYaw
                    90.0F                 // iniPitch
            );
            ph.setTarget(livingTarget);
        }

        // ═══════════════════════════════════════════════════════
        // 第②波：LightningSword × ceil(count1*2/3)（金，目标周围R3 Z+2）
        // rank=0 → count2=ceil(3*2/3)=2, rad=2PI/2=PI
        // ═══════════════════════════════════════════════════════
        // 注意：1.12.2 的 count 此时已经是 count1+rank=3，所以 ceil((3+0)*2/3)=2
        int count2 = (int) Math.ceil((double) count1 / 3.0D * 2.0D);
        double rad2 = Math.PI * 2 / count2;
        for (int i = 0; i < count2; i++) {
            double ran = rad2 * i;
            double dist = 3.0;
            double sx = tpos.x + Math.sin(ran) * dist;
            double sy = tpos.y + (1.6 + level.random.nextDouble()) * dist;
            double sz = tpos.z + Math.cos(ran) * dist + 2.0;
            int dir = -(int) Math.toDegrees(Math.PI + ran);

            LightningSwordEntity ls = LightningSwordEntity.spawn(level, user,
                    new Vec3(sx, sy, sz), Vec3.ZERO,
                    magicDamage, LIGHTNING_COLOR,
                    40,                       // lifetime
                    7 + i * 2 + 15,           // interval
                    dir,                      // iniYaw
                    90.0F                     // iniPitch
            );
            ls.setTarget(livingTarget);
        }

        // ═══════════════════════════════════════════════════════
        // 第③波：LightningSword × count2（金，玩家周围随机位置出发）
        // 复刻 EntitySummonedSword 构造器的横向随机散布
        // interval=7+i, lifetime=30
        // ═══════════════════════════════════════════════════════
        for (int i = 0; i < count2; i++) {
            // EntitySummonedSword's old constructor scattered each sword around the user.
            double lateral = (level.random.nextFloat() - 0.5D) * 2.0D;
            double userYaw = Math.toRadians(-user.getYRot() + 90.0F);
            double dist = 2.0D;
            Vec3 playerSwordPos = user.position().add(
                    lateral * Math.sin(userYaw) * dist,
                    (1.0D - Math.abs(lateral)) * dist,
                    lateral * Math.cos(userYaw) * dist);
            Vec3 initialMotion = user.getLookAngle().scale(1.75D);

            LightningSwordEntity ls = LightningSwordEntity.spawn(level, user,
                    playerSwordPos, initialMotion,
                    magicDamage, LIGHTNING_COLOR,
                    30,                // lifetime
                    7 + i,             // interval
                    user.getYRot(),     // old constructor inherited the user's aim
                    user.getXRot()
            );
            ls.setTarget(livingTarget);
        }

        // ═══════════════════════════════════════════════════════
        // 第④波：LightningSword × 1（金，目标头顶Y+4，垂直下落）
        // interval=7+count2/2+10, lifetime=40, pitch=90 → 朝下
        // ═══════════════════════════════════════════════════════
        double ran4 = rad2 * count2;
        int dir4 = -(int) Math.toDegrees(Math.PI + ran4);

        LightningSwordEntity ls4 = LightningSwordEntity.spawn(level, user,
                new Vec3(tpos.x, tpos.y + 4.0, tpos.z), Vec3.ZERO,
                magicDamage, LIGHTNING_COLOR,
                40,                          // lifetime
                7 + count2 / 2 + 10,         // interval
                dir4,                        // iniYaw
                90.0F                        // iniPitch → 朝下
        );
        ls4.setTarget(livingTarget);

        return super.doArts(type, user);
    }
}

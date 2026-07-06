package com.wjx.kablade.specialeffect;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.KabladeCapabilities;
import com.wjx.kablade.init.ModMobEffects;
import com.wjx.kablade.init.ModSpecialEffects;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 乱流 —— 「藏锋」专属特殊效果。
 * <p>
 * 从 1.12.2 {@code SETurbulence} 移植而来：
 * 持有者被攻击后进入 100 tick 的「乱流」状态；
 * 在此期间若持有者攻击敌人，则对目标召唤雷电、造成 4 点额外伤害并附加麻痹效果。
 * <p>
 * 乱流状态通过 {@link com.wjx.kablade.data.IPlayerPropertyData} capability 存储，
 * 激活时在 HUD「斩无不断:效果」区以紫色文字显示。
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Turbulence extends SpecialEffect {

    /** capability key，对应 HUD 属性注册和 lang key {@code prop.kablade.turbulence}。 */
    public static final String PROP_KEY = "turbulence";

    private static final int TURBULENCE_TICKS = 100;
    private static final float DAMAGE_BASE = 4.0F;
    private static final float DAMAGE_FACTOR = 0.5F;
    private static final int PARALYSIS_DURATION = 100;
    private static final int PARALYSIS_AMPLIFIER = 1;

    public Turbulence() {
        super(-1, false, true);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        Entity source = event.getSource().getEntity();
        LivingEntity victim = event.getEntity();
        Level level = victim.level();

        // 持有者被活体实体攻击时进入乱流状态（环境伤害不触发）
        if (source instanceof LivingEntity && victim instanceof Player player && hasEffect(player)) {
            player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                    .ifPresent(cap -> cap.set(PROP_KEY, TURBULENCE_TICKS));
        }

        // 持有者攻击敌人且处于乱流状态时触发额外效果
        if (source instanceof Player player && hasEffect(player)) {
            boolean active = player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                    .map(cap -> cap.isActive(PROP_KEY))
                    .orElse(false);
            if (active) {
                LivingEntity target = event.getEntity();
                if (!SaTargeting.canDamage(player, target)) {
                    return;
                }
                player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                        .ifPresent(cap -> cap.set(PROP_KEY, 0));
                // 纯视觉闪电（与 1.12.2 一致，不点燃/不伤旁人）
                LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
                bolt.setVisualOnly(true);
                bolt.setPos(target.getX(), target.getY(), target.getZ());
                level.addFreshEntity(bolt);
                target.hurt(level.damageSources().playerAttack(player),
                        dynamicDamage(player.getMainHandItem()));
                target.addEffect(new MobEffectInstance(ModMobEffects.PARALYSIS.get(),
                        PARALYSIS_DURATION, PARALYSIS_AMPLIFIER));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isClient()) {
            return;
        }
        Player player = event.player;
        if (!hasEffect(player)) {
            // 切刀后清除乱流状态
            player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                    .ifPresent(cap -> cap.set(PROP_KEY, 0));
            return;
        }
        // 每 tick 递减（capability 内部自动在值 ≤ 0 时移除条目）
        player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                .ifPresent(cap -> {
                    int cur = cap.get(PROP_KEY);
                    if (cur > 0) {
                        cap.set(PROP_KEY, cur - 1);
                    }
                });
    }

    private static boolean hasEffect(Player player) {
        ItemStack blade = player.getMainHandItem();
        if (!(blade.getItem() instanceof ItemSlashBlade)) {
            return false;
        }
        return blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.TURBULENCE.getId()))
                .orElse(false);
    }

    private static float dynamicDamage(ItemStack blade) {
        float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        return (DAMAGE_BASE + MathFunc.amplifierCalc(bladeAttack, DAMAGE_FACTOR)) * 2.0F;
    }
}

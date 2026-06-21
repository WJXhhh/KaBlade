package com.wjx.kablade.specialeffect;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModMobEffects;
import com.wjx.kablade.init.ModSpecialEffects;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 乱流 —— 「藏锋」专属特殊效果。
 * <p>
 * 从 1.12.2 {@code SETurbulence} 移植而来：
 * 持有者被攻击后进入 100 tick 的「乱流」状态；
 * 在此期间若持有者攻击敌人，则对目标召唤雷电、造成 4 点额外伤害并附加麻痹效果。
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Turbulence extends SpecialEffect {

    private static final int TURBULENCE_TICKS = 100;
    private static final float EXTRA_DAMAGE = 4.0F;
    private static final int PARALYSIS_DURATION = 100;
    private static final int PARALYSIS_AMPLIFIER = 1;

    /** 用 Map 替代 1.12.2 的 KaBladePlayerProp 来跟踪玩家的乱流计数。 */
    private static final Map<UUID, Integer> TURBULENCE_MAP = new HashMap<>();

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

        // 持有者被攻击时进入乱流状态
        if (victim instanceof Player player && hasEffect(player)) {
            TURBULENCE_MAP.put(player.getUUID(), TURBULENCE_TICKS);
        }

        // 持有者攻击敌人且处于乱流状态时触发额外效果
        if (source instanceof Player player && hasEffect(player)) {
            Integer ticks = TURBULENCE_MAP.get(player.getUUID());
            if (ticks != null && ticks > 0) {
                TURBULENCE_MAP.put(player.getUUID(), 0);
                LivingEntity target = event.getEntity();
                LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
                bolt.setPos(target.getX(), target.getY(), target.getZ());
                level.addFreshEntity(bolt);
                target.hurt(level.damageSources().playerAttack(player), EXTRA_DAMAGE);
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
            TURBULENCE_MAP.remove(player.getUUID());
            return;
        }
        UUID id = player.getUUID();
        TURBULENCE_MAP.merge(id, 0, (oldVal, zero) -> Math.max(0, oldVal - 1));
        if (TURBULENCE_MAP.getOrDefault(id, 0) <= 0) {
            TURBULENCE_MAP.remove(id);
        }
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
}

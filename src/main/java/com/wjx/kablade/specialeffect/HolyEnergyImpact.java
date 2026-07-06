package com.wjx.kablade.specialeffect;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.KabladeCapabilities;
import com.wjx.kablade.init.ModSpecialEffects;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.capability.concentrationrank.CapabilityConcentrationRank;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 圣能冲击 —— 「3rd圣遗物」专属特殊效果。
 * <p>
 * 从 1.12.2 {@code SEHolyEnergyImpact} 移植而来：
 * 持有者评级越高，拔刀造成的伤害越高。
 * <ul>
 *   <li>SSS → +40%</li>
 *   <li>SS  → +33%</li>
 *   <li>S   → +26%</li>
 *   <li>A   → +20%</li>
 *   <li>B   → +14%</li>
 *   <li>C   → +8%</li>
 *   <li>D   → +4%</li>
 *   <li>无评级 → 0%</li>
 * </ul>
 * SE 激活时，左下角的斩无不断效果也会显示当前评级等级。
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HolyEnergyImpact extends SpecialEffect {

    private static final String PROP_KEY = "holy_energy_impact";

    /**
     * 每个评级等级对应的伤害倍率。
     * index = rank.level（0=NONE, 1=D, 2=C, 3=B, 4=A, 5=S, 6=SS, 7=SSS）。
     */
    private static final float[] RANK_BOOST = {
            1.00F,  // NONE (0) — 无评级
            1.04F,  // D    (1) — +4%
            1.08F,  // C    (2) — +8%
            1.14F,  // B    (3) — +14%
            1.20F,  // A    (4) — +20%
            1.26F,  // S    (5) — +26%
            1.33F,  // SS   (6) — +33%
            1.40F,  // SSS  (7) — +40%
    };

    public HolyEnergyImpact() {
        super(-1, false, true);
    }

    /**
     * 持有者使用拔刀剑攻击时，根据评级提升伤害。
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        // 仅当攻击者是持有此 SE 的玩家时生效
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        if (!hasEffect(player)) {
            return;
        }
        if (!SaTargeting.canDamage(player, event.getEntity())) {
            return;
        }

        int rank = player.getCapability(CapabilityConcentrationRank.RANK_POINT)
                .map(cap -> cap.getRank(player.level().getGameTime()).level)
                .orElse(0);

        float boost = RANK_BOOST[Math.min(rank, RANK_BOOST.length - 1)];
        event.setAmount(event.getAmount() * boost);
    }

    /**
     * 每 tick 同步评级数据到 capability，用于 HUD 显示。
     * SE 激活时显示"圣能冲击"条目，数值反映当前评级等级。
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.side.isClient()) {
            return;
        }

        Player player = event.player;
        player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA).ifPresent(data -> {
            if (hasEffect(player)) {
                int rank = player.getCapability(CapabilityConcentrationRank.RANK_POINT)
                        .map(cap -> cap.getRank(player.level().getGameTime()).level)
                        .orElse(0);
                // 统一设为 1，HUD 始终全满黄色显示"圣能冲击"
                data.set(PROP_KEY, 1);
            } else {
                data.remove(PROP_KEY);
            }
        });
    }

    private static boolean hasEffect(Player player) {
        ItemStack blade = player.getMainHandItem();
        if (!(blade.getItem() instanceof ItemSlashBlade)) {
            return false;
        }
        return blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.HOLY_ENERGY_IMPACT.getId()))
                .orElse(false);
    }
}

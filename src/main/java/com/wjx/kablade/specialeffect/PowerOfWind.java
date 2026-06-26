package com.wjx.kablade.specialeffect;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.KabladeCapabilities;
import com.wjx.kablade.init.ModSpecialEffects;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * 风之力 —— 「妖精剑·希尔文」专属 SE。
 * <p>
 * 对应 1.12.2 的 {@code SEPowerOfWind}。
 * <ul>
 *   <li>当玩家手持带有此 SE 的拔刀剑时，根据玩家移动速度的 1/10 追加攻击伤害</li>
 *   <li>向玩家 capability 写入 {@code fair_pow = 1/0} 供 HUD 显示</li>
 *   <li>非激活时清零 modifier</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PowerOfWind extends SpecialEffect {

    private static final UUID MODIFIER_UUID =
            UUID.fromString("739B518D-F9EF-04F7-AD8E-98AE7D3C5FE8");
    private static final String MODIFIER_NAME = "pow_att";

    /** 状态切换标记，对应 1.12.2 的 flagpow，避免每 tick 重复 sync。 */
    private static boolean wasActive = false;

    public PowerOfWind() {
        super(-1, true, true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Player player = event.player;

        ItemStack blade = player.getMainHandItem();
        boolean hasEffect = isBladeWithEffect(blade);

        if (hasEffect) {
            // 激活：写入 FAIR_POW、追加攻击力
            double speed = player.getAttribute(Attributes.MOVEMENT_SPEED).getValue();
            double extraAttack = speed / 10.0;

            var attrib = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attrib != null) {
                attrib.removeModifier(MODIFIER_UUID);
                attrib.addTransientModifier(
                        new AttributeModifier(MODIFIER_UUID, MODIFIER_NAME,
                                extraAttack, AttributeModifier.Operation.ADDITION));
            }

            if (!wasActive) {
                player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                        .ifPresent(cap -> cap.set("fair_pow", 1));
                wasActive = true;
            }
        } else {
            // 非激活：清零 FAIR_POW、移除 modifier
            var attrib = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attrib != null) {
                attrib.removeModifier(MODIFIER_UUID);
            }

            if (wasActive) {
                player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                        .ifPresent(cap -> cap.set("fair_pow", 0));
                wasActive = false;
            }
        }
    }

    private static boolean isBladeWithEffect(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemSlashBlade)) return false;
        return stack.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.POWER_OF_WIND.getId()))
                .orElse(false);
    }
}

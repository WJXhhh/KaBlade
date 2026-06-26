package com.wjx.kablade.event;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.KabladeCapabilities;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * 维护风之结界的短时属性增益。
 *
 * <p>结界实体会每 tick 把范围内玩家的剩余时间刷新为 5 tick。
 * 玩家离开或结界消失后，计时归零并移除临时修饰符。
 * 数值与 1.12.2 实现一致：移速 +50%、攻速 +25%、攻击 +20%（均为基础值乘算）。</p>
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WindEnchantmentBoostHandler {

    private static final String BOOST_KEY = "wind_enchantment_boost";
    private static final UUID MODIFIER_UUID =
            UUID.fromString("4c6e9fd8-9b41-4d2a-b15b-860751f7a4bd");

    private WindEnchantmentBoostHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }

        Player player = event.player;
        player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA).ifPresent(data -> {
            int remaining = data.get(BOOST_KEY);
            if (remaining > 0) {
                applyIfMissing(player.getAttribute(Attributes.MOVEMENT_SPEED), 0.50);
                applyIfMissing(player.getAttribute(Attributes.ATTACK_SPEED), 0.25);
                applyIfMissing(player.getAttribute(Attributes.ATTACK_DAMAGE), 0.20);
                data.set(BOOST_KEY, remaining - 1);
            } else {
                remove(player.getAttribute(Attributes.MOVEMENT_SPEED));
                remove(player.getAttribute(Attributes.ATTACK_SPEED));
                remove(player.getAttribute(Attributes.ATTACK_DAMAGE));
            }
        });
    }

    private static void applyIfMissing(AttributeInstance attribute, double amount) {
        if (attribute == null || attribute.getModifier(MODIFIER_UUID) != null) {
            return;
        }
        attribute.addTransientModifier(new AttributeModifier(
                MODIFIER_UUID,
                "kablade.wind_enchantment",
                amount,
                AttributeModifier.Operation.MULTIPLY_BASE));
    }

    private static void remove(AttributeInstance attribute) {
        if (attribute != null && attribute.getModifier(MODIFIER_UUID) != null) {
            attribute.removeModifier(MODIFIER_UUID);
        }
    }
}

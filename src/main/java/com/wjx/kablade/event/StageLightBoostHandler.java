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
 * 维护聚光舞台的短时属性增益。
 *
 * <p>{@link com.wjx.kablade.entity.StageLightEntity} 每 tick 把范围内玩家的剩余时间刷新为 10 tick。
 * 玩家离开或舞台消失后，计时归零并移除临时修饰符。
 * 效果：攻击伤害 +10%、攻击速度 +10%（基础值乘算）。</p>
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StageLightBoostHandler {

    private static final String STAGE_LIGHT_KEY = "stage_light";
    private static final UUID MODIFIER_UUID =
            UUID.fromString("7a3b8c9d-0e1f-4a5b-8c7d-9e0f1a2b3c4d");

    private StageLightBoostHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }

        Player player = event.player;
        player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA).ifPresent(data -> {
            int remaining = data.get(STAGE_LIGHT_KEY);
            if (remaining > 0) {
                applyIfMissing(player.getAttribute(Attributes.ATTACK_DAMAGE), 0.10);
                applyIfMissing(player.getAttribute(Attributes.ATTACK_SPEED), 0.10);
                data.set(STAGE_LIGHT_KEY, remaining - 1);
            } else {
                remove(player.getAttribute(Attributes.ATTACK_DAMAGE));
                remove(player.getAttribute(Attributes.ATTACK_SPEED));
            }
        });
    }

    private static void applyIfMissing(AttributeInstance attribute, double amount) {
        if (attribute == null || attribute.getModifier(MODIFIER_UUID) != null) {
            return;
        }
        attribute.addTransientModifier(new AttributeModifier(
                MODIFIER_UUID,
                "kablade.stage_light",
                amount,
                AttributeModifier.Operation.MULTIPLY_BASE));
    }

    private static void remove(AttributeInstance attribute) {
        if (attribute != null && attribute.getModifier(MODIFIER_UUID) != null) {
            attribute.removeModifier(MODIFIER_UUID);
        }
    }
}

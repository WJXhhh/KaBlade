package com.wjx.kablade.event;

import com.wjx.kablade.Main;
import com.wjx.kablade.config.KabladeConfig;
import mods.flammpfeil.slashblade.event.SlashBladeRegistryEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 在本模组的拔刀剑被创建时（{@link SlashBladeRegistryEvent.Post}），按 {@link KabladeConfig} 里配置的
 * 全局倍率缩放它的基础攻击与最大耐久。只对 {@code kablade} 命名空间的刀生效，不影响拔刀剑本体及其他附属的刀。
 *
 * <p>为什么拦在这里：拔刀剑的属性在 {@code SlashBladeDefinition.getBlade()} 创建成品时就被烤进了
 * capability（即 NBT），运行时从 capability 读、不实时读定义。所以只要在「创建那一刻」把成品刀
 * capability 里的值乘上倍率写回去即可——无需触碰冻结的数据包注册表，也不用 Mixin。
 *
 * <p>注意：只影响之后新创建的刀；存档里已经造好的刀其 NBT 已固定，不会被追溯修改。
 */
@Mod.EventBusSubscriber(modid = Main.MODID)   // 默认 FORGE 总线，SlashBladeRegistryEvent 正是走这条总线发布
public final class BladeAttributeOverride {

    private BladeAttributeOverride() {
    }

    @SubscribeEvent
    public static void onBladeCreated(SlashBladeRegistryEvent.Post event) {
        // 只处理本模组（kablade 命名空间）的刀，拔刀剑本体及其他附属的刀保持原样。
        if (!Main.MODID.equals(event.getSlashBladeDefinition().getName().getNamespace())) {
            return;
        }

        // 配置尚未加载时（理论上刀的创建都在加载之后，这里仅作保险）直接跳过，避免读取未加载的配置抛异常。
        if (!KabladeConfig.SPEC.isLoaded()) {
            return;
        }

        double attackMul = KabladeConfig.ATTACK_MULTIPLIER.get();
        double durabilityMul = KabladeConfig.DURABILITY_MULTIPLIER.get();
        if (attackMul == 1.0D && durabilityMul == 1.0D) {
            return; // 都是默认倍率，无需改动
        }

        event.getBlade().getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            if (attackMul != 1.0D) {
                state.setBaseAttackModifier((float) (state.getBaseAttackModifier() * attackMul));
            }
            if (durabilityMul != 1.0D) {
                int baseMaxDamage = state.getMaxDamage();
                if (baseMaxDamage > 0) {  // 原本不可破坏/无耐久（<=0）的刀保持原样
                    state.setMaxDamage(Math.max(1, (int) Math.round(baseMaxDamage * durabilityMul)));
                }
            }
        });
    }
}

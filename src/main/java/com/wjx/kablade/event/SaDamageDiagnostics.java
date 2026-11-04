package com.wjx.kablade.event;

import com.wjx.kablade.Main;
import com.wjx.kablade.compat.SstBladeCompat;
import com.wjx.kablade.util.SaCombatScaling;
import mods.flammpfeil.slashblade.capability.concentrationrank.CapabilityConcentrationRank;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

/** Read-only, self-only diagnostics for checking installed modpack damage growth. */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SaDamageDiagnostics {
    private SaDamageDiagnostics() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("kablade").then(Commands.literal("damageinfo")
                .executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    var blade = player.getMainHandItem();
                    var state = blade.getCapability(ItemSlashBlade.BLADESTATE).resolve();
                    if (!(blade.getItem() instanceof ItemSlashBlade) || state.isEmpty()) {
                        context.getSource().sendFailure(Component.literal("请先手持拔刀剑。"));
                        return 0;
                    }
                    String rank = player.getCapability(CapabilityConcentrationRank.RANK_POINT)
                            .map(value -> value.getRank(player.level().getGameTime()).name()).orElse("NONE");
                    String message = String.format(Locale.ROOT,
                            "基础攻击 %.2f；当前攻击 %.2f；锻造 %d；经验 %d；评级 %s\n"
                                    + "SA 攻击成长 ×%.3f（含评级、精炼、附魔及重锋属性倍率）\n"
                                    + "SST 亲密度伤害 +%.2f%%（由整合包另结算一次）",
                            state.get().getBaseAttackModifier(), SaCombatScaling.currentAttack(player, blade),
                            state.get().getRefine(), player.experienceLevel, rank,
                            SaCombatScaling.damageMultiplier(player), SstBladeCompat.affinityBonus(player));
                    context.getSource().sendSuccess(() -> Component.literal(message), false);
                    return 1;
                })));
    }
}

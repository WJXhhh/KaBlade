package com.wjx.kablade.slasharts;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModComboStates;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.capability.inputstate.CapabilityInputState;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.util.InputCommand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;

public final class PiercingChargeEvents {

    private static final String CHARGE_ACTION_EVENT =
            "mods.flammpfeil.slashblade.event.SlashBladeEvent$ChargeActionEvent";
    private static final int START_CHARGE_TICKS = 3;

    private PiercingChargeEvents() {
    }

    @SuppressWarnings("unchecked")
    public static void tryRegister() {
        try {
            Class<?> eventClass = Class.forName(CHARGE_ACTION_EVENT);
            if (!Event.class.isAssignableFrom(eventClass)) {
                Main.LOGGER.warn("[{}] PiercingChargeEvents skipped: {} is not a Forge event",
                        Main.MODID, CHARGE_ACTION_EVENT);
                return;
            }

            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false,
                    (Class<Event>) eventClass.asSubclass(Event.class), PiercingChargeEvents::onChargeAction);
            Main.LOGGER.info("[{}] PiercingChargeEvents registered ({})", Main.MODID, CHARGE_ACTION_EVENT);
        } catch (ClassNotFoundException e) {
            Main.LOGGER.info("[{}] PiercingChargeEvents skipped: SlashBlade ChargeActionEvent is unavailable",
                    Main.MODID);
        }
    }

    private static void onChargeAction(Event event) {
        LivingEntity user = invokeNoArg(event, "getEntityLiving", LivingEntity.class);
        ISlashBladeState state = invokeNoArg(event, "getSlashBladeState", ISlashBladeState.class);
        Integer chargeTicks = invokeIntNoArg(event, "getChargeTicks");
        if (chargeTicks == null) {
            chargeTicks = invokeIntNoArg(event, "getElapsed");
        }

        if (user == null || state == null || chargeTicks == null) {
            return;
        }
        if (user.level().isClientSide() || chargeTicks < START_CHARGE_TICKS) {
            return;
        }
        if (!isRightClicking(user) || isLeftClicking(user)) {
            return;
        }
        if ((state.getSlashArts() instanceof MoltenBladeArts
                || state.getSlashArts() instanceof InductionCollapseArts)
                && canStartPiercingCharge(state.getComboSeq())) {
            setPiercingChargeCombo(event, user, state);
        }
    }

    private static boolean isRightClicking(LivingEntity user) {
        return user.getCapability(CapabilityInputState.INPUT_STATE)
                .map(input -> input.getCommands().contains(InputCommand.R_DOWN)
                        || input.getCommands().contains(InputCommand.R_CLICK))
                .orElse(false);
    }

    private static boolean isLeftClicking(LivingEntity user) {
        return user.getCapability(CapabilityInputState.INPUT_STATE)
                .map(input -> input.getCommands().contains(InputCommand.L_DOWN)
                        || input.getCommands().contains(InputCommand.L_CLICK))
                .orElse(false);
    }

    private static boolean canStartPiercingCharge(ResourceLocation combo) {
        return ComboStateRegistry.NONE.getId().equals(combo)
                || ComboStateRegistry.STANDBY.getId().equals(combo)
                || ComboStateRegistry.COMBO_A1.getId().equals(combo)
                || ComboStateRegistry.COMBO_A1_END.getId().equals(combo)
                || ComboStateRegistry.COMBO_A1_END2.getId().equals(combo);
    }

    private static void setPiercingChargeCombo(Event event, LivingEntity user, ISlashBladeState state) {
        ResourceLocation combo = ModComboStates.PIERCING_CHARGE.getId();
        if (invokeSetComboState(event, combo)) {
            return;
        }
        state.updateComboSeq(user, combo);
    }

    private static boolean invokeSetComboState(Event event, ResourceLocation combo) {
        try {
            event.getClass().getMethod("setComboState", ResourceLocation.class).invoke(event, combo);
            return true;
        } catch (ReflectiveOperationException | SecurityException e) {
            return false;
        }
    }

    private static <T> T invokeNoArg(Object target, String methodName, Class<T> type) {
        try {
            Object value = target.getClass().getMethod(methodName).invoke(target);
            return type.isInstance(value) ? type.cast(value) : null;
        } catch (ReflectiveOperationException | SecurityException e) {
            return null;
        }
    }

    private static Integer invokeIntNoArg(Object target, String methodName) {
        try {
            Object value = target.getClass().getMethod(methodName).invoke(target);
            return value instanceof Integer i ? i : null;
        } catch (ReflectiveOperationException | SecurityException e) {
            return null;
        }
    }
}

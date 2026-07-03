package com.wjx.kablade.event;

import com.wjx.kablade.Main;
import com.wjx.kablade.config.KabladeConfig;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;

/**
 * Applies Kablade's global blade attribute multipliers when SlashBlade 1.9+
 * fires SlashBladeRegistryEvent.Post. Older SlashBlade builds do not have that
 * event, so this class keeps the event type behind runtime reflection.
 */
public final class BladeAttributeOverride {

    private static final String REGISTRY_POST_EVENT =
            "mods.flammpfeil.slashblade.event.SlashBladeRegistryEvent$Post";

    private BladeAttributeOverride() {
    }

    @SuppressWarnings("unchecked")
    public static void tryRegister() {
        try {
            Class<?> eventClass = Class.forName(REGISTRY_POST_EVENT);
            if (!Event.class.isAssignableFrom(eventClass)) {
                Main.LOGGER.warn("[{}] BladeAttributeOverride skipped: {} is not a Forge event",
                        Main.MODID, REGISTRY_POST_EVENT);
                return;
            }

            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false,
                    (Class<Event>) eventClass.asSubclass(Event.class), BladeAttributeOverride::onBladeCreated);
            Main.LOGGER.info("[{}] BladeAttributeOverride registered (SlashBlade 1.9+)", Main.MODID);
        } catch (ClassNotFoundException e) {
            Main.LOGGER.info("[{}] BladeAttributeOverride skipped: SlashBladeRegistryEvent.Post is unavailable",
                    Main.MODID);
        }
    }

    private static void onBladeCreated(Event event) {
        ResourceLocation bladeName = readBladeName(event);
        if (bladeName == null || !Main.MODID.equals(bladeName.getNamespace())) {
            return;
        }

        if (!KabladeConfig.SPEC.isLoaded()) {
            return;
        }

        double attackMul = KabladeConfig.ATTACK_MULTIPLIER.get();
        double durabilityMul = KabladeConfig.DURABILITY_MULTIPLIER.get();
        if (attackMul == 1.0D && durabilityMul == 1.0D) {
            return;
        }

        ItemStack blade = invokeNoArg(event, "getBlade", ItemStack.class);
        if (blade == null) {
            return;
        }

        blade.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            if (attackMul != 1.0D) {
                state.setBaseAttackModifier((float) (state.getBaseAttackModifier() * attackMul));
            }
            if (durabilityMul != 1.0D) {
                int baseMaxDamage = state.getMaxDamage();
                if (baseMaxDamage > 0) {
                    state.setMaxDamage(Math.max(1, (int) Math.round(baseMaxDamage * durabilityMul)));
                }
            }
        });
    }

    private static ResourceLocation readBladeName(Event event) {
        Object definition = invokeRawNoArg(event, "getSlashBladeDefinition");
        return definition == null ? null : invokeNoArg(definition, "getName", ResourceLocation.class);
    }

    private static Object invokeRawNoArg(Object target, String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException | SecurityException e) {
            return null;
        }
    }

    private static <T> T invokeNoArg(Object target, String methodName, Class<T> type) {
        Object value = invokeRawNoArg(target, methodName);
        return type.isInstance(value) ? type.cast(value) : null;
    }
}

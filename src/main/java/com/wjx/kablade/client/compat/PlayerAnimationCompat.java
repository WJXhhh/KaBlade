package com.wjx.kablade.client.compat;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModComboStates;
import com.wjx.kablade.util.ResourceUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Safe registration helper for Player Animator animation tracks.
 * Loads Player Animation classes reflectively so compile time doesn't require dev.kosmx.playerAnim.
 */
public final class PlayerAnimationCompat {

    private PlayerAnimationCompat() {
    }

    public static void init() {
        if (ModList.get().isLoaded("playeranimator")) {
            registerAnimations();
        }
    }

    @SuppressWarnings("unchecked")
    private static void registerAnimations() {
        try {
            Class<?> overriderClass = Class.forName("mods.flammpfeil.slashblade.compat.playerAnim.PlayerAnimationOverrider");
            Object overrider = overriderClass.getMethod("getInstance").invoke(null);
            Map<ResourceLocation, Object> animMap = (Map<ResourceLocation, Object>) overriderClass.getMethod("getAnimation").invoke(overrider);

            Class<?> vmdAnimClass = Class.forName("mods.flammpfeil.slashblade.compat.playerAnim.VmdAnimation");
            Constructor<?> ctor = vmdAnimClass.getConstructor(ResourceLocation.class, double.class, double.class, boolean.class);
            Object anim = ctor.newInstance(
                    ResourceUtil.getLocation("combostate/fusion_nuclear_shock_player.vmd"),
                    0.0D,
                    67.0D,
                    false);

            Method setBlendArms = vmdAnimClass.getMethod("setBlendArms", boolean.class);
            Method setBlendLegs = vmdAnimClass.getMethod("setBlendLegs", boolean.class);
            setBlendArms.invoke(anim, true);
            setBlendLegs.invoke(anim, true);

            animMap.put(ModComboStates.FUSION_NUCLEAR_SHOCK.getId(), anim);
            Main.LOGGER.info("Registered Player Animator tracking for Nuclear Shock");

            Object valkyrieAnim = ctor.newInstance(
                    ResourceUtil.getLocation("combostate/valkyrie_impact_player.vmd"),
                    0.0D,
                    67.0D,
                    false);
            setBlendArms.invoke(valkyrieAnim, true);
            setBlendLegs.invoke(valkyrieAnim, true);
            animMap.put(ModComboStates.VALKYRIE_IMPACT.getId(), valkyrieAnim);
            Main.LOGGER.info("Registered Player Animator tracking for Valkyrie Impact");
        } catch (Throwable t) {
            Main.LOGGER.warn("Failed to register Player Animator animations: {}", t.getMessage());
        }
    }
}

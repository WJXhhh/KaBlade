package com.wjx.kablade.init;

import com.wjx.kablade.Main;
import com.wjx.kablade.util.ResourceUtil;
import mods.flammpfeil.slashblade.init.DefaultResources;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModComboStates {

    public static final DeferredRegister<ComboState> REGISTRY =
            DeferredRegister.create(ComboState.REGISTRY_KEY, Main.MODID);

    public static final RegistryObject<ComboState> LIGHTS_ON_STAGE_CHADI = REGISTRY.register(
            "lights_on_stage_chadi",
            () -> ComboState.Builder.newInstance()
                    .startAndEnd(0, 23)
                    .priority(50)
                    .motionLoc(ResourceUtil.getLocation("combostate/chadi.vmd"))
                    .next(entity -> ComboStateRegistry.NONE.getId())
                    .nextOfTimeout(entity -> ComboStateRegistry.NONE.getId())
                    .build());

    public static final RegistryObject<ComboState> PIERCING_CHARGE = REGISTRY.register(
            "piercing_charge",
            () -> ComboState.Builder.newInstance()
                    .startAndEnd(1, 33)
                    .priority(50)
                    .timeout(72000)
                    .motionLoc(DefaultResources.testLocation)
                    .next(entity -> ComboStateRegistry.PIERCING_2.getId())
                    .nextOfTimeout(entity -> ComboStateRegistry.NONE.getId())
                    .build());

    public static final RegistryObject<ComboState> PIERCING_CHARGE_CANCEL = REGISTRY.register(
            "piercing_charge_cancel",
            () -> ComboState.Builder.newInstance()
                    .startAndEnd(33, 55)
                    .priority(50)
                    .motionLoc(DefaultResources.testLocation)
                    .next(entity -> ComboStateRegistry.NONE.getId())
                    .nextOfTimeout(entity -> ComboStateRegistry.NONE.getId())
                    .build());

    private ModComboStates() {
    }
}

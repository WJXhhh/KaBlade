package com.wjx.kablade.init;

import com.wjx.kablade.Main;
import com.wjx.kablade.util.ResourceUtil;
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

    private ModComboStates() {
    }
}

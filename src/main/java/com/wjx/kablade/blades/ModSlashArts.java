package com.wjx.kablade.blades;

import com.wjx.kablade.Main;
import com.wjx.kablade.slasharts.RockStrikeArts;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModSlashArts {

    public static final DeferredRegister<SlashArts> REGISTRY =
            DeferredRegister.create(SlashArts.REGISTRY_KEY, Main.MODID);

    public static final RegistryObject<SlashArts> ROCK_STRIKE = REGISTRY.register(
            "rock_strike",
            () -> new RockStrikeArts(entity -> ComboStateRegistry.NONE.getId()));

    private ModSlashArts() {
    }
}

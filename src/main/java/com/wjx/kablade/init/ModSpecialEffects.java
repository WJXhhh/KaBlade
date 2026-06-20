package com.wjx.kablade.init;

import com.wjx.kablade.Main;
import com.wjx.kablade.specialeffect.Oripursuit;
import com.wjx.kablade.specialeffect.Phoenix;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * KBlade2 的拔刀剑特殊效果注册中心。
 */
public final class ModSpecialEffects {

    public static final DeferredRegister<SpecialEffect> REGISTRY =
            DeferredRegister.create(SpecialEffect.REGISTRY_KEY, Main.MODID);

    public static final RegistryObject<SpecialEffect> ORIPURSUIT = REGISTRY.register(
            "oripursuit", Oripursuit::new);

    public static final RegistryObject<SpecialEffect> PHOENIX = REGISTRY.register(
            "phoenix", Phoenix::new);

    private ModSpecialEffects() {
    }
}

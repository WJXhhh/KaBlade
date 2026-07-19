package com.wjx.kablade.init;

import com.wjx.kablade.Main;
import com.wjx.kablade.specialeffect.BurstDrive;
import com.wjx.kablade.specialeffect.DivinePenalty;
import com.wjx.kablade.specialeffect.EMInduction;
import com.wjx.kablade.specialeffect.EMPulsar;
import com.wjx.kablade.specialeffect.FuelTheRuin;
import com.wjx.kablade.specialeffect.GlacialBane;
import com.wjx.kablade.specialeffect.HolyEnergyImpact;
import com.wjx.kablade.specialeffect.Oripursuit;
import com.wjx.kablade.specialeffect.Phoenix;
import com.wjx.kablade.specialeffect.PowerOfWind;
import com.wjx.kablade.specialeffect.RagingIzumo;
import com.wjx.kablade.specialeffect.RoaringNimbus;
import com.wjx.kablade.specialeffect.SPLighting;
import com.wjx.kablade.specialeffect.ThunderBlitz;
import com.wjx.kablade.specialeffect.ThunderDivinePenalty;
import com.wjx.kablade.specialeffect.Turbulence;
import com.wjx.kablade.specialeffect.UndyingSaltiness;
import com.wjx.kablade.specialeffect.TrueSelf;
import com.wjx.kablade.specialeffect.Unthinkable;
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

    public static final RegistryObject<SpecialEffect> TURBULENCE = REGISTRY.register(
            "turbulence", Turbulence::new);

    public static final RegistryObject<SpecialEffect> DIVINE_PENALTY = REGISTRY.register(
            "divine_penalty", DivinePenalty::new);

    public static final RegistryObject<SpecialEffect> SP_LIGHTING = REGISTRY.register(
            "sp_lighting", SPLighting::new);

    public static final RegistryObject<SpecialEffect> BURST_DRIVE = REGISTRY.register(
            "burst_drive", BurstDrive::new);

    public static final RegistryObject<SpecialEffect> POWER_OF_WIND = REGISTRY.register(
            "power_of_wind", PowerOfWind::new);

    public static final RegistryObject<SpecialEffect> HOLY_ENERGY_IMPACT = REGISTRY.register(
            "holy_energy_impact", HolyEnergyImpact::new);

    public static final RegistryObject<SpecialEffect> UNDYING_SALTINESS = REGISTRY.register(
            "undying_saltiness", UndyingSaltiness::new);

    public static final RegistryObject<SpecialEffect> RAGING_IZUMO = REGISTRY.register(
            "raging_izumo", RagingIzumo::new);

    public static final RegistryObject<SpecialEffect> EM_INDUCTION = REGISTRY.register(
            "em_induction", EMInduction::new);

    public static final RegistryObject<SpecialEffect> EM_PULSAR = REGISTRY.register(
            "em_pulsar", EMPulsar::new);

    public static final RegistryObject<SpecialEffect> ROARING_NIMBUS = REGISTRY.register(
            "roaring_nimbus", RoaringNimbus::new);

    public static final RegistryObject<SpecialEffect> GLACIAL_BANE = REGISTRY.register(
            "glacial_bane", GlacialBane::new);

    public static final RegistryObject<SpecialEffect> THUNDER_BLITZ = REGISTRY.register(
            "thunder_blitz", ThunderBlitz::new);

    public static final RegistryObject<SpecialEffect> THUNDER_DIVINE_PENALTY = REGISTRY.register(
            "thunder_divine_penalty", ThunderDivinePenalty::new);

    public static final RegistryObject<SpecialEffect> TRUE_SELF = REGISTRY.register(
            "true_self", TrueSelf::new);

    public static final RegistryObject<SpecialEffect> UNTHINKABLE = REGISTRY.register(
            "unthinkable", Unthinkable::new);

    public static final RegistryObject<SpecialEffect> FUEL_THE_RUIN = REGISTRY.register(
            "fuel_the_ruin", FuelTheRuin::new);

    private ModSpecialEffects() {
    }
}

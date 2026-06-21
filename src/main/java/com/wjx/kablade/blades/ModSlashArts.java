package com.wjx.kablade.blades;

import com.wjx.kablade.Main;
import com.wjx.kablade.slasharts.AbsoluteZeroArts;
import com.wjx.kablade.slasharts.AuroraShiningArts;
import com.wjx.kablade.slasharts.BladeWardArts;
import com.wjx.kablade.slasharts.BreakTheDawnArts;
import com.wjx.kablade.slasharts.ChopWillowArts;
import com.wjx.kablade.slasharts.CutMetalArts;
import com.wjx.kablade.slasharts.DomainSuppressionArts;
import com.wjx.kablade.slasharts.FireOfSinArts;
import com.wjx.kablade.slasharts.FrostBladeArts;
import com.wjx.kablade.slasharts.FrostCometArts;
import com.wjx.kablade.slasharts.InductionCollapseArts;
import com.wjx.kablade.slasharts.LacerateBladeArts;
import com.wjx.kablade.slasharts.MoltenBladeArts;
import com.wjx.kablade.slasharts.RockStrikeArts;
import com.wjx.kablade.slasharts.ShockImpactArts;
import com.wjx.kablade.slasharts.VorpalHoleArts;
import com.wjx.kablade.slasharts.ZaizanArts;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModSlashArts {

    public static final DeferredRegister<SlashArts> REGISTRY =
            DeferredRegister.create(SlashArts.REGISTRY_KEY, Main.MODID);

    public static final RegistryObject<SlashArts> ROCK_STRIKE = REGISTRY.register(
            "rock_strike",
            () -> new RockStrikeArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> BREAK_THE_DAWN = REGISTRY.register(
            "break_the_dawn",
            () -> new BreakTheDawnArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> AURORA_SHINING = REGISTRY.register(
            "aurora_shining",
            () -> new AuroraShiningArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> CUT_METAL = REGISTRY.register(
            "cut_metal",
            () -> new CutMetalArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> DOMAIN_SUPPRESSION = REGISTRY.register(
            "domain_suppression",
            () -> new DomainSuppressionArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> LACERATE_BLADE = REGISTRY.register(
            "lacerate_blade",
            () -> new LacerateBladeArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> CHOP_WILLOW = REGISTRY.register(
            "chop_willow",
            () -> new ChopWillowArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> FIRE_OF_SIN = REGISTRY.register(
            "fire_of_sin",
            () -> new FireOfSinArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> FROST_COMET = REGISTRY.register(
            "frost_comet",
            () -> new FrostCometArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> MOLTEN_BLADE = REGISTRY.register(
            "molten_blade",
            () -> new MoltenBladeArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> ABSOLUTE_ZERO = REGISTRY.register(
            "absolute_zero",
            () -> new AbsoluteZeroArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> FROST_BLADE = REGISTRY.register(
            "frost_blade",
            () -> new FrostBladeArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> BLADE_WARD = REGISTRY.register(
            "blade_ward",
            () -> new BladeWardArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> SHOCK_IMPACT = REGISTRY.register(
            "shock_impact",
            () -> new ShockImpactArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> VORPAL_HOLE = REGISTRY.register(
            "vorpal_hole",
            () -> new VorpalHoleArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> ZAIZAN = REGISTRY.register(
            "zaizan",
            () -> new ZaizanArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> INDUCTION_COLLAPSE = REGISTRY.register(
            "induction_collapse",
            () -> new InductionCollapseArts(entity -> ComboStateRegistry.NONE.getId()));

    private ModSlashArts() {
    }
}

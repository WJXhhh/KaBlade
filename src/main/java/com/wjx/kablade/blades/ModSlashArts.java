package com.wjx.kablade.blades;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModComboStates;
import com.wjx.kablade.slasharts.AbsoluteZeroArts;
import com.wjx.kablade.slasharts.AuroraShiningArts;
import com.wjx.kablade.slasharts.BladeWardArts;
import com.wjx.kablade.slasharts.BreakTheDawnArts;
import com.wjx.kablade.slasharts.ChopWillowArts;
import com.wjx.kablade.slasharts.CrimsonSakuraArts;
import com.wjx.kablade.slasharts.CutMetalArts;
import com.wjx.kablade.slasharts.DomainSuppressionArts;
import com.wjx.kablade.slasharts.FallingPetalsArts;
import com.wjx.kablade.slasharts.FireOfSinArts;
import com.wjx.kablade.slasharts.FengxuanArts;
import com.wjx.kablade.slasharts.FlashArts;
import com.wjx.kablade.slasharts.FrostBladeArts;
import com.wjx.kablade.slasharts.FrostCometArts;
import com.wjx.kablade.slasharts.AquaEdgeArts;
import com.wjx.kablade.slasharts.InductionCollapseArts;
import com.wjx.kablade.slasharts.KamiOfWarArts;
import com.wjx.kablade.slasharts.LacerateBladeArts;
import com.wjx.kablade.slasharts.LaveDriveArts;
import com.wjx.kablade.slasharts.LethalThrustArts;
import com.wjx.kablade.slasharts.LiediArts;
import com.wjx.kablade.slasharts.LightningSwordsArts;
import com.wjx.kablade.slasharts.LightsOnStageArts;
import com.wjx.kablade.slasharts.LoveIsWarArts;
import com.wjx.kablade.slasharts.MagChaosBladeArts;
import com.wjx.kablade.slasharts.MoltenBladeArts;
import com.wjx.kablade.slasharts.MoonFangArts;
import com.wjx.kablade.slasharts.OverSlashArts;
import com.wjx.kablade.slasharts.PhantomButterflyArts;
import com.wjx.kablade.slasharts.PhantomButterflySArts;
import com.wjx.kablade.slasharts.RagingFireArts;
import com.wjx.kablade.slasharts.RainbowSlashArts;
import com.wjx.kablade.slasharts.RockStrikeArts;
import com.wjx.kablade.slasharts.StarBurstArts;
import com.wjx.kablade.slasharts.StarChaseArts;
import com.wjx.kablade.slasharts.RandomSaArts;
import com.wjx.kablade.slasharts.ShockImpactArts;
import com.wjx.kablade.slasharts.SnowDanceArts;
import com.wjx.kablade.slasharts.SoulOfFrostArts;
import com.wjx.kablade.slasharts.ThunderEdgeArts;
import com.wjx.kablade.slasharts.SwordEnlightenmentArts;
import com.wjx.kablade.slasharts.UtpalaAuraArts;
import com.wjx.kablade.slasharts.VorpalHoleArts;
import com.wjx.kablade.slasharts.WindEnchantmentArts;
import com.wjx.kablade.slasharts.YuqiArts;
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

    public static final RegistryObject<SlashArts> FALLING_PETALS = REGISTRY.register(
            "falling_petals",
            () -> new FallingPetalsArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> CRIMSON_SAKURA = REGISTRY.register(
            "crimson_sakura",
            () -> new CrimsonSakuraArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> LETHAL_THRUST = REGISTRY.register(
            "lethal_thrust",
            () -> new LethalThrustArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> LOVE_IS_WAR = REGISTRY.register(
            "love_is_war",
            () -> new LoveIsWarArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> INDUCTION_COLLAPSE = REGISTRY.register(
            "induction_collapse",
            () -> new InductionCollapseArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> OVER_SLASH = REGISTRY.register(
            "over_slash",
            () -> new OverSlashArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> LIGHTNING_SWORDS = REGISTRY.register(
            "lightning_swords",
            () -> new LightningSwordsArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> LAVE_DRIVE = REGISTRY.register(
            "lave_drive",
            () -> new LaveDriveArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> AQUA_EDGE = REGISTRY.register(
            "aqua_edge",
            () -> new AquaEdgeArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> LIGHTS_ON_STAGE = REGISTRY.register(
            "lights_on_stage",
            () -> new LightsOnStageArts(entity -> ModComboStates.LIGHTS_ON_STAGE_CHADI.getId()));

    public static final RegistryObject<SlashArts> KAMI_OF_WAR = REGISTRY.register(
            "kami_of_war",
            () -> new KamiOfWarArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> WIND_ENCHANTMENT = REGISTRY.register(
            "wind_enchantment",
            () -> new WindEnchantmentArts(entity -> ComboStateRegistry.NONE.getId()));

    // ── 万物皆刃线 ──
    public static final RegistryObject<SlashArts> RAGING_FIRE = REGISTRY.register(
            "raging_fire",
            () -> new RagingFireArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> STAR_BURST = REGISTRY.register(
            "star_burst",
            () -> new StarBurstArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> RAINBOW_SLASH = REGISTRY.register(
            "rainbow_slash",
            () -> new RainbowSlashArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> FENGXUAN = REGISTRY.register(
            "fengxuan",
            () -> new FengxuanArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> YUQI = REGISTRY.register(
            "yuqi",
            () -> new YuqiArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> RANDOM_SA = REGISTRY.register(
            "random_sa",
            () -> new RandomSaArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> STAR_CHASE = REGISTRY.register(
            "star_chase",
            () -> new StarChaseArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> MOON_FANG = REGISTRY.register(
            "moon_fang",
            () -> new MoonFangArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> MAG_CHAOS_BLADE = REGISTRY.register(
            "mag_chaos_blade",
            () -> new MagChaosBladeArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> SNOW_DANCE = REGISTRY.register(
            "snow_dance",
            () -> new SnowDanceArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> THUNDER_EDGE = REGISTRY.register(
            "thunder_edge",
            () -> new ThunderEdgeArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> UTPALA_AURA = REGISTRY.register(
            "utpala_aura",
            () -> new UtpalaAuraArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> SWORD_ENLIGHTENMENT = REGISTRY.register(
            "sword_enlightenment",
            () -> new SwordEnlightenmentArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> SOUL_OF_FROST = REGISTRY.register(
            "soul_of_frost",
            () -> new SoulOfFrostArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> FLASH = REGISTRY.register(
            "flash",
            () -> new FlashArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> LIEDI = REGISTRY.register(
            "liedi",
            () -> new LiediArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> PHANTOM_BUTTERFLY = REGISTRY.register(
            "phantom_butterfly",
            () -> new PhantomButterflyArts(entity -> ComboStateRegistry.NONE.getId()));

    public static final RegistryObject<SlashArts> PHANTOM_BUTTERFLY_S = REGISTRY.register(
            "phantom_butterfly_s",
            () -> new PhantomButterflySArts(entity -> ComboStateRegistry.NONE.getId()));

    private ModSlashArts() {
    }
}

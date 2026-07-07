package com.wjx.kablade.init;

import com.wjx.kablade.Main;
import com.wjx.kablade.entity.*;
import com.wjx.kablade.entity.WindEnchantmentEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** DeferredRegister hub for this mod's entities. */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> REGISTRY =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Main.MODID);

    /** 「岩石撼击」的岩刺表现实体。 */
    public static final RegistryObject<EntityType<RockSpikeEntity>> ROCK_SPIKE = REGISTRY.register(
            "rock_spike",
            () -> EntityType.Builder.<RockSpikeEntity>of(RockSpikeEntity::new, MobCategory.MISC)
                    .sized(0.6F, 2.0F)
                    .clientTrackingRange(8)
                    .updateInterval(20)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("rock_spike"));

    /** 「极光映天」的极光帷幕表现实体。 */
    public static final RegistryObject<EntityType<AuroraVeilEntity>> AURORA_VEIL = REGISTRY.register(
            "aurora_veil",
            () -> EntityType.Builder.<AuroraVeilEntity>of(AuroraVeilEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("aurora_veil"));

    /** 「弧光破晓」的破晓弧月表现实体。 */
    public static final RegistryObject<EntityType<DawnCrescentEntity>> DAWN_CRESCENT = REGISTRY.register(
            "dawn_crescent",
            () -> EntityType.Builder.<DawnCrescentEntity>of(DawnCrescentEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("dawn_crescent"));

    /** 「斩铁断金」的白色环形刀光表现实体。 */
    public static final RegistryObject<EntityType<CutMetalRingEntity>> CUT_METAL_RING = REGISTRY.register(
            "cut_metal_ring",
            () -> EntityType.Builder.<CutMetalRingEntity>of(CutMetalRingEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(12)
                    .updateInterval(20)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("cut_metal_ring"));

    /** 「领域压杀」的源能自由剑。 */
    public static final RegistryObject<EntityType<OriginFreeSwordEntity>> ORIGIN_FREE_SWORD = REGISTRY.register(
            "origin_free_sword",
            () -> EntityType.Builder.<OriginFreeSwordEntity>of(OriginFreeSwordEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("origin_free_sword"));

    /** 「时空黑洞」的反力场黑洞实体。 */
    public static final RegistryObject<EntityType<VorpalBlackHoleEntity>> VORPAL_BLACK_HOLE = REGISTRY.register(
            "vorpal_black_hole",
            () -> EntityType.Builder.<VorpalBlackHoleEntity>of(VorpalBlackHoleEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("vorpal_black_hole"));

    /** 「雷切」SA 召唤的护盾实体。 */
    public static final RegistryObject<EntityType<RaikiriShieldEntity>> RAIKIRI_SHIELD = REGISTRY.register(
            "raikiri_shield",
            () -> EntityType.Builder.<RaikiriShieldEntity>of(RaikiriShieldEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("raikiri_shield"));

    /** 「高频坍缩」SA 召唤的禁锢力场实体。 */
    public static final RegistryObject<EntityType<ConfinementForceFieldEntity>> CONFINEMENT_FORCE_FIELD = REGISTRY.register(
            "confinement_force_field",
            () -> EntityType.Builder.<ConfinementForceFieldEntity>of(ConfinementForceFieldEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("confinement_force_field"));

    /** 「寒霜灵刃」的追踪冰晶飞剑与命中晶簇。 */
    public static final RegistryObject<EntityType<FrostBladeEntity>> FROST_BLADE_EDGE = REGISTRY.register(
            "frost_blade_edge",
            () -> EntityType.Builder.<FrostBladeEntity>of(FrostBladeEntity::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("frost_blade_edge"));

    /** Visible freeze domain for Ice Epiphyllum's Snow Dance. */
    public static final RegistryObject<EntityType<FreezeDomainEntity>> FREEZE_DOMAIN = REGISTRY.register(
            "freeze_domain",
            () -> EntityType.Builder.<FreezeDomainEntity>of(FreezeDomainEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("freeze_domain"));

    /** Visual and hit timing anchor for Frozen Naraka's Utpala Aura. */
    public static final RegistryObject<EntityType<UtpalaAuraEntity>> UTPALA_AURA = REGISTRY.register(
            "utpala_aura",
            () -> EntityType.Builder.<UtpalaAuraEntity>of(UtpalaAuraEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("utpala_aura"));

    /** Visual anchor for the Shock Impact SA. */
    public static final RegistryObject<EntityType<ShockImpactEntity>> SHOCK_IMPACT = REGISTRY.register(
            "shock_impact",
            () -> EntityType.Builder.<ShockImpactEntity>of(ShockImpactEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("shock_impact"));

    /** Visual anchor for Key of Castigation's Thunder Edge SA. */
    public static final RegistryObject<EntityType<ThunderEdgeAttackEntity>> THUNDER_EDGE_ATTACK = REGISTRY.register(
            "thunder_edge_attack",
            () -> EntityType.Builder.<ThunderEdgeAttackEntity>of(ThunderEdgeAttackEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("thunder_edge_attack"));

    /** Visual and hitbox entity for AllWeapon Fengxuan's modified SlashDimension. */
    public static final RegistryObject<EntityType<FengxuanDimensionEntity>> FENGXUAN_DIMENSION = REGISTRY.register(
            "fengxuan_dimension",
            () -> EntityType.Builder.<FengxuanDimensionEntity>of(FengxuanDimensionEntity::new, MobCategory.MISC)
                    .sized(4.0F, 4.0F)
                    .clientTrackingRange(12)
                    .updateInterval(20)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("fengxuan_dimension"));

    /** Visual anchor for the Zaizan SA. */
    public static final RegistryObject<EntityType<ZaizanEntity>> ZAIZAN = REGISTRY.register(
            "zaizan",
            () -> EntityType.Builder.<ZaizanEntity>of(ZaizanEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("zaizan"));

    public static final RegistryObject<EntityType<CrimsonSakuraAttackEntity>> CRIMSON_SAKURA = REGISTRY.register(
            "crimson_sakura",
            () -> EntityType.Builder.<CrimsonSakuraAttackEntity>of(CrimsonSakuraAttackEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("crimson_sakura"));

    /** 「风之结界」光环实体。 */
    public static final RegistryObject<EntityType<TunaEntity>> TUNA = REGISTRY.register(
            "tuna",
            () -> EntityType.Builder.<TunaEntity>of(TunaEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("tuna"));

    public static final RegistryObject<EntityType<RainUmbrellaEntity>> RAIN_UMBRELLA = REGISTRY.register(
            "rain_umbrella",
            () -> EntityType.Builder.<RainUmbrellaEntity>of(RainUmbrellaEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("rain_umbrella"));

    public static final RegistryObject<EntityType<SummonedHedraEntity>> SUMMONED_HEDRA = REGISTRY.register(
            "summoned_hedra",
            () -> EntityType.Builder.<SummonedHedraEntity>of(SummonedHedraEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.2F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("summoned_hedra"));

    public static final RegistryObject<EntityType<WindEnchantmentEntity>> WIND_ENCHANTMENT = REGISTRY.register(
            "wind_enchantment",
            () -> EntityType.Builder.<WindEnchantmentEntity>of(WindEnchantmentEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("wind_enchantment"));

    // ── SP Light 线 SA 实体 ──────────────────────────────────

    /** 通用可变速驱动实体（ExSaEntityDrive 基类）。 */
    public static final RegistryObject<EntityType<ExSlashDriveEntity>> EX_SLASH_DRIVE = REGISTRY.register(
            "ex_slash_drive",
            () -> EntityType.Builder.<ExSlashDriveEntity>of(ExSlashDriveEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("ex_slash_drive"));

    /** 「撕裂灵刃」暗红驱动实体。 */
    public static final RegistryObject<EntityType<LacerateDriveEntity>> LACERATE_DRIVE = REGISTRY.register(
            "lacerate_drive",
            () -> EntityType.Builder.<LacerateDriveEntity>of(LacerateDriveEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("lacerate_drive"));

    /** 熔岩驱动（FlareEdge）。 */
    public static final RegistryObject<EntityType<FlareEdgeEntity>> FLARE_EDGE = REGISTRY.register(
            "flare_edge",
            () -> EntityType.Builder.<FlareEdgeEntity>of(FlareEdgeEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("flare_edge"));

    /** 苍流刃（AquaEdge）。 */
    public static final RegistryObject<EntityType<AquaEdgeEntity>> AQUA_EDGE = REGISTRY.register(
            "aqua_edge",
            () -> EntityType.Builder.<AquaEdgeEntity>of(AquaEdgeEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("aqua_edge"));

    /** 幻影剑Ex。 */
    public static final RegistryObject<EntityType<PhantomSwordExEntity>> PHANTOM_SWORD_EX = REGISTRY.register(
            "phantom_sword_ex",
            () -> EntityType.Builder.<PhantomSwordExEntity>of(PhantomSwordExEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("phantom_sword_ex"));

    /** AllWeapon Phantom Butterfly's 1.12.2 butterfly-shaped phantom sword. */
    public static final RegistryObject<EntityType<ButterflySwordEntity>> BUTTERFLY_SWORD = REGISTRY.register(
            "butterfly_sword",
            () -> EntityType.Builder.<ButterflySwordEntity>of(ButterflySwordEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("butterfly_sword"));

    /** 「追星剑」金色归航幻影剑（奉神刀·鹿）。 */
    public static final RegistryObject<EntityType<StarSwordEntity>> STAR_SWORD = REGISTRY.register(
            "star_sword",
            () -> EntityType.Builder.<StarSwordEntity>of(StarSwordEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("star_sword"));

    /** 闪电剑。 */
    public static final RegistryObject<EntityType<LightningSwordEntity>> LIGHTNING_SWORD = REGISTRY.register(
            "lightning_sword",
            () -> EntityType.Builder.<LightningSwordEntity>of(LightningSwordEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("lightning_sword"));

    /** 「聚光舞台」的金白色舞台光环表现实体。 */
    public static final RegistryObject<EntityType<StageLightEntity>> STAGE_LIGHT = REGISTRY.register(
            "stage_light",
            () -> EntityType.Builder.<StageLightEntity>of(StageLightEntity::new, MobCategory.MISC)
                    .sized(1.0F, 0.25F)
                    .clientTrackingRange(12)
                    .updateInterval(20)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .build("stage_light"));

    private ModEntities() {
    }
}

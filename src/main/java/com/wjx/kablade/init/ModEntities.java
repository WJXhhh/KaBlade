package com.wjx.kablade.init;

import com.wjx.kablade.Main;
import com.wjx.kablade.entity.AuroraVeilEntity;
import com.wjx.kablade.entity.ConfinementForceFieldEntity;
import com.wjx.kablade.entity.CutMetalRingEntity;
import com.wjx.kablade.entity.DawnCrescentEntity;
import com.wjx.kablade.entity.OriginFreeSwordEntity;
import com.wjx.kablade.entity.RaikiriShieldEntity;
import com.wjx.kablade.entity.RockSpikeEntity;
import com.wjx.kablade.entity.VorpalBlackHoleEntity;
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

    private ModEntities() {
    }
}

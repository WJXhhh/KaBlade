package com.wjx.kablade.init;

import com.wjx.kablade.Main;
import com.wjx.kablade.entity.RockSpikeEntity;
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

    private ModEntities() {
    }
}

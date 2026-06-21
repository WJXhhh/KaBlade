package com.wjx.kablade.init;

import com.wjx.kablade.Main;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCK_REGISTRY =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Main.MODID);

    public static final RegistryObject<Block> RIMMED_EARTH = BLOCK_REGISTRY.register(
            "rimmed_earth",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIRT)
                    .strength(0.5f)));

    /** 铬矿石——需要铁镐以上开采。 */
    public static final RegistryObject<Block> CHROMIUM_ORE = BLOCK_REGISTRY.register(
            "chromium_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.5f, 3.0f)
                    .requiresCorrectToolForDrops()));

    /** 极光矿石——生成在寒带群系 Y=40~80，需要铁镐以上开采。 */
    public static final RegistryObject<Block> AURORA_ORE = BLOCK_REGISTRY.register(
            "aurora_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f, 3.0f)
                    .requiresCorrectToolForDrops()));

    /** 辉钼矿石——主世界 Y=40~80 生成，需要铁镐以上开采。 */
    public static final RegistryObject<Block> MOLYBDENITE = BLOCK_REGISTRY.register(
            "molybdenite",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.5f, 3.0f)
                    .requiresCorrectToolForDrops()));
}

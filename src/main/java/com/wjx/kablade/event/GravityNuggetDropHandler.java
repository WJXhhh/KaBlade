package com.wjx.kablade.event;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModBlocks;
import com.wjx.kablade.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GravityNuggetDropHandler {
    private static final double STONE_CHANCE = 0.003D;
    private static final double KABLADE_ORE_CHANCE = 0.01D;

    private GravityNuggetDropHandler() {
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        double chance = getDropChance(event.getState());
        if (chance <= 0.0D || level.random.nextDouble() >= chance) {
            return;
        }

        BlockPos pos = event.getPos();
        ItemEntity item = new ItemEntity(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                new ItemStack(ModItems.GRAVITY_NUGGET.get()));
        level.addFreshEntity(item);
    }

    private static double getDropChance(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.STONE) {
            return STONE_CHANCE;
        }
        if (block == ModBlocks.CHROMIUM_ORE.get()
                || block == ModBlocks.MOLYBDENITE.get()
                || block == ModBlocks.AURORA_ORE.get()) {
            return KABLADE_ORE_CHANCE;
        }
        return 0.0D;
    }
}

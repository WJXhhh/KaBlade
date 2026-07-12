package com.wjx.kablade.event;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Restores the 1.12.2 chance of finding a petal while breaking grass or leaves. */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PetalDropHandler {
    private static final double DROP_CHANCE = 0.03D;

    private PetalDropHandler() {
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !isPetalSource(event.getState())
                || level.random.nextDouble() >= DROP_CHANCE) {
            return;
        }

        BlockPos pos = event.getPos();
        level.addFreshEntity(new ItemEntity(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                new ItemStack(ModItems.PETAL.get())));
    }

    private static boolean isPetalSource(BlockState state) {
        return state.is(BlockTags.LEAVES)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.GRASS)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.DEAD_BUSH);
    }
}

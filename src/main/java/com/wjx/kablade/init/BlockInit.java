package com.wjx.kablade.init;

import com.wjx.kablade.objects.blocks.BlockBase;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import java.util.ArrayList;
import java.util.List;

import static com.wjx.kablade.Main.TABKABLADE;

public class BlockInit {

    public static final List<Block> Blocks = new ArrayList<>();

    public static final Block RIMMED_EARTH = new BlockBase("rimmed_earth", Material.GROUND, TABKABLADE);
}

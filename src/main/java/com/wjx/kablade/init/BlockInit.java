package com.wjx.kablade.init;

import com.wjx.kablade.objects.blocks.BlockBase;
import com.wjx.kablade.objects.blocks.BlockOre;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import java.util.ArrayList;
import java.util.List;

import static com.wjx.kablade.Main.TABKABLADE;
import static com.wjx.kablade.Main.TABKABLADE_ORE;

public class BlockInit {

    public static final List<Block> Blocks = new ArrayList<>();

    public static final Block RIMMED_EARTH = new BlockBase("rimmed_earth", Material.GROUND, TABKABLADE);

    public static final Block CHROMIUM_ORE = new BlockOre("chromium_ore",TABKABLADE_ORE,2.5f,1);
    public static final Block MOLYBDENITE = new BlockOre("molybdenite",TABKABLADE_ORE,2.5f,1);
}

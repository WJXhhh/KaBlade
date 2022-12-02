package com.wjx.kablade.init;

import com.wjx.kablade.objects.blocks.BlockBase;
import com.wjx.kablade.objects.blocks.BlockOre;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.wjx.kablade.Main.TABKABLADE;
import static com.wjx.kablade.Main.TABKABLADE_ORE;

public class BlockInit {

    public static final List<Block> Blocks = new ArrayList<>();

    public static final Block RIMMED_EARTH = new BlockBase("rimmed_earth", Material.GROUND, TABKABLADE);

    public static final Block CHROMIUM_ORE = new BlockOre("chromium_ore",TABKABLADE_ORE,2.5f,1);
    public static final Block MOLYBDENITE = new BlockOre("molybdenite",TABKABLADE_ORE,2.5f,1);
    public static final Block AURORA_ORE = new BlockOre("aurora_ore",TABKABLADE_ORE,3,2){
        @Nonnull
        @Override
        public Item getItemDropped(IBlockState state, Random rand, int fortune) {
            return ItemInit.AURORA_FRAGMENT;
        }

        // SRG func_149745_a，用于决定掉落的物品数量
        @Override
        public int quantityDropped(Random random) {
            return (random.nextInt(2) + 1);
        }

        // SRG func_149679_a，用于决定受时运影响时掉落的物品数量
        @Override
        public int quantityDroppedWithBonus(int fortune, Random random) {
            if (fortune > 0) {
                int bonusFactor = Math.max(random.nextInt(fortune + 2) - 1, 0);
                return this.quantityDropped(random) * (bonusFactor + 1);
            } else {
                return this.quantityDropped(random);
            }
        }
    };
}

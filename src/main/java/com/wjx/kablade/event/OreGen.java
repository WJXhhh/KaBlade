package com.wjx.kablade.event;

import com.wjx.kablade.Main;
import com.wjx.kablade.config.ModConfig;
import com.wjx.kablade.init.BlockInit;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

public class OreGen implements IWorldGenerator {
    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        //如果在0号世界(主世界),就生成矿物
        if (world.provider.getDimension() == 0){
            generateOverworld(random, chunkX, chunkZ, world, chunkGenerator, chunkProvider);
        }
    }

    //在世界生成矿物的信息
    private void generateOverworld(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator,IChunkProvider chunkProvider) {
        generateOre(BlockInit.MOLYBDENITE.getDefaultState(), world, random, chunkX*16, chunkZ*16, 40, 80, random.nextInt(3)+ ModConfig.GeneralConf.MOLYBDENITE_SIZE, ModConfig.GeneralConf.MOLYBDENITE_CHANCE);
        generateOre(BlockInit.CHROMIUM_ORE.getDefaultState(),world,random,chunkX*16,chunkZ*16,20,80,random.nextInt(2)+ModConfig.GeneralConf.CHROMIUM_SIZE,ModConfig.GeneralConf.CHROMIUM_CHANCE);
        if (Main.ModHelper.checkBiome(world,new BlockPos(chunkX*16,60,chunkZ*16),Main.ModHelper.COLD_BIOMES))
        generateOre(BlockInit.AURORA_ORE.getDefaultState(),world,random,chunkX*16,chunkZ*16,40,80,random.nextInt(2)+ModConfig.GeneralConf.AURORA_SIZE,ModConfig.GeneralConf.AURORA_CHANCE);    }
    //生成的矿物   生成矿物的世界 生成数(随机的)  生成的x ,z坐标   Y最小,Y最大坐标在(minY,maxY)高度区间中生成矿石 矿脉大小 生成概率
    private void generateOre(IBlockState ore, World world, Random random, int x, int z, int minY, int maxY, int size, int chances) {
        Random r = new Random();
        int deltaY = maxY - minY;
        for (int i = 0; i < chances; i++)
        {
            BlockPos pos = new BlockPos(x+r.nextInt(16), minY+r.nextInt(deltaY), z+random.nextInt(16));

            WorldGenMinable generator = new WorldGenMinable(ore, size);
            generator.generate (world, r, pos);
        }
    }
}

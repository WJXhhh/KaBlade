package com.wjx.kablade.worldcraft;

import com.wjx.kablade.Main;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.BladeStandEntity;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

/**
 * 「万物皆刃」世界合成的公共工具：方块阵匹配（含 4 向旋转）、刀架/基础刀判定、
 * 结果刀构建（继承击杀数/骄魂/精炼）。复刻 1.12.2 {@code CommonProxy} + {@code ArrayLib}。
 */
public final class WorldCraftUtil {

    /** 1.12.2 standType==1「耀魂铁锭」对应重锋的铁质刀架物品。 */
    private static final Set<ResourceLocation> BLADE_STAND_IDS = Set.of(
            ResourceLocation.fromNamespaceAndPath("slashblade", "bladestand_1"),
            ResourceLocation.fromNamespaceAndPath("slashblade", "bladestand_2"),
            ResourceLocation.fromNamespaceAndPath("slashblade", "bladestand_v"),
            ResourceLocation.fromNamespaceAndPath("slashblade", "bladestand_s"),
            ResourceLocation.fromNamespaceAndPath("slashblade", "bladestand_1w"),
            ResourceLocation.fromNamespaceAndPath("slashblade", "bladestand_2w")
    );

    private WorldCraftUtil() {
    }

    public static Block block(Level lvl, BlockPos pos) {
        return lvl.getBlockState(pos).getBlock();
    }

    /** 刀架是否为铁质（耀魂铁锭）刀架。 */
    public static boolean isIronStand(BladeStandEntity stand) {
        Item type = stand.currentType;
        return type != null && BLADE_STAND_IDS.contains(ForgeRegistries.ITEMS.getKey(type));
    }

    /** 刀架上是否为「基础未命名」拔刀剑（只有基础刀才能被世界合成转化）。 */
    public static boolean isBaseBlade(ItemStack blade, ISlashBladeState state) {
        if (!(blade.getItem() instanceof ItemSlashBlade)) {
            return false;
        }
        String key = state.getTranslationKey();
        if (key == null || key.isBlank() || state.isEmpty()) {
            return true;
        }
        ResourceLocation itemKey = ForgeRegistries.ITEMS.getKey(blade.getItem());
        String defaultItemKey = itemKey == null
                ? ""
                : "item." + itemKey.getNamespace() + "." + itemKey.getPath();
        return key.equals(defaultItemKey) && state.getModel().isEmpty() && state.getTexture().isEmpty();
    }

    /**
     * 4 角 + 中心方块阵匹配（含 4 向旋转）。坐标读取沿用 1.12.2：
     * ur=(+1,+1) ul=(+1,-1) dl=(-1,-1) dr=(-1,+1)，全部取 center 的 {@code dy} 层。
     *
     * @param cornersCw 期望的 4 角方块，按 ur→ul→dl→dr 环绕顺序给出
     */
    public static boolean cornersAnyRotation(Level lvl, BlockPos center, int dy,
                                             Block centerBlock, Block[] cornersCw) {
        if (block(lvl, center.offset(0, dy, 0)) != centerBlock) {
            return false;
        }
        Block[] actual = {
                block(lvl, center.offset(1, dy, 1)),    // ur
                block(lvl, center.offset(1, dy, -1)),   // ul
                block(lvl, center.offset(-1, dy, -1)),  // dl
                block(lvl, center.offset(-1, dy, 1))    // dr
        };
        Block[] expected = cornersCw.clone();
        for (int r = 0; r < 4; r++) {
            if (sameOrder(actual, expected)) {
                return true;
            }
            rotateCw(expected);
        }
        return false;
    }

    /** 固定朝向的 4 角 + 中心匹配（不旋转，用于对称结构层）。 */
    public static boolean corners(Level lvl, BlockPos center, int dy,
                                  Block centerBlock, Block ur, Block ul, Block dl, Block dr) {
        return block(lvl, center.offset(0, dy, 0)) == centerBlock
                && block(lvl, center.offset(1, dy, 1)) == ur
                && block(lvl, center.offset(1, dy, -1)) == ul
                && block(lvl, center.offset(-1, dy, -1)) == dl
                && block(lvl, center.offset(-1, dy, 1)) == dr;
    }

    /** 消耗方块阵：把指定层的 4 角 + 中心清为空气。 */
    public static void clearCorners(Level lvl, BlockPos center, int dy) {
        lvl.setBlockAndUpdate(center.offset(0, dy, 0), Blocks.AIR.defaultBlockState());
        lvl.setBlockAndUpdate(center.offset(1, dy, 1), Blocks.AIR.defaultBlockState());
        lvl.setBlockAndUpdate(center.offset(1, dy, -1), Blocks.AIR.defaultBlockState());
        lvl.setBlockAndUpdate(center.offset(-1, dy, -1), Blocks.AIR.defaultBlockState());
        lvl.setBlockAndUpdate(center.offset(-1, dy, 1), Blocks.AIR.defaultBlockState());
    }

    /**
     * 用命名刀定义构建结果刀，并继承原基础刀的击杀数/骄魂/精炼。找不到定义返回空。
     *
     * @param key 命名刀定义路径（kablade 命名空间下，如 {@code "liurrh"}）
     */
    public static ItemStack buildResult(ServerLevel level, String key, ISlashBladeState src) {
        Registry<SlashBladeDefinition> registry = SlashBlade.getSlashBladeDefinitionRegistry(level);
        SlashBladeDefinition def = registry.get(ResourceLocation.fromNamespaceAndPath(Main.MODID, key));
        if (def == null) {
            return ItemStack.EMPTY;
        }
        ItemStack res = def.getBlade();
        res.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(s -> {
            s.setKillCount(src.getKillCount());
            s.setProudSoulCount(src.getProudSoulCount());
            s.setRefine(src.getRefine());
        });
        return res;
    }

    static boolean sameOrder(Block[] a, Block[] b) {
        for (int i = 0; i < 4; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    /** 环绕顺序 ur→ul→dl→dr 的一次循环移位 = 把期望阵旋转 90°。 */
    static void rotateCw(Block[] cw) {
        Block last = cw[3];
        cw[3] = cw[2];
        cw[2] = cw[1];
        cw[1] = cw[0];
        cw[0] = last;
    }
}

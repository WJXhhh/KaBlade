package com.wjx.kablade.worldcraft;

import com.wjx.kablade.Main;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.BladeStandEntity;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 「万物皆刃」世界合成监听器：基础拔刀剑放在铁质刀架上，满足方块阵 / 维度 / 光照 /
 * 精炼 / 骄魂 / 附魔等条件后，以特定来源的伤害「击打刀架」即就地变身为命名刀。
 *
 * <p>挂接重锋 {@link SlashBladeEvent.BladeStandAttackEvent}（与重锋自带的 SE/SA 拓印同一事件）；
 * 命中后 {@code stand.setItem(result)} 并 {@code setCanceled(true)}，复刻 1.12.2 {@code CommonProxy}。
 */
@Mod.EventBusSubscriber(modid = Main.MODID)
public final class WorldCraftEvents {

    private WorldCraftEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()
                || !(event.level instanceof ServerLevel level)
                || level.getGameTime() % 20 != 0) {
            return;
        }

        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof BladeStandEntity stand) || !WorldCraftUtil.isIronStand(stand)) {
                continue;
            }
            BlockPos pos = stand.blockPosition();
            if (!isInLava(level, pos)) {
                continue;
            }

            ItemStack blade = stand.getItem();
            if (blade.isEmpty()) {
                continue;
            }
            ISlashBladeState state = blade.getCapability(mods.flammpfeil.slashblade.item.ItemSlashBlade.BLADESTATE)
                    .orElse(null);
            if (state == null || !WorldCraftUtil.isBaseBlade(blade, state)) {
                continue;
            }

            int refine = state.getRefine();
            int fireProtection = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_PROTECTION, blade);
            if (level.dimension() != Level.NETHER || refine < 50 || fireProtection < 4) {
                continue;
            }

            ItemStack res = WorldCraftUtil.buildResult(level, "liurrh", state);
            if (res.isEmpty()) {
                continue;
            }
            stand.setItem(res);
        }
    }

    // ── 击打类配方（左键 / 实体攻击 / 环境伤害） ──────────────────────────────

    @SubscribeEvent
    public static void onBladeStandAttack(SlashBladeEvent.BladeStandAttackEvent event) {
        BladeStandEntity stand = event.getBladeStand();
        Level level = stand.level();
        if (level.isClientSide() || !(level instanceof ServerLevel server)) {
            return;
        }
        ItemStack blade = event.getBlade();
        ISlashBladeState state = event.getSlashBladeState();
        if (state == null || !WorldCraftUtil.isBaseBlade(blade, state) || !WorldCraftUtil.isIronStand(stand)) {
            return;
        }

        DamageSource src = event.getDamageSource();
        BlockPos pos = stand.blockPosition();
        int refine = state.getRefine();
        int proud = state.getProudSoulCount();

        String result = match(server, stand, blade, state, src, pos, refine, proud);
        if (result == null) {
            return;
        }
        ItemStack res = WorldCraftUtil.buildResult(server, result, state);
        if (res.isEmpty()) {
            return;
        }
        stand.setItem(res);
        event.setCanceled(true);
    }

    /** 命中则返回目标命名刀的定义路径，否则 null。消耗方块的副作用在各分支内完成。 */
    private static String match(ServerLevel level, BladeStandEntity stand, ItemStack blade,
                                ISlashBladeState state, DamageSource src, BlockPos pos,
                                int refine, int proud) {

        // 夜空之剑「阐释者」：玩家击打，下方 3×3 角阵（睡莲/唱片机/仙人掌/冰 + 荧石芯，4 向旋转），精炼≥50，骄魂≥200
        if (isPlayerHit(src) && refine >= 50 && proud >= 200
                && WorldCraftUtil.cornersAnyRotation(level, pos, -1, Blocks.GLOWSTONE,
                new Block[]{Blocks.LILY_PAD, Blocks.JUKEBOX, Blocks.CACTUS, Blocks.ICE})) {
            WorldCraftUtil.clearCorners(level, pos, -1);
            return "chanshizhe";
        }

        // 光剑「监视者」：雷击刀架，光照≥10，精炼≥50，基础刀带摔落保护Ⅳ
        if (src.is(DamageTypes.LIGHTNING_BOLT)
                && level.getMaxLocalRawBrightness(pos) >= 10
                && refine >= 50
                && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FALL_PROTECTION, blade) >= 4) {
            return "guangjian";
        }

        // 奉神刀「鹿」：玩家击打，上下三层黑曜石/信标/石英/玻璃顶结构，精炼≥250
        if (isPlayerHit(src) && refine >= 250
                && WorldCraftUtil.corners(level, pos, -1, Blocks.BEACON,
                        Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.OBSIDIAN)
                && WorldCraftUtil.corners(level, pos, 0, Blocks.AIR,
                        Blocks.QUARTZ_BLOCK, Blocks.QUARTZ_BLOCK, Blocks.QUARTZ_BLOCK, Blocks.QUARTZ_BLOCK)
                && WorldCraftUtil.corners(level, pos, 1, Blocks.AIR,
                        Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.OBSIDIAN)
                && WorldCraftUtil.block(level, pos.offset(0, 2, 0)) == Blocks.GLASS) {
            level.setBlockAndUpdate(pos.offset(0, -1, 0), Blocks.AIR.defaultBlockState());
            return "fengshen";
        }

        // 白「天锁斩月」：玩家手持命名为「Moe_Meng」的命名牌击打，精炼≥50
        if (refine >= 50) {
            Player player = playerOf(src);
            if (player != null) {
                ItemStack held = player.getMainHandItem();
                if (held.getItem() == Items.NAME_TAG
                        && held.getHoverName().getString().equals("Moe_Meng")) {
                    return "zhanyue";
                }
            }
        }

        // ── 击打类 AllWeapon 配方 ──────────────────────────────────────────────

        // 极寒刃「雪走」：雪人攻击刀架，钻石阵（4 向旋转），精炼≥50
        if (src.getEntity() instanceof net.minecraft.world.entity.animal.SnowGolem
                && refine >= 50
                && isSnowIceDiamond(level, pos)) {
            clearSnowIceDiamond(level, pos);
            return "xuezou";
        }

        // 死神「幽之名」：恶魂火球攻击，L 形灵魂沙 + 南瓜头，精炼≥50，抢夺Ⅲ+亡灵杀手Ⅳ
        if (src.getEntity() instanceof net.minecraft.world.entity.monster.Ghast
                && refine >= 50
                && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MOB_LOOTING, blade) >= 3
                && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SMITE, blade) >= 4
                && WorldCraftUtil.block(level, pos.offset(1, 0, 0)) == Blocks.SOUL_SAND
                && WorldCraftUtil.block(level, pos.offset(1, 1, 0)) == Blocks.SOUL_SAND
                && WorldCraftUtil.block(level, pos.offset(1, 1, 1)) == Blocks.SOUL_SAND
                && WorldCraftUtil.block(level, pos.offset(1, 1, -1)) == Blocks.SOUL_SAND
                && WorldCraftUtil.block(level, pos.offset(1, 2, 0)) == Blocks.PUMPKIN) {
            clearLShape(level, pos);
            return "youming";
        }

        // 灾厄之源「劫」：苦力怕爆炸，刀架下方 1 个灵魂沙，精炼≥20
        if (src.getEntity() instanceof net.minecraft.world.entity.monster.Creeper
                && refine >= 20
                && WorldCraftUtil.block(level, pos.below()) == Blocks.SOUL_SAND) {
            level.setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
            return "jie";
        }

        // 花天狂骨「花天」：苦力怕爆炸，刀架下方 1 个花，精炼≥10
        if (src.getEntity() instanceof net.minecraft.world.entity.monster.Creeper
                && refine >= 10
                && isAnyFlower(level, pos)) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            return "htkg";
        }

        // 机翼「极意」：苦力怕爆炸，对角火/冰 + 灵魂沙，精炼≥25，耐久Ⅲ
        if (src.getEntity() instanceof net.minecraft.world.entity.monster.Creeper
                && refine >= 25
                && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, blade) >= 3
                && isJiYiPattern(level, pos)) {
            clearJiYiPattern(level, pos);
            return "jiyi";
        }

        // 白兽剑王「巨狮」：苦力怕爆炸，十字 TNT + 黑曜石，精炼≥50，锋利Ⅴ
        if (src.getEntity() instanceof net.minecraft.world.entity.monster.Creeper
                && refine >= 50
                && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, blade) >= 5
                && isCross(level, pos.below(), Blocks.OBSIDIAN, Blocks.TNT)) {
            clearCross(level, pos.below());
            return "baishoujianwang";
        }

        // 白月钉：苦力怕爆炸，钻石阵（黑曜石，4 向旋转），精炼≥50，保护Ⅱ
        if (src.getEntity() instanceof net.minecraft.world.entity.monster.Creeper
                && refine >= 50
                && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, blade) >= 2
                && WorldCraftUtil.corners(level, pos, -1, Blocks.OBSIDIAN,
                Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.OBSIDIAN, Blocks.OBSIDIAN)) {
            WorldCraftUtil.clearCorners(level, pos, -1);
            return "baiyueding";
        }

        // 白の契约：苦力怕爆炸，十字雪块 + 黑曜石，精炼≥50，保护Ⅱ
        if (src.getEntity() instanceof net.minecraft.world.entity.monster.Creeper
                && refine >= 50
                && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, blade) >= 2
                && isCross(level, pos.below(), Blocks.SNOW_BLOCK, Blocks.OBSIDIAN)) {
            clearCross(level, pos.below());
            return "baiqiyue";
        }

        // 不可视之刃「风之影」：玩家击打，Y≥250，精炼≥50，摔落保护Ⅳ
        if (isPlayerHit(src) && pos.getY() >= 250
                && refine >= 50
                && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FALL_PROTECTION, blade) >= 4) {
            return "fengzhiying";
        }

        if (isPlayerHit(src)
                && refine >= 10
                && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLAST_PROTECTION, blade) >= 4
                && isFlowerField(level, pos, 5)) {
            clearFlowerField(level, pos, 5);
            return "huami";
        }

        return null;
    }

    // ── 右键交互类配方（PlayerInteractEvent） ────────────────────────────────

    /**
     * 右键击打刀架：白兰剑和生机「绿萝」。
     * <p>
     * 1.12.2 原版通过 {@code AWRec.java} 的 {@code hit==1}（右键）实现。
     */
    @SubscribeEvent
    public static void onRightClickStand(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || event.getEntity() == null) {
            return;
        }
        Entity target = event.getTarget();
        if (!(target instanceof BladeStandEntity stand)) {
            return;
        }
        if (!WorldCraftUtil.isIronStand(stand)) {
            return;
        }

        ItemStack blade = stand.getItem();
        ISlashBladeState state = blade.getCapability(mods.flammpfeil.slashblade.item.ItemSlashBlade.BLADESTATE).orElse(null);
        if (state == null || !WorldCraftUtil.isBaseBlade(blade, state)) {
            return;
        }

        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = stand.blockPosition();
        int refine = state.getRefine();

        // 白兰剑：刀架站在花上，精炼≥10
        if (refine >= 10 && isAnyFlower(level, pos)) {
            ItemStack res = WorldCraftUtil.buildResult(level, "bailan", state);
            if (!res.isEmpty()) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                stand.setItem(res);
                event.setCanceled(true);
            }
            return;
        }

        // 生机「绿萝」：刀架站在树叶上，精炼≥10
        if (refine >= 10 && isAnyLeaves(level, pos.below())) {
            ItemStack res = WorldCraftUtil.buildResult(level, "lvluo", state);
            if (!res.isEmpty()) {
                level.setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
                stand.setItem(res);
                event.setCanceled(true);
            }
        }
    }

    // ── 方块阵辅助方法 ──────────────────────────────────────────────────────

    /** 雪走钻石阵：中心冰，四角雪块（4 向旋转） */
    private static boolean isSnowIceDiamond(Level lvl, BlockPos center) {
        Block centerBlock = WorldCraftUtil.block(lvl, center.below());
        if (centerBlock != Blocks.ICE) {
            return false;
        }
        return WorldCraftUtil.block(lvl, center.offset(1, -1, 1)) == Blocks.SNOW_BLOCK
                && WorldCraftUtil.block(lvl, center.offset(1, -1, -1)) == Blocks.SNOW_BLOCK
                && WorldCraftUtil.block(lvl, center.offset(-1, -1, -1)) == Blocks.SNOW_BLOCK
                && WorldCraftUtil.block(lvl, center.offset(-1, -1, 1)) == Blocks.SNOW_BLOCK;
    }

    /** 消耗雪走钻石阵 */
    private static void clearSnowIceDiamond(Level lvl, BlockPos center) {
        WorldCraftUtil.clearCorners(lvl, center, -1);
        lvl.setBlockAndUpdate(center.below(), Blocks.AIR.defaultBlockState());
    }

    /** 消耗幽之名 L 形结构 */
    private static void clearLShape(Level lvl, BlockPos pos) {
        lvl.setBlockAndUpdate(pos.offset(1, 0, 0), Blocks.AIR.defaultBlockState());
        lvl.setBlockAndUpdate(pos.offset(1, 1, 0), Blocks.AIR.defaultBlockState());
        lvl.setBlockAndUpdate(pos.offset(1, 1, 1), Blocks.AIR.defaultBlockState());
        lvl.setBlockAndUpdate(pos.offset(1, 1, -1), Blocks.AIR.defaultBlockState());
        lvl.setBlockAndUpdate(pos.offset(1, 2, 0), Blocks.AIR.defaultBlockState());
    }

    /** 机翼「极意」对角阵：火/冰在四角交替，中心灵魂沙。 */
    private static boolean isJiYiPattern(Level lvl, BlockPos pos) {
        return WorldCraftUtil.cornersAnyRotation(lvl, pos, -1, Blocks.SOUL_SAND,
                new Block[]{Blocks.ICE, Blocks.FIRE, Blocks.ICE, Blocks.FIRE});
    }

    /** 消耗机翼对角阵 */
    private static void clearJiYiPattern(Level lvl, BlockPos pos) {
        WorldCraftUtil.clearCorners(lvl, pos, -1);
    }

    private static boolean isCross(Level lvl, BlockPos center, Block centerBlock, Block armBlock) {
        return WorldCraftUtil.block(lvl, center) == centerBlock
                && WorldCraftUtil.block(lvl, center.north()) == armBlock
                && WorldCraftUtil.block(lvl, center.south()) == armBlock
                && WorldCraftUtil.block(lvl, center.east()) == armBlock
                && WorldCraftUtil.block(lvl, center.west()) == armBlock;
    }

    private static void clearCross(Level lvl, BlockPos center) {
        lvl.setBlockAndUpdate(center, Blocks.AIR.defaultBlockState());
        lvl.setBlockAndUpdate(center.north(), Blocks.AIR.defaultBlockState());
        lvl.setBlockAndUpdate(center.south(), Blocks.AIR.defaultBlockState());
        lvl.setBlockAndUpdate(center.east(), Blocks.AIR.defaultBlockState());
        lvl.setBlockAndUpdate(center.west(), Blocks.AIR.defaultBlockState());
    }

    /** 任意花方块（1.20.1 花方块类名已变，用已知实例检查） */
    private static boolean isAnyFlower(Level lvl, BlockPos pos) {
        Block b = WorldCraftUtil.block(lvl, pos);
        return b == Blocks.DANDELION || b == Blocks.POPPY || b == Blocks.BLUE_ORCHID
                || b == Blocks.ALLIUM || b == Blocks.AZURE_BLUET || b == Blocks.RED_TULIP
                || b == Blocks.ORANGE_TULIP || b == Blocks.WHITE_TULIP || b == Blocks.PINK_TULIP
                || b == Blocks.OXEYE_DAISY || b == Blocks.CORNFLOWER || b == Blocks.LILY_OF_THE_VALLEY
                || b == Blocks.WITHER_ROSE || b == Blocks.SUNFLOWER || b == Blocks.LILAC
                || b == Blocks.ROSE_BUSH || b == Blocks.PEONY;
    }

    private static boolean isFlowerField(Level lvl, BlockPos center, int radius) {
        if (!isAnyFlower(lvl, center)) {
            return false;
        }
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (!isAnyFlower(lvl, center.offset(x, 0, z))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void clearFlowerField(Level lvl, BlockPos center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                lvl.setBlockAndUpdate(center.offset(x, 0, z), Blocks.AIR.defaultBlockState());
            }
        }
    }

    /** 任意树叶方块 */
    private static boolean isAnyLeaves(Level lvl, BlockPos pos) {
        Block b = WorldCraftUtil.block(lvl, pos);
        return b == Blocks.OAK_LEAVES || b == Blocks.SPRUCE_LEAVES || b == Blocks.BIRCH_LEAVES
                || b == Blocks.JUNGLE_LEAVES || b == Blocks.ACACIA_LEAVES || b == Blocks.DARK_OAK_LEAVES
                || b == Blocks.MANGROVE_LEAVES || b == Blocks.CHERRY_LEAVES;
    }

    // ── 工具方法 ────────────────────────────────────────────────────────────

    private static boolean isPlayerHit(DamageSource src) {
        return src.getEntity() instanceof Player;
    }

    private static Player playerOf(DamageSource src) {
        Entity e = src.getEntity();
        return e instanceof Player p ? p : null;
    }

    private static boolean isInLava(Level level, BlockPos pos) {
        return WorldCraftUtil.block(level, pos) == Blocks.LAVA;
    }
}

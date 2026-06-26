package com.wjx.kablade.worldcraft;

import com.wjx.kablade.Main;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.BladeStandEntity;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
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
        // 炎王「流刃若火」：下界，刀架立于岩浆上，火焰伤害击打，精炼≥50，基础刀带火焰附加Ⅳ
        if (src.is(DamageTypeTags.IS_FIRE)
                && level.dimension() == Level.NETHER
                && WorldCraftUtil.block(level, pos) == Blocks.LAVA
                && refine >= 50
                && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, blade) >= 4) {
            return "liurrh";
        }

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

        return null;
    }

    private static boolean isPlayerHit(DamageSource src) {
        return src.getEntity() instanceof Player;
    }

    private static Player playerOf(DamageSource src) {
        Entity e = src.getEntity();
        return e instanceof Player p ? p : null;
    }
}

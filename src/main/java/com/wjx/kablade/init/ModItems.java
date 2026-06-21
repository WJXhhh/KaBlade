package com.wjx.kablade.init;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.wjx.kablade.Main;
import com.wjx.kablade.api.CustomBladeModel;
import com.wjx.kablade.object.item.ordinary.ItemBase;
import com.wjx.kablade.object.item.KbladeBladeItem;
import com.wjx.kablade.util.creative_tab.CreativeTabBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class ModItems {

    public static final Map<String,ItemBase> KABLADE_ITEMS = Maps.newHashMap();

    public static final DeferredRegister<Item> ITEM_REGISTRY =
            DeferredRegister.create(ForgeRegistries.ITEMS, Main.MODID);

    //Blade Item
    @CustomBladeModel
    public static final RegistryObject<Item> KABLADE_BLADE = registerItem(
            "kablade_blade_named",
            KbladeBladeItem::new);

    //Honkai Blade Item（崩坏线拔刀剑的统一载体物品）
    @CustomBladeModel
    public static final RegistryObject<Item> KABLADE_HONKAI_BLADE = registerItem(
            "kablade_honkai_named",
            KbladeBladeItem::new);

    //SP Light Blade Item（龙一文字线拔刀剑的统一载体物品）
    @CustomBladeModel
    public static final RegistryObject<Item> KABLADE_SL_BLADE = registerItem(
            "kablade_sl_named",
            KbladeBladeItem::new);

    //Creative Tab Icon
    public static final RegistryObject<Item> MAIN_MATER = registerItemBase("main_mater");
    public static final RegistryObject<Item> NOTED = registerItemBase("noted");

    //ORDINARY
    public static final RegistryObject<Item> RIMMED_EARTH_STICK = registerItemBase("rimmed_earth_stick",Main.TAB_KABLADE);

    // ─── Chromium ──────────────────────────────────────────────────
    public static final RegistryObject<Item> CHROMIUM_ORE = registerBlockItem(ModBlocks.CHROMIUM_ORE, Main.TAB_KABLADE);
    public static final RegistryObject<Item> CHROMIUM_INGOT = registerItemBase("chromium_ingot", Main.TAB_KABLADE);

    // ─── Aurora Ore ────────────────────────────────────────────────
    public static final RegistryObject<Item> AURORA_ORE = registerBlockItem(ModBlocks.AURORA_ORE, Main.TAB_KABLADE);

    // ─── Gravity ───────────────────────────────────────────────────
    public static final RegistryObject<Item> GRAVITY_NUGGET = registerItemBase("gravity_nugget", Main.TAB_KABLADE);
    public static final RegistryObject<Item> GRAVITY_CRYSTAL = registerItemBase("gravity_crystal", Main.TAB_KABLADE);

    // ─── Thunder ────────────────────────────────────────────────────
    public static final RegistryObject<Item> THUNDER_CRYSTAL = registerItemBase("thunder_crystal", Main.TAB_KABLADE);

    // ─── Aurora Metal ──────────────────────────────────────────────

    /** 铬 Tier：铁与钻石之间，附魔能力较高（17）。 */
    public static final Tier CHROMIUM_TIER = TierSortingRegistry.registerTier(
            new Tier() {
                @Override public int getUses()            { return 800; }
                @Override public float getSpeed()         { return 6.8f; }
                @Override public float getAttackDamageBonus() { return 2.6f; }
                @Override public int getLevel()            { return 2; }
                @Override public int getEnchantmentValue() { return 17; }
                @Override public Ingredient getRepairIngredient() {
                    return Ingredient.of(CHROMIUM_INGOT.get());
                }
            },
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "chromium"),
            List.of(Tiers.IRON),
            List.of(Tiers.DIAMOND)
    );

    // 铬工具
    public static final RegistryObject<Item> CHROMIUM_SWORD = registerItem("chromium_sword",
            () -> new SwordItem(CHROMIUM_TIER, 3, -2.4f, new Item.Properties()), Main.TAB_KABLADE);
    public static final RegistryObject<Item> CHROMIUM_PICKAXE = registerItem("chromium_pickaxe",
            () -> new PickaxeItem(CHROMIUM_TIER, 1, -2.8f, new Item.Properties()), Main.TAB_KABLADE);
    public static final RegistryObject<Item> CHROMIUM_AXE = registerItem("chromium_axe",
            () -> new AxeItem(CHROMIUM_TIER, 5.5f, -3.1f, new Item.Properties()), Main.TAB_KABLADE);
    public static final RegistryObject<Item> CHROMIUM_HOE = registerItem("chromium_hoe",
            () -> new HoeItem(CHROMIUM_TIER, -3, -1.0f, new Item.Properties()), Main.TAB_KABLADE);

    /** 极光金属 Tier：介于钻石与下界合金之间，附魔能力极高（25）。 */
    public static final Tier AURORA_METAL_TIER = TierSortingRegistry.registerTier(
            new Tier() {
                @Override public int getUses()            { return 1000; }
                @Override public float getSpeed()         { return 7.5f; }
                @Override public float getAttackDamageBonus() { return 3.2f; }
                @Override public int getLevel()            { return 4; }
                @Override public int getEnchantmentValue() { return 25; }
                @Override public Ingredient getRepairIngredient() {
                    return Ingredient.of(AURORA_METAL_INGOT.get());
                }
            },
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "aurora_metal"),
            List.of(Tiers.DIAMOND),
            List.of(Tiers.NETHERITE)
    );

    // 材料
    public static final RegistryObject<Item> AURORA_FRAGMENT = registerItemBase("aurora_fragment", Main.TAB_KABLADE);
    public static final RegistryObject<Item> AURORA_METAL_INGOT = registerItemBase("aurora_metal_ingot", Main.TAB_KABLADE);
    public static final RegistryObject<Item> STURDY_GLASS_STICK = registerItemBase("sturdy_glass_stick", Main.TAB_KABLADE);

    // 工具
    public static final RegistryObject<Item> AURORA_METAL_SWORD = registerItem("aurora_metal_sword",
            () -> new SwordItem(AURORA_METAL_TIER, 3, -2.4f, new Item.Properties()), Main.TAB_KABLADE);
    public static final RegistryObject<Item> AURORA_METAL_PICKAXE = registerItem("aurora_metal_pickaxe",
            () -> new PickaxeItem(AURORA_METAL_TIER, 1, -2.8f, new Item.Properties()), Main.TAB_KABLADE);
    public static final RegistryObject<Item> AURORA_METAL_AXE = registerItem("aurora_metal_axe",
            () -> new AxeItem(AURORA_METAL_TIER, 5.5f, -3.1f, new Item.Properties()), Main.TAB_KABLADE);
    public static final RegistryObject<Item> AURORA_METAL_HOE = registerItem("aurora_metal_hoe",
            () -> new HoeItem(AURORA_METAL_TIER, -3, -1.0f, new Item.Properties()), Main.TAB_KABLADE);

    // ─── Molybdenum / Chromoly ─────────────────────────────────────
    public static final RegistryObject<Item> MOLYBDENITE = registerBlockItem(ModBlocks.MOLYBDENITE, Main.TAB_KABLADE);
    public static final RegistryObject<Item> MOLYBDENUM_INGOT = registerItemBase("molybdenum_ingot", Main.TAB_KABLADE);
    public static final RegistryObject<Item> CRUDE_CHROMOLY = registerItemBase("crude_chromoly", Main.TAB_KABLADE);
    public static final RegistryObject<Item> CHROMOLY_INGOT = registerItemBase("chromoly_ingot", Main.TAB_KABLADE);

    /** 钼 Tier：铁与钻石之间，附魔能力高（20）。 */
    public static final Tier MOLYBDENUM_TIER = TierSortingRegistry.registerTier(
            new Tier() {
                @Override public int getUses()            { return 700; }
                @Override public float getSpeed()         { return 7.0f; }
                @Override public float getAttackDamageBonus() { return 2.4f; }
                @Override public int getLevel()            { return 2; }
                @Override public int getEnchantmentValue() { return 20; }
                @Override public Ingredient getRepairIngredient() {
                    return Ingredient.of(MOLYBDENUM_INGOT.get());
                }
            },
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "molybdenum"),
            List.of(Tiers.IRON),
            List.of(Tiers.DIAMOND)
    );

    /** 铬钼钢 Tier：铁与钻石之间，略强于钼，附魔能力 22。 */
    public static final Tier CHROMOLY_TIER = TierSortingRegistry.registerTier(
            new Tier() {
                @Override public int getUses()            { return 820; }
                @Override public float getSpeed()         { return 7.2f; }
                @Override public float getAttackDamageBonus() { return 2.8f; }
                @Override public int getLevel()            { return 2; }
                @Override public int getEnchantmentValue() { return 22; }
                @Override public Ingredient getRepairIngredient() {
                    return Ingredient.of(CHROMOLY_INGOT.get());
                }
            },
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "chromoly"),
            List.of(Tiers.IRON),
            List.of(Tiers.DIAMOND)
    );

    // 钼工具
    public static final RegistryObject<Item> MOLYBDENUM_SWORD = registerItem("molybdenum_sword",
            () -> new SwordItem(MOLYBDENUM_TIER, 3, -2.4f, new Item.Properties()), Main.TAB_KABLADE);
    public static final RegistryObject<Item> MOLYBDENUM_PICKAXE = registerItem("molybdenum_pickaxe",
            () -> new PickaxeItem(MOLYBDENUM_TIER, 1, -2.8f, new Item.Properties()), Main.TAB_KABLADE);
    public static final RegistryObject<Item> MOLYBDENUM_AXE = registerItem("molybdenum_axe",
            () -> new AxeItem(MOLYBDENUM_TIER, 5.5f, -3.1f, new Item.Properties()), Main.TAB_KABLADE);
    public static final RegistryObject<Item> MOLYBDENUM_HOE = registerItem("molybdenum_hoe",
            () -> new HoeItem(MOLYBDENUM_TIER, -3, -1.0f, new Item.Properties()), Main.TAB_KABLADE);

    // 铬钼钢工具
    public static final RegistryObject<Item> CHROMOLY_SWORD = registerItem("chromoly_sword",
            () -> new SwordItem(CHROMOLY_TIER, 3, -2.4f, new Item.Properties()), Main.TAB_KABLADE);
    public static final RegistryObject<Item> CHROMOLY_PICKAXE = registerItem("chromoly_pickaxe",
            () -> new PickaxeItem(CHROMOLY_TIER, 1, -2.8f, new Item.Properties()), Main.TAB_KABLADE);
    public static final RegistryObject<Item> CHROMOLY_AXE = registerItem("chromoly_axe",
            () -> new AxeItem(CHROMOLY_TIER, 5.5f, -3.1f, new Item.Properties()), Main.TAB_KABLADE);
    public static final RegistryObject<Item> CHROMOLY_HOE = registerItem("chromoly_hoe",
            () -> new HoeItem(CHROMOLY_TIER, -3, -1.0f, new Item.Properties()), Main.TAB_KABLADE);

    private ModItems() {

    }

    public static RegistryObject<Item> registerItemBase(String registryName, Item.Properties properties) {
        return ITEM_REGISTRY.register(registryName, () -> new ItemBase(properties));
    }

    public static RegistryObject<Item> registerItemBase(String registryName) {
        return ITEM_REGISTRY.register(registryName, () -> new ItemBase(new Item.Properties()));
    }

    public static RegistryObject<Item> registerItemBase(String registryName, CreativeTabBuilder builder, Item.Properties properties) {
        return ITEM_REGISTRY.register(registryName, () -> new ItemBase(properties).setCreativeTab(builder));
    }

    public static RegistryObject<Item> registerItemBase(String registryName,CreativeTabBuilder builder) {
        return ITEM_REGISTRY.register(registryName, () -> new ItemBase(new Item.Properties()).setCreativeTab(builder));
    }

    public static RegistryObject<Item> registerItem(String registryName, Supplier<Item> supplier) {
        return ITEM_REGISTRY.register(registryName, supplier);
    }

    public static RegistryObject<Item> registerItem(String registryName, Supplier<Item> supplier, CreativeTabBuilder tab) {
        RegistryObject<Item> ro = ITEM_REGISTRY.register(registryName, supplier);
        tab.addStack(() -> ro.get().getDefaultInstance());
        return ro;
    }

    /**
     * 为方块注册对应的 {@link BlockItem}（注册名沿用方块本身的注册名），把它收进
     *
     * <p>这里用 {@code () -> item.get()...} 的 Supplier 延迟取值：注册尚未完成时不能调用
     * {@code .get()}，交给 {@link CreativeTabBuilder#addStack} 在物品栏填充时再取。
     */
    public static RegistryObject<Item> registerBlockItem(RegistryObject<Block> block, CreativeTabBuilder tab) {
        RegistryObject<Item> item = ITEM_REGISTRY.register(Objects.requireNonNull(block.getId()).getPath(),
                () -> new BlockItem(block.get(), new Item.Properties()));
        tab.addStack(() -> item.get().getDefaultInstance());
        return item;
    }


    /**
     * 为方块注册对应的 {@link BlockItem}（注册名沿用方块本身的注册名），把它收进
     *
     * <p>这里用 {@code () -> item.get()...} 的 Supplier 延迟取值：注册尚未完成时不能调用
     */
    public static RegistryObject<Item> registerBlockItem(RegistryObject<Block> block) {
        return ITEM_REGISTRY.register(Objects.requireNonNull(block.getId()).getPath(),
                () -> new BlockItem(block.get(), new Item.Properties()));
    }

    //Block Item
    public static final RegistryObject<Item> RIMMED_EARTH = registerBlockItem(ModBlocks.RIMMED_EARTH, Main.TAB_KABLADE);
}

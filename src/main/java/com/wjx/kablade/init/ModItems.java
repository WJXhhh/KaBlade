package com.wjx.kablade.init;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.wjx.kablade.Main;
import com.wjx.kablade.api.CustomBladeModel;
import com.wjx.kablade.object.item.ordinary.ItemBase;
import com.wjx.kablade.object.item.KbladeBladeItem;
import com.wjx.kablade.util.creative_tab.CreativeTabBuilder;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
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

    //Creative Tab Icon
    public static final RegistryObject<Item> MAIN_MATER = registerItemBase("main_mater");
    public static final RegistryObject<Item> NOTED = registerItemBase("noted",Main.TAB_KABLADE_NOTED);

    //ORDINARY
    public static final RegistryObject<Item> RIMMED_EARTH_STICK = registerItemBase("rimmed_earth_stick",Main.TAB_KABLADE);

    public static final RegistryObject<Item> KABLADE_CORE = registerItemBase("kablade_core",Main.TAB_KABLADE);


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

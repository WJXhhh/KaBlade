package com.wjx.kablade.util.creative_tab;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import org.apache.commons.compress.utils.Lists;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * 对 {@link CreativeModeTab.Builder} 的轻量封装：先把「要展示哪些物品」收集起来，
 * 等到 {@link #registerTab} 被调用时再一次性构建并注册成创造模式物品栏。
 *
 * <p>为什么要延迟收集而不是直接 {@code addItem(someItem)}？因为物品
 * （{@link net.minecraftforge.registries.RegistryObject}）在 mod 构造阶段还没注册完成，
 * 这时调用 {@code .get()} 会抛 "Registry Object not present"。所以这里只存「如何取得物品」的
 * 回调，真正取值发生在物品栏被填充时——那时注册早已完成。
 */
public class CreativeTabBuilder {

    /** 原版的物品栏 builder，承载标题、图标等设置。 */
    private final CreativeModeTab.Builder tabBuilder;

    /**
     * 所有「填充物品栏」的回调，按加入顺序依次执行。
     * 静态物品（{@link #addItem}/{@link #addStack}）和动态物品（{@link #addDisplayItems}）
     * 统一归一成同一种回调存放，避免维护两份列表。
     */
    private final List<BiConsumer<CreativeModeTab.ItemDisplayParameters, CreativeModeTab.Output>> displayItems =
            Lists.newArrayList();

    public CreativeTabBuilder(CreativeModeTab.Builder builder) {
        this.tabBuilder = builder;
    }

    /** 加入单个物品（使用其默认堆叠）。 */
    public void addItem(Item item) {
        addStack(item::getDefaultInstance);
    }

    /** 加入一个物品堆叠；用 {@link Supplier} 延迟取值以避开注册时序问题。 */
    public void addStack(Supplier<ItemStack> stack) {
        addDisplayItems((params, output) -> output.accept(stack.get()));
    }

    /**
     * 加入需要根据物品栏参数动态生成内容的回调
     */
    public void addDisplayItems(BiConsumer<CreativeModeTab.ItemDisplayParameters, CreativeModeTab.Output> display) {
        displayItems.add(display);
    }

    /**
     * 把收集到的内容绑定到 builder，并将物品栏注册进 {@code register}。
     * 需在所有 {@code add*} 调用之后、mod 构造期内调用一次。
     */
    public void registerTab(String name, DeferredRegister<CreativeModeTab> register) {
        tabBuilder.displayItems((params, output) ->
                displayItems.forEach(display -> display.accept(params, output)));
        register.register(name, tabBuilder::build);
    }
}

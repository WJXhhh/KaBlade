package com.wjx.kablade.util.creative_tab;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.compress.utils.Lists;

import java.util.List;
import java.util.function.Supplier;
import java.util.function.BiConsumer;

public class CreativeTabBuilder {
    CreativeModeTab.Builder tabBuilder;
    RegistryObject<CreativeModeTab> registryTab;

    List<Supplier<ItemStack>> itemList = Lists.newArrayList();
    List<BiConsumer<CreativeModeTab.ItemDisplayParameters, CreativeModeTab.Output>> displayItems =
            Lists.newArrayList();

    public CreativeTabBuilder(CreativeModeTab.Builder builder) {
        this.tabBuilder = builder;
        this.registryTab = null;
    }

    public void registerTab(String name,DeferredRegister<CreativeModeTab> register) {
        tabBuilder.displayItems((params, output) -> {
            itemList.forEach(stack -> output.accept(stack.get()));
            displayItems.forEach(display -> display.accept(params, output));
        });
        registryTab = register.register(name, () -> tabBuilder.build());
    }

    public void addItem(Item item) {
        addStack(item::getDefaultInstance);
    }

    public void addStack(Supplier<ItemStack> stack) {
        itemList.add(stack);
    }

    public void addDisplayItems(
            BiConsumer<CreativeModeTab.ItemDisplayParameters, CreativeModeTab.Output> display) {
        displayItems.add(display);
    }
}

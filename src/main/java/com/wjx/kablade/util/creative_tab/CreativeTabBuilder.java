package com.wjx.kablade.util.creative_tab;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.compress.utils.Lists;

import java.util.List;

public class CreativeTabBuilder {
    CreativeModeTab.Builder tabBuilder;
    RegistryObject<CreativeModeTab> registryTab;

    List<Item> itemList = Lists.newArrayList();

    public CreativeTabBuilder(CreativeModeTab.Builder builder) {
        this.tabBuilder = builder;
        this.registryTab = null;
    }

    public void registerTab(String name,DeferredRegister<CreativeModeTab> register) {
        tabBuilder.displayItems((params, output) -> itemList.forEach(output::accept));
        registryTab = register.register(name, () -> tabBuilder.build());
    }

    public void addItem(Item item) {
        itemList.add(item);
    }
}

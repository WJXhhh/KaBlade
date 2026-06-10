package com.wjx.kablade.object.item.ordinary;

import com.wjx.kablade.util.creative_tab.CreativeTabBuilder;
import com.wjx.kablade.util.creative_tab.IHasCreativeTab;
import net.minecraft.world.item.Item;

/**
 * Base class for this mod's plain items. Subclass it for items that need custom behaviour, or use it
 * directly with a {@link Item.Properties} for simple materials.
 */
public class ItemBase extends Item implements IHasCreativeTab {

    public ItemBase(Properties properties) {
        super(properties);
    }

    public ItemBase() {
        this(new Properties());
    }

    public Item setCreativeTab(CreativeTabBuilder builder) {
        builder.addItem(this);
        return this;
    }
}
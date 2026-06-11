package com.wjx.kablade.object.item;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.item.ItemTierSlashBlade;

/**
 * Shared carrier item for every Kablade blade definition.
 * Specific blades are loaded from SlashBladeDefinition, not encoded in this class.
 */
public final class KbladeBladeItem extends ItemSlashBlade {

    public KbladeBladeItem() {
        super(new ItemTierSlashBlade(100, 4.0F), 4, -2.4F,
                new Properties().stacksTo(1));
    }
}

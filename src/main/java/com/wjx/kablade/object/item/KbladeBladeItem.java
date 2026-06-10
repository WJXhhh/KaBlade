package com.wjx.kablade.object.item;

import com.wjx.kablade.util.creative_tab.CreativeTabBuilder;
import com.wjx.kablade.util.creative_tab.IHasCreativeTab;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.item.ItemTierSlashBlade;
import net.minecraft.world.item.Item;

/**
 * Example custom blade — extends SlashBlade's {@link ItemSlashBlade}. Its in-world / inventory
 * appearance comes from SlashBlade's BladeModel (wired via the {@code @CustomBladeModel} annotation
 * on its registry field) plus the blade-state NBT.
 *
 * <p>The constructor arguments below are identical to SlashBlade's base "無名" blade (its
 * {@code slashblade} item), so this blade behaves the same. Note: ItemSlashBlade's
 * {@code getAttributeModifiers} keeps the vanilla ATTACK_SPEED modifier derived from the
 * {@code attackSpeed} float (so it must match to feel the same), while ATTACK_DAMAGE is recomputed
 * at runtime from the blade-state NBT. Durability comes from the tier's {@code uses} (40).
 */
public class KbladeBladeItem extends ItemSlashBlade implements IHasCreativeTab {

    public KbladeBladeItem(String itemId) {
        // Mirrors SlashBlade's base blade: ItemTierSlashBlade(uses=40, attackBonus=4.0F),
        // attackDamage=4, attackSpeed=0.0F, plain Item.Properties (no stacksTo — durability makes it unstackable).
        super(new ItemTierSlashBlade(40, 4.0F), 4, 0.0F, new Properties());
    }

    @Override
    public Item setCreativeTab(CreativeTabBuilder builder) {
        builder.addItem(this);
        return this;
    }
}
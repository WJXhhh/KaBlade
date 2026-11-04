package com.wjx.kablade;

import com.wjx.kablade.init.ModItems;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Main.MODID)
@PrefixGameTestTemplate(false)
public final class RepairRecipeGameTests {
    private RepairRecipeGameTests() {
    }

    @GameTest(template = "multipart_targeting")
    public static void kbladeRepairProducesOriginalNameless(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        var container = new TransientCraftingContainer(new InventoryMenu(player.getInventory(), false, player), 2, 2);
        var recipe = new RepairItemRecipe(new ResourceLocation(Main.MODID, "repair_test"), CraftingBookCategory.EQUIPMENT);
        for (Item item : new Item[]{ModItems.KABLADE_BLADE.get(), ModItems.KABLADE_HONKAI_BLADE.get(),
                ModItems.KABLADE_SL_BLADE.get(), ModItems.KABLADE_AW_BLADE.get(), Items.IRON_SWORD}) {
            ItemStack first = new ItemStack(item);
            ItemStack second = new ItemStack(item);
            first.setDamageValue(40);
            second.setDamageValue(40);
            container.setItem(0, first);
            container.setItem(1, second);
            helper.assertTrue(recipe.matches(container, helper.getLevel()), "repair must match " + item);
            ItemStack result = recipe.assemble(container, helper.getLevel().registryAccess());
            Item expected = item == Items.IRON_SWORD ? item : SlashBladeItems.SLASHBLADE.get();
            helper.assertTrue(result.is(expected), "incorrect repair result for " + item);
            helper.assertTrue(result.getDamageValue() == 0, "repair durability was not preserved for " + item);
        }
        helper.succeed();
    }
}

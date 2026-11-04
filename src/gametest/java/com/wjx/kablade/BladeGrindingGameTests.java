package com.wjx.kablade;

import com.wjx.kablade.init.ModItems;
import com.wjx.kablade.recipe.BladeGrindingRecipe;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Main.MODID)
@PrefixGameTestTemplate(false)
public final class BladeGrindingGameTests {
    private BladeGrindingGameTests() {}

    @GameTest(template = "multipart_targeting")
    public static void repairsBladesWithoutLosingTheirData(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        var grid = new TransientCraftingContainer(new InventoryMenu(player.getInventory(), false, player), 2, 2);
        var recipe = new BladeGrindingRecipe(new ResourceLocation(Main.MODID, "blade_grinding_test"), CraftingBookCategory.EQUIPMENT);
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(new ResourceLocation(Main.MODID, "blade_grinding")).isPresent(),
                "grinding recipe must load from the datapack");
        for (Item item : new Item[]{SlashBladeItems.SLASHBLADE.get(), ModItems.KABLADE_BLADE.get(),
                ModItems.KABLADE_HONKAI_BLADE.get(), ModItems.KABLADE_SL_BLADE.get(), ModItems.KABLADE_AW_BLADE.get()}) {
            ItemStack blade = new ItemStack(item);
            blade.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> state.setNonEmpty());
            blade.setDamageValue(10);
            blade.setHoverName(Component.literal("Test blade"));
            blade.enchant(Enchantments.SHARPNESS, 3);
            blade.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
                state.setProudSoulCount(100);
                state.setKillCount(20);
            });
            grid.setItem(1, new ItemStack(Items.COBBLESTONE, 5));
            grid.setItem(2, blade);
            helper.assertTrue(blade.getDamageValue() == 10, "test blade has no damage for " + item + ": " + blade.getDamageValue());
            helper.assertTrue(grid.getItem(1).is(net.minecraft.tags.ItemTags.STONE_TOOL_MATERIALS), "cobblestone tag is missing");
            helper.assertTrue(blade.getCapability(ItemSlashBlade.BLADESTATE).map(s -> s.getProudSoulCount() == 100).orElse(false), "test soul setup failed");
            helper.assertTrue(recipe.matches(grid, helper.getLevel()), "recipe must match " + item);
            ItemStack result = recipe.assemble(grid, helper.getLevel().registryAccess());
            helper.assertTrue(result.is(item) && result.getDamageValue() == 9, "wrong result or durability for " + item);
            helper.assertTrue(result.hasCustomHoverName() && result.getEnchantmentLevel(Enchantments.SHARPNESS) == 3,
                    "name or enchantment was lost");
            var state = result.getCapability(ItemSlashBlade.BLADESTATE).orElseThrow(IllegalStateException::new);
            helper.assertTrue(state.getProudSoulCount() == 98 && state.getKillCount() == 20, "blade state was lost");
            helper.assertTrue(blade.getDamageValue() == 10 && blade.getCapability(ItemSlashBlade.BLADESTATE)
                    .map(s -> s.getProudSoulCount() == 100).orElse(false), "preview changed the input");
            grid.setItem(0, new ItemStack(Items.DIRT));
            helper.assertTrue(!recipe.matches(grid, helper.getLevel()), "extra input must not be consumed");
            grid.setItem(0, ItemStack.EMPTY);
            grid.setItem(1, new ItemStack(Items.DIRT));
            helper.assertTrue(!recipe.matches(grid, helper.getLevel()), "invalid material must not match");
            grid.setItem(1, new ItemStack(Items.COBBLESTONE));
            blade.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(s -> s.setProudSoulCount(2));
            helper.assertTrue(!recipe.matches(grid, helper.getLevel()), "insufficient souls must not match");
        }
        helper.succeed();
    }
}

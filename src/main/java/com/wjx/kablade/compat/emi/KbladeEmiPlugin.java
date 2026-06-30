package com.wjx.kablade.compat.emi;

import com.wjx.kablade.init.ModItems;
import com.wjx.kablade.util.ResourceUtil;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiStack;
import mods.flammpfeil.slashblade.recipe.SlashBladeShapedRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;

/**
 * 为本模组的拔刀剑与刀配方提供 EMI 兼容。
 * <p>
 * 通过 {@code @EmiEntrypoint} 自动发现（仅当 EMI 存在时加载）。功能：
 * <ul>
 *   <li>注册四类载体物品的默认比较器，使不同命名刀在 EMI 中显示为独立物品</li>
 *   <li>将 {@code slashblade:shaped_blade} 刀配方注册为 EMI 工作台配方</li>
 * </ul>
 */
@EmiEntrypoint
public class KbladeEmiPlugin implements EmiPlugin {

    /** 刀配方分类（使用工作台图标，与普通合成表同组显示）。 */
    static final EmiRecipeCategory BLADE_CATEGORY = new EmiRecipeCategory(
            ResourceUtil.getLocation("shaped_blade"),
            EmiStack.of(Blocks.CRAFTING_TABLE));

    @Override
    public void register(EmiRegistry registry) {
        // ——— 自定义比较器：让 EMI 按 bladeState.translationKey 区分命名刀 ———
        Comparison bladeComparison = Comparison.of((a, b) -> {
            String keyA = getBladeTranslationKey(a);
            String keyB = getBladeTranslationKey(b);
            return !keyA.isEmpty() && keyA.equals(keyB);
        });
        registry.setDefaultComparison(ModItems.KABLADE_BLADE.get(), bladeComparison);
        registry.setDefaultComparison(ModItems.KABLADE_HONKAI_BLADE.get(), bladeComparison);
        registry.setDefaultComparison(ModItems.KABLADE_SL_BLADE.get(), bladeComparison);
        registry.setDefaultComparison(ModItems.KABLADE_AW_BLADE.get(), bladeComparison);

        // ——— 注册刀配方 ———
        registry.addCategory(BLADE_CATEGORY);
        registry.addWorkstation(BLADE_CATEGORY, EmiStack.of(Blocks.CRAFTING_TABLE));

        RecipeManager rm = registry.getRecipeManager();
        var access = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.registryAccess()
                : null;
        if (access == null) return;

        for (var recipe : rm.getAllRecipesFor(RecipeType.CRAFTING)) {
            if (recipe instanceof SlashBladeShapedRecipe sbsr) {
                registry.addRecipe(new EmiShapedBladeRecipe(BLADE_CATEGORY, sbsr, access));
            }
        }
    }

    /** 从 EmiStack 的 NBT 中提取 bladeState.translationKey。 */
    private static String getBladeTranslationKey(EmiStack stack) {
        if (stack.getNbt() != null && stack.getNbt().contains("bladeState")) {
            return stack.getNbt().getCompound("bladeState").getString("translationKey");
        }
        // 后备：从 ItemStack 本体读 capability（兼容 SlashBlade 已水化的堆叠）
        ItemStack itemStack = stack.getItemStack();
        if (!itemStack.isEmpty()) {
            return itemStack.getCapability(
                    mods.flammpfeil.slashblade.item.ItemSlashBlade.BLADESTATE)
                    .map(mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState::getTranslationKey)
                    .orElse("");
        }
        return "";
    }
}

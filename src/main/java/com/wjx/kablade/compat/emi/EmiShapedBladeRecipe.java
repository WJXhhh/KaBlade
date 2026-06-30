package com.wjx.kablade.compat.emi;

import com.wjx.kablade.client.BladeRecipePreviewHydrator;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.recipe.SlashBladeIngredient;
import mods.flammpfeil.slashblade.recipe.SlashBladeShapedRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

/**
 * EMI 配方包装 —— 将 {@link SlashBladeShapedRecipe}（slashblade:shaped_blade）渲染为
 * 工作台合成配方。
 * <p>
 * 从配方中读取 pattern + key，逐一转为 {@link EmiIngredient}，并将结果刀通过
 * {@link BladeRecipePreviewHydrator} 水化为完整的命名刀显示。
 */
public final class EmiShapedBladeRecipe extends BasicEmiRecipe {

    private static final int WIDTH = 116;
    private static final int HEIGHT = 54;

    /** 3×3 输入槽偏移（左上角起始）。 */
    private static final int SLOT_X = 0;
    private static final int SLOT_Y = 0;
    private static final int SLOT_SIZE = 18;

    /** 输出槽偏移（右侧）。 */
    private static final int OUTPUT_X = 94;
    private static final int OUTPUT_Y = 18;

    /** 箭头位置。 */
    private static final int ARROW_X = 62;
    private static final int ARROW_Y = 18;

    public EmiShapedBladeRecipe(EmiRecipeCategory category, SlashBladeShapedRecipe recipe,
                                RegistryAccess registryAccess) {
        super(category, recipe.getId(), WIDTH, HEIGHT);

        // 解析输入 —— 按 pattern 排列成 3×3 网格，空白处填 EMPTY
        List<EmiIngredient> inputList = new ArrayList<>(9);
        for (int row = 0; row < recipe.getHeight(); row++) {
            for (int col = 0; col < recipe.getWidth(); col++) {
                int idx = row * recipe.getWidth() + col;
                if (idx < recipe.getIngredients().size()) {
                    Ingredient ing = recipe.getIngredients().get(idx);
                    inputList.add(toEmiIngredient(ing));
                } else {
                    inputList.add(EmiStack.EMPTY);
                }
            }
        }
        // 补齐 3×3
        while (inputList.size() < 9) {
            inputList.add(EmiStack.EMPTY);
        }
        this.inputs = inputList;

        // 解析输出
        ItemStack output = resolveOutput(recipe, registryAccess);
        this.outputs = List.of(EmiStack.of(output));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        // 3×3 输入槽
        for (int i = 0; i < 9; i++) {
            int x = SLOT_X + (i % 3) * SLOT_SIZE;
            int y = SLOT_Y + (i / 3) * SLOT_SIZE;
            if (i < inputs.size() && !inputs.get(i).isEmpty()) {
                widgets.addSlot(inputs.get(i), x, y);
            } else {
                widgets.addSlot(EmiStack.EMPTY, x, y);
            }
        }

        // 箭头
        widgets.addFillingArrow(ARROW_X, ARROW_Y, 40);

        // 输出槽
        widgets.addSlot(outputs.get(0), OUTPUT_X, OUTPUT_Y)
                .large(true)
                .recipeContext(this);
    }

    // ——— 辅助方法 ———

    /** 将 Ingredient 转为 EmiIngredient，对 {@link SlashBladeIngredient} 做水化处理。 */
    private static EmiIngredient toEmiIngredient(Ingredient ing) {
        if (ing instanceof SlashBladeIngredient) {
            // 对 blade 原料获取其匹配堆叠并水化
            ItemStack[] items = ing.getItems();
            List<EmiStack> emiStacks = new ArrayList<>();
            for (ItemStack stack : items) {
                if (!stack.isEmpty()) {
                    BladeRecipePreviewHydrator.hydrate(stack);
                    emiStacks.add(EmiStack.of(stack));
                }
            }
            if (!emiStacks.isEmpty()) {
                return EmiIngredient.of(emiStacks);
            }
        }
        // 普通原料
        return EmiIngredient.of(ing);
    }

    /** 获取成品刀 ItemStack。优先通过 {@code SlashBladeDefinition.getBlade()} 获取完整命名刀。 */
    private static ItemStack resolveOutput(SlashBladeShapedRecipe recipe, RegistryAccess registryAccess) {
        ItemStack result = recipe.getResultItem(registryAccess);
        if (result.getItem() instanceof ItemSlashBlade) {
            // 尝试水化成本模组的命名刀
            BladeRecipePreviewHydrator.hydrate(result);
        }
        return result;
    }
}

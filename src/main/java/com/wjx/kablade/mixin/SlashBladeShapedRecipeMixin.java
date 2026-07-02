package com.wjx.kablade.mixin;

import com.wjx.kablade.Main;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.recipe.SlashBladeShapedRecipe;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(SlashBladeShapedRecipe.class)
public abstract class SlashBladeShapedRecipeMixin {

    @Unique
    private static final Set<ResourceLocation> KABLADE_LOGGED_FIXED_RECIPES = ConcurrentHashMap.newKeySet();

    @Inject(method = "getResultItem", at = @At("RETURN"), cancellable = true)
    private void kablade$keepKbladeCarrierInPreview(RegistryAccess registryAccess,
                                                    CallbackInfoReturnable<ItemStack> cir) {
        ItemStack fixed = kablade$fixKbladeCarrier(cir.getReturnValue(), registryAccess);
        if (fixed != cir.getReturnValue()) {
            cir.setReturnValue(fixed);
        }
    }

    @Inject(method = "assemble", at = @At("RETURN"), cancellable = true)
    private void kablade$keepKbladeCarrierAfterCraft(CraftingContainer container, RegistryAccess registryAccess,
                                                     CallbackInfoReturnable<ItemStack> cir) {
        ItemStack fixed = kablade$fixKbladeCarrier(cir.getReturnValue(), registryAccess);
        if (fixed != cir.getReturnValue()) {
            cir.setReturnValue(fixed);
        }
    }

    @Unique
    private ItemStack kablade$fixKbladeCarrier(ItemStack original, RegistryAccess registryAccess) {
        if (original.isEmpty() || registryAccess == null) {
            return original;
        }

        SlashBladeShapedRecipe recipe = (SlashBladeShapedRecipe) (Object) this;
        ResourceLocation outputBlade = recipe.getOutputBlade();
        if (outputBlade == null || !Main.MODID.equals(outputBlade.getNamespace())) {
            return original;
        }

        SlashBladeDefinition definition = registryAccess
                .registryOrThrow(SlashBladeDefinition.REGISTRY_KEY)
                .get(outputBlade);
        if (definition == null) {
            return original;
        }

        Item carrier = ForgeRegistries.ITEMS.getValue(definition.getItemName());
        if (!(carrier instanceof ItemSlashBlade) || original.is(carrier)) {
            return original;
        }

        ItemStack fixed = new ItemStack(carrier, original.getCount());
        fixed.setTag(original.getTag() == null ? null : original.getTag().copy());
        fixed.setDamageValue(original.getDamageValue());
        kablade$syncBladeStateFromTag(fixed);

        if (KABLADE_LOGGED_FIXED_RECIPES.add(recipe.getId())) {
            ResourceLocation originalId = ForgeRegistries.ITEMS.getKey(original.getItem());
            ResourceLocation fixedId = ForgeRegistries.ITEMS.getKey(carrier);
            Main.LOGGER.info("[{}] Fixed SlashBlade shaped result carrier recipe={} blade={} {} -> {}",
                    Main.MODID, recipe.getId(), outputBlade, originalId, fixedId);
        }
        return fixed;
    }

    @Unique
    private static void kablade$syncBladeStateFromTag(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains("bladeState")) {
            return;
        }
        stack.getCapability(ItemSlashBlade.BLADESTATE)
                .ifPresent(state -> state.deserializeNBT(stack.getTag().getCompound("bladeState")));
    }
}

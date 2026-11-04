package com.wjx.kablade.mixin;

import com.wjx.kablade.object.item.KbladeBladeItem;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RepairItemRecipe.class)
public abstract class RepairItemRecipeMixin {
    @Inject(method = "assemble(Lnet/minecraft/world/inventory/CraftingContainer;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"), cancellable = true)
    private void kablade$repairToOriginalNameless(CraftingContainer container, RegistryAccess registryAccess,
                                                  CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = cir.getReturnValue();
        if (!(result.getItem() instanceof KbladeBladeItem)) {
            return;
        }

        ItemStack nameless = new ItemStack(SlashBladeItems.SLASHBLADE.get(), result.getCount());
        if (result.hasTag()) {
            nameless.setTag(result.getTag().copy());
        }
        nameless.setDamageValue(result.getDamageValue());
        cir.setReturnValue(nameless);
    }
}

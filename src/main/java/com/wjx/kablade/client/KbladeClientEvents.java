package com.wjx.kablade.client;

import com.wjx.kablade.Main;
import com.wjx.kablade.api.CustomBladeModel;
import com.wjx.kablade.init.ModItems;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * Client-only mod-bus subscriber. After models are baked, replaces the inventory model of every item
 * whose {@link ModItems} field is annotated {@link CustomBladeModel} with SlashBlade's
 * {@link BladeModel}, so the item renders as a 3D blade driven by its blade-state NBT.
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class KbladeClientEvents {

    private KbladeClientEvents() {
    }

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public static void onModifyBakingResult(final ModelEvent.ModifyBakingResult event) {
        for (Field field : ModItems.class.getDeclaredFields()) {
            if (!field.isAnnotationPresent(CustomBladeModel.class)) {
                continue;
            }
            try {
                RegistryObject<? extends Item> ro = (RegistryObject<? extends Item>) field.get(null);
                Item item = ro.get();
                ModelResourceLocation loc = new ModelResourceLocation(
                        Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(item)), "inventory");
                BakedModel original = event.getModels().get(loc);
                event.getModels().put(loc, new BladeModel(original, event.getModelBakery()));
            } catch (IllegalAccessException ignored) {
            }
        }
    }
}
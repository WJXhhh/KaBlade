package com.wjx.kablade.mixin;

import cn.mmf.lastsmith.blades.BladeLoader;
import cn.mmf.lastsmith.blades.compat.ItemSlashBladeRF;
import cn.mmf.lastsmith.blades.compat.ItemSlashBladeWind;
import cn.mmf.lastsmith.blades.vanilla.VanillaBladeRegister;
import cn.mmf.lastsmith.event.RegisterSlashBladeEvent;
import cn.mmf.lastsmith.util.BladeUtil;
import com.wjx.kablade.AllWeapon.blade.items.Item_AwNamed;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_HonkaiNamed;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_KaNamed;
import com.wjx.kablade.SlashBlade.blades.bladeitem.MagicBlade;
import com.wjx.kablade.config.ModConfig;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Loader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;

@Mixin(value = VanillaBladeRegister.class,remap = false)
public class LastSmithMixin {
    @Shadow
    @Final
    private static String[] defaultBewitched;

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void onSlashBladeRegister(RegisterSlashBladeEvent event) {
        SlashBlade.BladeRegistry.forEach((name, blade) -> {
            if (!(blade.getItem() instanceof ItemSlashBladeNamed))
                return;
            NBTTagCompound oldNBT = ItemSlashBlade.getItemTagCompound(blade);
            ItemStack newBlade = new ItemStack(BladeLoader.bladeNamed);

            for (String s : ModConfig.GeneralConf.TLS_forbidden){
                if(blade.getItem().getRegistryName().getNamespace().equals(s)){
                    return;
                }
            }

            if (Loader.isModLoaded("slashblade_addon")) {
                if (blade.getItem() instanceof ItemSlashBladeRF) {
                    newBlade = new ItemStack(BladeLoader.rfblade);
                    BladeUtil.getInstance().ModelOnName.set(oldNBT, ItemSlashBladeNamed.ModelName.get(oldNBT));
                    BladeUtil.getInstance().TextureOnName.set(oldNBT, ItemSlashBladeNamed.TextureName.get(oldNBT));
                    newBlade.setTagCompound(oldNBT);
                    SlashBlade.BladeRegistry.put(name, newBlade);
                    return;
                }
                if (Loader.isModLoaded("thaumcraft") && blade.getItem() instanceof ItemSlashBladeWind)
                    newBlade = new ItemStack(BladeLoader.windBlade);
            }
            for(String bewitched : defaultBewitched) {
                if (ItemSlashBladeNamed.CurrentItemName.get(oldNBT)
                        .equalsIgnoreCase(bewitched))
                    BladeUtil.getInstance().IsBewitchedActived.set(oldNBT, true);
            }
            newBlade.setTagCompound(oldNBT);
            SlashBlade.BladeRegistry.put(name, newBlade);
        });
    }
}

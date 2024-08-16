package com.wjx.kablade.SlashBlade.blades.bladeitem;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.Util.ItemSlashUtil;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import static com.wjx.kablade.Main.bladestr;

public class Item_HonkaiNamed extends ItemSlashBladeNamed {
    public Item_HonkaiNamed(ToolMaterial par2EnumToolMaterial, float baseAttackModifiers, String name) {
        super(par2EnumToolMaterial, baseAttackModifiers);
        this.setRegistryName(name);
        ForgeRegistries.ITEMS.register(this);
        ItemSlashUtil.KAITEMBLADE.add(this);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> subItems) {
        if (this.isInCreativeTab(tab)) {
            for(String bladename : BladeLoader.NamedHonkai){
                ItemStack blade = SlashBlade.findItemStack(bladestr,bladename,1);
                NBTTagCompound tag = getItemTagCompound(blade);
                if(blade.getTranslationKey().equals("item.wjx.blade.honkai.murauson")){
                    NBTTagCompound s = blade.getTagCompound();
                    s.isEmpty();
                }
                if(!blade.isEmpty()) {

                        subItems.add(blade);

                }
            }
        }
    }
}

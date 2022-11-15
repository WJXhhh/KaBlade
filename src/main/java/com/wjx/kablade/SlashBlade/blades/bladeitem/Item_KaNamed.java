package com.wjx.kablade.SlashBlade.blades.bladeitem;

import com.wjx.kablade.Main;
import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.Util.ItemSlashUtil;
import com.wjx.kablade.creativeTab.tabkablade_blades;
import com.wjx.kablade.creativeTab.tabkablade_bladesgod;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import static com.wjx.kablade.Main.bladestr;

public class Item_KaNamed extends ItemSlashBladeNamed {
    public Item_KaNamed(ToolMaterial par2EnumToolMaterial, float baseAttackModifiers, String name) {
        super(par2EnumToolMaterial, baseAttackModifiers);
        this.setRegistryName(name);
        ForgeRegistries.ITEMS.register(this);
        ItemSlashUtil.KAITEMBLADE.add(this);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> subItems) {
        if (this.isInCreativeTab(tab)) {
            for(String bladename : BladeLoader.NamedBlades){
                ItemStack blade = SlashBlade.findItemStack(bladestr,bladename,1);
                NBTTagCompound tag = getItemTagCompound(blade);
                if(!blade.isEmpty()) {

                        subItems.add(blade);

                }
            }
        }
    }
}

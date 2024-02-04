package com.wjx.kablade.AllWeapon.blade.items;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.Util.ItemSlashUtil;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.List;

import static com.wjx.kablade.Main.bladestr;

public class Item_AwNamed extends ItemSlashBladeNamed {
    public Item_AwNamed(ToolMaterial par2EnumToolMaterial, float baseAttackModifiers, String name) {
        super(par2EnumToolMaterial, baseAttackModifiers);
        this.setRegistryName(name);
        ForgeRegistries.ITEMS.register(this);
        ItemSlashUtil.KAITEMBLADE.add(this);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return super.getItemStackDisplayName(stack);
    }

    @Override
    public void addInformationSwordClass(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, List par3List, boolean par4) {
        if (par1ItemStack.getTagCompound() != null && par1ItemStack.getTagCompound().getCompoundTag("allweapon").getInteger("OverLimi") > 0) {
            NBTTagCompound alTag = par1ItemStack.getTagCompound().getCompoundTag("allweapon");
            par3List.add(I18n.format("info.allweapon.break",alTag.getInteger("OverLimi")));
        }else{
            super.addInformationSwordClass(par1ItemStack, par2EntityPlayer, par3List, par4);
        }

    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> subItems) {
        if (this.isInCreativeTab(tab)) {
            for(String bladename : BladeLoader.AwBlades){
                ItemStack blade = SlashBlade.findItemStack(bladestr,bladename,1);
                NBTTagCompound tag = getItemTagCompound(blade);
                if(!blade.isEmpty()) {

                        subItems.add(blade);

                }
            }
        }
    }
}

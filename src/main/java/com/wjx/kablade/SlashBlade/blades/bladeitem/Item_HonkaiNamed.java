package com.wjx.kablade.SlashBlade.blades.bladeitem;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.Util.ItemSlashUtil;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;

import static com.wjx.kablade.Main.bladestr;

public class Item_HonkaiNamed extends ItemSlashBladeNamed {
    private static final String GALACTIC_NOVA = "wjx.blade.honkai.galactic";
    private static final int LEGACY_GALACTIC_SA = 7;
    private static final int LIGHTS_ON_STAGE_SA = 461;

    public Item_HonkaiNamed(ToolMaterial par2EnumToolMaterial, float baseAttackModifiers, String name) {
        super(par2EnumToolMaterial, baseAttackModifiers);
        this.setRegistryName(name);
        ForgeRegistries.ITEMS.register(this);
        ItemSlashUtil.KAITEMBLADE.add(this);
    }

    @Override
    public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        super.onUpdate(stack, worldIn, entityIn, itemSlot, isSelected);
        if (worldIn.isRemote || stack.isEmpty()) {
            return;
        }
        NBTTagCompound tag = getItemTagCompound(stack);
        if (GALACTIC_NOVA.equals(CurrentItemName.get(tag))
                && ItemSlashBlade.SpecialAttackType.get(tag) == LEGACY_GALACTIC_SA) {
            ItemSlashBlade.SpecialAttackType.set(tag, LIGHTS_ON_STAGE_SA);
        }
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

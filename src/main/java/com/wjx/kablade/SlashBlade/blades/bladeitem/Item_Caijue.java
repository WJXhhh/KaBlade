package com.wjx.kablade.SlashBlade.blades.bladeitem;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.Util.ItemSlashUtil;
import com.wjx.kablade.event.UpdateColor;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.EnumSet;
import java.util.List;

import static com.wjx.kablade.Main.bladestr;

public class Item_Caijue extends ItemSlashBladeNamed {
    public Item_Caijue(ToolMaterial par2EnumToolMaterial, float baseAttackModifiers, String name) {
        super(par2EnumToolMaterial, baseAttackModifiers);
        this.setRegistryName(name);
        ForgeRegistries.ITEMS.register(this);
        ItemSlashUtil.KAITEMBLADE.add(this);
    }

    @SideOnly(Side.CLIENT)
    public void addInformationSwordClass(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, List par3List, boolean par4) {
        EnumSet<SwordType> swordType = this.getSwordType(par1ItemStack);
        NBTTagCompound tag = getItemTagCompound(par1ItemStack);

        par3List.add(UpdateColor.makeColour2(I18n.translateToLocal("info.slashblade.godkey.3")));


    }

    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
        if(entity instanceof EntityLivingBase)
        {
            entity.getEntityData().setBoolean("dizui", true);
            entity.getEntityData().setInteger("dizuitime", 300);
        }
        super.onLeftClickEntity(stack, player, entity);
        return true;
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase par2EntityLivingBase, EntityLivingBase par3EntityLivingBase) {
        if (par2EntityLivingBase != null ) {
            par2EntityLivingBase.getEntityData().setBoolean("dizui",true);
            par2EntityLivingBase.getEntityData().setInteger("dizuitime", 300);
        }
        //super.hitEntity(stack,par2EntityLivingBase,par3EntityLivingBase);
        super.hitEntity(stack,par2EntityLivingBase,par3EntityLivingBase);
        par2EntityLivingBase.attackEntityFrom(DamageSource.causeMobDamage(par3EntityLivingBase),64f);



        return true;
    }

    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        //tooltip.add(String.format("SBCOLOR: %s",stack.getTagCompound().getInteger("SummonedSwordColor")));
        //tooltip.add("");
        tooltip.add(UpdateColor.makeColourPur(I18n.translateToLocal("info.slashblade.dizui.1")));
        tooltip.add("");
        tooltip.add(UpdateColor.makeColourPur(I18n.translateToLocal("info.slashblade.dizui.2")));
        tooltip.add("");
        tooltip.add(UpdateColor.makeColourPur(I18n.translateToLocal("info.slashblade.dizui.3")));
        tooltip.add("");
        tooltip.add(UpdateColor.makeColourPur(I18n.translateToLocal("info.slashblade.dizui.4")));
        tooltip.add(UpdateColor.makeColourPur(I18n.translateToLocal("info.slashblade.dizui.5")));
        tooltip.add(UpdateColor.makeColourPur(I18n.translateToLocal("info.slashblade.dizui.6")));

    }



    @SideOnly(Side.CLIENT)
    public void addInformationSpecialAttack(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, List par3List, boolean par4) {
        EnumSet<SwordType> swordType = this.getSwordType(par1ItemStack);
        if (swordType.contains(SwordType.Bewitched)) {
            Object tag = getItemTagCompound(par1ItemStack);
            String key = "flammpfeil.slashblade.specialattack." + this.getSpecialAttack(par1ItemStack).toString();
            par3List.add(String.format("SA:" + UpdateColor.makeColourPur(I18n.translateToLocal(key))));
        }

    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> subItems) {
        if (this.isInCreativeTab(tab)) {
            for(String bladename : BladeLoader.DIZUI){
                ItemStack blade = SlashBlade.findItemStack(bladestr,bladename,1);
                NBTTagCompound tag = getItemTagCompound(blade);
                if(!blade.isEmpty()) {

                        subItems.add(blade);

                }
            }
        }
    }
}

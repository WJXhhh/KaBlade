package com.wjx.kablade.SlashBlade.blades.bladeitem;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.Util.ItemSlashUtil;
import com.wjx.kablade.event.KillEvent;
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
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.EnumSet;
import java.util.List;

import static com.wjx.kablade.Main.bladestr;

public class MagicBlade extends ItemSlashBladeNamed {
    public MagicBlade(ToolMaterial par2EnumToolMaterial, float baseAttackModifiers, String name) {
        super(par2EnumToolMaterial, baseAttackModifiers);
        this.setRegistryName(name);
        ForgeRegistries.ITEMS.register(this);
        ItemSlashUtil.KAITEMBLADE.add(this);
    }

    public String getItemStackDisplayName(ItemStack stack) {
        return UpdateColor.makeColourRainbow(I18n.translateToLocal("name.slashblade.magic"));
    }

    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
        if(entity instanceof EntityPlayer){
            KillEvent.killplayer((EntityLivingBase) entity, player);
        }else if(entity instanceof  EntityLivingBase){
            KillEvent.killutil((EntityLivingBase) entity,player);
        }

        super.onLeftClickEntity(stack, player, entity);
        return true;
    }


    @Override
    public void setDamage(ItemStack stack, int damage) {
        super.setDamage(stack, damage);
    }

    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        //tooltip.add(String.format("SBCOLOR: %s",stack.getTagCompound().getInteger("SummonedSwordColor")));
        //tooltip.add("");
        tooltip.add(UpdateColor.makeColourPur(I18n.translateToLocal("info.slashblade.magic.1")));
        tooltip.add("");
        tooltip.add(UpdateColor.makeColourPur(I18n.translateToLocal("info.slashblade.magic.2")));
        tooltip.add("");
        tooltip.add(UpdateColor.makeColourPur(I18n.translateToLocal("info.slashblade.magic.3")));
        tooltip.add("");
        tooltip.add(UpdateColor.makeColourPur(I18n.translateToLocal("info.slashblade.magic.4")));
        tooltip.add("");
        tooltip.add(UpdateColor.makeColourPur(I18n.translateToLocal("info.slashblade.magic.5")));

    }

    @SideOnly(Side.CLIENT)
    public void addInformationSpecialAttack(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, List par3List, boolean par4) {
        EnumSet<SwordType> swordType = this.getSwordType(par1ItemStack);
        if (swordType.contains(SwordType.Bewitched)) {
            Object tag = getItemTagCompound(par1ItemStack);
            String key = "flammpfeil.slashblade.specialattack." + this.getSpecialAttack(par1ItemStack).toString();
            par3List.add(String.format("SA:" + UpdateColor.makeColourRainbow(I18n.translateToLocal(key))));
        }

    }

    @SideOnly(Side.CLIENT)
    public void addInformationKillCount(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, List par3List, boolean par4) {
        this.getSwordType(par1ItemStack);
        Object tag = getItemTagCompound(par1ItemStack);
        par3List.add("KillCount : " + UpdateColor.makeColourRainbow(I18n.translateToLocal("info.infinity")));
    }

    @SideOnly(Side.CLIENT)
    public void addInformationProudSoul(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, List par3List, boolean par4) {
        this.getSwordType(par1ItemStack);
        Object tag = getItemTagCompound(par1ItemStack);
        par3List.add("ProudSoul : " + UpdateColor.makeColourRainbow(I18n.translateToLocal("info.infinity")));
    }

    @SideOnly(Side.CLIENT)
    public void addInformationRepairCount(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, List par3List, boolean par4) {
        Object tag = getItemTagCompound(par1ItemStack);
        par3List.add("Refine : " + UpdateColor.makeColourRainbow(I18n.translateToLocal("info.infinity")));
    }

    /*@SideOnly(Side.CLIENT)
    public void addInformationMaxAttack(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, List par3List, boolean par4) {
        NBTTagCompound tag = getItemTagCompound(par1ItemStack);
        float repair = (float)RepairCount.get(tag);
        EnumSet<SwordType> swordType = this.getSwordType(par1ItemStack);
        par3List.add("");
        par3List.add("RankAttackDamage");
        String header;
        String template;
        if (swordType.contains(SwordType.FiercerEdge)) {
            header = "§6B-A§r/§4S-SSS§r/§5Limit";
            template = ChatFormatting.GOLD + "+" + 32768.0 + ChatFormatting.WHITE + "/" + ChatFormatting.DARK_RED + "+" + 32768.0 + ChatFormatting.WHITE + "/" + ChatFormatting.DARK_PURPLE + "+" + 32768.0;
        } else {
            header = "§6B-SS§r/§4SSS§r/§5Limit";
            template = ChatFormatting.GOLD + "+" + 32768.0 + ChatFormatting.WHITE + "/" + ChatFormatting.DARK_RED + "+" + 32768.0 + ChatFormatting.WHITE + "/" + ChatFormatting.DARK_PURPLE + "+" + 32768.0;
        }

        float baseModif = this.getBaseAttackModifiers(tag);
        float maxBonus = 10.0F + repair;
        float level = (float)par2EntityPlayer.experienceLevel;
        float sss = baseModif + Math.min(maxBonus, level);
        par3List.add(header);
        par3List.add(String.format(template, baseModif, sss, baseModif + maxBonus));
    }*/
    @SideOnly(Side.CLIENT)
    public void addInformationSwordClass(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, List par3List, boolean par4) {
        EnumSet<SwordType> swordType = this.getSwordType(par1ItemStack);
        NBTTagCompound tag = getItemTagCompound(par1ItemStack);

        par3List.add(UpdateColor.makeColourPur(I18n.translateToLocal("info.slashblade.deamon")));


    }
    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> subItems) {
        if (this.isInCreativeTab(tab)) {
            for(String bladename : BladeLoader.NamedGod){
                ItemStack blade = SlashBlade.findItemStack(bladestr,bladename,1);
                NBTTagCompound tag = getItemTagCompound(blade);
                if(!blade.isEmpty()) {

                        subItems.add(blade);

                }
            }
        }
    }
}

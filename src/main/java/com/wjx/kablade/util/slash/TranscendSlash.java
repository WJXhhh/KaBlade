package com.wjx.kablade.util.slash;

import com.wjx.kablade.Main;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;

import java.util.EnumSet;

public class TranscendSlash extends ItemSlashBladeNamed {
    private boolean firstload;
    public boolean isUsing = false;

    public TranscendSlash(){
        super(ToolMaterial.IRON,4.0f);
        this.firstload = false;
        setMaxDamage(40);
        setCreativeTab(Main.TABKABLADE_BLADES);
    }

    public void onUpdate(ItemStack stack, World world, Entity entity, int i, boolean b){
        super.onUpdate(stack, world, entity, i, b);
        if(!this.isUsing) SlashUpdateEvent.setTimestop(Boolean.valueOf(false));
        if(entity instanceof EntityPlayer) this.isUsing = false;
        ItemSlashBlade.specialAttacks.put(Integer.valueOf(678),new Delete());
        NBTTagCompound tag = stack.getTagCompound();
        tag.setInteger("HideFlags",6);
        ItemSlashBladeNamed.NamedBlades.add("flammpfeil.slashblade.named.tran");
        ItemSlashBladeNamed.CurrentItemName.set(tag, "flammpfeil.slashblade.named.tran");
        ItemSlashBladeNamed.CustomMaxDamage.set(tag, Integer.valueOf(32767));
        ItemSlashBladeNamed.IsDefaultBewitched.set(tag, Boolean.valueOf(true));
        ItemSlashBlade.SummonedSwordColor.set(tag, 0x4091FF);
        ItemSlashBlade.setBaseAttackModifier(tag, 32767.0F);
        ItemSlashBlade.SpecialAttackType.set(tag, Integer.valueOf(678));
        ItemSlashBlade.TextureName.set(tag, "named/transcend/texture");
        ItemSlashBlade.ModelName.set(tag, "named/transcend/model");
        ItemSlashBlade.RepairCount.set(tag, Integer.valueOf(100000));
        ItemSlashBlade.KillCount.set(tag, Integer.valueOf(1000000));
        ItemSlashBlade.ProudSoul.set(tag, Integer.valueOf(1000000));
        SpecialEffects.addEffect(stack, "SETranscend",1000);
        if(!this.firstload){
            this.firstload = true;
            stack.setTagCompound(tag);
        }
        if (EnchantmentHelper.getEnchantmentLevel(Enchantments.INFINITY, stack) <= 0) stack.addEnchantment(Enchantments.INFINITY, 127);
        if (EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, stack) <= 0) stack.addEnchantment(Enchantments.POWER, 127);
        if(EnchantmentHelper.getEnchantmentLevel(Enchantments.PUNCH, stack) <= 0) stack.addEnchantment(Enchantments.PUNCH, 127);
        tag.setBoolean("Unbreakable", true);
    }

    public void onUsingTick(ItemStack stack, EntityLivingBase player, int count) {
        super.onUsingTick(stack, player, count);
        EnumSet<SwordType> swordType = getSwordType(stack);
        int var6 = getMaxItemUseDuration(stack) - count;
        if (ItemSlashBladeNamed.RequiredChargeTick < var6) {
            this.isUsing = true;
            player.playSound(SoundEvents.ENTITY_ENDERMEN_TELEPORT, 0.5F, 1.0F);
            SlashUpdateEvent.setTimestop(Boolean.valueOf(true));
        } else {
            SlashUpdateEvent.setTimestop(Boolean.valueOf(false));
            this.isUsing = false;
        }
    }

    private void removeEffect(EntityPlayer player, Potion potion) {
        if (player.getActivePotionEffect(potion) != null)
            player.removePotionEffect(potion);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> itemStacks){
        if(this.isInCreativeTab(tab)){
            if(tab == Main.TABKABLADE_BLADES) itemStacks.add(SlashBlade.findItemStack(SlashBlade.modid,"flammpfeil.slashblade.named.tran",1));
        }
    }

    public void setDamage(ItemStack itemStack, int Damage) {
        super.setDamage(itemStack, 0);
    }
}

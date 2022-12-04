package com.wjx.kablade.enchantments;

import com.wjx.kablade.init.EnchantmentInit;
import com.wjx.kablade.util.Reference;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.ResourceLocation;

public class EnchantmentSlow extends Enchantment {
    public EnchantmentSlow() {
        super(Rarity.UNCOMMON, EnumEnchantmentType.WEAPON, new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND});
        this.setName("kablade_slow");
        this.setRegistryName(new ResourceLocation(Reference.MODID + ":kablade_slow"));
        EnchantmentInit.ENCHANTMENTS.add(this);
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public int getMinLevel() {
        return 1;
    }
}

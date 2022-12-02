package com.wjx.kablade.init;

import com.google.common.collect.Lists;
import com.wjx.kablade.enchantments.EnchantmentSlow;
import net.minecraft.enchantment.Enchantment;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;

public class EnchantmentInit {
    public static final ArrayList<Enchantment> ENCHANTMENTS = Lists.newArrayList();

    public static final Enchantment ENCHANTMENT_SLOW = new EnchantmentSlow();

    public static void registerEnchantments(){
        ForgeRegistries.ENCHANTMENTS.registerAll(ENCHANTMENTS.toArray(new Enchantment[0]));
    }
}

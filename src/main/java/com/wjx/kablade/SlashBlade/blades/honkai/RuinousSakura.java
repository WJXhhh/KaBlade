package com.wjx.kablade.SlashBlade.blades.honkai;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_HonkaiNamed;
import com.wjx.kablade.SlashBlade.blades.recipe.SlashBladeRecipeModding;
import com.wjx.kablade.init.ItemInit;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.named.event.LoadEvent;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.wjx.kablade.Main.bladestr;

/** 魂妖刀「血樱寂灭」——赤染樱的高阶升级形态。 */
public class RuinousSakura {
    private final String name = "wjx.blade.honkai.ruinous_sakura";
    private final ItemStack customblade = new ItemStack(BladeLoader.ITEM_HONKAI_NAMED, 1, 0);
    private final NBTTagCompound tag = new NBTTagCompound();

    public RuinousSakura() {
        customblade.setTagCompound(tag);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void init(LoadEvent.InitEvent event) {
        Item_HonkaiNamed.CurrentItemName.set(tag, name);
        Item_HonkaiNamed.CustomMaxDamage.set(tag, 1000);
        ItemSlashBlade.TextureName.set(tag, "kablade/Honkai/RuinousSakura/mdl");
        ItemSlashBlade.ModelName.set(tag, "kablade/Honkai/RuinousSakura/mdl");
        ItemSlashBlade.BaseAttackModifier.set(tag, 34.0F);
        ItemSlashBlade.AttackAmplifier.set(tag, 2.5F);
        ItemSlashBlade.SpecialAttackType.set(tag, 461);
        ItemSlashBladeNamed.SummonedSwordColor.set(tag, 0xF3002C);
        Item_HonkaiNamed.IsDefaultBewitched.set(tag, true);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);

        customblade.addEnchantment(Enchantments.UNBREAKING, 5);
        customblade.addEnchantment(Enchantments.FIRE_ASPECT, 5);
        customblade.addEnchantment(Enchantments.SHARPNESS, 6);
        customblade.addEnchantment(Enchantments.POWER, 4);
        customblade.addEnchantment(Enchantments.SMITE, 5);
        SpecialEffects.addEffect(customblade, BladeProxy.FuelTheRuin);

        ItemSlashBladeNamed.NamedBlades.add(name);
        SlashBlade.registerCustomItemStack(name, customblade);
        BladeLoader.NamedHonkai.add(name);

        ItemStack result = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack previous = SlashBlade.findItemStack(bladestr,
                "wjx.blade.honkai.florid_sakura", 1);
        IRecipe recipe = new SlashBladeRecipeModding(
                new ResourceLocation(bladestr, "ruinous_sakura"), result, previous,
                "ABA",
                "CDC",
                "EEE",
                'A', new ItemStack(Blocks.LEAVES2, 1, 0),
                'B', new ItemStack(ItemInit.SUPERCONDUCTING_METAL),
                'C', new ItemStack(ItemInit.PETAL),
                'D', previous,
                'E', new ItemStack(Blocks.MAGMA));
        SlashBlade.addRecipe("ruinous_sakura", recipe);
    }
}

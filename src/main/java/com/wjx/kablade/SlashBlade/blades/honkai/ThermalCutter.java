package com.wjx.kablade.SlashBlade.blades.honkai;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_HonkaiNamed;
import com.wjx.kablade.SlashBlade.blades.recipe.SlashBladeRecipeModding;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.RecipeAwakeBlade;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.named.event.LoadEvent;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.wjx.kablade.Main.bladestr;

public class ThermalCutter {
    String name = "wjx.blade.honkai.thermal_cutter";
    String key = "wjx.blade.honkai.thermal_cutter";

    public ThermalCutter(){
        MinecraftForge.EVENT_BUS.register(this);
    }

    private ItemStack customblade = new ItemStack(BladeLoader.ITEM_HONKAI_NAMED,1,0);

    private NBTTagCompound tag = new NBTTagCompound();
    {
        customblade.setTagCompound(tag);
    }
    @SubscribeEvent
    public void init(LoadEvent.InitEvent event) {
        Item_HonkaiNamed.CurrentItemName.set(tag, name);
        Item_HonkaiNamed.CustomMaxDamage.set(tag, 500);

        ItemSlashBlade.TextureName.set(tag, "kablade/Honkai/Rhomphaia/tex/the");
        ItemSlashBlade.ModelName.set(tag, "kablade/Honkai/Rhomphaia/mdl");

        ItemSlashBlade.SpecialAttackType.set(tag,286);
        ItemSlashBlade.setBaseAttackModifier(tag,2);
        customblade.getTagCompound().setFloat("baseAttackModifier",11.0F);
        ItemSlashBlade.AttackAmplifier.set(tag,1.5f);
        customblade.addEnchantment(Enchantments.FIRE_ASPECT,2);
        ItemSlashBladeNamed.SummonedSwordColor.set(tag,0xDAA520);
        Item_HonkaiNamed.IsDefaultBewitched.set(tag, true);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        SlashBlade.registerCustomItemStack(this.name, customblade);
        BladeLoader.NamedHonkai.add(name);
        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prevblade = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.fuhezhuque", 1);
        IRecipe recipe = new SlashBladeRecipeModding(new ResourceLocation(bladestr,"thermal_cutter"),
                blackblade, prevblade,
                "  B",
                " B ",
                "A  ",
                'A', prevblade,
                'B',new ItemStack(Items.BLAZE_ROD,1));

        SlashBlade.addRecipe("thermal_cutter", recipe);
    }
}

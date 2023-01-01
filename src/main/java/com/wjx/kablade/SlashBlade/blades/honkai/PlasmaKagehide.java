package com.wjx.kablade.SlashBlade.blades.honkai;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_HonkaiNamed;
import com.wjx.kablade.SlashBlade.blades.recipe.SlashBladeTwoRecipeModding;
import com.wjx.kablade.enchantments.EnchantmentSlow;
import com.wjx.kablade.init.EnchantmentInit;
import com.wjx.kablade.init.ItemInit;
import com.wjx.kablade.util.Reference;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.RecipeAwakeBlade;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.named.event.LoadEvent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.wjx.kablade.Main.bladestr;

public class PlasmaKagehide {
    String name = "wjx.blade.honkai.plasma_kagehide";
    String key = "wjx.blade.honkai.raikiri";

    public PlasmaKagehide(){
        MinecraftForge.EVENT_BUS.register(this);
    }

    private ItemStack customblade = new ItemStack(BladeLoader.ITEM_HONKAI_NAMED,1,0);

    private NBTTagCompound tag = new NBTTagCompound();
    {
        customblade.setTagCompound(tag);
    }
    @SubscribeEvent
    public void init(LoadEvent.InitEvent event) {


        //ItemStack customblade = new ItemStack(STAR,1,0);
        //NBTTagCompound tag = new NBTTagCompound();
        //customblade.setTagCompound(tag);

        //tag.setBoolean("Unbreakable",true);
        Item_HonkaiNamed.CurrentItemName.set(tag, name);
        Item_HonkaiNamed.CustomMaxDamage.set(tag, 800);

        ItemSlashBlade.TextureName.set(tag, "kablade/Honkai/Raikiri_Plas/texPlasmaKagehide");
        ItemSlashBlade.ModelName.set(tag, "kablade/Honkai/Raikiri_Plas/mdlRaikiri_Plas");

        ItemSlashBlade.SpecialAttackType.set(tag, 293);
        customblade.getTagCompound().setFloat("baseAttackModifier",18.0F);
        customblade.addEnchantment(Enchantments.SMITE,2);
        customblade.addEnchantment(Enchantments.SHARPNESS,2);
        Item_HonkaiNamed.IsDefaultBewitched.set(tag, true);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        SlashBlade.registerCustomItemStack(this.name, customblade);
        BladeLoader.NamedHonkai.add(name);
        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prevblade = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.crystal_cutter", 1);
        ItemStack prevblade2 = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.pulse_katana_t17", 1);
        IRecipe recipe = new SlashBladeTwoRecipeModding(new ResourceLocation(bladestr,"plasma_kagehide"),
                blackblade, prevblade,prevblade2,
                new Object[]{
                        "  C",
                        " B ",
                        "A  ",
                        'A', prevblade,
                        'B',prevblade2,
                        'C', new ItemStack(ItemInit.MOLYBDENUM_SWORD),
                });

        SlashBlade.addRecipe("plasma_kagehide", recipe);
    }
}

package com.wjx.kablade.SlashBlade.blades;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_KaNamed;
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

public class ForestShadow {
    String name = "wjx.blade.forest_shadow";
    String key = "wjx.blade.forest_shadow";

    public ForestShadow(){
        MinecraftForge.EVENT_BUS.register(this);
    }

    private ItemStack customBlade = new ItemStack(BladeLoader.ITEM_KABLADE_NAMED,1,0);

    private NBTTagCompound Tag = new NBTTagCompound();
    {
        customBlade.setTagCompound(Tag);
    }

    @SubscribeEvent
    public void init(LoadEvent.InitEvent event) {
        Item_KaNamed.CurrentItemName.set(Tag, name);
        Item_KaNamed.CustomMaxDamage.set(Tag, 400);

        ItemSlashBlade.TextureName.set(Tag, "kablade/ForestShadow/texForestShadow");
        ItemSlashBlade.ModelName.set(Tag, "kablade/ForestShadow/mdlForestShadow");

        customBlade.getTagCompound().setFloat("baseAttackModifier",9.0F);
        ItemSlashBlade.AttackAmplifier.set(Tag,1.5f);
        ItemSlashBlade.SpecialAttackType.set(Tag,296);
        customBlade.addEnchantment(Enchantments.POWER,2);
        Item_KaNamed.IsDefaultBewitched.set(Tag, true);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(Tag, 1);
        ItemSlashBlade.SummonedSwordColor.set(Tag, 3388211);
        SlashBlade.registerCustomItemStack(this.name, customBlade);
        BladeLoader.NamedBlades.add(name);
        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prevblade = SlashBlade.findItemStack(bladestr, "wjx.blade.noted_vine", 1);
        IRecipe recipe = new SlashBladeRecipeModding(new ResourceLocation(bladestr,"forest_shadow"),
                blackblade, prevblade,
                new Object[]{
                        " C ",
                        "ABA",
                        " A ",
                        'A', new ItemStack(Items.EMERALD,1),
                        'B',prevblade,
                        'C', new ItemStack(Items.DYE,1,15),
                });

        SlashBlade.addRecipe("forest_shadow", recipe);
    }
}

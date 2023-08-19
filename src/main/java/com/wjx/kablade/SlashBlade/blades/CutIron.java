package com.wjx.kablade.SlashBlade.blades;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_KaNamed;
import com.wjx.kablade.init.ItemInit;
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
import net.minecraftforge.oredict.OreDictionary;

import static com.wjx.kablade.Main.bladestr;

public class CutIron {
    String name = "wjx.blade.cut_iron";
    String key = "wjx.blade.cut_iron";

    public CutIron(){
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

        ItemSlashBlade.TextureName.set(Tag, "kablade/cutIron/texCutIron");
        ItemSlashBlade.ModelName.set(Tag, "kablade/cutIron/mdlCutIron");

        ItemSlashBlade.setBaseAttackModifier(Tag,8);
        customBlade.getTagCompound().setFloat("baseAttackModifier",8.0F);
        ItemSlashBlade.SpecialAttackType.set(Tag,290);
        //ItemSlashBlade.SpecialAttackType.set(tag, 257);
        customBlade.addEnchantment(Enchantments.SHARPNESS,4);
        customBlade.addEnchantment(Enchantments.POWER,1);
        Item_KaNamed.IsDefaultBewitched.set(Tag, true);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(Tag, 1);
        SlashBlade.registerCustomItemStack(this.name, customBlade);
        BladeLoader.NamedBlades.add(name);
        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prevblade = SlashBlade.findItemStack(bladestr, "wjx.blade.bamboo_battler", 1);
        IRecipe recipe = new RecipeAwakeBlade(new ResourceLocation(bladestr,"cut_iron"),
                blackblade, prevblade,
                "CCC",
                "BAB",
                "B B",
                'A', prevblade,
                'B', "ingotChromium",
                'C',new ItemStack(Items.DIAMOND));

        SlashBlade.addRecipe("cut_iron", recipe);
    }


}

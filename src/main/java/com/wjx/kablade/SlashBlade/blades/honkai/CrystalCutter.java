package com.wjx.kablade.SlashBlade.blades.honkai;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_HonkaiNamed;
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

public class CrystalCutter {
    String name = "wjx.blade.honkai.crystal_cutter";
    String key = "wjx.blade.honkai.crystal_cutter";

    public CrystalCutter(){
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
        Item_HonkaiNamed.CustomMaxDamage.set(tag, 680);

        ItemSlashBlade.TextureName.set(tag, "kablade/Honkai/Rhomphaia/tex/cry");
        ItemSlashBlade.ModelName.set(tag, "kablade/Honkai/Rhomphaia/mdl");

        ItemSlashBlade.SpecialAttackType.set(tag,287);
        ItemSlashBlade.setBaseAttackModifier(tag,2);
        customblade.getTagCompound().setFloat("baseAttackModifier",15.0F);
        customblade.addEnchantment(Enchantments.SMITE,2);
        Item_HonkaiNamed.IsDefaultBewitched.set(tag, true);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        SlashBlade.registerCustomItemStack(this.name, customblade);
        BladeLoader.NamedHonkai.add(name);
        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prevblade = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.fuheliuye", 1);
        IRecipe recipe = new RecipeAwakeBlade(new ResourceLocation(bladestr,"crystal_cutter"),
                blackblade, prevblade,
                "  B",
                " B ",
                "A  ",
                'A', prevblade,
                'B',new ItemStack(Item.getItemFromBlock(Blocks.DIAMOND_BLOCK),1));

        SlashBlade.addRecipe("crystal_cutter", recipe);
    }
}

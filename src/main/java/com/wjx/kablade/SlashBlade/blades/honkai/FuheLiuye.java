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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.wjx.kablade.Main.bladestr;

public class FuheLiuye {
    String name = "wjx.blade.honkai.fuheliuye";
    String key = "wjx.blade.honkai.fuheliuye";

    public FuheLiuye(){
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
        Item_HonkaiNamed.CustomMaxDamage.set(tag, 360);

        //ItemSlashBlade.TextureName.set(tag, "kablade/BambooLumi/tex");
        //ItemSlashBlade.ModelName.set(tag, "kablade/BambooIron/blade");
        ItemSlashBlade.TextureName.set(tag, "kablade/Honkai/Fuhe/tex/Weapon_Katana_M8_71_2");
        ItemSlashBlade.ModelName.set(tag, "kablade/Honkai/Fuhe/mdlfuhe");
        customblade.getTagCompound().setFloat("baseAttackModifier",8.0F);
        ItemSlashBlade.AttackAmplifier.set(tag,1.3f);
        ItemSlashBlade.SpecialAttackType.set(tag, 284);
        customblade.addEnchantment(Enchantments.SHARPNESS,1);
        Item_HonkaiNamed.IsDefaultBewitched.set(tag, true);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        SlashBlade.registerCustomItemStack(this.name, customblade);
        BladeLoader.NamedHonkai.add(name);
        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prevblade = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.muraseshu", 1);
        //ItemStack prevblade2 = SlashBlade.findItemStack(bladestr, "wjx.blade.bamboo_iron", 1);
        IRecipe recipe = new SlashBladeRecipeModding(new ResourceLocation(bladestr,"fuheliuye"),
                blackblade, prevblade,
                new Object[]{
                        "  C",
                        " B ",
                        "A  ",
                        'A', prevblade,
                        'B',Item.getItemFromBlock(Blocks.DIAMOND_BLOCK),
                        'C', new ItemStack(Item.getItemFromBlock(Blocks.LEAVES)),
                });

        SlashBlade.addRecipe("fuheliuye", recipe);
    }
}

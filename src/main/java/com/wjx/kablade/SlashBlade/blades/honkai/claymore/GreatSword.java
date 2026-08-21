package com.wjx.kablade.SlashBlade.blades.honkai.claymore;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_HonkaiGreatswordNamed;
import com.wjx.kablade.SlashBlade.blades.recipe.SlashBladeRecipeModding;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
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

public class GreatSword {
    String name = "wjx.blade.honkai.greatsword";
    String key = "wjx.blade.honkai.greatsword";

    public GreatSword() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    private ItemStack customblade = new ItemStack(BladeLoader.ITEM_HONKAI_GREATSWORD_NAMED, 1, 0);

    private NBTTagCompound tag = new NBTTagCompound();
    {
        customblade.setTagCompound(tag);
    }

    @SubscribeEvent
    public void init(LoadEvent.InitEvent event) {
        Item_HonkaiGreatswordNamed.CurrentItemName.set(tag, name);
        Item_HonkaiGreatswordNamed.CustomMaxDamage.set(tag, 300);

        ItemSlashBlade.TextureName.set(tag, "kablade/Honkai/claymore/greatsword/tex");
        ItemSlashBlade.ModelName.set(tag, "kablade/Honkai/claymore/greatsword/mdl");
        customblade.getTagCompound().setFloat("baseAttackModifier", 8.0F);
        ItemSlashBlade.AttackAmplifier.set(tag, 1.2F);
        Item_HonkaiGreatswordNamed.IsDefaultBewitched.set(tag, false);
        customblade.addEnchantment(Enchantments.UNBREAKING, 1);

        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        SlashBlade.registerCustomItemStack(this.name, customblade);
        BladeLoader.NamedHonkaiGreatsword.add(name);

        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        IRecipe recipe = new SlashBladeRecipeModding(new ResourceLocation(bladestr, "greatsword"),
                blackblade, ItemStack.EMPTY,
                new Object[]{
                        " ZZ",
                        " JZ",
                        "J  ",
                        'J', Items.DIAMOND_SWORD,
                        'Z', new ItemStack(Item.getItemFromBlock(Blocks.DIAMOND_BLOCK)),
                });

        SlashBlade.addRecipe("greatsword", recipe);
    }
}

package com.wjx.kablade.SlashBlade.blades.honkaip2;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_HonkaiNamed;
import com.wjx.kablade.SlashBlade.blades.recipe.SlashBladeRecipeModding;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.named.event.LoadEvent;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
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

public class OneSaltyTuna {
    String name = "wjx.blade.honkai.tuna";
    String key = "wjx.blade.honkai.tuna";

    public OneSaltyTuna(){
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
        Item_HonkaiNamed.CustomMaxDamage.set(tag, 500);

        //ItemSlashBlade.TextureName.set(tag, "kablade/BambooLumi/tex");
        //ItemSlashBlade.ModelName.set(tag, "kablade/BambooIron/blade");
        ItemSlashBlade.TextureName.set(tag, "kablade/honkaip2/OneSaltyTuna/tex");
        ItemSlashBlade.ModelName.set(tag, "kablade/honkaip2/OneSaltyTuna/mdl");


        customblade.getTagCompound().setFloat("baseAttackModifier",12.0F);
        ItemSlashBlade.SpecialAttackType.set(tag,456);
        ItemSlashBlade.AttackAmplifier.set(tag,1.5f);
        customblade.addEnchantment(Enchantments.SMITE,2);
        Item_HonkaiNamed.IsDefaultBewitched.set(tag, true);
        SpecialEffects.addEffect(customblade, BladeProxy.UndyingSaltiness);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        SlashBlade.registerCustomItemStack(this.name, customblade);
        BladeLoader.NamedHonkai.add(name);
        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prevblade = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.murayoto", 1);
        //ItemStack prevblade2 = SlashBlade.findItemStack(bladestr, "wjx.blade.bamboo_iron", 1);
        IRecipe recipe = new SlashBladeRecipeModding(new ResourceLocation(bladestr,"tuna"),
                blackblade, prevblade,
                new Object[]{
                        "B C",
                        " C ",
                        "A B",
                        'A', prevblade,
                        'B',new ItemStack(Items.COOKED_BEEF),
                        'C', new ItemStack(Item.getItemFromBlock(Blocks.GOLD_BLOCK)),
                });

        SlashBlade.addRecipe("tuna", recipe);
    }
}

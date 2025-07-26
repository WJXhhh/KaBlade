package com.wjx.kablade.SlashBlade.blades.honkai;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_HonkaiNamed;
import com.wjx.kablade.SlashBlade.blades.recipe.SlashBladeRecipeModding;
import com.wjx.kablade.SlashBlade.blades.recipe.SlashBladeTwoRecipeModding;
import com.wjx.kablade.init.ItemInit;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.RecipeAwakeBlade;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.named.event.LoadEvent;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.wjx.kablade.Main.bladestr;

public class PledgeOfRain {
    String name = "wjx.blade.honkai.pledge_of_rain";
    String key = "wjx.blade.honkai.pledge_of_rain";

    public ThirdSacredRelic(){
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
        Item_HonkaiNamed.CustomMaxDamage.set(tag, 820);

        ItemSlashBlade.TextureName.set(tag, "kablade/Honkai/3rdSacredRelic/tex3rdSacredRelic");
        ItemSlashBlade.ModelName.set(tag, "kablade/Honkai/3rdSacredRelic/mdl3rdSacredRelic");

        ItemSlashBlade.SpecialAttackType.set(tag,7);
        ItemSlashBlade.setBaseAttackModifier(tag,2);
        customblade.getTagCompound().setFloat("baseAttackModifier",20.0F);
        ItemSlashBlade.AttackAmplifier.set(tag,2.1f);
        customblade.addEnchantment(Enchantments.KNOCKBACK,2);
        customblade.addEnchantment(Enchantments.SHARPNESS,3);
        Item_HonkaiNamed.IsDefaultBewitched.set(tag, true);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        SlashBlade.registerCustomItemStack(this.name, customblade);
        BladeLoader.NamedHonkai.add(name);
        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prevblade = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.third_sacred", 1);
        IRecipe recipe = new SlashBladeRecipeModding(new ResourceLocation(bladestr,"pledge_of_rain"),
                blackblade, prevblade,
                "AAA",
                "CBC",
                "ADA",
                'A',new ItemStack(Items.PAPER),
                'B', prevblade,
                'C', new ItemStack(Items.SHULKER_SHELL),
                'D', new ItemStack(BLOCKS.LAPIS_LAZULI_BLOCK));

        SlashBlade.addRecipe("pledge_of_rain", recipe);
    }
}

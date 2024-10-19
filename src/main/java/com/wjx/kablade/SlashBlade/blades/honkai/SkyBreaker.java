package com.wjx.kablade.SlashBlade.blades.honkai;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.SlashBlade.SpeacialEffects.SEDivinePenalty;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_HonkaiNamed;
import com.wjx.kablade.SlashBlade.blades.recipe.SlashBladeRecipeModding;
import com.wjx.kablade.init.ItemInit;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.RecipeAwakeBlade;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.named.event.LoadEvent;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.wjx.kablade.Main.bladestr;

public class SkyBreaker {
    String name = "wjx.blade.honkai.sky_breaker";
    String key = "wjx.blade.honkai.sky_breaker";

    public SkyBreaker(){
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
        Item_HonkaiNamed.CustomMaxDamage.set(tag, 660);

        ItemSlashBlade.TextureName.set(tag, "kablade/Honkai/SkyBreaker/texSkyBreaker");
        ItemSlashBlade.ModelName.set(tag, "kablade/Honkai/SkyBreaker/mdlSkyBreaker");

        customblade.getTagCompound().setFloat("baseAttackModifier",15.0F);
        ItemSlashBlade.AttackAmplifier.set(tag,1.7f);
        customblade.addEnchantment(Enchantments.SMITE,2);
        customblade.addEnchantment(Enchantments.SHARPNESS,2);
        Item_HonkaiNamed.IsDefaultBewitched.set(tag, true);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        SpecialEffects.addEffect(customblade, BladeProxy.DivinePenalty);
        SlashBlade.registerCustomItemStack(this.name, customblade);
        BladeLoader.NamedHonkai.add(name);
        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prevblade = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.byoden", 1);
        IRecipe recipe = new SlashBladeRecipeModding(new ResourceLocation(bladestr,"sky_breaker"),
                blackblade, prevblade,
                "ADA",
                " B ",
                "ACA",
                'A',new ItemStack(Items.REDSTONE),
                'B', prevblade,
                'C', new ItemStack(ItemInit.CHROMOLY_INGOT),
                'D',new ItemStack(Items.GLOWSTONE_DUST));

        SlashBlade.addRecipe("sky_breaker", recipe);
    }
}

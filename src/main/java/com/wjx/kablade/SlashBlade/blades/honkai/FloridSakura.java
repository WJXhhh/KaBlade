package com.wjx.kablade.SlashBlade.blades.honkai;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_HonkaiNamed;
import com.wjx.kablade.SlashBlade.blades.recipe.SlashBladeThreeRecipeModding;
import com.wjx.kablade.config.ModConfig;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.RecipeAwakeBlade;
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

public class FloridSakura {
    String name = "wjx.blade.honkai.florid_sakura";
    String key = "wjx.blade.honkai.florid_sakura";

    public FloridSakura(){
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
        Item_HonkaiNamed.CustomMaxDamage.set(tag, 800);

        ItemSlashBlade.TextureName.set(tag, "kablade/Honkai/FloridSakura/texFloridSakura");
        ItemSlashBlade.ModelName.set(tag, "kablade/Honkai/FloridSakura/mdlFloridSakura");

        if (ModConfig.GeneralConf.Enable_New_SA_Id){
            ItemSlashBlade.SpecialAttackType.set(tag, 455);
        }
        else {
            ItemSlashBlade.SpecialAttackType.set(tag,305);
        }
        customblade.getTagCompound().setFloat("baseAttackModifier",24.0F);
        ItemSlashBlade.AttackAmplifier.set(tag,1.7f);
        customblade.addEnchantment(Enchantments.FIRE_ASPECT,4);
        customblade.addEnchantment(Enchantments.SHARPNESS,5);
        customblade.addEnchantment(Enchantments.POWER,3);
        customblade.addEnchantment(Enchantments.SMITE,4);
        ItemSlashBladeNamed.SummonedSwordColor.set(tag,0xf3002c);
        Item_HonkaiNamed.IsDefaultBewitched.set(tag, true);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        SpecialEffects.addEffect(customblade, BladeProxy.Phoenix);
        SlashBlade.registerCustomItemStack(this.name, customblade);
        BladeLoader.NamedHonkai.add(name);
        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prevblade = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.phoenix", 1);
        ItemStack prevblade2 = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.third_sacred", 1);
        ItemStack prevblade3 = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.sakura_blossom", 1);
        IRecipe recipe = new SlashBladeThreeRecipeModding(new ResourceLocation(bladestr,"florid_sakura"),
                blackblade, prevblade,prevblade2,prevblade3,
                new Object[]{
                        "D C",
                        " B ",
                        "A D",
                        'D', Item.getItemFromBlock(Blocks.REDSTONE_BLOCK),
                        'A', prevblade,
                        'B', prevblade2,
                        'C', prevblade3,
                });

        SlashBlade.addRecipe("florid_sakura", recipe);
    }
}

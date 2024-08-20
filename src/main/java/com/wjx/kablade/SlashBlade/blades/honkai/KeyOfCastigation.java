package com.wjx.kablade.SlashBlade.blades.honkai;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_HonkaiNamed;
import com.wjx.kablade.SlashBlade.blades.recipe.SlashBladeTwoRecipeModding;
import com.wjx.kablade.init.ItemInit;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.named.event.LoadEvent;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.wjx.kablade.Main.bladestr;

public class KeyOfCastigation {
    String name = "wjx.blade.honkai.key_of_cas";
    String key = "wjx.blade.honkai.key_of_cas";

    public KeyOfCastigation(){
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
        Item_HonkaiNamed.CustomMaxDamage.set(tag, 1100);

        ItemSlashBlade.TextureName.set(tag, "kablade/Honkai/KeyOfCastigation/texKeyOfCastigation");
        ItemSlashBlade.ModelName.set(tag, "kablade/Honkai/KeyOfCastigation/mdlKeyOfCastigation");
        customblade.getTagCompound().setFloat("baseAttackModifier",25.0F);
        ItemSlashBlade.AttackAmplifier.set(tag,2.5f);
        customblade.addEnchantment(Enchantments.SHARPNESS,4);
        ItemSlashBlade.SpecialAttackType.set(tag,304);
        ItemSlashBlade.SummonedSwordColor.set(tag,10158335);
        customblade.addEnchantment(Enchantments.POWER,3);
        customblade.addEnchantment(Enchantments.SMITE,4);
        customblade.addEnchantment(Enchantments.SHARPNESS,4);
        Item_HonkaiNamed.IsDefaultBewitched.set(tag, true);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        SpecialEffects.addEffect(customblade, BladeProxy.ThunderBlitz);
        SlashBlade.registerCustomItemStack(this.name, customblade);
        BladeLoader.NamedHonkai.add(name);
        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prevblade = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.mag_storm", 1);
        ItemStack prevblade2 = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.nue", 1);
        IRecipe recipe = new SlashBladeTwoRecipeModding(new ResourceLocation(bladestr,"key_of_cas"),
                blackblade, prevblade,prevblade2,
                "C C",
                "CBC",
                " A ",
                'A', prevblade,
                'B',prevblade2,
                'C', new ItemStack(ItemInit.ELECTRO_SIGNET));
        SlashBlade.addRecipe("key_of_cas", recipe);
    }
}

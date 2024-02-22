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
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.wjx.kablade.Main.bladestr;

public class IceEpiphyllum {
    String name = "wjx.blade.honkai.ice_epiphyllum";
    String key = "wjx.blade.honkai.ice_epiphyllum";

    public IceEpiphyllum(){
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
        Item_HonkaiNamed.CustomMaxDamage.set(tag, 1000);

        ItemSlashBlade.TextureName.set(tag, "kablade/Honkai/IceEpiphyllum/texIceEpiphyllum");
        ItemSlashBlade.ModelName.set(tag, "kablade/Honkai/IceEpiphyllum/mdlIceEpiphyllum");
        customblade.getTagCompound().setFloat("baseAttackModifier",22.0F);
        ItemSlashBlade.AttackAmplifier.set(tag,2.5f);
        customblade.addEnchantment(Enchantments.SHARPNESS,3);
        ItemSlashBlade.SpecialAttackType.set(tag,299);
        ItemSlashBlade.SummonedSwordColor.set(tag,65535);
        customblade.addEnchantment(Enchantments.POWER,3);
        customblade.addEnchantment(Enchantments.SMITE,3);
        customblade.addEnchantment(Enchantments.SHARPNESS,4);
        Item_HonkaiNamed.IsDefaultBewitched.set(tag, true);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        SpecialEffects.addEffect(customblade, BladeProxy.GlacialBane);
        SlashBlade.registerCustomItemStack(this.name, customblade);
        BladeLoader.NamedHonkai.add(name);

        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prevblade = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.xuanyuan_katana", 1);
        ItemStack prevblade2 = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.third_sacred", 1);
        IRecipe recipe = new SlashBladeTwoRecipeModding(new ResourceLocation(bladestr,"ice_epiphyllum"),
                blackblade, prevblade,prevblade2,
                "CCC",
                "DBD",
                " A ",
                'A', prevblade,
                'B',prevblade2,
                'C', new ItemStack(ItemInit.IRON_COIL),
                'D',new ItemStack(Blocks.LAPIS_BLOCK));
        SlashBlade.addRecipe("ice_epiphyllum", recipe);
    }
}

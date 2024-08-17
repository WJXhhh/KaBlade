package com.wjx.kablade.SlashBlade.blades.honkai;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_HonkaiNamed;
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

public class Nue {
    String name = "wjx.blade.honkai.nue";
    String key = "wjx.blade.honkai.nue";

    public Nue(){
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
        Item_HonkaiNamed.CustomMaxDamage.set(tag, 750);

        ItemSlashBlade.TextureName.set(tag, "kablade/Honkai/Nue/texNue");
        ItemSlashBlade.ModelName.set(tag, "kablade/Honkai/Nue/mdlNue");

        customblade.getTagCompound().setFloat("baseAttackModifier",21.0F);
        ItemSlashBlade.AttackAmplifier.set(tag,2.1f);
        customblade.addEnchantment(Enchantments.KNOCKBACK,2);
        customblade.addEnchantment(Enchantments.SHARPNESS,5);
        Item_HonkaiNamed.IsDefaultBewitched.set(tag, true);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        ItemSlashBlade.SpecialAttackType.set(tag,302);
        SlashBlade.registerCustomItemStack(this.name, customblade);
        BladeLoader.NamedHonkai.add(name);
        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prevblade = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.dawn_breaker", 1);
        IRecipe recipe = new RecipeAwakeBlade(new ResourceLocation(bladestr,"nue"),
                blackblade, prevblade,
                " CC",
                " BC",
                "A  ",
                'A', prevblade,
                'B',new ItemStack(Items.FEATHER),
                'C', new ItemStack(ItemInit.MOLYBDENUM_SWORD));

        SlashBlade.addRecipe("nue", recipe);
    }
}

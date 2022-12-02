package com.wjx.kablade.SlashBlade.blades;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_KaNamed;
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

public class ArcLight {
    String name = "wjx.blade.arc_light";
    String key = "wjx.blade.arc_light";

    public ArcLight(){
        MinecraftForge.EVENT_BUS.register(this);
    }

    private ItemStack customBlade = new ItemStack(BladeLoader.ITEM_KABLADE_NAMED,1,0);

    private NBTTagCompound Tag = new NBTTagCompound();
    {
        customBlade.setTagCompound(Tag);
    }

    @SubscribeEvent
    public void init(LoadEvent.InitEvent event) {
        Item_KaNamed.CurrentItemName.set(Tag, name);
        Item_KaNamed.CustomMaxDamage.set(Tag, 800);

        ItemSlashBlade.TextureName.set(Tag, "kablade/arcLight/texArcLight");
        ItemSlashBlade.ModelName.set(Tag, "kablade/arcLight/mdlArcLight");

        ItemSlashBlade.setBaseAttackModifier(Tag,8);
        customBlade.getTagCompound().setFloat("baseAttackModifier",8.0F);
        ItemSlashBlade.SpecialAttackType.set(Tag,289);
        //ItemSlashBlade.SpecialAttackType.set(tag, 257);
        customBlade.addEnchantment(Enchantments.SMITE,3);
        customBlade.addEnchantment(Enchantments.POWER,1);
        Item_KaNamed.IsDefaultBewitched.set(Tag, true);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(Tag, 1);
        ItemSlashBlade.SummonedSwordColor.set(Tag, 0xfff8ca);
        SlashBlade.registerCustomItemStack(this.name, customBlade);
        BladeLoader.NamedBlades.add(name);
        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prevblade = SlashBlade.findItemStack(bladestr, "wjx.blade.bamboo_lumi", 1);
        IRecipe recipe = new RecipeAwakeBlade(new ResourceLocation(bladestr,"arc_light"),
                blackblade, prevblade,
                "BCB",
                " A ",
                "B B",
                'A', prevblade,
                'B', new ItemStack(Items.GLOWSTONE_DUST),
                'C',new ItemStack(Item.getItemFromBlock(Blocks.IRON_BLOCK)));

        SlashBlade.addRecipe("arc_light", recipe);
    }


}

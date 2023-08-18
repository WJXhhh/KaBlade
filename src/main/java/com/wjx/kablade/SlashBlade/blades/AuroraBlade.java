package com.wjx.kablade.SlashBlade.blades;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_KaNamed;
import com.wjx.kablade.init.ItemInit;
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

public class AuroraBlade {
    String name = "wjx.blade.aurora_blade";
    String key = "wjx.blade.aurora_blade";

    public AuroraBlade(){
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
        Item_KaNamed.CustomMaxDamage.set(Tag, 500);

        ItemSlashBlade.TextureName.set(Tag, "kablade/AuroraBlade/texAuroraBlade2");
        ItemSlashBlade.ModelName.set(Tag, "kablade/AuroraBlade/mdlAuroraBlade");

        customBlade.getTagCompound().setFloat("baseAttackModifier",11.0F);
        customBlade.getTagCompound().setBoolean("isAurora",true);
        ItemSlashBlade.SpecialAttackType.set(Tag,294);
        customBlade.addEnchantment(Enchantments.SMITE,4);
        customBlade.addEnchantment(Enchantments.POWER,2);
        Item_KaNamed.IsDefaultBewitched.set(Tag, true);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(Tag, 1);
        ItemSlashBlade.SummonedSwordColor.set(Tag, 65476);
        SlashBlade.registerCustomItemStack(this.name, customBlade);
        BladeLoader.NamedBlades.add(name);
        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prevblade = SlashBlade.findItemStack(bladestr, "wjx.blade.arc_light", 1);
        IRecipe recipe = new RecipeAwakeBlade(new ResourceLocation(bladestr,"aurora_blade"),
                blackblade, prevblade,
                "BCB",
                " A ",
                "   ",
                'A', prevblade,
                'B', new ItemStack(Items.DIAMOND),
                'C',new ItemStack(ItemInit.AURORA_METAL_SWORD));

        SlashBlade.addRecipe("aurora_blade", recipe);
    }




}

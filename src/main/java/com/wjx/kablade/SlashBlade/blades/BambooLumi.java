package com.wjx.kablade.SlashBlade.blades;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_KaNamed;
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

public class BambooLumi {
    String name = "wjx.blade.bamboo_lumi";
    String key = "wjx.blade.bamboo_lumi";

    public BambooLumi(){
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
        Item_KaNamed.CustomMaxDamage.set(Tag, 400);

        ItemSlashBlade.TextureName.set(Tag, "kablade/BambooLumi/tex");
        ItemSlashBlade.ModelName.set(Tag, "kablade/BambooIron/blade");

        ItemSlashBlade.setBaseAttackModifier(Tag,6);
        customBlade.getTagCompound().setFloat("baseAttackModifier",7.0F);
        //ItemSlashBlade.SpecialAttackType.set(tag, 257);
        customBlade.addEnchantment(Enchantments.SMITE,2);
        Item_KaNamed.IsDefaultBewitched.set(Tag, false);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(Tag, 1);

        SlashBlade.registerCustomItemStack(this.name, customBlade);
        BladeLoader.NamedBlades.add(name);
        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prevblade = SlashBlade.findItemStack(bladestr, "wjx.blade.bamboo_iron", 1);
        IRecipe recipe = new RecipeAwakeBlade(new ResourceLocation(bladestr,"bamboo_lumi"),
                blackblade, prevblade,
                new Object[]{
                        "  B",
                        " B ",
                        "A  ",
                        'A', prevblade,
                        'B', new ItemStack(Items.GLOWSTONE_DUST),
                });

        SlashBlade.addRecipe("bamboo_lumi", recipe);
    }


}

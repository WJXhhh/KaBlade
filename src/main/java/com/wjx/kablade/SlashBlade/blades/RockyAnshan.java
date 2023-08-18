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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.wjx.kablade.Main.bladestr;

public class RockyAnshan {
    String name = "wjx.blade.rocky_anshan";
    String key = "wjx.blade.rocky_anshan";

    public RockyAnshan(){
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
        Item_KaNamed.CustomMaxDamage.set(Tag, 150);

        ItemSlashBlade.TextureName.set(Tag, "kablade/Rocky/Anshan/tex");
        ItemSlashBlade.ModelName.set(Tag, "kablade/HangTu/mdl");

        ItemSlashBlade.setBaseAttackModifier(Tag,5);
        customBlade.getTagCompound().setFloat("baseAttackModifier",5F);
        //ItemSlashBlade.SpecialAttackType.set(tag, 257);
        customBlade.addEnchantment(Enchantments.UNBREAKING,2);
        Item_KaNamed.IsDefaultBewitched.set(Tag, false);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(Tag, 1);

        SlashBlade.registerCustomItemStack(this.name, customBlade);
        BladeLoader.NamedBlades.add(name);
        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prevblade = SlashBlade.findItemStack(bladestr, "wjx.blade.hangtu", 1);
        IRecipe recipe = new RecipeAwakeBlade(new ResourceLocation(bladestr,"rocky_anshan"),
                blackblade, prevblade,
                new Object[]{
                        "  B",
                        " B ",
                        "A  ",
                        'A', prevblade,
                        'B', new ItemStack(Item.getItemFromBlock(Blocks.STONE),1,5),
                });

        SlashBlade.addRecipe("rocky_anshan", recipe);
    }


}

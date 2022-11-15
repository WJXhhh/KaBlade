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

public class RockyHuagang {
    String name = "wjx.blade.rocky_huagang";
    String key = "wjx.blade.rocky_huagang";

    public RockyHuagang(){
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
        Item_KaNamed.CustomMaxDamage.set(Tag, 280);

        ItemSlashBlade.TextureName.set(Tag, "kablade/Rocky/Huagang/tex");
        ItemSlashBlade.ModelName.set(Tag, "kablade/HangTu/mdl");

        ItemSlashBlade.setBaseAttackModifier(Tag,6);
        ItemSlashBlade.AttackAmplifier.set(Tag,2.0f);
        customBlade.getTagCompound().setFloat("baseAttackModifier",5F);
        //ItemSlashBlade.SpecialAttackType.set(tag, 257);
        customBlade.addEnchantment(Enchantments.SHARPNESS,2);
        Item_KaNamed.IsDefaultBewitched.set(Tag, false);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(Tag, 1);

        SlashBlade.registerCustomItemStack(this.name, customBlade);
        BladeLoader.NamedBlades.add(name);
        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prevblade = SlashBlade.findItemStack(bladestr, "wjx.blade.hangtu", 1);
        IRecipe recipe = new RecipeAwakeBlade(new ResourceLocation(bladestr,"rocky_huagang"),
                blackblade, prevblade,
                new Object[]{
                        "  B",
                        " B ",
                        "A  ",
                        'A', prevblade,
                        'B', new ItemStack(Item.getItemFromBlock(Blocks.STONE),1,1),
                });

        SlashBlade.addRecipe("rocky_huagang", recipe);
    }


}

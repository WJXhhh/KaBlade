package com.wjx.kablade.SlashBlade.blades;

import com.wjx.kablade.Main;
import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.RecipeBlade;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_KaNamed;
import com.wjx.kablade.init.BlockInit;
import com.wjx.kablade.init.ItemInit;
import com.wjx.kablade.util.Reference;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.RecipeAwakeBlade;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.named.event.LoadEvent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.wjx.kablade.Main.bladestr;
import static mods.flammpfeil.slashblade.SlashBlade.bladeNamed;
import static mods.flammpfeil.slashblade.SlashBlade.bladeWood;

public class hangtublade {
    String name = "wjx.blade.hangtu";
    String key = "wjx.blade.hangtu";

    public hangtublade(){
        MinecraftForge.EVENT_BUS.register(this);
    }

    private ItemStack customblade = new ItemStack(BladeLoader.ITEM_KABLADE_NAMED,1,0);

    private NBTTagCompound tag = new NBTTagCompound();
    {
        customblade.setTagCompound(tag);
    }
    @SubscribeEvent
    public void init(LoadEvent.InitEvent event) {

        Item_KaNamed.CurrentItemName.set(tag, name);
        Item_KaNamed.CustomMaxDamage.set(tag, 60);

        ItemSlashBlade.TextureName.set(tag, "kablade/HangTu/tex");
        ItemSlashBlade.ModelName.set(tag, "kablade/HangTu/mdl");

        ItemSlashBlade.setBaseAttackModifier(tag,2);
        ItemSlashBlade.AttackAmplifier.set(tag,1.1f);

        customblade.getTagCompound().setFloat("baseAttackModifier",2.0F);
        Item_KaNamed.IsDefaultBewitched.set(tag, true);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        SlashBlade.registerCustomItemStack(this.name, customblade);
        BladeLoader.NamedBlades.add(name);
        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        IRecipe recipe = new RecipeAwakeBlade(new ResourceLocation(bladestr,"hangtu"),
                blackblade, ItemStack.EMPTY,
                new Object[]{
                        "  B",
                        " B ",
                        "A  ",
                        'A', ItemInit.RIMMED_EARTH_STICK,
                        'B', Item.getItemFromBlock(BlockInit.RIMMED_EARTH),
                });

        SlashBlade.addRecipe("hangtu", recipe);
    }
}

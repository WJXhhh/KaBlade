package com.wjx.kablade.SlashBlade.blades;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_KaNamed;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.RecipeAwakeBlade;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.named.event.LoadEvent;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.wjx.kablade.Main.bladestr;

public class BlackSteel {
    String name = "wjx.blade.black_steel";
    String key = "wjx.blade.black_steel";

    public BlackSteel(){
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
        Item_KaNamed.CustomMaxDamage.set(Tag, 200);

        ItemSlashBlade.TextureName.set(Tag, "kablade/BlackSteel/blacksteeltexture");
        ItemSlashBlade.ModelName.set(Tag, "kablade/BlackSteel/blacksteel");

        ItemSlashBlade.setBaseAttackModifier(Tag,6);
        customBlade.getTagCompound().setFloat("baseAttackModifier",6.0F);
        ItemSlashBlade.AttackAmplifier.set(Tag,1.2f);
        Item_KaNamed.IsDefaultBewitched.set(Tag, true);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(Tag, 1);
        SlashBlade.registerCustomItemStack(this.name, customBlade);
        BladeLoader.NamedBlades.add(name);
        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prev_blade = SlashBlade.findItemStack(bladestr,"wjx.blade.hangtu",1);
        IRecipe recipe = new RecipeAwakeBlade(new ResourceLocation(bladestr,"black_steel"),
                blackblade, prev_blade,
                new Object[]{
                        "BCB",
                        "CAC",
                        "BCB",
                        'A', prev_blade,
                        'B', new ItemStack(Items.COAL),
                        'C',new ItemStack(Items.IRON_INGOT),
                });

        SlashBlade.addRecipe("black_steel", recipe);
    }


}

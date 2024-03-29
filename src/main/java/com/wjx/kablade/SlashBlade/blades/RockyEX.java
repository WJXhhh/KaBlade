package com.wjx.kablade.SlashBlade.blades;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_KaNamed;
import com.wjx.kablade.SlashBlade.blades.recipe.SlashBladeThreeRecipeModding;
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
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import static com.wjx.kablade.Main.bladestr;

public class RockyEX {
    String name = "wjx.blade.rocky_ex";
    String key = "wjx.blade.rocky_ex";

    public RockyEX(){
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

        ItemSlashBlade.TextureName.set(Tag, "kablade/RockEx/tex");
        ItemSlashBlade.ModelName.set(Tag, "kablade/RockEx/mdl");

        ItemSlashBlade.setBaseAttackModifier(Tag,6);
        ItemSlashBlade.SpecialAttackType.set(Tag,288);
        customBlade.getTagCompound().setFloat("baseAttackModifier",6.0F);
        ItemSlashBlade.AttackAmplifier.set(Tag,1.5f);
        //ItemSlashBlade.SpecialAttackType.set(tag, 257);
        customBlade.addEnchantment(Enchantments.UNBREAKING,2);
        customBlade.addEnchantment(Enchantments.SHARPNESS,2);
        Item_KaNamed.IsDefaultBewitched.set(Tag, true);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(Tag, 1);

        SlashBlade.registerCustomItemStack(this.name, customBlade);
        BladeLoader.NamedBlades.add(name);
        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prevblade = SlashBlade.findItemStack(bladestr, "wjx.blade.rocky_anshan", 1);
        ItemStack prevblade2 = SlashBlade.findItemStack(bladestr, "wjx.blade.rocky_huagang", 1);
        ItemStack prevblade3 = SlashBlade.findItemStack(bladestr, "wjx.blade.rocky_shanchang", 1);
        IRecipe recipe = new SlashBladeThreeRecipeModding(new ResourceLocation(bladestr,"rocky_ex"),
                blackblade, prevblade,prevblade2,prevblade3,
                new Object[]{
                        "  C",
                        " B ",
                        "AD ",
                        'A',Item.getItemFromBlock(Blocks.IRON_BLOCK),
                        'B', prevblade,
                        'C', prevblade2,
                        'D', prevblade3,
                });

        SlashBlade.addRecipe("rocky_ex", recipe);
    }


}

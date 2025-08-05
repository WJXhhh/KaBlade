package com.wjx.kablade.SlashBlade.blades.honkaip2;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_HonkaiNamed;
import com.wjx.kablade.SlashBlade.blades.recipe.SlashBladeRecipeModding;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.named.event.LoadEvent;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.wjx.kablade.Main.bladestr;


public class VibroCutter {
    String name = "wjx.blade.honkai.vibro_cutter";
    String key = "wjx.blade.honkai.vibro_cutter";

    public VibroCutter(){
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
        Item_HonkaiNamed.CustomMaxDamage.set(tag, 550);

        ItemSlashBlade.TextureName.set(tag, "kablade/honkaip2/VibroCutter/tex");
        ItemSlashBlade.ModelName.set(tag, "kablade/honkaip2/VibroCutter/mdl");

        ItemSlashBlade.SpecialAttackType.set(tag,458);
        //ItemSlashBlade.setBaseAttackModifier(tag,2);
        customblade.getTagCompound().setFloat("baseAttackModifier",14.0F);
        ItemSlashBlade.AttackAmplifier.set(tag,2.1f);
        customblade.addEnchantment(Enchantments.SMITE,2);
        customblade.addEnchantment(Enchantments.SHARPNESS,2);
        Item_HonkaiNamed.IsDefaultBewitched.set(tag, true);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        SlashBlade.registerCustomItemStack(this.name, customblade);
        BladeLoader.NamedHonkai.add(name);
        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prevblade = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.wjx.blade.honkai.byorai", 1);
        IRecipe recipe = new SlashBladeRecipeModding(new ResourceLocation(bladestr,"vibro_cutter"),
                blackblade, prevblade,
                "AAA",
                " B ",
                "ADA",
                'A',new ItemStack(Items.PRISMARINE_CRYSTALS),
                'B', prevblade,
                'D', new ItemStack(Blocks.LAPIS_BLOCK));

        SlashBlade.addRecipe("vibro_cutter", recipe);
    }
}

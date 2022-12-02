package com.wjx.kablade.SlashBlade.blades.honkai;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_HonkaiNamed;
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

public class PulseKatanaType19 {
    String name = "wjx.blade.honkai.pulse_katana_t19";
    String key = "wjx.blade.honkai.pulse_katana_t19";

    public PulseKatanaType19(){
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
        Item_HonkaiNamed.CustomMaxDamage.set(tag, 650);

        ItemSlashBlade.TextureName.set(tag, "kablade/Honkai/PulseKatanas/tex/t19");
        ItemSlashBlade.ModelName.set(tag, "kablade/Honkai/PulseKatanas/mdl");

        ItemSlashBlade.setBaseAttackModifier(tag,2);
        customblade.getTagCompound().setFloat("baseAttackModifier",14.0F);
        customblade.addEnchantment(Enchantments.KNOCKBACK,1);
        Item_HonkaiNamed.IsDefaultBewitched.set(tag, true);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        SlashBlade.registerCustomItemStack(this.name, customblade);
        BladeLoader.NamedHonkai.add(name);
        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prevblade = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.murahori", 1);
        IRecipe recipe = new RecipeAwakeBlade(new ResourceLocation(bladestr,"pulse_katana_t19"),
                blackblade, prevblade,
                "  C",
                " B ",
                "A  ",
                'A', prevblade,
                'B',new ItemStack(Items.REDSTONE,1),
                'C', new ItemStack(Item.getItemFromBlock(Blocks.PISTON)));

        SlashBlade.addRecipe("pulse_katana_t19", recipe);
    }
}

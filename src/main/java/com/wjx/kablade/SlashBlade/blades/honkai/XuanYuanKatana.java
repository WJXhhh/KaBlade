package com.wjx.kablade.SlashBlade.blades.honkai;

import com.wjx.kablade.Main;
import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_HonkaiNamed;
import com.wjx.kablade.enchantments.EnchantmentSlow;
import com.wjx.kablade.init.EnchantmentInit;
import com.wjx.kablade.init.ItemInit;
import com.wjx.kablade.util.Reference;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.RecipeAwakeBlade;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.named.event.LoadEvent;
import net.minecraft.enchantment.Enchantment;
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

public class XuanYuanKatana {
    String name = "wjx.blade.honkai.xuanyuan_katana";
    String key = "wjx.blade.honkai.xuanyuan_katana";

    public XuanYuanKatana(){
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
        Item_HonkaiNamed.CustomMaxDamage.set(tag, 600);

        ItemSlashBlade.TextureName.set(tag, "kablade/Honkai/XuanYuanKatana/texHonkaiXuanyuanKatana");
        ItemSlashBlade.ModelName.set(tag, "kablade/Honkai/XuanYuanKatana/mdlHonkaiXuanyuanKatana");

        ItemSlashBlade.SpecialAttackType.set(tag,285);
        ItemSlashBlade.setBaseAttackModifier(tag,2);
        customblade.getTagCompound().setFloat("baseAttackModifier",14.0F);
        customblade.addEnchantment(Enchantments.KNOCKBACK,2);
        Item_HonkaiNamed.IsDefaultBewitched.set(tag, true);
        ItemSlashBlade.SummonedSwordColor.set(tag,65518);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        SlashBlade.registerCustomItemStack(this.name, customblade);
        BladeLoader.NamedHonkai.add(name);
        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prevblade = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.pulse_katana_t17", 1);
        IRecipe recipe = new RecipeAwakeBlade(new ResourceLocation(bladestr,"xuanyuan_katana"),
                blackblade, prevblade,
                "  C",
                " B ",
                "A  ",
                'A', prevblade,
                'B',new ItemStack(ItemInit.CHROMIUM_INGOT,1),
                'C', new ItemStack(Item.getItemFromBlock(Blocks.SNOW)));

        SlashBlade.addRecipe("xuanyuan_katana", recipe);
    }
}

package com.wjx.kablade.SlashBlade.blades.honkai;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_HonkaiNamed;
import com.wjx.kablade.SlashBlade.blades.recipe.SlashBladeNamedRecipe;
import com.wjx.kablade.init.ItemInit;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
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

/**
 * 地藏御魂。数值、双前置刀配方与默认妖化属性均对应 1.20 版本。
 */
public class JizoMitama {
    public static final String NAME = "wjx.blade.honkai.jizo_mitama";

    private final ItemStack blade = new ItemStack(BladeLoader.ITEM_HONKAI_NAMED);
    private final NBTTagCompound tag = new NBTTagCompound();

    public JizoMitama() {
        blade.setTagCompound(tag);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void init(LoadEvent.InitEvent event) {
        Item_HonkaiNamed.CurrentItemName.set(tag, NAME);
        Item_HonkaiNamed.CustomMaxDamage.set(tag, 750);
        ItemSlashBlade.TextureName.set(tag, "kablade/Honkai/JizoMitama/tex");
        ItemSlashBlade.ModelName.set(tag, "kablade/Honkai/JizoMitama/mdl");
        ItemSlashBlade.BaseAttackModifier.set(tag, 31.0F);
        ItemSlashBlade.SpecialAttackType.set(tag, 468);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        Item_HonkaiNamed.IsDefaultBewitched.set(tag, true);

        blade.addEnchantment(Enchantments.UNBREAKING, 6);
        blade.addEnchantment(Enchantments.KNOCKBACK, 2);
        blade.addEnchantment(Enchantments.SHARPNESS, 7);
        blade.addEnchantment(Enchantments.SMITE, 3);

        ItemSlashBladeNamed.NamedBlades.add(NAME);
        SlashBlade.registerCustomItemStack(NAME, blade);
        BladeLoader.NamedHonkai.add(NAME);

        ItemStack result = SlashBlade.findItemStack(bladestr, NAME, 1);
        ItemStack vorpal = SlashBlade.findItemStack(
                bladestr, "wjx.blade.honkai.vorpal_sword", 1);
        ItemStack nue = SlashBlade.findItemStack(
                bladestr, "wjx.blade.honkai.nue", 1);
        IRecipe recipe = new SlashBladeNamedRecipe(
                new ResourceLocation(bladestr, "jizo_mitama"), result, nue,
                "ABA", "CDC", "EFE",
                'A', new ItemStack(Items.SKULL, 1, 1),
                'B', vorpal,
                'C', new ItemStack(ItemInit.SUPERCONDUCTING_METAL),
                'D', nue,
                'E', new ItemStack(Items.BONE),
                'F', new ItemStack(Items.END_CRYSTAL));
        SlashBlade.addRecipe("jizo_mitama", recipe);
    }
}

package com.wjx.kablade.SlashBlade.blades.honkaip2;

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

/** 涤罪七雷·鸣雷见。 */
public class SevenThundersNarukami {
    public static final String NAME = "wjx.blade.honkai.str_narukami";
    private final ItemStack blade = new ItemStack(BladeLoader.ITEM_HONKAI_NAMED);
    private final NBTTagCompound tag = new NBTTagCompound();

    public SevenThundersNarukami() { blade.setTagCompound(tag); MinecraftForge.EVENT_BUS.register(this); }

    @SubscribeEvent
    public void init(LoadEvent.InitEvent event) {
        Item_HonkaiNamed.CurrentItemName.set(tag, NAME);
        Item_HonkaiNamed.CustomMaxDamage.set(tag, 1600);
        ItemSlashBlade.TextureName.set(tag, "kablade/Honkai/SevenThundersNarukami/tex");
        ItemSlashBlade.ModelName.set(tag, "kablade/Honkai/SevenThundersNarukami/mdl");
        tag.setFloat("baseAttackModifier", 64.0F);
        ItemSlashBlade.SpecialAttackType.set(tag, 465);
        ItemSlashBlade.SummonedSwordColor.set(tag, 0xC15CFF);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        Item_HonkaiNamed.IsDefaultBewitched.set(tag, true);
        blade.addEnchantment(Enchantments.UNBREAKING, 10);
        blade.addEnchantment(Enchantments.POWER, 10);
        blade.addEnchantment(Enchantments.SMITE, 10);
        blade.addEnchantment(Enchantments.SHARPNESS, 10);
        ItemSlashBladeNamed.NamedBlades.add(NAME);
        SlashBlade.registerCustomItemStack(NAME, blade);
        BladeLoader.NamedHonkai.add(NAME);

        ItemStack result = SlashBlade.findItemStack(bladestr, NAME, 1);
        ItemStack previous = SlashBlade.findItemStack(bladestr, SevenThundersRumble.NAME, 1);
        IRecipe recipe = new SlashBladeNamedRecipe(
                new ResourceLocation(bladestr, "str_narukami"), result, previous,
                "ABA", "BCB", "ABA",
                'A', new ItemStack(ItemInit.SUPERCONDUCTING_METAL),
                'B', new ItemStack(Items.NETHER_STAR), 'C', previous);
        SlashBlade.addRecipe("str_narukami", recipe);
    }
}

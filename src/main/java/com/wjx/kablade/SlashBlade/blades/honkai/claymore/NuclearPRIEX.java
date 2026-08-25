package com.wjx.kablade.SlashBlade.blades.honkai.claymore;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_HonkaiGreatswordNamed;
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

/** 融核动力剑·改。 */
public class NuclearPRIEX {
    private final String name = "wjx.blade.honkai.nuclear_pri_ex";
    private final ItemStack customblade = new ItemStack(BladeLoader.ITEM_HONKAI_GREATSWORD_NAMED, 1, 0);
    private final NBTTagCompound tag = new NBTTagCompound();

    public NuclearPRIEX() {
        customblade.setTagCompound(tag);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void init(LoadEvent.InitEvent event) {
        Item_HonkaiGreatswordNamed.CurrentItemName.set(tag, name);
        Item_HonkaiGreatswordNamed.CustomMaxDamage.set(tag, 450);
        ItemSlashBlade.TextureName.set(tag, "kablade/Honkai/claymore/nuclear_pri/tex2");
        ItemSlashBlade.ModelName.set(tag, "kablade/Honkai/claymore/nuclear_pri/mdl");
        ItemSlashBlade.BaseAttackModifier.set(tag, 11.0F);
        ItemSlashBlade.AttackAmplifier.set(tag, 1.2F);
        ItemSlashBlade.SpecialAttackType.set(tag, 469);
        Item_HonkaiGreatswordNamed.IsDefaultBewitched.set(tag, true);
        customblade.addEnchantment(Enchantments.UNBREAKING, 2);
        customblade.addEnchantment(Enchantments.SHARPNESS, 1);
        customblade.addEnchantment(Enchantments.KNOCKBACK, 1);

        ItemSlashBladeNamed.NamedBlades.add(name);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        SlashBlade.registerCustomItemStack(name, customblade);
        BladeLoader.NamedHonkaiGreatsword.add(name);

        ItemStack result = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack previous = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.nuclear_pri", 1);
        IRecipe recipe = new SlashBladeRecipeModding(new ResourceLocation(bladestr, "nuclear_pri_ex"),
                result, previous, new Object[]{
                " DR", " BD", "R  ",
                'B', previous,
                'D', Items.DIAMOND,
                'R', new ItemStack(Blocks.REDSTONE_BLOCK)
        });
        SlashBlade.addRecipe("nuclear_pri_ex", recipe);
    }
}

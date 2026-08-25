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

/** 超重剑·冲锋。 */
public class Vanguard {
    private final String name = "wjx.blade.honkai.vanguard";
    private final ItemStack customblade = new ItemStack(BladeLoader.ITEM_HONKAI_GREATSWORD_NAMED, 1, 0);
    private final NBTTagCompound tag = new NBTTagCompound();

    public Vanguard() {
        customblade.setTagCompound(tag);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void init(LoadEvent.InitEvent event) {
        Item_HonkaiGreatswordNamed.CurrentItemName.set(tag, name);
        Item_HonkaiGreatswordNamed.CustomMaxDamage.set(tag, 550);
        ItemSlashBlade.TextureName.set(tag, "kablade/Honkai/claymore/super_heavy_sword/tex_2");
        ItemSlashBlade.ModelName.set(tag, "kablade/Honkai/claymore/super_heavy_sword/mdl");
        ItemSlashBlade.BaseAttackModifier.set(tag, 13.0F);
        ItemSlashBlade.AttackAmplifier.set(tag, 1.2F);
        ItemSlashBlade.SpecialAttackType.set(tag, 470);
        Item_HonkaiGreatswordNamed.IsDefaultBewitched.set(tag, true);
        customblade.addEnchantment(Enchantments.UNBREAKING, 2);
        customblade.addEnchantment(Enchantments.SHARPNESS, 1);
        customblade.addEnchantment(Enchantments.KNOCKBACK, 2);

        ItemSlashBladeNamed.NamedBlades.add(name);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        SlashBlade.registerCustomItemStack(name, customblade);
        BladeLoader.NamedHonkaiGreatsword.add(name);

        ItemStack result = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack previous = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.nuclear_pri_ex", 1);
        IRecipe recipe = new SlashBladeRecipeModding(new ResourceLocation(bladestr, "vanguard"),
                result, previous, new Object[]{
                " D ", "GBG", "O O",
                'D', Items.DIAMOND,
                'G', Items.GLOWSTONE_DUST,
                'B', previous,
                'O', new ItemStack(Blocks.OBSIDIAN)
        });
        SlashBlade.addRecipe("vanguard", recipe);
    }
}

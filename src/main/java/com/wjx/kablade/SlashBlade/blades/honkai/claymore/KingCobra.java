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
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.wjx.kablade.Main.bladestr;

/** 超重剑·王蛇。 */
public class KingCobra {
    private final String name = "wjx.blade.honkai.king_cobra";
    private final ItemStack customblade = new ItemStack(BladeLoader.ITEM_HONKAI_GREATSWORD_NAMED, 1, 0);
    private final NBTTagCompound tag = new NBTTagCompound();

    public KingCobra() {
        customblade.setTagCompound(tag);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void init(LoadEvent.InitEvent event) {
        Item_HonkaiGreatswordNamed.CurrentItemName.set(tag, name);
        Item_HonkaiGreatswordNamed.CustomMaxDamage.set(tag, 550);
        ItemSlashBlade.TextureName.set(tag, "kablade/Honkai/claymore/super_heavy_sword/tex_1");
        ItemSlashBlade.ModelName.set(tag, "kablade/Honkai/claymore/super_heavy_sword/mdl");
        ItemSlashBlade.BaseAttackModifier.set(tag, 15.0F);
        ItemSlashBlade.AttackAmplifier.set(tag, 1.2F);
        Item_HonkaiGreatswordNamed.IsDefaultBewitched.set(tag, true);
        customblade.addEnchantment(Enchantments.UNBREAKING, 2);
        customblade.addEnchantment(Enchantments.SHARPNESS, 2);
        customblade.addEnchantment(Enchantments.KNOCKBACK, 1);

        ItemSlashBladeNamed.NamedBlades.add(name);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        SlashBlade.registerCustomItemStack(name, customblade);
        BladeLoader.NamedHonkaiGreatsword.add(name);

        ItemStack result = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack previous = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.nuclear_pri_ex", 1);
        IRecipe recipe = new SlashBladeRecipeModding(new ResourceLocation(bladestr, "king_cobra"),
                result, previous, new Object[]{
                " B ", "CSC", "O O",
                'B', new ItemStack(Blocks.BONE_BLOCK),
                'C', new ItemStack(Blocks.CACTUS),
                'S', previous,
                'O', new ItemStack(Blocks.OBSIDIAN)
        });
        SlashBlade.addRecipe("king_cobra", recipe);
    }
}

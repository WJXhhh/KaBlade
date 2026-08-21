package com.wjx.kablade.SlashBlade.blades.honkaip2;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_HonkaiNamed;
import com.wjx.kablade.SlashBlade.blades.recipe.SlashBladeTwoRecipeModding;
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

/** 涤罪七雷·鸣；替代旧 CaiJueDizui 的物品与 SA 入口。 */
public class SevenThundersRumble {
    public static final String NAME = "wjx.blade.honkai.str_rumble";
    private final ItemStack blade = new ItemStack(BladeLoader.ITEM_HONKAI_NAMED);
    private final NBTTagCompound tag = new NBTTagCompound();

    public SevenThundersRumble() { blade.setTagCompound(tag); MinecraftForge.EVENT_BUS.register(this); }

    @SubscribeEvent
    public void init(LoadEvent.InitEvent event) {
        Item_HonkaiNamed.CurrentItemName.set(tag, NAME);
        Item_HonkaiNamed.CustomMaxDamage.set(tag, 1300);
        ItemSlashBlade.TextureName.set(tag, "kablade/Honkai/SevenThundersRumble/tex");
        ItemSlashBlade.ModelName.set(tag, "kablade/Honkai/SevenThundersRumble/mdl");
        tag.setFloat("baseAttackModifier", 49.0F);
        ItemSlashBlade.SpecialAttackType.set(tag, 464);
        ItemSlashBlade.SummonedSwordColor.set(tag, 0x9B00FF);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        Item_HonkaiNamed.IsDefaultBewitched.set(tag, true);
        blade.addEnchantment(Enchantments.UNBREAKING, 5);
        blade.addEnchantment(Enchantments.POWER, 7);
        blade.addEnchantment(Enchantments.SMITE, 7);
        blade.addEnchantment(Enchantments.SHARPNESS, 7);
        ItemSlashBladeNamed.NamedBlades.add(NAME);
        SlashBlade.registerCustomItemStack(NAME, blade);
        BladeLoader.NamedHonkai.add(NAME);

        ItemStack result = SlashBlade.findItemStack(bladestr, NAME, 1);
        ItemStack castigation = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.key_of_cas", 1);
        ItemStack unity = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.domain_of_unity", 1);
        IRecipe recipe = new SlashBladeTwoRecipeModding(
                new ResourceLocation(bladestr, "str_rumble"), result, castigation, unity,
                "ABA", "ACA", "DED",
                'A', new ItemStack(ItemInit.ELECTRO_SIGNET),
                'B', castigation, 'C', unity,
                'D', new ItemStack(Items.DRAGON_BREATH),
                'E', new ItemStack(Items.END_CRYSTAL));
        SlashBlade.addRecipe("str_rumble", recipe);
    }
}

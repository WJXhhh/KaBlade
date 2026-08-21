package com.wjx.kablade.SlashBlade.blades.honkaip2;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_HonkaiNamed;
import com.wjx.kablade.SlashBlade.blades.recipe.SlashBladeNamedRecipe;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.named.event.LoadEvent;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
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

/** 澄凝之境：自性纯一。 */
public class DomainOfUnity {
    private static final String NAME = "wjx.blade.honkai.domain_of_unity";
    private final ItemStack blade = new ItemStack(BladeLoader.ITEM_HONKAI_NAMED);
    private final NBTTagCompound tag = new NBTTagCompound();

    public DomainOfUnity() {
        blade.setTagCompound(tag);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void init(LoadEvent.InitEvent event) {
        Item_HonkaiNamed.CurrentItemName.set(tag, NAME);
        Item_HonkaiNamed.CustomMaxDamage.set(tag, 1400);
        ItemSlashBlade.TextureName.set(tag, "kablade/Honkai/DomainOfUnity/tex");
        ItemSlashBlade.ModelName.set(tag, "kablade/Honkai/DomainOfUnity/mdl");
        tag.setFloat("baseAttackModifier", 37.0F);
        ItemSlashBlade.AttackAmplifier.set(tag, 2.5F);
        ItemSlashBlade.SpecialAttackType.set(tag, 463);
        ItemSlashBlade.SummonedSwordColor.set(tag, 0xFFAAFF);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        Item_HonkaiNamed.IsDefaultBewitched.set(tag, true);
        blade.addEnchantment(Enchantments.UNBREAKING, 5);
        blade.addEnchantment(Enchantments.POWER, 6);
        blade.addEnchantment(Enchantments.SMITE, 5);
        blade.addEnchantment(Enchantments.SHARPNESS, 6);
        SpecialEffects.addEffect(blade, BladeProxy.Unthinkable);
        ItemSlashBladeNamed.NamedBlades.add(NAME);
        SlashBlade.registerCustomItemStack(NAME, blade);
        BladeLoader.NamedHonkai.add(NAME);

        ItemStack result = SlashBlade.findItemStack(bladestr, NAME, 1);
        ItemStack previous = SlashBlade.findItemStack(bladestr,
                "wjx.blade.honkai.key_of_limpidity", 1);
        IRecipe recipe = new SlashBladeNamedRecipe(
                new ResourceLocation(bladestr, "domain_of_unity"), result, previous,
                "ABA", "CDC", "EFE",
                'A', new ItemStack(Items.QUARTZ),
                'B', new ItemStack(Blocks.GLASS),
                'C', new ItemStack(Blocks.END_ROD),
                'D', previous,
                'E', new ItemStack(Items.NETHER_STAR),
                'F', new ItemStack(Blocks.BEACON));
        SlashBlade.addRecipe("domain_of_unity", recipe);
    }
}

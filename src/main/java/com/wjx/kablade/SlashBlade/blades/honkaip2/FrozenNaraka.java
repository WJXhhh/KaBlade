package com.wjx.kablade.SlashBlade.blades.honkaip2;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_HonkaiNamed;
import com.wjx.kablade.SlashBlade.blades.recipe.SlashBladeRecipeModding;
import com.wjx.kablade.init.ItemInit;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.named.event.LoadEvent;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.wjx.kablade.Main.bladestr;

public class FrozenNaraka {
    String name = "wjx.blade.honkai.frozen_naraka";
    String key = "wjx.blade.honkai.frozen_naraka";

    public FrozenNaraka(){
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
        Item_HonkaiNamed.CustomMaxDamage.set(tag, 1200);

        ItemSlashBlade.TextureName.set(tag, "kablade/honkaip2/FrozenNaraka/tex");
        ItemSlashBlade.ModelName.set(tag, "kablade/honkaip2/FrozenNaraka/mdl");
        customblade.getTagCompound().setFloat("baseAttackModifier",32.0F);
        ItemSlashBlade.AttackAmplifier.set(tag,2.5f);
        ItemSlashBlade.SpecialAttackType.set(tag,459);
        ItemSlashBlade.SummonedSwordColor.set(tag,65535);
        customblade.addEnchantment(Enchantments.UNBREAKING,5);
        customblade.addEnchantment(Enchantments.POWER,4);
        customblade.addEnchantment(Enchantments.SMITE,4);
        customblade.addEnchantment(Enchantments.SHARPNESS,5);
        Item_HonkaiNamed.IsDefaultBewitched.set(tag, true);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        SpecialEffects.addEffect(customblade, BladeProxy.GlacialBane);
        SlashBlade.registerCustomItemStack(this.name, customblade);
        BladeLoader.NamedHonkai.add(name);

        ItemStack result = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack previous = SlashBlade.findItemStack(bladestr,
                "wjx.blade.honkai.ice_epiphyllum", 1);
        IRecipe recipe = new SlashBladeRecipeModding(
                new ResourceLocation(bladestr, "frozen_naraka"), result, previous,
                "DBD",
                "CAC",
                "DCD",
                'A', previous,
                'B', new ItemStack(ItemInit.SUPERCONDUCTING_METAL),
                'C', new ItemStack(Blocks.SOUL_SAND),
                'D', new ItemStack(Blocks.RED_FLOWER, 1, 1));
        SlashBlade.addRecipe("frozen_naraka", recipe);
    }
}

package com.wjx.kablade.SlashBlade.blades.honkaip2;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_HonkaiNamed;
import com.wjx.kablade.SlashBlade.blades.recipe.SlashBladeTwoRecipeModding;
import com.wjx.kablade.init.ItemInit;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.named.event.LoadEvent;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
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
        customblade.getTagCompound().setFloat("baseAttackModifier",26.0F);
        ItemSlashBlade.AttackAmplifier.set(tag,2.5f);
        ItemSlashBlade.SpecialAttackType.set(tag,299);
        customblade.addEnchantment(Enchantments.POWER,4);
        customblade.addEnchantment(Enchantments.SMITE,4);
        customblade.addEnchantment(Enchantments.SHARPNESS,5);
        Item_HonkaiNamed.IsDefaultBewitched.set(tag, true);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        SpecialEffects.addEffect(customblade, BladeProxy.GlacialBane);
        SlashBlade.registerCustomItemStack(this.name, customblade);
        BladeLoader.NamedHonkai.add(name);

        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prevblade = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.xuanyuan_katana", 1);
        ItemStack prevblade2 = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.third_sacred", 1);
        IRecipe recipe = new SlashBladeTwoRecipeModding(new ResourceLocation(bladestr,"frozen_naraka"),
                blackblade, prevblade,prevblade2,
                "CCC",
                "DBD",
                " A ",
                'A', prevblade,
                'B',prevblade2,
                'C', new ItemStack(ItemInit.IRON_COIL),
                'D',new ItemStack(Blocks.LAPIS_BLOCK));
        SlashBlade.addRecipe("frozen_naraka", recipe);
    }
}

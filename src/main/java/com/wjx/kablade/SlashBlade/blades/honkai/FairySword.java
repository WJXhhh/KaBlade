package com.wjx.kablade.SlashBlade.blades.honkai;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_HonkaiNamed;
import com.wjx.kablade.init.ItemInit;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.RecipeAwakeBlade;
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

public class FairySword {
    String name = "wjx.blade.honkai.fairy_sword";
    String key = "wjx.blade.honkai.fairy_sword";

    public FutsunushiTo(){
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
        Item_HonkaiNamed.CustomMaxDamage.set(tag, 720);

        ItemSlashBlade.TextureName.set(tag, "kablade/Honkai/FairySword/texFutsunushiTo");
        ItemSlashBlade.ModelName.set(tag, "kablade/Honkai/FutsunushiTo/mdlFutsunushiTo");
        customblade.getTagCompound().setFloat("baseAttackModifier",15.0F);
        customblade.addEnchantment(Enchantments.SHARPNESS,2);
        Item_HonkaiNamed.IsDefaultBewitched.set(tag, true);
        ItemSlashBlade.SummonedSwordColor.set(tag,16642509);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.SpecialAttackType.set(tag, 300);
        SpecialEffects.addEffect(customblade, BladeProxy.RagingIzumo);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        SlashBlade.registerCustomItemStack(this.name, customblade);
        BladeLoader.NamedHonkai.add(name);

        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prevblade = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.byorai", 1);
        //ItemStack prevblade2 = SlashBlade.findItemStack(bladestr, "wjx.blade.bamboo_iron", 1);
        IRecipe recipe = new RecipeAwakeBlade(new ResourceLocation(bladestr,"osahoko"),
                blackblade, prevblade,
                "  C",
                " B ",
                "A B",
                'A', prevblade,
                'B',new ItemStack(Blocks.GLOWSTONE),
                'C', new ItemStack(ItemInit.THUNDER_CRYSTAL));

        SlashBlade.addRecipe("osahoko", recipe);
    }
}

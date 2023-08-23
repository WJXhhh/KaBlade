package com.wjx.kablade.SlashBlade.blades;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_KaNamed;
import com.wjx.kablade.init.ItemInit;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.RecipeAwakeBlade;
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

public class Originyer {
    String name = "wjx.blade.originyer";
    String key = "wjx.blade.originyer";

    public Originyer(){
        MinecraftForge.EVENT_BUS.register(this);
    }

    private ItemStack customBlade = new ItemStack(BladeLoader.ITEM_KABLADE_NAMED,1,0);

    private NBTTagCompound Tag = new NBTTagCompound();
    {
        customBlade.setTagCompound(Tag);
    }

    @SubscribeEvent
    public void init(LoadEvent.InitEvent event) {
        Item_KaNamed.CurrentItemName.set(Tag, name);
        Item_KaNamed.CustomMaxDamage.set(Tag, 400);

        ItemSlashBlade.TextureName.set(Tag, "kablade/Originyer/texOriginyer");
        ItemSlashBlade.ModelName.set(Tag, "kablade/Originyer/mdlOriginyer");

        ItemSlashBlade.setBaseAttackModifier(Tag,8);
        customBlade.getTagCompound().setFloat("baseAttackModifier",13.0F);
        ItemSlashBlade.SpecialAttackType.set(Tag,297);
        ItemSlashBlade.SummonedSwordColor.set(Tag,65535);
        customBlade.addEnchantment(Enchantments.SHARPNESS,5);
        customBlade.addEnchantment(Enchantments.POWER,2);
        Item_KaNamed.IsDefaultBewitched.set(Tag, true);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(Tag, 1);
        SpecialEffects.addEffect(customBlade,BladeProxy.Oripursuit);
        SlashBlade.registerCustomItemStack(this.name, customBlade);
        BladeLoader.NamedBlades.add(name);
        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        ItemStack prevblade = SlashBlade.findItemStack(bladestr, "wjx.blade.cut_iron", 1);
        IRecipe recipe = new RecipeAwakeBlade(new ResourceLocation(bladestr,"originyer"),
                blackblade, prevblade,
                "BDB",
                "CAC",
                "BCB",
                'A', prevblade,
                'B', new ItemStack(ItemInit.GRAVITY_NUGGET),
                'C',new ItemStack(Items.DIAMOND),
                'D',new ItemStack(Blocks.REDSTONE_BLOCK));
        SlashBlade.addRecipe("originyer", recipe);
    }


}

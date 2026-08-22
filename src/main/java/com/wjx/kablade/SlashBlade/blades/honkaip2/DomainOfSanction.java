package com.wjx.kablade.SlashBlade.blades.honkaip2;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_HonkaiNamed;
import com.wjx.kablade.SlashBlade.blades.recipe.SlashBladeNamedRecipe;
import com.wjx.kablade.init.ItemInit;
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

/** 天殛之境：裁决。 */
public class DomainOfSanction {
    public static final String NAME="wjx.blade.honkai.domain_of_sanction";
    private final ItemStack blade=new ItemStack(BladeLoader.ITEM_HONKAI_NAMED); private final NBTTagCompound tag=new NBTTagCompound();
    public DomainOfSanction(){blade.setTagCompound(tag);MinecraftForge.EVENT_BUS.register(this);}
    @SubscribeEvent public void init(LoadEvent.InitEvent event){
        Item_HonkaiNamed.CurrentItemName.set(tag,NAME);Item_HonkaiNamed.CustomMaxDamage.set(tag,1300);
        ItemSlashBlade.TextureName.set(tag,"kablade/Honkai/DomainOfSanction/tex");ItemSlashBlade.ModelName.set(tag,"kablade/Honkai/DomainOfSanction/mdl");
        tag.setFloat("baseAttackModifier",35F);ItemSlashBlade.AttackAmplifier.set(tag,2.5F);ItemSlashBlade.SpecialAttackType.set(tag,466);
        ItemSlashBlade.SummonedSwordColor.set(tag,0x9B00FF);ItemSlashBlade.StandbyRenderType.set(tag,1);Item_HonkaiNamed.IsDefaultBewitched.set(tag,true);
        blade.addEnchantment(Enchantments.UNBREAKING,5);blade.addEnchantment(Enchantments.POWER,4);blade.addEnchantment(Enchantments.SMITE,5);blade.addEnchantment(Enchantments.SHARPNESS,6);
        SpecialEffects.addEffect(blade,BladeProxy.ThunderBlitz);SpecialEffects.addEffect(blade,BladeProxy.ThunderDivinePenalty);
        ItemSlashBladeNamed.NamedBlades.add(NAME);SlashBlade.registerCustomItemStack(NAME,blade);BladeLoader.NamedHonkai.add(NAME);
        ItemStack result=SlashBlade.findItemStack(bladestr,NAME,1);ItemStack previous=SlashBlade.findItemStack(bladestr,"wjx.blade.honkai.key_of_cas",1);
        IRecipe recipe=new SlashBladeNamedRecipe(new ResourceLocation(bladestr,"domain_of_sanction"),result,previous,
                "ABA","CDC","EFE",'A',new ItemStack(Items.DRAGON_BREATH),'B',new ItemStack(ItemInit.SUPERCONDUCTING_METAL),'C',new ItemStack(Blocks.OBSIDIAN),'D',previous,'E',new ItemStack(ItemInit.THUNDER_CRYSTAL),'F',new ItemStack(Items.SKULL,1,5));
        SlashBlade.addRecipe("domain_of_sanction",recipe);
    }
}

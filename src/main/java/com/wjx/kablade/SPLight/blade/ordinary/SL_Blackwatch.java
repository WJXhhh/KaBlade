package com.wjx.kablade.SPLight.blade.ordinary;

import com.wjx.kablade.AllWeapon.blade.items.Item_AwNamed;
import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.SlashBlade.blades.recipe.SPLightRecipe;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.named.event.LoadEvent;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import mods.flammpfeil.slashblade.util.SlashBladeHooks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;

import static com.wjx.kablade.Main.bladestr;

public class SL_Blackwatch {
    String name = "wjx.splight.blackwatch";

    public SL_Blackwatch(){
        MinecraftForge.EVENT_BUS.register(this);
    }

    private ItemStack customblade = new ItemStack(BladeLoader.ITEM_SL,1,0);

    private NBTTagCompound Tag = new NBTTagCompound();
    {
        customblade.setTagCompound(Tag);
    }

    @SubscribeEvent
    public void init(LoadEvent.InitEvent event){
        Item_AwNamed.CurrentItemName.set(Tag,name);
        Item_AwNamed.CustomMaxDamage.set(Tag,191);
        ItemSlashBlade.ModelName.set(Tag,"splight/splight/blackwatch/model");
        ItemSlashBlade.TextureName.set(Tag, "splight/splight/blackwatch/texture");
        //ItemSlashBlade.IsNoScabbard.set(Tag,true);
        ItemSlashBlade.SpecialAttackType.set(Tag,352);
        ItemSlashBladeNamed.IsDefaultBewitched.set(Tag, true);
        ItemSlashBlade.BaseAttackModifier.set(Tag,12f);
        customblade.addEnchantment(Enchantments.LOOTING,2);
        customblade.addEnchantment(Enchantments.POWER,5);
        customblade.addEnchantment(Enchantments.UNBREAKING,3);
        customblade.addEnchantment(Enchantments.SHARPNESS,4);
        customblade.addEnchantment(Enchantments.BANE_OF_ARTHROPODS,2);
        SpecialEffects.addEffect(customblade, BladeProxy.SPLighting);
        SpecialEffects.addEffect(customblade, BladeProxy.BurstDrive);
        //customblade.addEnchantment(Enchantments.FIRE_ASPECT,20);
        //customblade.addEnchantment(Enchantments.POWER,20);
        //customblade.addEnchantment(Enchantments.,20);
        SlashBlade.registerCustomItemStack(name,customblade);
        BladeLoader.SLBlades.add(name);
        //AchievementList.registerAchievement("name",customblade,null);

        ItemStack targetBlade = SlashBlade.findItemStack(bladestr,name,1);
        ItemStack initialBlade = SlashBlade.findItemStack(bladestr,SL_Young1.name,1);
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(initialBlade);
        ItemSlashBlade.KillCount.set(tag,1000);
        ItemSlashBlade.ProudSoul.set(tag,10000);

        Map<Enchantment, Integer> eMap = EnchantmentHelper.getEnchantments(initialBlade);
        if(eMap.containsKey(Enchantments.POWER)){
            eMap.put(Enchantments.POWER,5);
        }
        if(eMap.containsKey(Enchantments.SHARPNESS)){
            eMap.put(Enchantments.SHARPNESS,5);
        }
        EnchantmentHelper.setEnchantments(eMap,initialBlade);

        ItemStack sp1 = SlashBlade.findItemStack("flammpfeil.slashblade", "sphere_bladesoul", 1);
        ItemStack sp2 = SlashBlade.findItemStack("flammpfeil.slashblade", "sphere_bladesoul", 1);

        ItemSlashBlade.SpecialAttackType.set(sp1.getTagCompound(),4);
        if (Loader.isModLoaded("slashblade_addon")){
            ItemSlashBlade.SpecialAttackType.set(sp2.getTagCompound(),37);
        }
        else{
            ItemSlashBlade.SpecialAttackType.set(sp2.getTagCompound(),6);
        }

        IRecipe recipe = new SPLightRecipe(new ResourceLocation(bladestr,"sp_black"),targetBlade, initialBlade,sp1,sp2,
                new Object[]{
                        "DWD",
                        "ZBZ",
                        "GXG",
                        'D',
                        new ItemStack(Items.NETHER_STAR),
                        'W',
                        sp2,
                        'Z',
                        new ItemStack(Items.RECORD_CHIRP),
                        'B',
                        initialBlade,
                        'G',
                        SlashBlade.getCustomBlade("flammpfeil.slashblade:slashblade"),
                        'X',
                        sp1
                });
        SlashBlade.addRecipe("sp_black", recipe);
    }

    @SubscribeEvent
    public void postinit(LoadEvent.PostInitEvent event) {
        SlashBladeHooks.EventBus.register(this);
    }
}

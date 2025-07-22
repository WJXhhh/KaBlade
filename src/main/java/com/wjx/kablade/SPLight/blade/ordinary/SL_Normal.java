package com.wjx.kablade.SPLight.blade.ordinary;

import com.wjx.kablade.AllWeapon.blade.items.Item_AwNamed;
import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.SlashBlade.blades.recipe.SlashBladeRecipeModding;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.named.event.LoadEvent;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import mods.flammpfeil.slashblade.util.SlashBladeHooks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;

import static com.wjx.kablade.Main.bladestr;

public class SL_Normal {
    public static String name = "wjx.splight.normal";

    public SL_Normal(){
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
        Item_AwNamed.CustomMaxDamage.set(Tag,60);
        ItemSlashBlade.ModelName.set(Tag,"named/sange/sange");
        ItemSlashBlade.TextureName.set(Tag, "splight/splight/normal/texture");
        //ItemSlashBlade.IsNoScabbard.set(Tag,true);
        ItemSlashBlade.SpecialAttackType.set(Tag,350);
        ItemSlashBladeNamed.IsDefaultBewitched.set(Tag, true);
        ItemSlashBlade.BaseAttackModifier.set(Tag,8f);
        customblade.addEnchantment(Enchantments.LOOTING,1);
        customblade.addEnchantment(Enchantments.POWER,3);
        customblade.addEnchantment(Enchantments.UNBREAKING,1);
        customblade.addEnchantment(Enchantments.SHARPNESS,3);
        SpecialEffects.addEffect(customblade, BladeProxy.SPLighting);
        //customblade.addEnchantment(Enchantments.FIRE_ASPECT,20);
        //customblade.addEnchantment(Enchantments.POWER,20);
        //customblade.addEnchantment(Enchantments.,20);
        SlashBlade.registerCustomItemStack(name,customblade);
        BladeLoader.SLBlades.add(name);
        //AchievementList.registerAchievement("name",customblade,null);

        ItemStack targetBlade = SlashBlade.findItemStack(bladestr,name,1);
        ItemStack initialBlade = SlashBlade.findItemStack(bladestr,SL_Initial.name,1);
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(initialBlade);
        ItemSlashBlade.KillCount.set(tag,200);
        ItemSlashBlade.ProudSoul.set(tag,2000);
        initialBlade.addEnchantment(Enchantments.SHARPNESS,3);
        Map<Enchantment, Integer> eMap = EnchantmentHelper.getEnchantments(initialBlade);
        if(eMap.containsKey(Enchantments.POWER)){
            eMap.put(Enchantments.POWER,3);
        }
        EnchantmentHelper.setEnchantments(eMap,initialBlade);
        IRecipe recipe = new SlashBladeRecipeModding(new ResourceLocation(bladestr,"sp_normal"),targetBlade, initialBlade,
                new Object[]{
                        "QSQ",
                        "SKS",
                        "ISI",
                        'S',
                        SlashBlade.findItemStack(bladestr,"sphere_bladesoul",1),
                        'K',
                        initialBlade,
                        'Q',
                        new ItemStack(Blocks.DIAMOND_BLOCK),
                        'I',
                        new ItemStack(Items.BLAZE_ROD)
                });
        SlashBlade.addRecipe("sp_normal", recipe);
    }

    @SubscribeEvent
    public void postinit(LoadEvent.PostInitEvent event) {
        SlashBladeHooks.EventBus.register(this);
    }
}

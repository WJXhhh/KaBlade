package com.wjx.kablade.SPLight.blade.ordinary;

import com.wjx.kablade.AllWeapon.blade.items.Item_AwNamed;
import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.BladeProxy;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.named.event.LoadEvent;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import mods.flammpfeil.slashblade.util.SlashBladeHooks;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class SL_Young2 {
    String name = "wjx.splight.young2";

    public SL_Young2(){
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
        Item_AwNamed.CustomMaxDamage.set(Tag,114);
        ItemSlashBlade.ModelName.set(Tag,"splight/splight/young/model");
        ItemSlashBlade.TextureName.set(Tag, "splight/splight/young/texture_g");
        //ItemSlashBlade.IsNoScabbard.set(Tag,true);
        ItemSlashBlade.SpecialAttackType.set(Tag,4);
        ItemSlashBladeNamed.IsDefaultBewitched.set(Tag, true);
        ItemSlashBlade.BaseAttackModifier.set(Tag,9f);
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
    }

    @SubscribeEvent
    public void postinit(LoadEvent.PostInitEvent event) {
        SlashBladeHooks.EventBus.register(this);
    }
}

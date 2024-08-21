package com.wjx.kablade.AllWeapon.blade.ordinary;

import com.wjx.kablade.AllWeapon.blade.items.Item_AwNamed;
import com.wjx.kablade.SlashBlade.BladeLoader;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.named.event.LoadEvent;
import mods.flammpfeil.slashblade.util.SlashBladeHooks;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AL_Baishoujianwang {
    String name = "wjx.allweapon.baishoujianwang";

    public AL_Baishoujianwang(){
        MinecraftForge.EVENT_BUS.register(this);
    }

    private ItemStack customblade = new ItemStack(BladeLoader.ITEM_AW,1,0);

    private NBTTagCompound Tag = new NBTTagCompound();
    {
        customblade.setTagCompound(Tag);
    }

    @SubscribeEvent
    public void init(LoadEvent.InitEvent event){
        Item_AwNamed.CurrentItemName.set(Tag,name);
        Item_AwNamed.CustomMaxDamage.set(Tag,230);
        ItemSlashBlade.TextureName.set(Tag, "allweapon/baishoujianwang/texture");
        ItemSlashBlade.ModelName.set(Tag, "allweapon/baishoujianwang/model");
        ItemSlashBladeNamed.IsDefaultBewitched.set(Tag, true);
        ItemSlashBlade.StandbyRenderType.set(Tag, 2);
        ItemSlashBlade.SummonedSwordColor.set(Tag, 0xaaaaaa);
        ItemSlashBlade.SpecialAttackType.set(Tag,404);
        ItemSlashBlade.BaseAttackModifier.set(Tag,10f);
        customblade.addEnchantment(Enchantments.SHARPNESS,10);
        //customblade.addEnchantment(Enchantments.POWER,20);
        //customblade.addEnchantment(Enchantments.,20);
        SlashBlade.registerCustomItemStack(name,customblade);
        BladeLoader.AwBlades.add(name);
        //AchievementList.registerAchievement("name",customblade,null);
    }

    @SubscribeEvent
    public void postinit(LoadEvent.PostInitEvent event) {
        SlashBladeHooks.EventBus.register(this);
    }
}

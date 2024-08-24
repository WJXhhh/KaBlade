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

public class AL_Fengshen {

    String name = "wjx.allweapon.fengshen";

    public AL_Fengshen(){
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
        Item_AwNamed.CustomMaxDamage.set(Tag,250);
        ItemSlashBlade.TextureName.set(Tag, "allweapon/fengshen/texture");
        ItemSlashBlade.ModelName.set(Tag, "allweapon/fengshen/model");
        ItemSlashBladeNamed.IsDefaultBewitched.set(Tag, true);
        ItemSlashBlade.StandbyRenderType.set(Tag, 2);
        ItemSlashBlade.SummonedSwordColor.set(Tag, 0xbbbb1e);
        ItemSlashBlade.SpecialAttackType.set(Tag,410);
        ItemSlashBlade.BaseAttackModifier.set(Tag,14f);

        customblade.addEnchantment(Enchantments.INFINITY, 20);
        customblade.addEnchantment(Enchantments.SHARPNESS, 20);
        customblade.addEnchantment(Enchantments.LOOTING, 20);
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

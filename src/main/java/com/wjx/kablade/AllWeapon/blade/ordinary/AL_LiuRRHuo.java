package com.wjx.kablade.AllWeapon.blade.ordinary;

import com.wjx.kablade.AllWeapon.blade.items.Item_AwNamed;
import com.wjx.kablade.SlashBlade.BladeLoader;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.named.event.LoadEvent;
import mods.flammpfeil.slashblade.stats.AchievementList;
import net.minecraft.init.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AL_LiuRRHuo {

    String name = "wjx.allweapon.liurrh";

    public AL_LiuRRHuo(){
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
        Item_AwNamed.CustomMaxDamage.set(Tag,100);
        ItemSlashBlade.TextureName.set(Tag, "allweapon/lrrh/texture");
        ItemSlashBlade.ModelName.set(Tag, "allweapon/lrrh/model");
        ItemSlashBladeNamed.IsDefaultBewitched.set(Tag, true);
        ItemSlashBlade.StandbyRenderType.set(Tag, 2);
        ItemSlashBlade.SummonedSwordColor.set(Tag, 0xFF0000);
        ItemSlashBlade.SpecialAttackType.set(Tag,400);
        customblade.addEnchantment(Enchantments.FIRE_ASPECT,20);
        customblade.addEnchantment(Enchantments.POWER,20);
        //customblade.addEnchantment(Enchantments.,20);
        SlashBlade.registerCustomItemStack(name,customblade);
        BladeLoader.AwBlades.add(name);
        //AchievementList.registerAchievement("name",customblade,null);
    }
}

package com.wjx.kablade.SlashBlade.blades.honkai;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_Caijue;
import com.wjx.kablade.SlashBlade.blades.bladeitem.MagicBlade;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.named.event.LoadEvent;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.wjx.kablade.SlashBlade.BladeLoader.ITEM_DIZUI;
import static com.wjx.kablade.SlashBlade.BladeLoader.ITEM_MAGIC;

public class CaiJueDizui {
    String name = "wjx.blade.honkai.dizui";
    String key = "wjx.blade.honkai.dizui";



    public CaiJueDizui(){
        MinecraftForge.EVENT_BUS.register(this);
    }
    private ItemStack customblade = new ItemStack(ITEM_DIZUI,1,0);

    private NBTTagCompound tag = new NBTTagCompound();
    {
        customblade.setTagCompound(tag);
    }
    static int sSC = 0xFF1493;

    @SubscribeEvent
    public void init(LoadEvent.InitEvent event) {


        //ItemStack customblade = new ItemStack(STAR,1,0);
        //NBTTagCompound tag = new NBTTagCompound();
        //customblade.setTagCompound(tag);

        tag.setBoolean("Unbreakable",true);
        Item_Caijue.CurrentItemName.set(tag, name);
        Item_Caijue.CustomMaxDamage.set(tag, 32767);

        ItemSlashBlade.TextureName.set(tag, "kablade/Honkai/Dizui/Weapon_Katana_M45_347_5_Texture_Color");
        ItemSlashBlade.ModelName.set(tag, "kablade/Honkai/Dizui/mdldizui");
        //ItemSlashBlade.ProudSoul.set(tag, 32767);
        //ItemSlashBlade.KillCount.set(tag, 888888);
        customblade.addEnchantment(Enchantments.UNBREAKING, 10);
        customblade.addEnchantment(Enchantments.SHARPNESS, 10);
        customblade.addEnchantment(Enchantments.POWER, 10);
        //customblade.addEnchantment(Enchantments.LOOTING,100);
        customblade.addEnchantment(Enchantments.INFINITY,10);

        //tag.setInteger("HideFlags",1);



        //customblade.getTagCompound().setBoolean("isUniverseBlade",true);
        //ItemSlashBlade.BaseAttackModifier.set(tag, 32768.0F);
        //ItemSlashBlade.setBaseAttackModifier(tag,32768.0F);
        customblade.getTagCompound().setFloat("baseAttackModifier",64.0F);

        ItemSlashBlade.SpecialAttackType.set(tag, 282);
        Item_Caijue.IsDefaultBewitched.set(tag, true);
        ItemSlashBlade.SummonedSwordColor.set(tag, sSC);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        SlashBlade.registerCustomItemStack(this.name, customblade);
        BladeLoader.DIZUI.add(name);
        //System.out.println("autumn:starinit");
    }
}

package com.wjx.kablade.SlashBlade.blades;

import com.wjx.kablade.SlashBlade.BladeLoader;
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

import static com.wjx.kablade.SlashBlade.BladeLoader.ITEM_MAGIC;

public class MoDao {
    String name = "wjx.blade.magic";
    String key = "wjx.blade.magic";



    public MoDao(){
        MinecraftForge.EVENT_BUS.register(this);
    }
    private ItemStack customblade = new ItemStack(ITEM_MAGIC,1,0);

    private NBTTagCompound tag = new NBTTagCompound();
    {
        customblade.setTagCompound(tag);
    }
    static int sSC = 16718929;

    @SubscribeEvent
    public void init(LoadEvent.InitEvent event) {


        //ItemStack customblade = new ItemStack(STAR,1,0);
        //NBTTagCompound tag = new NBTTagCompound();
        //customblade.setTagCompound(tag);

        tag.setBoolean("Unbreakable",true);
        MagicBlade.CurrentItemName.set(tag, name);
        MagicBlade.CustomMaxDamage.set(tag, 888888888);

        ItemSlashBlade.TextureName.set(tag, "kablade/MagicBlade/tex");
        ItemSlashBlade.ModelName.set(tag, "kablade/MagicBlade/mdl");
        ItemSlashBlade.ProudSoul.set(tag, 888888888);
        ItemSlashBlade.KillCount.set(tag, 888888);
        customblade.addEnchantment(Enchantments.UNBREAKING, 100);
        customblade.addEnchantment(Enchantments.SHARPNESS, 100);
        customblade.addEnchantment(Enchantments.POWER, 100);
        //customblade.addEnchantment(Enchantments.LOOTING,100);
        customblade.addEnchantment(Enchantments.INFINITY,100);

        tag.setInteger("HideFlags",1);



        //customblade.getTagCompound().setBoolean("isUniverseBlade",true);
        //ItemSlashBlade.BaseAttackModifier.set(tag, 32768.0F);
        //ItemSlashBlade.setBaseAttackModifier(tag,32768.0F);
        customblade.getTagCompound().setFloat("baseAttackModifier",32768.0F);
        ItemSlashBlade.SpecialAttackType.set(tag, 280);
        MagicBlade.IsDefaultBewitched.set(tag, true);
        ItemSlashBlade.SummonedSwordColor.set(tag, sSC);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        SlashBlade.registerCustomItemStack(this.name, customblade);
        BladeLoader.NamedGod.add(name);
        //System.out.println("autumn:starinit");
    }
}

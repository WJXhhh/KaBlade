package com.wjx.kablade.util.slash;

import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.named.event.LoadEvent;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import mods.flammpfeil.slashblade.tileentity.DummyTileEntity;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class LoaderSlash {
    public static final Item tran = new TranscendSlash();

    public LoaderSlash(){
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void registerItem(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(tran.setRegistryName("transcend", "tran"));
    }

    @SubscribeEvent
    public void registerModel(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(tran, 0, new ModelResourceLocation("flammpfeil.slashblade:model/named/blade.obj"));
        ForgeHooksClient.registerTESRItemStack(tran, 0, DummyTileEntity.class);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void load(LoadEvent.InitEvent event) {
        if(Loader.isModLoaded("flammpfeil.slashblade")) {
            ItemStack blade = new ItemStack((Item)SlashBlade.bladeNamed, 1, 0);
            String name = "flammpfeil.slashblade.named.tran";
            ItemStack tran = new ItemStack(LoaderSlash.tran, 1, 0);
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("HideFlags",6);
            ItemSlashBladeNamed.CurrentItemName.set(tag, name);
            ItemSlashBladeNamed.CustomMaxDamage.set(tag, Integer.valueOf(32767));
            ItemSlashBladeNamed.IsDefaultBewitched.set(tag, Boolean.valueOf(true));
            ItemSlashBladeNamed.NamedBlades.add("flammpfeil.slashblade.named.tran");
            ItemSlashBlade.setBaseAttackModifier(tag, 32767.0F);
            ItemSlashBlade.SpecialAttackType.set(tag, Integer.valueOf(678));
            ItemSlashBlade.TextureName.set(tag, "named/transcend/texture");
            ItemSlashBlade.ModelName.set(tag, "named/transcend/model");
            ItemSlashBlade.RepairCount.set(tag, Integer.valueOf(100000));
            ItemSlashBlade.KillCount.set(tag, Integer.valueOf(1000000));
            ItemSlashBlade.ProudSoul.set(tag, Integer.valueOf(1000000));
            SpecialEffects.addEffect(blade, "SETranscend",1000);
            tag.setBoolean("Unbreakable", true);
            tran.setTagCompound(tag);
            tran.addEnchantment(Enchantments.INFINITY, 127);
            tran.addEnchantment(Enchantments.POWER, 127);
            tran.addEnchantment(Enchantments.PUNCH, 127);
            SlashBlade.registerCustomItemStack(name, tran);
        }
    }
}

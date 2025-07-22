package com.wjx.kablade.SPLight.blade.ordinary;

import com.wjx.kablade.AllWeapon.blade.items.Item_AwNamed;
import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.blades.recipe.SlashBladeRecipeModding;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.named.event.LoadEvent;
import mods.flammpfeil.slashblade.util.SlashBladeHooks;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import static com.wjx.kablade.Main.bladestr;

public class SL_Initial {
    public static String name = "wjx.splight.initial";

    public SL_Initial(){
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
        Item_AwNamed.CustomMaxDamage.set(Tag,1500);
        ItemSlashBlade.ModelName.set(Tag,"named/sange/sange");
        ItemSlashBlade.TextureName.set(Tag, "splight/splight/normal/texture");
        ItemSlashBlade.IsNoScabbard.set(Tag,true);
        ItemSlashBladeNamed.IsDefaultBewitched.set(Tag, true);
        ItemSlashBlade.BaseAttackModifier.set(Tag,7f);
        customblade.addEnchantment(Enchantments.LOOTING,1);
        customblade.addEnchantment(Enchantments.POWER,2);
        customblade.addEnchantment(Enchantments.UNBREAKING,1);
        //customblade.addEnchantment(Enchantments.FIRE_ASPECT,20);
        //customblade.addEnchantment(Enchantments.POWER,20);
        //customblade.addEnchantment(Enchantments.,20);
        SlashBlade.registerCustomItemStack(name,customblade);
        BladeLoader.SLBlades.add(name);
        //AchievementList.registerAchievement("name",customblade,null);

        ItemStack targetBlade = SlashBlade.findItemStack(bladestr,name,1);
        ItemStack initialBlade = new ItemStack(SlashBlade.weapon);
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(initialBlade);
        ItemSlashBlade.KillCount.set(tag,100);
        ItemSlashBlade.ProudSoul.set(tag,1000);
        initialBlade.addEnchantment(Enchantments.LOOTING,1);
        IRecipe recipe = new SlashBladeRecipeModding(new ResourceLocation(bladestr,"sp_initial"),targetBlade, initialBlade,
                new Object[]{
                        "QSQ",
                        "SKS",
                        "ISI",
                        'S',
                        SlashBlade.findItemStack(bladestr,"proudsoul",1),
                        'K',
                        initialBlade,
                        'Q',
                        new ItemStack(Items.GOLD_NUGGET),
                        'I',
                        new ItemStack(Blocks.CACTUS)
                });
        SlashBlade.addRecipe("sp_initial", recipe);
    }

    @SubscribeEvent
    public void postinit(LoadEvent.PostInitEvent event) {
        SlashBladeHooks.EventBus.register(this);
    }

}

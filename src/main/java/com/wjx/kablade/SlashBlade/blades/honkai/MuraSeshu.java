package com.wjx.kablade.SlashBlade.blades.honkai;

import com.wjx.kablade.SlashBlade.BladeLoader;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_HonkaiNamed;
import com.wjx.kablade.SlashBlade.blades.bladeitem.Item_KaNamed;
import com.wjx.kablade.init.BlockInit;
import com.wjx.kablade.init.ItemInit;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.RecipeAwakeBlade;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.named.event.LoadEvent;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.wjx.kablade.Main.bladestr;

public class MuraSeshu {
    String name = "wjx.blade.honkai.muraseshu";
    String key = "wjx.blade.honkai.muraseshu";

    public MuraSeshu(){
        MinecraftForge.EVENT_BUS.register(this);
    }

    private ItemStack customblade = new ItemStack(BladeLoader.ITEM_HONKAI_NAMED,1,0);

    private NBTTagCompound tag = new NBTTagCompound();
    {
        customblade.setTagCompound(tag);
    }
    @SubscribeEvent
    public void init(LoadEvent.InitEvent event) {


        //ItemStack customblade = new ItemStack(STAR,1,0);
        //NBTTagCompound tag = new NBTTagCompound();
        //customblade.setTagCompound(tag);

        //tag.setBoolean("Unbreakable",true);
        Item_HonkaiNamed.CurrentItemName.set(tag, name);
        Item_HonkaiNamed.CustomMaxDamage.set(tag, 450);

        //ItemSlashBlade.TextureName.set(tag, "kablade/BambooLumi/tex");
        //ItemSlashBlade.ModelName.set(tag, "kablade/BambooIron/blade");
        ItemSlashBlade.TextureName.set(tag, "kablade/Honkai/Muramasa/tex/SeshuMuramasa");
        ItemSlashBlade.ModelName.set(tag, "kablade/Honkai/Muramasa/mdlmura");

        ItemSlashBlade.setBaseAttackModifier(tag,2);
        ItemSlashBlade.AttackAmplifier.set(tag,8.0f);
        //ItemSlashBlade.KillCount.set(tag, 0);

        //customblade.addEnchantment(Enchantments.LOOTING,100);
        //customblade.addEnchantment(Enchantments.INFINITY,100);

        //tag.setInteger("HideFlags",1);




        //ItemSlashBlade.BaseAttackModifier.set(tag, 32768.0F);
        //ItemSlashBlade.setBaseAttackModifier(tag,32768.0F);
        customblade.getTagCompound().setFloat("baseAttackModifier",8.0F);
        //ItemSlashBlade.SpecialAttackType.set(tag, 257);
        Item_HonkaiNamed.IsDefaultBewitched.set(tag, false);
        ItemSlashBladeNamed.NamedBlades.add(this.name);
        ItemSlashBlade.StandbyRenderType.set(tag, 1);
        SlashBlade.registerCustomItemStack(this.name, customblade);
        BladeLoader.NamedHonkai.add(name);
        ItemStack blackblade = SlashBlade.findItemStack(bladestr, name, 1);
        IRecipe recipe = new RecipeAwakeBlade(new ResourceLocation(bladestr,"muraseshu"),
                blackblade, ItemStack.EMPTY,
                new Object[]{
                        "  B",
                        " B ",
                        "A  ",
                        'A', Items.DIAMOND_SWORD,
                        'B', new ItemStack(Item.getItemFromBlock(Blocks.IRON_BLOCK)),
                });

        SlashBlade.addRecipe("muraseshu", recipe);
    }
}

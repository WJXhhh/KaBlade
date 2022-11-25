package com.wjx.kablade.init;

import com.wjx.kablade.objects.items.ItemBase;
import com.wjx.kablade.objects.items.NormalTools;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.common.util.EnumHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.wjx.kablade.Main.TABKABLADE;
import static com.wjx.kablade.Main.TABKABLADE_ORE;

public class ItemInit {
    public static List<Item> ITEMS = new ArrayList<>();


    public static Item RIMMED_EARTH_STICK = new ItemBase("rimmed_earth_stick",TABKABLADE);

    public static Item CRUDE_CHROMOLY = new ItemBase("crude_chromoly",TABKABLADE);
    public static Item GRAVITY_NUGGET = new ItemBase("gravity_nugget",TABKABLADE){
        @Override
        public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
            super.addInformation(stack, worldIn, tooltip, flagIn);
            tooltip.add(I18n.translateToLocal("info.item.gravity_nugget"));
        }
    };
    public static Item GRAVITY_CRYSTAL = new ItemBase("gravity_crystal",TABKABLADE){
        @Override
        public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
            super.addInformation(stack, worldIn, tooltip, flagIn);
            tooltip.add(I18n.translateToLocal("info.item.gravity_crystal"));
        }
    };

    public static Item CHROMIUM_INGOT = new ItemBase("chromium_ingot",TABKABLADE);
    public static Item MOLYBDENUM_INGOT = new ItemBase("molybdenum_ingot",TABKABLADE);
    public static Item CHROMOLY_INGOT = new ItemBase("chromoly_ingot",TABKABLADE){
        @Override
        public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
            super.addInformation(stack, worldIn, tooltip, flagIn);
            tooltip.add(I18n.translateToLocal("info.item.chromoly_ingot"));
        }
    };;

    public static Item.ToolMaterial CHROMIUM_MATERIAL= EnumHelper.addToolMaterial("KABLADE_CHROMIUM",2, 800, 6.8F, 2.6F, 17).setRepairItem(new ItemStack(CHROMIUM_INGOT,1));
    public static Item.ToolMaterial MOLYBDENUM_MATERIAL= EnumHelper.addToolMaterial("KABLADE_MOLYBDENUM",2, 700, 7F, 2.4F, 20).setRepairItem(new ItemStack(MOLYBDENUM_INGOT,1));
    public static Item.ToolMaterial CHROMOLY_MATERIAL = EnumHelper.addToolMaterial("KABLADE_CHROMOLY",2,820,7.2f,2.8f,22).setRepairItem(new ItemStack(CHROMOLY_INGOT,1));

    public static Item CHROMIUM_SWORD = new NormalTools.ToolSword("chromium_sword", CHROMIUM_MATERIAL,TABKABLADE);
    public static Item CHROMIUM_AXE = new NormalTools.ToolAxe("chromium_axe", CHROMIUM_MATERIAL,TABKABLADE);
    public static Item CHROMIUM_PICKAXE = new NormalTools.ToolPickaxe("chromium_pickaxe", CHROMIUM_MATERIAL,TABKABLADE);
    public static Item CHROMIUM_HOE = new NormalTools.ToolHoe("chromium_hoe", CHROMIUM_MATERIAL,TABKABLADE);
    public static Item MOLYBDENUM_SWORD = new NormalTools.ToolSword("molybdenum_sword", MOLYBDENUM_MATERIAL,TABKABLADE);
    public static Item MOLYBDENUM_AXE = new NormalTools.ToolAxe("molybdenum_axe", MOLYBDENUM_MATERIAL,TABKABLADE);
    public static Item MOLYBDENUM_PICKAXE = new NormalTools.ToolPickaxe("molybdenum_pickaxe", MOLYBDENUM_MATERIAL,TABKABLADE);
    public static Item MOLYBDENUM_HOE = new NormalTools.ToolHoe("molybdenum_hoe", MOLYBDENUM_MATERIAL,TABKABLADE);
    public static Item CHROMOLY_SWORD = new NormalTools.ToolSword("chromoly_sword",CHROMOLY_MATERIAL,TABKABLADE);
    public static Item CHROMOLY_AXE = new NormalTools.ToolAxe("chromoly_axe",CHROMOLY_MATERIAL,TABKABLADE);
    public static Item CHROMOLY_PICKAXE = new NormalTools.ToolPickaxe("chromoly_pickaxe",CHROMOLY_MATERIAL,TABKABLADE);
    public static Item CHROMOLY_HOE = new NormalTools.ToolHoe("chromoly_hoe",CHROMOLY_MATERIAL,TABKABLADE);


    //TempIcon
    public static Item ICON_MAIN = new ItemBase("main_icon");
    public static Item ICON_NOTED = new ItemBase("noted_icon");
    public static Item ICON_HONKAI = new ItemBase("honkai_icon");
    public static Item ICON_GOD= new ItemBase("god_icon");
    public static Item ICON_ORE = new ItemBase("ore_icon");

}

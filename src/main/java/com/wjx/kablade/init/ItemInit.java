package com.wjx.kablade.init;

import com.wjx.kablade.objects.items.ItemBase;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.List;

import static com.wjx.kablade.Main.TABKABLADE;

public class ItemInit {
    public static List<Item> ITEMS = new ArrayList<>();

    public static Item RIMMED_EARTH_STICK = new ItemBase("rimmed_earth_stick",TABKABLADE);

    //TempIcon
    public static Item ICON_MAIN = new ItemBase("main_icon");
    public static Item ICON_NOTED = new ItemBase("noted_icon");
    public static Item ICON_HONKAI = new ItemBase("honkai_icon");
    public static Item ICON_GOD= new ItemBase("god_icon");

}

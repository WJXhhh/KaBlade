package com.wjx.kablade;

import com.wjx.kablade.init.BlockInit;
import com.wjx.kablade.init.ItemInit;
import net.minecraftforge.oredict.OreDictionary;

public class OreDicHandler {
    public static void registerOreDic(){
        OreDictionary.registerOre("oreChromium", BlockInit.CHROMIUM_ORE);
        OreDictionary.registerOre("oreMolybdenite", BlockInit.MOLYBDENITE);

        OreDictionary.registerOre("ingotChromium", ItemInit.CHROMIUM_INGOT);
        OreDictionary.registerOre("ingotMolybdenum", ItemInit.MOLYBDENUM_INGOT);
        OreDictionary.registerOre("ingotChromoly",ItemInit.CHROMOLY_INGOT);
    }
}

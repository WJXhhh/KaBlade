package com.wjx.kablade.gui.config;

import com.wjx.kablade.Main;
import com.wjx.kablade.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ConfigGuiFactory implements IModGuiFactory {

    public static class ConfigGui extends GuiConfig {
        public ConfigGui(GuiScreen parentScreen) {

            super(parentScreen, getConfigElements(), Main.MODID,false,false,"Kablade Cfg");
        }

        private static List<IConfigElement> getConfigElements(){
            List<IConfigElement> list = new ArrayList<IConfigElement>();

            for(String categoryName : ModConfig.config.getCategoryNames()){
                list.add(
                        new ConfigElement(ModConfig.config.getCategory(categoryName))
                );
            }

            return list;
        }

    }



    @Override
    public void initialize(Minecraft minecraftInstance) {

    }

    @Override
    public boolean hasConfigGui() {
        return true;
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        return new ConfigGui(parentScreen);
    }


    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

}

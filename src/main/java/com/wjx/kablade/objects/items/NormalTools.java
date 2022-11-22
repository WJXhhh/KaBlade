package com.wjx.kablade.objects.items;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ItemInit;
import com.wjx.kablade.util.interfaces.IHasModel;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.*;

public class NormalTools {
    public static class ToolHoe extends ItemHoe implements IHasModel{
        public ToolHoe(String name, ToolMaterial material, CreativeTabs tabs){
            super(material);
            setUnlocalizedName(name);
            setRegistryName(name);
            setCreativeTab(tabs);

            ItemInit.ITEMS.add(this);

        }
        @Override
        public void registerModels(){
            Main.proxy.registerItemRenderer(this,0,"inventory");

        }
    }
    public static class ToolAxe extends ItemAxe implements IHasModel {
        public ToolAxe(String name, ToolMaterial material, CreativeTabs tabs){
            super(material,material.getAttackDamage()+5.5f,-3f);
            setUnlocalizedName(name);
            setRegistryName(name);
            setCreativeTab(tabs);

            ItemInit.ITEMS.add(this);

        }
        @Override
        public void registerModels(){
            Main.proxy.registerItemRenderer(this,0,"inventory");

        }
    }
    public static class ToolPickaxe extends ItemPickaxe implements IHasModel{
        public ToolPickaxe(String name, ToolMaterial material, CreativeTabs tabs){
            super(material);
            setUnlocalizedName(name);
            setRegistryName(name);
            setCreativeTab(tabs);

            ItemInit.ITEMS.add(this);

        }
        @Override
        public void registerModels(){
            Main.proxy.registerItemRenderer(this,0,"inventory");

        }
    }
    public static class ToolSword extends ItemSword implements IHasModel{
        public ToolSword(String name, ToolMaterial material, CreativeTabs tabs){
            super(material);
            setUnlocalizedName(name);
            setRegistryName(name);
            setCreativeTab(tabs);

            ItemInit.ITEMS.add(this);

        }
        @Override
        public void registerModels(){
            Main.proxy.registerItemRenderer(this,0,"inventory");

        }
    }

    public static class ToolShovel extends ItemSpade implements IHasModel{
        public ToolShovel(String name, ToolMaterial material, CreativeTabs tabs){
            super(material);
            setUnlocalizedName(name);
            setRegistryName(name);
            setCreativeTab(tabs);

            ItemInit.ITEMS.add(this);

        }
        @Override
        public void registerModels(){
            Main.proxy.registerItemRenderer(this,0,"inventory");

        }
    }

}

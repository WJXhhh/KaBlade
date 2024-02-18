package com.wjx.kablade.init;

import com.wjx.kablade.objects.items.ItemBase;
import com.wjx.kablade.objects.items.NormalTools;
import com.wjx.kablade.util.KaBladeEntityProperties;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.common.util.EnumHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.wjx.kablade.Main.TABKABLADE;

public class ItemInit {
    public static List<Item> ITEMS = new ArrayList<>();


    public static Item RIMMED_EARTH_STICK = new ItemBase("rimmed_earth_stick",TABKABLADE);
    public static Item STURDY_GLASS_STICK = new ItemBase("sturdy_glass_stick",TABKABLADE);

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
    public static Item AURORA_FRAGMENT =new ItemBase("aurora_fragment",TABKABLADE){
        @Override
        public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
            super.addInformation(stack, worldIn, tooltip, flagIn);
            tooltip.add(I18n.translateToLocal("info.item.aurora_fragment"));
        }
    };

    public static Item THUNDER_CRYSTAL = new ItemBase("thunder_crystal",TABKABLADE){
        @Override
        public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
            super.addInformation(stack, worldIn, tooltip, flagIn);
            tooltip.add(I18n.translateToLocal("info.item.thunder_crystal"));
        }

        @Nonnull
        @Override
        public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
            int pX =pos.getX();
            int pY = pos.getY();
            int pZ = pos.getZ();
            if (!worldIn.isRemote){
                worldIn.addWeatherEffect(new EntityLightningBolt(worldIn,pX,pY,pZ,false));
                worldIn.createExplosion(player,pX,pY +1,pZ,2f,true);
            }
            player.getHeldItem(hand).shrink(1);
            return super.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
        }

        @Override
        public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
            if (entity instanceof EntityLivingBase){
                if (!entity.world.isRemote){
                    KaBladeEntityProperties.getPropCompound(entity).setInteger(KaBladeEntityProperties.THUNDER_CRYSTAL_ATTACK,100);
                    stack.shrink(1);
                }
            }
            return super.onLeftClickEntity(stack, player, entity);
        }
    };


    public static Item PETAL=new ItemBase("petal",TABKABLADE){
        @Override
        public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
            super.addInformation(stack, worldIn, tooltip, flagIn);
            tooltip.add(I18n.translateToLocal("info.item.petal"));
        }

        @Override
        public int getItemBurnTime(ItemStack itemStack) {
            return 50;
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
    public static Item AURORA_METAL_INGOT = new ItemBase("aurora_metal_ingot",TABKABLADE){
        @Override
        public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
            super.addInformation(stack, worldIn, tooltip, flagIn);
            tooltip.add(I18n.translateToLocal("info.item.aurora_metal_ingot"));
        }
    };

    public static Item IRON_COIL = new ItemBase("iron_coil",TABKABLADE);

    public static Item.ToolMaterial CHROMIUM_MATERIAL= EnumHelper.addToolMaterial("KABLADE_CHROMIUM",2, 800, 6.8F, 2.6F, 17).setRepairItem(new ItemStack(CHROMIUM_INGOT,1));
    public static Item.ToolMaterial MOLYBDENUM_MATERIAL= EnumHelper.addToolMaterial("KABLADE_MOLYBDENUM",2, 700, 7F, 2.4F, 20).setRepairItem(new ItemStack(MOLYBDENUM_INGOT,1));
    public static Item.ToolMaterial CHROMOLY_MATERIAL = EnumHelper.addToolMaterial("KABLADE_CHROMOLY",2,820,7.2f,2.8f,22).setRepairItem(new ItemStack(CHROMOLY_INGOT,1));
    public static Item.ToolMaterial AURORA_METAL = EnumHelper.addToolMaterial("KABLADE_AURORA_METAL",4,1000,7.5f,3.2f,25).setRepairItem(new ItemStack(AURORA_METAL_INGOT,1));

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
    public static Item AURORA_METAL_SWORD = new NormalTools.ToolSword("aurora_metal_sword",AURORA_METAL,TABKABLADE);
    public static Item AURORA_METAL_PICKAXE = new NormalTools.ToolPickaxe("aurora_metal_pickaxe",AURORA_METAL,TABKABLADE);
    public static Item AURORA_METAL_AXE = new NormalTools.ToolAxe("aurora_metal_axe",AURORA_METAL,TABKABLADE);
    public static Item AURORA_METAL_HOE = new NormalTools.ToolHoe("aurora_metal_hoe",AURORA_METAL,TABKABLADE);


    //TempIcon
    public static Item ICON_MAIN = new ItemBase("main_icon");
    public static Item ICON_NOTED = new ItemBase("noted_icon");
    public static Item ICON_HONKAI = new ItemBase("honkai_icon");
    public static Item ICON_GOD= new ItemBase("god_icon");
    public static Item ICON_ORE = new ItemBase("ore_icon");

}

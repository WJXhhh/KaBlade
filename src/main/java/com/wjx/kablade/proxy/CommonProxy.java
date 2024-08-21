package com.wjx.kablade.proxy;

import com.wjx.kablade.AllWeapon.awlib.ArrayLib;
import com.wjx.kablade.Main;
import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.capability.CapabilityLoader;
import com.wjx.kablade.event.WorldEvent;
import com.wjx.kablade.init.EnchantmentInit;
import com.wjx.kablade.network.*;
import com.wjx.kablade.AllWeapon.event.AWWorldEvent;
import com.wjx.kablade.util.BladeStandHurtManager;
import mods.flammpfeil.slashblade.ItemSlashBladeNamed;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.entity.EntityBladeStand;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.block.*;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

import static com.wjx.kablade.Main.*;

public class CommonProxy{

    public void registerMessage(){
        PACKET_HANDLER.registerMessage(MessageRemoteLightingHandler.class, MessageRemoteLighting.class,0, Side.CLIENT);
        PACKET_HANDLER.registerMessage(MessageHandlerSlashPotion.class, MessageSlashPotion.class,2,Side.CLIENT);
        PACKET_HANDLER.registerMessage(MessageHandlerSpawnParticle.class,MessageSpawnParticle.class,3,Side.CLIENT);
        PACKET_HANDLER.registerMessage(MessageHandlerUpdateKaBladeProp.class,MessageUpdateKaBladeProp.class,4,Side.CLIENT);
        PACKET_HANDLER.registerMessage(MessageHandlerUpdateKaBladePlayerProp.class,MessageUpdateKaBladePlayerProp.class,5,Side.CLIENT);
        PACKET_HANDLER.registerMessage(MessageHandlerResetSend.class,MessageResetSend.class,6,Side.CLIENT);
        PACKET_HANDLER.registerMessage(MessageHandlerDizuiKuo.class,MessageDizuiKuo.class,7,Side.CLIENT);
        PACKET_HANDLER.registerMessage(MessageHandlerMagChaosBladeEffectUpdate.class, MessageMagChaosBladeEffectUpdate.class,8,Side.CLIENT);
        PACKET_HANDLER.registerMessage(MessageHandlerSpawnColorfulSmoke.class, MessageSpawnColorfulSmoke.class,9,Side.CLIENT);
    }

    public void registerItemRenderer(Item item, int meta, String id){

    }

    public void init(FMLInitializationEvent event) {

        new WorldEvent();
        if (Main.EnableAllWeapon) {
            new AWWorldEvent();

            BladeStandHurtManager.events.add(new BladeStandHurtManager.BladeStandHurtEvent() {
                @Override
                public void run(EntityBladeStand curEntity, DamageSource damageSource) {
                    if (damageSource.isFireDamage()) {
                        EntityBladeStand stand = (EntityBladeStand) curEntity;
                        int type = stand.getStandType();//耀魂碎片:0 耀魂铁锭:1 耀魂宝珠:2 破碎的耀魂:3
                        int dimension = stand.dimension;
                        ItemStack blade = stand.getBlade();
                        World world = stand.world;
                        NBTTagCompound tag = blade.getTagCompound();
                        ItemStack targetBlade = SlashBlade.findItemStack(bladestr,"slashbladeNamed",1);
                        if (blade.getTranslationKey().equals(targetBlade.getTranslationKey())) {//流刃若火
                            if (world.getBlockState(new BlockPos(Math.floor(stand.posX),Math.round(stand.posY), Math.floor(stand.posZ))).getBlock().equals(Blocks.LAVA) && type == 1 && dimension == -1) {
                                if (mods.flammpfeil.slashblade.item.ItemSlashBlade.RepairCount.get(tag) >= 50) {
                                    NBTTagList list = blade.getEnchantmentTagList();
                                    boolean flag = false;
                                    for (int i = 0; i < list.tagCount(); i++) {
                                        NBTTagCompound nbtTagCompound = ((NBTTagCompound) (list.get(i)));
                                        if (nbtTagCompound.getShort("id") == 1 && nbtTagCompound.getShort("lvl") == 4) {
                                            flag = true;
                                        }
                                    }
                                    if (flag) {
                                        ItemStack res = SlashBlade.findItemStack(bladestr, "wjx.allweapon.liurrh", 1);
                                        //logger.warn(res.getDisplayName());
                                        NBTTagCompound rt = res.getTagCompound();
                                        mods.flammpfeil.slashblade.item.ItemSlashBlade.KillCount.set(rt, mods.flammpfeil.slashblade.item.ItemSlashBlade.KillCount.get(tag));
                                        mods.flammpfeil.slashblade.item.ItemSlashBlade.ProudSoul.set(rt, mods.flammpfeil.slashblade.item.ItemSlashBlade.ProudSoul.get(tag));
                                        mods.flammpfeil.slashblade.item.ItemSlashBlade.RepairCount.set(rt, ItemSlashBlade.RepairCount.get(tag));
                                        stand.setBlade(res);
                                    }
                                }
                            }

                        }
                    }
                }

            });



            BladeStandHurtManager.events.add(new BladeStandHurtManager.BladeStandHurtEvent() {
                @Override
                public void run(EntityBladeStand curEntity, DamageSource damageSource) {
                    if(damageSource.getTrueSource() instanceof EntityPlayer && curEntity.hasBlade()){
                        EntityPlayer player = (EntityPlayer) damageSource.getTrueSource();
                        ItemStack blade = curEntity.getBlade();
                        ItemStack targetBlade = SlashBlade.findItemStack(bladestr,"slashbladeNamed",1);
                        NBTTagCompound tag = blade.getTagCompound();
                        if(player.getHeldItemMainhand().getItem().equals(Items.DIAMOND_SWORD)&&blade.getTranslationKey().equals(targetBlade.getTranslationKey())&&curEntity.getStandType()==1)
                        {
                            Class[][] rec = new Class[][]{
                                    {BlockJukebox.class, BlockAir.class, BlockLilyPad.class},
                                    {BlockAir.class, BlockGlowstone.class, BlockAir.class},
                                    {BlockCactus.class, BlockAir.class, BlockIce.class}

                            };
                            Vec3d pos =new Vec3d(Math.floor(curEntity.posX),Math.round(curEntity.posY-1), Math.floor(curEntity.posZ));
                            boolean flag = false;
                            World world = curEntity.world;
                            Block ur = world.getBlockState(new BlockPos(pos.x+1, pos.y, pos.z+1)).getBlock();
                            Block ul = world.getBlockState(new BlockPos(pos.x+1, pos.y, pos.z-1)).getBlock();
                            Block dr = world.getBlockState(new BlockPos(pos.x-1, pos.y, pos.z+1)).getBlock();
                            Block dl = world.getBlockState(new BlockPos(pos.x-1, pos.y, pos.z-1)).getBlock();
                            Block ud =world.getBlockState(new BlockPos(pos.x, pos.y, pos.z)).getBlock();
                            if(ur.getClass()==rec[0][2]&&ul.getClass()==rec[0][0]&&dr.getClass()==rec[2][2]&&dl.getClass()==rec[2][0]&&ud.getClass()==rec[1][1]){
                                flag=true;
                            }

                            Class[][] rec1 = ArrayLib.RotateClockwise(rec,3,3);
                            if(ur.getClass()==rec1[0][2]&&ul.getClass()==rec1[0][0]&&dr.getClass()==rec1[2][2]&&dl.getClass()==rec1[2][0]&&ud.getClass()==rec[1][1]){
                                flag=true;
                            }

                            Class[][] rec2 = ArrayLib.RotateClockwise(rec1,3,3);
                            if(ur.getClass()==rec2[0][2]&&ul.getClass()==rec2[0][0]&&dr.getClass()==rec2[2][2]&&dl.getClass()==rec2[2][0]&&ud.getClass()==rec[1][1]){
                                flag=true;
                            }

                            Class[][] rec3 = ArrayLib.RotateClockwise(rec2,3,3);
                            if(ur.getClass()==rec3[0][2]&&ul.getClass()==rec3[0][0]&&dr.getClass()==rec3[2][2]&&dl.getClass()==rec3[2][0]&&ud.getClass()==rec[1][1]){
                                flag=true;
                            }

                            if(flag && ItemSlashBlade.RepairCount.get(blade.getTagCompound())>=50&& ItemSlashBlade.ProudSoul.get(blade.getTagCompound())>=200){
                                ItemStack res = SlashBlade.findItemStack(bladestr, "wjx.allweapon.chanshizhe", 1);
                                world.setBlockState(new BlockPos(pos.x+1, pos.y, pos.z+1),Blocks.AIR.getDefaultState());
                                world.setBlockState(new BlockPos(pos.x+1, pos.y, pos.z-1),Blocks.AIR.getDefaultState());
                                world.setBlockState(new BlockPos(pos.x-1, pos.y, pos.z+1),Blocks.AIR.getDefaultState());
                                world.setBlockState(new BlockPos(pos.x-1, pos.y, pos.z-1),Blocks.AIR.getDefaultState());
                                world.setBlockState(new BlockPos(pos.x, pos.y, pos.z),Blocks.AIR.getDefaultState());
                                player.getHeldItemMainhand().shrink(1);
                                NBTTagCompound rt = res.getTagCompound();
                                mods.flammpfeil.slashblade.item.ItemSlashBlade.KillCount.set(rt, mods.flammpfeil.slashblade.item.ItemSlashBlade.KillCount.get(tag));
                                mods.flammpfeil.slashblade.item.ItemSlashBlade.ProudSoul.set(rt, mods.flammpfeil.slashblade.item.ItemSlashBlade.ProudSoul.get(tag));
                                mods.flammpfeil.slashblade.item.ItemSlashBlade.RepairCount.set(rt, ItemSlashBlade.RepairCount.get(tag));
                                curEntity.setBlade(res);
                            }
                        }

                    }
                }
            });

            BladeStandHurtManager.events.add(new BladeStandHurtManager.BladeStandHurtEvent() {
                @Override
                public void run(EntityBladeStand curEntity, DamageSource damageSource) {
                    if(damageSource.getTrueSource() instanceof EntityCreeper && curEntity.hasBlade()){
                        ItemStack blade = curEntity.getBlade();
                        ItemStack targetBlade = SlashBlade.findItemStack(bladestr,"slashbladeNamed",1);
                        NBTTagCompound tag = blade.getTagCompound();
                        if(blade.getTranslationKey().equals(targetBlade.getTranslationKey())&&curEntity.getStandType()==1)
                        {
                            Class[][] rec = new Class[][]{
                                    {BlockObsidian.class, BlockAir.class, BlockObsidian.class},
                                    {BlockAir.class, BlockObsidian.class, BlockAir.class},
                                    {BlockObsidian.class, BlockAir.class, BlockObsidian.class}

                            };
                            Vec3d pos =new Vec3d(Math.floor(curEntity.posX),Math.round(curEntity.posY-1), Math.floor(curEntity.posZ));
                            boolean flag = false;
                            World world = curEntity.world;
                            Block ur = world.getBlockState(new BlockPos(pos.x+1, pos.y, pos.z+1)).getBlock();
                            Block ul = world.getBlockState(new BlockPos(pos.x+1, pos.y, pos.z-1)).getBlock();
                            Block dr = world.getBlockState(new BlockPos(pos.x-1, pos.y, pos.z+1)).getBlock();
                            Block dl = world.getBlockState(new BlockPos(pos.x-1, pos.y, pos.z-1)).getBlock();
                            Block ud =world.getBlockState(new BlockPos(pos.x, pos.y, pos.z)).getBlock();
                            if(ur.getClass()==rec[0][2]&&ul.getClass()==rec[0][0]&&dr.getClass()==rec[2][2]&&dl.getClass()==rec[2][0]&&ud.getClass()==rec[1][1]){
                                flag=true;
                            }



                            if(flag && ItemSlashBlade.RepairCount.get(blade.getTagCompound())>=50&& EnchantmentHelper.getEnchantmentLevel(Enchantments.PROTECTION,blade)>=2){
                                ItemStack res = SlashBlade.findItemStack(bladestr, "wjx.allweapon.baiyueding", 1);
                                world.setBlockState(new BlockPos(pos.x+1, pos.y, pos.z+1),Blocks.AIR.getDefaultState());
                                world.setBlockState(new BlockPos(pos.x+1, pos.y, pos.z-1),Blocks.AIR.getDefaultState());
                                world.setBlockState(new BlockPos(pos.x-1, pos.y, pos.z+1),Blocks.AIR.getDefaultState());
                                world.setBlockState(new BlockPos(pos.x-1, pos.y, pos.z-1),Blocks.AIR.getDefaultState());
                                world.setBlockState(new BlockPos(pos.x, pos.y, pos.z),Blocks.AIR.getDefaultState());

                                NBTTagCompound rt = res.getTagCompound();
                                mods.flammpfeil.slashblade.item.ItemSlashBlade.KillCount.set(rt, mods.flammpfeil.slashblade.item.ItemSlashBlade.KillCount.get(tag));
                                mods.flammpfeil.slashblade.item.ItemSlashBlade.ProudSoul.set(rt, mods.flammpfeil.slashblade.item.ItemSlashBlade.ProudSoul.get(tag));
                                mods.flammpfeil.slashblade.item.ItemSlashBlade.RepairCount.set(rt, ItemSlashBlade.RepairCount.get(tag));
                                curEntity.setBlade(res);
                            }
                        }

                    }
                }
            });

            BladeStandHurtManager.events.add(new BladeStandHurtManager.BladeStandHurtEvent() {
                @Override
                public void run(EntityBladeStand curEntity, DamageSource damageSource) {
                    if(damageSource.getTrueSource() instanceof EntityCreeper && curEntity.hasBlade()){
                        ItemStack blade = curEntity.getBlade();
                        ItemStack targetBlade = SlashBlade.findItemStack(bladestr,"slashbladeNamed",1);
                        NBTTagCompound tag = blade.getTagCompound();
                        if(blade.getTranslationKey().equals(targetBlade.getTranslationKey())&&curEntity.getStandType()==1)
                        {
                            Class[][] rec = new Class[][]{
                                    { BlockAir.class, BlockObsidian.class,  BlockAir.class},
                                    {BlockObsidian.class, BlockSnowBlock.class, BlockObsidian.class},
                                    { BlockAir.class, BlockObsidian.class,  BlockAir.class}

                            };
                            Vec3d pos =new Vec3d(Math.floor(curEntity.posX),Math.round(curEntity.posY-1), Math.floor(curEntity.posZ));
                            boolean flag = false;
                            World world = curEntity.world;
                            Block ur = world.getBlockState(new BlockPos(pos.x+1, pos.y, pos.z)).getBlock();
                            Block ul = world.getBlockState(new BlockPos(pos.x, pos.y, pos.z-1)).getBlock();
                            Block dr = world.getBlockState(new BlockPos(pos.x-1, pos.y, pos.z)).getBlock();
                            Block dl = world.getBlockState(new BlockPos(pos.x, pos.y, pos.z-1)).getBlock();
                            Block ud =world.getBlockState(new BlockPos(pos.x, pos.y, pos.z)).getBlock();
                            if(ur.getClass()==rec[0][1]&&ul.getClass()==rec[1][0]&&dr.getClass()==rec[1][2]&&dl.getClass()==rec[2][1]&&ud.getClass()==rec[1][1]){
                                flag=true;

                            }





                            if(flag && ItemSlashBlade.RepairCount.get(blade.getTagCompound())>=50&& EnchantmentHelper.getEnchantmentLevel(Enchantments.PROTECTION,blade)>=2){
                                ItemStack res = SlashBlade.findItemStack(bladestr, "wjx.allweapon.baiqiyue", 1);
                                world.setBlockState(new BlockPos(pos.x+1, pos.y, pos.z),Blocks.AIR.getDefaultState());
                                world.setBlockState(new BlockPos(pos.x, pos.y, pos.z-1),Blocks.AIR.getDefaultState());
                                world.setBlockState(new BlockPos(pos.x, pos.y, pos.z+1),Blocks.AIR.getDefaultState());
                                world.setBlockState(new BlockPos(pos.x-1, pos.y, pos.z),Blocks.AIR.getDefaultState());
                                world.setBlockState(new BlockPos(pos.x, pos.y, pos.z),Blocks.AIR.getDefaultState());

                                NBTTagCompound rt = res.getTagCompound();
                                mods.flammpfeil.slashblade.item.ItemSlashBlade.KillCount.set(rt, mods.flammpfeil.slashblade.item.ItemSlashBlade.KillCount.get(tag));
                                mods.flammpfeil.slashblade.item.ItemSlashBlade.ProudSoul.set(rt, mods.flammpfeil.slashblade.item.ItemSlashBlade.ProudSoul.get(tag));
                                mods.flammpfeil.slashblade.item.ItemSlashBlade.RepairCount.set(rt, ItemSlashBlade.RepairCount.get(tag));
                                curEntity.setBlade(res);
                            }
                        }

                    }
                }
            });

            BladeStandHurtManager.events.add(new BladeStandHurtManager.BladeStandHurtEvent() {
                @Override
                public void run(EntityBladeStand curEntity, DamageSource damageSource) {
                    if(damageSource.getTrueSource() instanceof EntityCreeper && curEntity.hasBlade()){
                        ItemStack blade = curEntity.getBlade();
                        ItemStack targetBlade = SlashBlade.findItemStack(bladestr,"slashbladeNamed",1);
                        NBTTagCompound tag = blade.getTagCompound();
                        if(blade.getTranslationKey().equals(targetBlade.getTranslationKey())&&curEntity.getStandType()==1)
                        {
                            Class[][] rec = new Class[][]{
                                    { BlockAir.class, BlockTNT.class,  BlockAir.class},
                                    {BlockTNT.class, BlockObsidian.class, BlockTNT.class},
                                    { BlockAir.class, BlockTNT.class,  BlockAir.class}

                            };
                            Vec3d pos =new Vec3d(Math.floor(curEntity.posX),Math.round(curEntity.posY-1), Math.floor(curEntity.posZ));
                            boolean flag = false;
                            World world = curEntity.world;
                            Block ur = world.getBlockState(new BlockPos(pos.x+1, pos.y, pos.z)).getBlock();
                            Block ul = world.getBlockState(new BlockPos(pos.x, pos.y, pos.z-1)).getBlock();
                            Block dr = world.getBlockState(new BlockPos(pos.x-1, pos.y, pos.z)).getBlock();
                            Block dl = world.getBlockState(new BlockPos(pos.x, pos.y, pos.z-1)).getBlock();
                            Block ud =world.getBlockState(new BlockPos(pos.x, pos.y, pos.z)).getBlock();
                            if(ur.getClass()==rec[0][1]&&ul.getClass()==rec[1][0]&&dr.getClass()==rec[1][2]&&dl.getClass()==rec[2][1]&&ud.getClass()==rec[1][1]){
                                flag=true;
                            }



                            if(flag && ItemSlashBlade.RepairCount.get(blade.getTagCompound())>=50&& EnchantmentHelper.getEnchantmentLevel(Enchantments.SHARPNESS,blade)>=5){
                                ItemStack res = SlashBlade.findItemStack(bladestr, "wjx.allweapon.baishoujianwang", 1);
                                world.setBlockState(new BlockPos(pos.x+1, pos.y, pos.z+1),Blocks.AIR.getDefaultState());
                                world.setBlockState(new BlockPos(pos.x+1, pos.y, pos.z-1),Blocks.AIR.getDefaultState());
                                world.setBlockState(new BlockPos(pos.x-1, pos.y, pos.z+1),Blocks.AIR.getDefaultState());
                                world.setBlockState(new BlockPos(pos.x-1, pos.y, pos.z-1),Blocks.AIR.getDefaultState());
                                world.setBlockState(new BlockPos(pos.x, pos.y, pos.z),Blocks.AIR.getDefaultState());

                                NBTTagCompound rt = res.getTagCompound();
                                mods.flammpfeil.slashblade.item.ItemSlashBlade.KillCount.set(rt, mods.flammpfeil.slashblade.item.ItemSlashBlade.KillCount.get(tag));
                                mods.flammpfeil.slashblade.item.ItemSlashBlade.ProudSoul.set(rt, mods.flammpfeil.slashblade.item.ItemSlashBlade.ProudSoul.get(tag));
                                mods.flammpfeil.slashblade.item.ItemSlashBlade.RepairCount.set(rt, ItemSlashBlade.RepairCount.get(tag));
                                curEntity.setBlade(res);
                            }
                        }

                    }
                }
            });



        }
    }

    public void preInit(FMLPreInitializationEvent event) {


        MinecraftForge.EVENT_BUS.register(this);
        EnchantmentInit.registerEnchantments();
        new CapabilityLoader(event);
        if(Loader.isModLoaded("flammpfeil.slashblade")){
            BladeProxy.CommonLoader(this);
        }
        registerMessage();
    }

    public void postInit(FMLPostInitializationEvent event){

    }
}

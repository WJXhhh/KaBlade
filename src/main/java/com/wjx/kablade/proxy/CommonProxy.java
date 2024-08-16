package com.wjx.kablade.proxy;

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
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

import static com.wjx.kablade.Main.PACKET_HANDLER;
import static com.wjx.kablade.Main.bladestr;

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
                        if (blade.getItem().getClass() == ItemSlashBladeNamed.class) {//流刃若火
                            if (world.getBlockState(new BlockPos(Math.floor(stand.posX),Math.round(stand.posY), Math.floor(stand.posZ))).getBlock().equals(Blocks.LAVA) && type == 1 && dimension == -1) {
                                if (mods.flammpfeil.slashblade.item.ItemSlashBlade.RepairCount.get(tag) >= 100) {
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

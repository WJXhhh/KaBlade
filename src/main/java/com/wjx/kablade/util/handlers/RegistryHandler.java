package com.wjx.kablade.util.handlers;

import com.wjx.kablade.init.BlockInit;
import com.wjx.kablade.init.ItemInit;
import com.wjx.kablade.util.interfaces.IHasModel;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.wjx.kablade.util.handlers.TileEntityHandler.registerTileEntity;

@Mod.EventBusSubscriber
public class RegistryHandler {
    @SubscribeEvent
    public static void onItemRegister(RegistryEvent.Register<Item> event){
        event.getRegistry().registerAll(ItemInit.ITEMS.toArray(new Item[0]));
    }

    @SubscribeEvent
    public static void onBlockRegister(RegistryEvent.Register<Block> event){
        event.getRegistry().registerAll(BlockInit.Blocks.toArray(new Block[0]));
        registerTileEntity();

    }


    @SubscribeEvent
    public static void onModelRegister(ModelRegistryEvent event){
        for (Item item : ItemInit.ITEMS){
            if(item instanceof IHasModel){
                ((IHasModel)item).registerModels();

            }
        }

        for (Block block : BlockInit.Blocks){

            if (block instanceof IHasModel){
                ((IHasModel)block).registerModels();

            }
        }

    }

}

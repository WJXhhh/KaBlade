package com.wjx.kablade.event;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.ModItems;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Drops Electro Signet from charged creepers, matching the 1.12.2 material source. */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ElectroSignetDropHandler {

    private ElectroSignetDropHandler() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof Creeper creeper && creeper.isPowered()) {
            creeper.spawnAtLocation(new ItemStack(ModItems.ELECTRO_SIGNET.get()));
        }
    }
}

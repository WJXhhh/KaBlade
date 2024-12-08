package com.wjx.kablade.ExSA.ability;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class EnderTeleportCanceller {
    public static final String CancelingTimesStr = "SB.exsa.WarpCancel";

    public static void setTeleportCancel(Entity target, int ticks) {
        if (!(target instanceof EntityEnderman)) {
            return;
        }
        target.getEntityData().setLong(CancelingTimesStr, target.world.getTotalWorldTime() + (long)ticks);
    }

    @SubscribeEvent
    public void EnderTeleportEventHandler(EnderTeleportEvent event) {
        EntityLivingBase target = event.getEntityLiving();
        if (target == null) {
            return;
        }
        NBTTagCompound tag = target.getEntityData();
        if (!tag.hasKey(CancelingTimesStr)) {
            return;
        }
        long timeout = tag.getLong(CancelingTimesStr);
        long now = target.world.getTotalWorldTime();
        if (timeout == 0L) {
            return;
        }
        if (now < timeout && timeout - now < 12000L) {
            event.setCanceled(true);
        } else {
            tag.removeTag(CancelingTimesStr);
        }
    }
}

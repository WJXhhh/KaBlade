package com.wjx.kablade.capability.inters;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

public interface IPotionInSlash {
    NBTTagCompound getSlashPotions();
    void setSlashPotions(NBTTagCompound compound);
}

package com.wjx.kablade.capability;

import com.wjx.kablade.capability.inters.IPotionInSlash;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CapabilityLoader {
    @CapabilityInject(IPotionInSlash.class)
    public static Capability<IPotionInSlash> SlashPotion;

    public CapabilityLoader(FMLPreInitializationEvent event)
    {
        CapabilityManager.INSTANCE.register(IPotionInSlash.class, new CapabilitySlashPotion.Storage(), CapabilitySlashPotion.Implementation.class);
    }
}

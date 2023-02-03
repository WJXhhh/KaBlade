package com.wjx.kablade.util.slash;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.named.event.LoadEvent;
import mods.flammpfeil.slashblade.specialeffect.ISpecialEffect;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import net.minecraftforge.common.MinecraftForge;

public class SlashInit {

    public static void init(){
        ItemSlashBlade.specialAttacks.put(Integer.valueOf(678), new Delete());
        MinecraftForge.EVENT_BUS.register(new SlashUpdateEvent());
        SlashBlade.InitEventBus.register(new LoaderSlash());
        SpecialEffects.register(new SETranscend());
    }
}

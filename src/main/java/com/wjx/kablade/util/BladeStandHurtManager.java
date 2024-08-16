package com.wjx.kablade.util;

import com.google.common.collect.Lists;
import mods.flammpfeil.slashblade.entity.EntityBladeStand;
import net.minecraft.util.DamageSource;

import java.util.List;

public class BladeStandHurtManager {
    public static List<BladeStandHurtEvent> events = Lists.newArrayList();

    public static abstract class BladeStandHurtEvent{
        public abstract void run(EntityBladeStand curEntity, DamageSource damageSource);
    }
}

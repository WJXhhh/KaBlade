package com.wjx.kablade.init;

import com.google.common.collect.Lists;
import com.wjx.kablade.potion.PotionFreeze;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.potion.Potion;

import java.util.ArrayList;
import java.util.UUID;

public class PotionInit {
    public static ArrayList<Potion> potions = Lists.newArrayList();

    public static UUID UUID_FREEZE = UUID.randomUUID();

    public static final Potion FREEZE = new PotionFreeze(0).registerPotionAttributeModifier(SharedMonsterAttributes.MOVEMENT_SPEED,UUID_FREEZE.toString(),-1d,2);
}

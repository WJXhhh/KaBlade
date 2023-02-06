package com.wjx.kablade.init;

import com.google.common.collect.Lists;
import com.wjx.kablade.potion.PotionFreeze;
import com.wjx.kablade.potion.PotionParaly;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.potion.Potion;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.UUID;

public class PotionInit {
    public static ArrayList<Potion> potions = Lists.newArrayList();

    public static UUID UUID_FREEZE = UUID.fromString("3fccf4fc-3ea9-366b-98e6-607f8dcec98c");
    public static UUID UUID_PARALY = UUID.fromString("3fccf4fc-3ea9-366b-98e6-607f8dcec98d");

    public static final Potion FREEZE = new PotionFreeze(0).registerPotionAttributeModifier(SharedMonsterAttributes.MOVEMENT_SPEED,UUID_FREEZE.toString(),-1d,2);
    public static final Potion PARALY = new PotionParaly().registerPotionAttributeModifier(SharedMonsterAttributes.MOVEMENT_SPEED,UUID_PARALY.toString(),-8,1);

    public static void registerPotions(){
        ForgeRegistries.POTIONS.registerAll(potions.toArray(new Potion[0]));
    }
}

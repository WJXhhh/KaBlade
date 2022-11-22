package com.wjx.kablade.init;

import com.wjx.kablade.Entity.EntitySummonSwordFree;
import com.wjx.kablade.Entity.EntityVorpalBlackHole;
import com.wjx.kablade.Entity.SummonBladeOfFrostBlade;
import com.wjx.kablade.Main;
import com.wjx.kablade.util.Reference;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.EntityRegistry;

public class EntityInit {
    public static void registerEntity(){
        registerEntity("summon_blade_of_frost_blade", SummonBladeOfFrostBlade.class,13700001,50);
        registerEntity("summon_blade_free", EntitySummonSwordFree.class,13700002,50);
        registerEntity("vorpal_black_hole", EntityVorpalBlackHole.class,13700003,50);
    }

    private static void registerEntitySpawn(Class<? extends Entity> entityClass, int spawnWeight, int min, int max, EnumCreatureType typeOfCreature, Biome... biomes)
    {
        if (EntityLiving.class.isAssignableFrom(entityClass))
        {
            Class<? extends EntityLiving> entityLivingClass = entityClass.asSubclass(EntityLiving.class);
            EntityRegistry.addSpawn(entityLivingClass, spawnWeight, min, max, typeOfCreature, biomes);
        }
    }

    private static void registerEntity(String name, Class<? extends Entity> entity, int id, int range, int color1, int color2){
        EntityRegistry.registerModEntity(new ResourceLocation(Reference.MODID + ":" + name),entity,name,id, Main.instance,range,1,true,color1,color2);
    }

    private static void registerEntity(String name, Class<? extends Entity> entity, int id, int range){
        EntityRegistry.registerModEntity(new ResourceLocation(Reference.MODID + ":" + name),entity,name,id, Main.instance,range,1,true);
    }
}

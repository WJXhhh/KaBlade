package com.wjx.kablade.init;

import com.wjx.kablade.Entity.*;
import com.wjx.kablade.ExSA.entity.*;
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
        registerEntity("raikiri_blade", EntityRaikiriBlade.class,13700004,50);
        registerEntity("summoned_sword_base_plus",EntitySummonedSwordBasePlus.class,13700005,50);
        registerEntity("wine",EntityWine.class,13700006,50);
        registerEntity("freeze_domain", EntityFreezeDomain.class,13700007,50);
        registerEntity("wind_enchantment", EntityWindEnchantment.class,13700008,50);
        registerEntity("thunder_edge_attack", EntityThunderEdgeAttack.class,13700009,50);
        registerEntity("crimson_sakura",EntityCrimsonSakuraAttack.class,13700010,50);
        registerEntity("tuna", EntityTuna.class,13700011,50);
        registerEntity("rain_umbrella",EntityRainUmbrella.class,13700012,50);
        registerEntity("summon_hedra",EntitySummonHedra.class,13700013,50);
        registerEntity("confinement",EntityConfinementForceField.class,13700014,50);
        registerEntity("conceptual", EntityConceptual.class,13700015,50);





        //AW
        registerEntity("entity_driveadd",EntityDriveAdd.class,13700101,50);
        registerEntity("entity_slash_dimension_add", EntitySlashDimensionAdd.class,13700102,50);
        registerEntity("entity_butterfly", EntitySummonedButterfly.class,13700103,50);
        registerEntity("summoned_sword_base_potion", EntitySummonedSwordPotionEffectAdd.class,13700104,50);


        //EXSA
        registerEntity("entity_drive_exsa", ExSaEntityDrive.class,13700201,50);
        registerEntity("entity_aqua_edge_exsa", EntityAquaEdge.class,13700202,50);
        registerEntity("entity_flare_edge_exsa", EntityFlareEdge.class,13700203,50);
        registerEntity("entity_phantom_sword_exsa", EntityPhantomSwordEx.class,13700204,50);
        registerEntity("entity_lightning_sword", EntityLightningSword.class,13700205,50);
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

package com.wjx.kablade.util.handlers;

import com.google.common.collect.Lists;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IThrowableEntity;

import java.util.ArrayList;
import java.util.List;

public class PlayerThrowableHandler {
    public static List<Entity> getThrowableEntityForPlayer(World world, EntityPlayer player,Class<? extends Entity> throwableEntityClass){
        List<Entity> allEntities = world.getLoadedEntityList();
        ArrayList<Entity> curEntities = Lists.newArrayList();
        if (allEntities.isEmpty()){
            return curEntities;
        }
        for (Entity e : allEntities){
            if (e instanceof IThrowableEntity){
                if (((IThrowableEntity) e).getThrower() == player){
                    curEntities.add(e);
                }
            }
        }
        if (curEntities.isEmpty()){
            return curEntities;
        }
        ArrayList<Entity> curEntities2 = Lists.newArrayList();
        for (Entity e : curEntities){
            Class<? extends Entity> cla = e.getClass();
            if (cla == throwableEntityClass){
                curEntities2.add(e);
            }
        }
        return curEntities2;
    }
    public static List<Entity> getAllThrowableForPlayer(World world, EntityPlayer player){
        List<Entity> allEntities = world.loadedEntityList;
        ArrayList<Entity> curEntities = Lists.newArrayList();
        if (allEntities.isEmpty()){
            return curEntities;
        }
        for (Entity e : allEntities){
            if (e instanceof IThrowableEntity){
                if (((IThrowableEntity) e).getThrower() == player){
                    curEntities.add(e);
                }
            }
        }
            return curEntities;
    }
}

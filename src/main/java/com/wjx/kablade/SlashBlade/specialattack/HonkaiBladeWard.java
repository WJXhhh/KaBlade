package com.wjx.kablade.SlashBlade.specialattack;

import com.wjx.kablade.Entity.EntityRaikiriBlade;
import com.wjx.kablade.util.handlers.PlayerThrowableHandler;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.List;

public class HonkaiBladeWard extends SpecialAttackBase {
    @Override
    public String toString() {
        return "blade_ward";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        List<Entity> entities = PlayerThrowableHandler.getThrowableEntityForPlayer(entityPlayer.world,entityPlayer, EntityRaikiriBlade.class);
        if (entities.size() >= 1){
            for (Entity e :entities){
                e.setDead();
            }
        }
        World w = entityPlayer.world;
        if (!w.isRemote)
        w.spawnEntity(new EntityRaikiriBlade(w,entityPlayer));
    }
}

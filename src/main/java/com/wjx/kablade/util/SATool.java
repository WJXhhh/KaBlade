package com.wjx.kablade.util;

import mods.flammpfeil.slashblade.ItemSlashBlade;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class SATool {
    public static Entity getEntityToWatch(EntityPlayer player) {
        Entity target = null;
        for (int dist = 2; dist < 20; dist += 2) {
            Vec3d vec3 = player.getLook(1.0F);
            double dx = vec3.x * 3.0D;
            double dy = (double)player.getEyeHeight() + vec3.y * 3.0D;
            double dz = vec3.z * 3.0D;
            List<Entity> list = player.world.getEntitiesInAABBexcluding(player, player.getEntityBoundingBox().grow(8.0D, 8.0D, 8.0D).offset(dx, dy, dz), input -> input instanceof EntityMob && input.isEntityAlive());
            float distance = 30.0f;
            for (Entity curEntity : list) {
                float curDist = curEntity.getDistance(player);
                if (!(curDist < distance)) continue;
                target = curEntity;
                distance = curDist;
            }
            if (target != null) break;
        }
        return target;
    }
}

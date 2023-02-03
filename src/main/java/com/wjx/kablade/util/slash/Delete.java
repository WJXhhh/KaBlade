package com.wjx.kablade.util.slash;

import mods.flammpfeil.slashblade.entity.EntitySlashDimension;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class Delete extends SpecialAttackBase {
    public String toString() {
        return "delete";
    }

    public static void removeEntity(Entity entity, Entity player,Boolean b){
        if(entity instanceof EntityDelete) return;
        if(entity instanceof EntitySlashDimension) return;
        if(entity == player) return;
        if(player instanceof EntityPlayer && !entity.world.isRemote){
            EntityDelete dim = new EntityDelete(player.world, (EntityLivingBase)player, 1000.0f);
            if(dim != null) {
                dim.setPosition(entity.posX, entity.posY + entity.height / 2.0D, entity.posZ);
                dim.setLifeTime(10);
                dim.setIsSlashDimension(true);
                player.world.spawnEntity((Entity) dim);
            }
        }
        if(entity instanceof EntityPlayer){
            EntityPlayer victim = (EntityPlayer) entity;
            victim.clearActivePotions();
            victim.onDeath(DamageSource.OUT_OF_WORLD);
            victim.setLastAttackedEntity((Entity)victim);
            victim.getCombatTracker().trackDamage(new DamageSource("transcend"),Float.MAX_VALUE,Float.MAX_VALUE);
            victim.setHealth(0.0f);
            victim.onKillEntity((EntityLivingBase) victim);
            victim.world.setEntityState((Entity) victim,(byte) 2);
            victim.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(0.0D);
            victim.addStat(StatList.DEATHS,1);
            victim.setLastAttackedEntity((Entity)victim);
        }
        List<Entity> entityList = new ArrayList<>();
        entityList.add(entity);
        entity.isDead = true;
        entity.world.unloadEntities(entityList);
        entity.world.removeEntityDangerously(entity);
        entity.setInvisible(true);
        if(entity instanceof EntityLivingBase){
            EntityLivingBase entityLivingBase = (EntityLivingBase) entity;
            entityLivingBase.onRemovedFromWorld();
        }
    }

    public void doSpacialAttack(ItemStack stack, EntityPlayer player){
        World world = player.world;
        if((Minecraft.getMinecraft()).entityRenderer.getShaderGroup() != null) (Minecraft.getMinecraft()).entityRenderer.stopUseShader();
        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(stack);
        Entity target = null;
        int entityId = ItemSlashBlade.TargetEntityId.get(tag).intValue();
        if(entityId != 0) {
            Entity tmp = world.getEntityByID(entityId);
            if(tmp != null && tmp.getDistance((Entity) player) < 30.0F){
                target = tmp;
                removeEntity(target, (EntityPlayer) player, Boolean.valueOf(true));
            }
        }
        if(target == null){
            target = getEnityToWatch(player);
            if(target != null && !target.isDead) removeEntity(target, (Entity)player, Boolean.valueOf(true));
        }
        ItemSlashBlade.setComboSequence(tag, ItemSlashBlade.ComboSequence.SlashDim);
        player.playSound(SoundEvents.ENTITY_WITCH_HURT, 0.5F, 1.0F);
        if(target == null) {
            if(!player.world.isRemote){
                EntityDelete dim = new EntityDelete(player.world, (EntityLivingBase) player, 1000.0f);
                if(dim != null){
                    dim.setPosition(player.posX, player.posY + player.height / 2.0D, player.posZ);
                    dim.setLifeTime(10);
                    dim.setIsSlashDimension(true);
                    player.world.spawnEntity((Entity)dim);
                }
            }
        } else {
            removeEntity(target, (EntityPlayer) player, Boolean.valueOf(true));
        }
    }

    private Entity getEnityToWatch(EntityPlayer player){
        World world = player.world;
        Entity target = null;
        for (int dist = 2; dist < 20; dist += 2) {
            AxisAlignedBB bb = player.getEntityBoundingBox();
            Vec3d vec = player.getLookVec();
            vec = vec.normalize();
            bb = bb.grow(8.0D, 1.0D, 8.0D);
            bb = bb.offset(vec.x * dist, vec.y * dist, vec.z * dist);
            List<Entity> list = new ArrayList<>();
            List<Entity> list1 = player.world.getEntitiesWithinAABBExcludingEntity((Entity)player, bb);
            if (list1 != null && !list1.isEmpty())
                for (int i111 = 0; i111 < list1.size(); i111++) {
                    Entity entity = list1.get(i111);
                    if (entity != null && !entity.isDead)
                        list.add(entity);
                }
            float distance = 30.0F;
            for (Entity curEntity : list) {
                float curDist = curEntity.getDistance((Entity)player);
                if (curDist < distance) {
                    target = curEntity;
                    distance = curDist;
                }
            }
            if (target != null)
                break;
        }
        return target;
    }

    private void spawnParticle(World world, Entity target) {
        world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, target.posX, target.posY + target.height, target.posZ, 3.0D, 3.0D, 3.0D, new int[0]);
        world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, target.posX + 1.0D, target.posY + target.height + 1.0D, target.posZ, 3.0D, 3.0D, 3.0D, new int[0]);
        world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, target.posX, target.posY + target.height + 0.5D, target.posZ + 1.0D, 3.0D, 3.0D, 3.0D, new int[0]);
    }
}

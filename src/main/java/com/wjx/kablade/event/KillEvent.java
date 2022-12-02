package com.wjx.kablade.event;

import com.wjx.kablade.SlashBlade.blades.bladeitem.MagicBlade;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;

import java.util.ArrayList;
import java.util.List;

public class KillEvent {
    public static void killutil(Entity target,Entity hitter){
        if(true)
        {
            DamageSource ds = new DamageSource("universe").setDamageBypassesArmor().setDamageAllowedInCreativeMode();


            List<Entity> entitylist = new ArrayList();
            if (target instanceof EntityLivingBase) {
                Class<? extends EntityLivingBase> clazz = ((EntityLivingBase) target).getClass();
                WorldEvent.antiEntity.add(clazz);
            }


            entitylist.add(target);


            //((EntityLivingBase) target).setHealth(0);
            ((EntityLivingBase) target).setLastAttackedEntity(hitter);
            target.attackEntityFrom(DamageSource.GENERIC, Integer.MAX_VALUE);


            //if(target.isEntityAlive()) {
            //System.out.println("AUTUMN:ALIVE");
            ((EntityLivingBase) target).setLastAttackedEntity(hitter);
            target.world.unloadEntities(entitylist);
            ((EntityLivingBase) target).setLastAttackedEntity(hitter);
            target.world.loadedEntityList.remove(target);
            ((EntityLivingBase) target).setLastAttackedEntity(hitter);
            target.world.loadedEntityList.removeAll(entitylist);
            ((EntityLivingBase) target).setLastAttackedEntity(hitter);
            target.world.unloadEntities(entitylist);
            ((EntityLivingBase) target).setLastAttackedEntity(hitter);
            target.ticksExisted = 0;
            ((EntityLivingBase) target).setLastAttackedEntity(hitter);
            target.onRemovedFromWorld();
            target.onKillCommand();
            target.setDead();
            ((EntityLivingBase) target).onDeath(ds);
            target.isDead = true;
            target.world.onEntityRemoved(target);

            target.world.getChunk(target.chunkCoordX, target.chunkCoordZ).removeEntity(target);
            target.world.removeEntity(target);
            target.world.removeEntityDangerously(target);


            if (target instanceof EntityLivingBase) {
                Class<? extends EntityLivingBase> clazz = ((EntityLivingBase) target).getClass();
                WorldEvent.antiEntity.remove(clazz);
            }
            if (hitter instanceof EntityPlayer) {
                if(target instanceof EntityPlayer)
                {
                    EntityPlayer player = (EntityPlayer) hitter;
                    if(((EntityPlayer) hitter).getHeldItemMainhand().getItem() instanceof MagicBlade)
                    {
                        if (target.isDead) {
                            ((EntityPlayer) hitter).sendStatusMessage(new TextComponentString(target.getName() + UpdateColor.makeColourRainbow(I18n.translateToLocal("msg.yunluo"))), false);
                        }
                    }
                }
            }
        }
        /*if (target != null && hitter instanceof EntityPlayer) {
            EntityPlayer player1 = (EntityPlayer) hitter;
            target.world.addWeatherEffect(new RainBow( target.world,target.posX, target.posY, target.posZ));

        }
        if (target instanceof EntityLivingBase) {
            EntityLivingBase livtar = (EntityLivingBase) target;
            livtar.isDead = true;
            //livtar.getDataManager().(6, MathHelper.clamp(-10.f,0.0f,((EntityLivingBase) target).getMaxHealth()));
            livtar.world.getChunkFromChunkCoords(livtar.chunkCoordX, livtar.chunkCoordZ).removeEntity(livtar);
        }*/
        //target.onUpdate();

    }
    /*
    public static void killEntity(Entity entity, Entity player) {
        if (!entity.isDead) {
            if (entity != player) {
                if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).getHealth() <= 0.0F) {
                    entity.setDead();
                }

                if (entity instanceof EntityPlayer) {
                    EntityPlayer victim = (EntityPlayer) entity;

                    if (Loader.isModLoaded("LoliPickaxe")) {
                        LoliPickaxeUtil.killPlayer(victim, (EntityLivingBase) null);
                    }*/

                    //int i;
                    /*
                    for (i = 0; i < victim.inventory.mainInventory.size(); ++i) {
                        if (victim.inventory.mainInventory.get(i) != null) {
                            victim.inventory.mainInventory.set(i, null);
                        }
                    }

                    for (i = 0; i < victim.field_71071_by.field_70460_b.length; ++i) {
                        if (victim.field_71071_by.field_70460_b[i] != null) {
                            victim.field_71071_by.field_70460_b[i] = null;
                        }
                    }

                    victim.clearActivePotions();
                    victim.onDeath(DamageSource.OUT_OF_WORLD);
                    victim.setLastAttackedEntity(victim);
                    victim.getCombatTracker().trackDamage(DamageSource.OUT_OF_WORLD, Float.MAX_VALUE, Float.MAX_VALUE);
                    victim.setHealth(-1.0F);
                    victim.onKillEntity(victim);
                    victim.world.setEntityState(victim, (byte) 2);
                    //victim.setA(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(0.0D);
                    victim.addStat(StatList.DEATHS, 1);
                    victim.setLastAttackedEntity(victim);
                    if (victim != null && player instanceof EntityPlayer) {
                        EntityPlayer player1 = (EntityPlayer) player;
                        player1.sendStatusMessage(new TextComponentString(victim.getName() + UpdateColor.makeColourRainbow(I18n.translateToLocal("msg.yunluo"))) ,false);
                    }
                }

                if (entity instanceof EntityLivingBase) {
                    EntityLivingBase entityLivingBase = (EntityLivingBase) entity;
                    if (entityLivingBase != null && player instanceof EntityPlayer) {
                        EntityPlayer player1 = (EntityPlayer) player;
                        entityLivingBase.world.addWeatherEffect(new RainBow( entityLivingBase.world,entityLivingBase.posX, entityLivingBase.posY, entityLivingBase.posZ));

                    }

                    if (entityLivingBase.deathTime >= 21) {
                        List<Entity> entitylist = new ArrayList();
                        entitylist.add(entity);
                        entity.isDead = true;
                        entity.world.unloadEntities(entitylist);
                        entity.world.getChunkFromChunkCoords(entity.chunkCoordX, entity.chunkCoordZ).removeEntity(entity);
                        entity.world.loadedEntityList.remove(entity);
                        entity.world.onEntityRemoved(entity);
                    }

                    entityLivingBase.deathTime = 21;
                    entityLivingBase.onDeath(DamageSource.causePlayerDamage((EntityPlayer) player));
                    entityLivingBase.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(0.0D);
                    entityLivingBase.setAIMoveSpeed(0.0F);
                    entityLivingBase.setLastAttackedEntity(player);
                    entityLivingBase.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(-1.0D);
                    //entityLivingBase.getDataManager().set(6, MathHelper.clamp(-1.0F, 0.0F, entityLivingBase.getMaxHealth()));
                }

                if (!(entity instanceof EntityLivingBase)) {
                    List<Entity> entitylist = new ArrayList();
                    entitylist.add(entity);
                    entity.isDead = true;
                    entity.world.unloadEntities(entitylist);
                    entity.world.removeEntityDangerously(entity);
                }

                entity.onUpdate();
            }
        }
    }*/
}

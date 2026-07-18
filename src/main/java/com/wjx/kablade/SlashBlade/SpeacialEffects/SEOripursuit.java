package com.wjx.kablade.SlashBlade.SpeacialEffects;

import com.google.common.base.Predicates;
import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.util.*;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.entity.EntitySummonedSwordBase;
import mods.flammpfeil.slashblade.specialeffect.IRemovable;
import mods.flammpfeil.slashblade.specialeffect.ISpecialEffect;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;
import java.util.Set;

public class SEOripursuit implements ISpecialEffect, IRemovable {
    @Override
    public void register() {
        MinecraftForge.EVENT_BUS.register(this);
        SaEventManager.addSaEvent(lockEnemy);
        BladeAttackEventManager.addEvent(bladeAttackEvent);
    }

    @Override
    public int getDefaultRequiredLevel() {
        return -1;
    }

    @Override
    public String getEffectKey() {
        return "kablade.oripursuit";
    }

    @Override
    public boolean canCopy(ItemStack itemStack) {
        return true;
    }

    @Override
    public boolean canRemoval(ItemStack itemStack) {
        return !itemStack.getTranslationKey().equals("item.wjx.blade.originyer");
    }

    public static SaEvent lockEnemy = new SaEvent() {
        @Override
        public void run(ItemStack stack, EntityPlayer par3EntityPlayer, boolean isJust) {
            if (stack.getItem() instanceof ItemSlashBlade) {
                if (SpecialEffects.isEffective(par3EntityPlayer, stack, BladeProxy.Oripursuit) == SpecialEffects.State.Effective) {
                    World world = par3EntityPlayer.getEntityWorld();
                    double dist = 10;
                    Vec3d vec3d = par3EntityPlayer.getPositionEyes(1.0F);
                    Vec3d vec3d1 = par3EntityPlayer.getLook(1.0F);
                    Vec3d vec3d2 = vec3d.add(vec3d1.x * dist, vec3d1.y * dist, vec3d1.z * dist);
                    AxisAlignedBB searchBox = par3EntityPlayer.getEntityBoundingBox()
                            .grow(1.0D, 1.0D, 1.0D)
                            .union(new AxisAlignedBB(vec3d.x, vec3d.y, vec3d.z, vec3d2.x, vec3d2.y, vec3d2.z).grow(2.0D, 2.0D, 2.0D));
                    List<Entity> list = world.getEntitiesInAABBexcluding(par3EntityPlayer, searchBox, Predicates.and(EntitySelectors.NOT_SPECTATING, entity -> entity != null && entity.canBeCollidedWith() && (entity instanceof EntityPlayer || entity instanceof EntityLiving)));
                    if (!world.isRemote) {
                        for (Entity e : list) {
                            if (e instanceof EntityLivingBase) {
                                addPursuitTargetToPlayer(par3EntityPlayer, e);
                            }
                        }
                    }
                }
            }
        }
    };

    public static void addPursuitTargetToPlayer(EntityPlayer p, Entity e) {
        if (p != null && e != null) {
            if (e instanceof EntityLivingBase) {
                if (!EntityUUIDManager.hasUUID(e)) {
                    EntityUUIDManager.addRandomUUIDTOEntity(e);
                }
                KaBladePlayerProp.addLockedTarget(KaBladePlayerProp.getPropCompound(p), EntityUUIDManager.getEntityUUID(e));
                if (!p.world.isRemote) {
                    KaBladePlayerProp.updateNBTForClient(p);
                    KaBladeEntityProperties.updateNBTForClient(e);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerUpdate(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            EntityPlayer p = event.player;
            if (!p.world.isRemote) {
                NBTTagCompound prop = KaBladePlayerProp.getPropCompound(p);
                if (KaBladePlayerProp.hasAnyLockedTarget(prop)) {
                    KaBladePlayerProp.tickAllLockedTimers(prop);
                    KaBladePlayerProp.removeStaleTargets(prop, p.world);
                    KaBladePlayerProp.updateNBTForClient(p);
                }
            }
        }
    }

    public static BladeAttackEvent bladeAttackEvent = new BladeAttackEvent() {
        @Override
        public void run(ItemStack stack, EntityPlayer player, Entity entity) {
            if (entity != null && player != null) {
                if (entity instanceof EntityLivingBase) {
                    EntityLivingBase target = (EntityLivingBase) entity;
                    if (!target.world.isRemote && EntityUUIDManager.hasUUID(target)) {
                        Set<String> locked = KaBladePlayerProp.getLockedUUIDs(KaBladePlayerProp.getPropCompound(player));
                        if (locked.contains(EntityUUIDManager.getEntityUUID(target))) {
                            float extraDamage = MathFunc.amplifierCalc(ItemSlashBlade.BaseAttackModifier.get(stack.getTagCompound()),3);
                            EntitySummonedSwordBase sword = new EntitySummonedSwordBase(target.world, player, 4);
                            sword.setColor(65535);
                            target.world.spawnEntity(sword);
                        }
                    }
                }
            }
        }
    };

}

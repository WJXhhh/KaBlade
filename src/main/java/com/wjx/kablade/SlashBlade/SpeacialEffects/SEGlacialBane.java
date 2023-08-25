package com.wjx.kablade.SlashBlade.SpeacialEffects;

import com.google.common.base.Predicates;
import com.wjx.kablade.Entity.EntityFreezeDomain;
import com.wjx.kablade.Main;
import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.network.MessageSpawnParticle;
import com.wjx.kablade.util.*;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.entity.EntitySummonedSwordBase;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialeffect.IRemovable;
import mods.flammpfeil.slashblade.specialeffect.ISpecialEffect;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;
import java.util.Random;

public class SEGlacialBane implements ISpecialEffect, IRemovable {
    @Override
    public void register() {
        MinecraftForge.EVENT_BUS.register(this);
        BladeAttackEventManager.addEvent(bladeAttackEvent);
    }

    @Override
    public int getDefaultRequiredLevel() {
        return -1;
    }

    @Override
    public String getEffectKey() {
        return "kablade.glacial_bane";
    }

    @Override
    public boolean canCopy(ItemStack itemStack) {
        return true;
    }

    @Override
    public boolean canRemoval(ItemStack itemStack) {
        return !itemStack.getTranslationKey().equals("wjx.blade.honkai.ice_epiphyllum");
    }

    @SubscribeEvent
    public void onPlayerUpdate(TickEvent.PlayerTickEvent event){
        if (event.phase == TickEvent.Phase.END){
             EntityPlayer p = event.player;
             if (!p.world.isRemote){
                 NBTTagCompound c = KaBladePlayerProp.getPropCompound(p);
                 if (c.getInteger(KaBladePlayerProp.GLACIAL_BANE_EXTRA_TICK) < 120){
                     c.setInteger(KaBladePlayerProp.GLACIAL_BANE_EXTRA_TICK,c.getInteger(KaBladePlayerProp.GLACIAL_BANE_EXTRA_TICK) + 1);
                 }
             }
        }
    }

    public static BladeAttackEvent bladeAttackEvent = new BladeAttackEvent() {
        @Override
        public void run(ItemStack stack, EntityPlayer player, Entity entity) {
            if (entity != null && player != null){
                if (entity instanceof EntityLivingBase){
                    EntityLivingBase target = (EntityLivingBase) entity;
                    if (!target.world.isRemote){
                        if (SpecialEffects.isEffective(player,stack,BladeProxy.GlacialBane) == SpecialEffects.State.Effective){
                            NBTTagCompound c = KaBladePlayerProp.getPropCompound(player);
                            if (c.getInteger(KaBladePlayerProp.GLACIAL_BANE_EXTRA_TICK) > 100){
                                c.setInteger(KaBladePlayerProp.GLACIAL_BANE_EXTRA_TICK,0);
                                World world = player.world;
                                for (int i = 0;i<60;i++){
                                    double x1,z1;
                                    x1 = r(world.rand);
                                    z1 = r(world.rand);
                                    Main.PACKET_HANDLER.sendToAll(new MessageSpawnParticle(EnumParticleTypes.CLOUD,player.posX + (world.rand.nextDouble() * 3 * x1),player.posY + world.rand.nextDouble(),player.posZ + (world.rand.nextDouble() * 3 * z1)));
                                }
                                AxisAlignedBB bb = player.getEntityBoundingBox().grow(4,4,4).offset(player.motionX,player.motionY,player.motionZ);
                                List<Entity> l = world.getEntitiesInAABBexcluding(player,bb, input -> input != player&&input instanceof EntityLivingBase);
                                for (Entity e : l){
                                    if (e instanceof EntityLivingBase){
                                        e.attackEntityFrom(DamageSource.causePlayerDamage(player),5f);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    };

    static double r(Random rand){
        if (rand.nextBoolean()){
            return 1d;
        }else return -1d;
    }

}

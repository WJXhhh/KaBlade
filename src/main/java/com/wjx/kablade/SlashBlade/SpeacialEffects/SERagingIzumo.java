package com.wjx.kablade.SlashBlade.SpeacialEffects;

import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.init.PotionInit;
import com.wjx.kablade.network.MessageSpawnParticleBurst;
import com.wjx.kablade.util.*;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialeffect.IRemovable;
import mods.flammpfeil.slashblade.specialeffect.ISpecialEffect;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

import static com.wjx.kablade.Main.PACKET_HANDLER;
import static com.wjx.kablade.util.KaBladePlayerProp.RAGING_IZUMO_COLD_DOWN;

public class SERagingIzumo implements ISpecialEffect, IRemovable {


    public static BladeAttackEvent event = new BladeAttackEvent() {
        @Override
        public void run(ItemStack stack, EntityPlayer player, Entity entity) {
            if (player.world.isRemote) {
                return;
            }
            if (stack.getItem() instanceof ItemSlashBlade){
                if (SpecialEffects.isEffective(player,stack, BladeProxy.RagingIzumo) == SpecialEffects.State.Effective){
                    if (KaBladePlayerProp.getPropCompound(player).getInteger(RAGING_IZUMO_COLD_DOWN) <= 0) {
                        if (Math.random()<0.1){
                            KaBladePlayerProp.getPropCompound(player).setInteger(RAGING_IZUMO_COLD_DOWN, 20);
                            World world = player.world;
                            world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 4.0F, (1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F) * 0.7F);
                            world.addWeatherEffect(new EntityLightningBolt(world, player.posX, player.posY, player.posZ, true));

                            MessageSpawnParticleBurst burst = new MessageSpawnParticleBurst(player.posX, player.posY, player.posZ);
                            burst.addGroup(EnumParticleTypes.EXPLOSION_NORMAL.getParticleID(), 40, 2.0, (double) player.height, 2.0);
                            burst.addColorfulGroup(40, 2.0, (double) player.height, 2.0, 1f, 0.945f, 0.333f, 2);
                            PACKET_HANDLER.sendToAll(burst);
                            AxisAlignedBB bb = player.getEntityBoundingBox();
                            bb = bb.grow(5, 4, 5);
                            bb = bb.offset(player.motionX, player.motionY, player.motionZ);
                            List<Entity> entities = world.getEntitiesInAABBexcluding(player, bb, input -> input != player && input instanceof EntityLivingBase);
                            for (Entity e : entities) {
                                e.attackEntityFrom(DamageSource.causeExplosionDamage(player), 8f);
                                if (e instanceof EntityLivingBase) {
                                    EntityLivingBase en = (EntityLivingBase) e;
                                    en.addPotionEffect(new PotionEffect(PotionInit.PARALY, 40, 2));
                                }
                            }
                            player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH,100,2));
                        }
                    }
                }
            }

        }
    };

    @Override
    public boolean canCopy(ItemStack itemStack) {
        return true;
    }

    @Override
    public boolean canRemoval(ItemStack itemStack) {
        return !itemStack.getTranslationKey().equals("item.wjx.blade.honkai.futsunushi_to");
    }

    @Override
    public void register() {
        MinecraftForge.EVENT_BUS.register(this);
        BladeAttackEventManager.addEvent(event);
    }

    @Override
    public int getDefaultRequiredLevel() {
        return -1;
    }

    @Override
    public String getEffectKey() {
        return "kablade.raging_izumo";
    }

    @SubscribeEvent
    public void onPlayerUpdate(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && !event.player.world.isRemote) {
            if (KaBladePlayerProp.getPropCompound(event.player).getInteger(RAGING_IZUMO_COLD_DOWN) > 0) {
                KaBladeEntityProperties.doIntegerLower(KaBladePlayerProp.getPropCompound(event.player), RAGING_IZUMO_COLD_DOWN);
            }
        }
    }
}

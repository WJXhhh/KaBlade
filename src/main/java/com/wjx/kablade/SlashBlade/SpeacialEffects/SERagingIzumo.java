package com.wjx.kablade.SlashBlade.SpeacialEffects;

import com.sun.javafx.geom.Vec3f;
import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.init.PotionInit;
import com.wjx.kablade.network.MessageSpawnColorfulSmoke;
import com.wjx.kablade.network.MessageSpawnParticle;
import com.wjx.kablade.util.BladeAttackEvent;
import com.wjx.kablade.util.BladeAttackEventManager;
import com.wjx.kablade.util.KaBladeEntityProperties;
import com.wjx.kablade.util.KaBladePlayerProp;
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
import java.util.Random;

import static com.wjx.kablade.Main.PACKET_HANDLER;
import static com.wjx.kablade.util.KaBladePlayerProp.RAGING_IZUMO_COLD_DOWN;

public class SERagingIzumo implements ISpecialEffect, IRemovable {


    public static BladeAttackEvent event = new BladeAttackEvent() {
        @Override
        public void run(ItemStack stack, EntityPlayer player, Entity entity) {
            if (SpecialEffects.isEffective(player,stack, BladeProxy.RagingIzumo) == SpecialEffects.State.Effective){
                if (KaBladePlayerProp.getPropCompound(player).getInteger(RAGING_IZUMO_COLD_DOWN) <= 0) {
                    if (Math.random()<0.1){
                        KaBladePlayerProp.getPropCompound(player).setInteger(RAGING_IZUMO_COLD_DOWN, 20);
                        World world = player.world;
                        world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 4.0F, (1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F) * 0.7F);
                        world.addWeatherEffect(new EntityLightningBolt(world, player.posX, player.posY, player.posZ, true));

                        for (int i = 0; i < 40; ++i) {
                            Random r1 = new Random();
                            Random r2 = new Random(r1.nextLong());
                            int state1;
                            int state2;
                            if (r1.nextBoolean()) {
                                state1 = 1;
                            } else state1 = -1;
                            if (r2.nextBoolean()) {
                                state2 = 1;
                            } else state2 = -1;
                            PACKET_HANDLER.sendToAll(new MessageSpawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, player.posX + (world.rand.nextDouble() * 2 * state1), player.posY + world.rand.nextDouble() * (double) player.height, player.posZ + (world.rand.nextDouble() * 2 * state2), 0.0D, 0.0D, 0.0D));
                            PACKET_HANDLER.sendToAll(new MessageSpawnColorfulSmoke(player.posX + (world.rand.nextDouble() * 2 * state1), player.posY + world.rand.nextDouble() * (double) player.height, player.posZ + (world.rand.nextDouble() * 2 * state2), new Vec3f(1f, 0.945f, 0.333f), 2));
                        }
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
    };

    @Override
    public boolean canCopy(ItemStack itemStack) {
        return true;
    }

    @Override
    public boolean canRemoval(ItemStack itemStack) {
        return !itemStack.getTranslationKey().equals("wjx.blade.honkai.raging_izumo");
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
        if (event.phase == TickEvent.Phase.START) {
            if (KaBladePlayerProp.getPropCompound(event.player).getInteger(RAGING_IZUMO_COLD_DOWN) > 0) {
                KaBladeEntityProperties.doIntegerLower(KaBladePlayerProp.getPropCompound(event.player), RAGING_IZUMO_COLD_DOWN);
            }
        }
    }
}

package com.wjx.kablade.SlashBlade.specialattack;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.wjx.kablade.Entity.EntityThunderEdgeAttack;
import com.wjx.kablade.Main;
import com.wjx.kablade.init.PotionInit;
import com.wjx.kablade.network.MessageDizuiKuo;
import com.wjx.kablade.network.MessageMagChaosBladeEffectUpdate;
import com.wjx.kablade.util.KaBladePlayerProp;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.special_render.MagChaosBladeEffectRenderer;
import mods.flammpfeil.slashblade.ability.StylishRankManager;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.Objects;

public class HonkaiThunderEdge extends SpecialAttackBase {
    @Override
    public String toString() {
        return "thunder_edge";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.getEntityWorld();
        if (!world.isRemote){
            float extraDamage = (float) MathFunc.amplifierCalc(ItemSlashBlade.BaseAttackModifier.get(itemStack.getTagCompound()),12f);
            EntityThunderEdgeAttack t = new EntityThunderEdgeAttack(world,entityPlayer);
            world.spawnEntity(t);
            double dist = 6;
            Vec3d vec3d = entityPlayer.getPositionEyes(1.0F);
            Vec3d vec3d1 = entityPlayer.getLook(1.0F);
            Vec3d vec3d2 = vec3d.add(vec3d1.x * dist, vec3d1.y * dist, vec3d1.z * dist);
            List<Entity> pointedEntity = Lists.newArrayList();
            if (entityPlayer.getHeldItemMainhand().getItem() instanceof ItemSlashBlade && entityPlayer.getHeldItemMainhand().hasTagCompound()){
                ItemSlashBlade.setComboSequence(Objects.requireNonNull(entityPlayer.getHeldItemMainhand().getTagCompound()), ItemSlashBlade.ComboSequence.SlashDim);
            }
            List<Entity> list = world.getEntitiesInAABBexcluding(entityPlayer, entityPlayer.getEntityBoundingBox().expand(vec3d1.x * dist, vec3d1.y * dist, vec3d1.z * dist).grow(3.0D, 1.0D, 3.0D), Predicates.and(EntitySelectors.NOT_SPECTATING, entity -> entity != null && entity.canBeCollidedWith() && (entity instanceof EntityPlayer || entity instanceof EntityLiving)));
            double d2 = dist;
            if (!list.isEmpty()){
                for (Entity e : list){
                    if (e instanceof EntityLivingBase && !(e instanceof EntityPlayer)){
                        entityPlayer.onCriticalHit(e);

                        e.attackEntityFrom(DamageSource.causePlayerDamage(entityPlayer),(50f + extraDamage)*1.4f);
                        if (e instanceof EntityLivingBase)
                            itemStack.hitEntity((EntityLivingBase) e,entityPlayer);
                        ((EntityLivingBase) e).addPotionEffect(new PotionEffect(PotionInit.PARALY,100,5));
                        e.getEntityData().setBoolean("dizui",true);
                        e.getEntityData().setInteger("dizuitime", 300);
                        Main.PACKET_HANDLER.sendToAll(new MessageDizuiKuo(e.getEntityId()));
                    }
                }
            }
        }
    }
}

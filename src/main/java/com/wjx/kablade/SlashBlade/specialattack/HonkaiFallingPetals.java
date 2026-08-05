package com.wjx.kablade.SlashBlade.specialattack;

import com.google.common.base.Predicates;
import com.wjx.kablade.Main;
import com.wjx.kablade.event.WorldEvent;
import com.wjx.kablade.util.KaBladeEntityProperties;
import com.wjx.kablade.util.MathFunc;
import com.wjx.kablade.util.TargetingUtil;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialattack.SakuraEnd;
import mods.flammpfeil.slashblade.specialattack.Spear;
import mods.flammpfeil.slashblade.specialattack.SpecialAttackBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = Main.MODID)
public class HonkaiFallingPetals extends SpecialAttackBase {
    @Override
    public String toString() {
        return "falling_petals";
    }

    @Override
    public void doSpacialAttack(ItemStack itemStack, EntityPlayer entityPlayer) {
        World world = entityPlayer.getEntityWorld();
        Spear spear = new Spear();
        SakuraEnd sakuraEnd = new SakuraEnd();
        spear.doSpacialAttack(itemStack,entityPlayer);
        WorldEvent.addTickDelayTask(3,()-> sakuraEnd.doSpacialAttack(itemStack,entityPlayer));
        double dist = 10;
        Vec3d vec3d = entityPlayer.getPositionEyes(1.0F);
        Vec3d vec3d1 = entityPlayer.getLook(1.0F);
        Vec3d vec3d2 = vec3d.add(vec3d1.x * dist, vec3d1.y * dist, vec3d1.z * dist);
        Entity pointedEntity = null;
        List<Entity> list = world.getEntitiesInAABBexcluding(entityPlayer, entityPlayer.getEntityBoundingBox().grow(1.0D, 1.0D, 1.0D).union(new AxisAlignedBB(vec3d.x, vec3d.y, vec3d.z, vec3d2.x, vec3d2.y, vec3d2.z).grow(2.0D, 2.0D, 2.0D)), Predicates.and(EntitySelectors.NOT_SPECTATING, entity -> TargetingUtil.canSelectForDamage(entityPlayer, entity) && TargetingUtil.canUseEntityCollision(entity)));
        double d2 = dist;
        float extraDamage = MathFunc.amplifierCalc((ItemSlashBlade.BaseAttackModifier.get((itemStack.getTagCompound()))),12f);
        for (Entity entity1 : TargetingUtil.getDistinctDamageTargets(list)) {
            AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().grow(entity1.getCollisionBorderSize());
            RayTraceResult raytraceresult = axisalignedbb.calculateIntercept(vec3d, vec3d2);

            entityPlayer.onCriticalHit(entity1);
            EntityLivingBase effectTarget = TargetingUtil.getSelectionTarget(entity1);
            if (effectTarget != null) {
                effectTarget.hurtResistantTime = 0;
            }
            entity1.attackEntityFrom(DamageSource.causePlayerDamage(entityPlayer).setDamageBypassesArmor(),15f + extraDamage);
            if (effectTarget != null) {
                effectTarget.hurtResistantTime = 0;
                itemStack.hitEntity(effectTarget,entityPlayer);
            }

            if (axisalignedbb.contains(vec3d)) {
                if (d2 >= 0.0D) {
                    pointedEntity = entity1;
                    d2 = 0.0D;
                }
            } else if (raytraceresult != null) {
                double d3 = vec3d.distanceTo(raytraceresult.hitVec);

                if (d3 < d2 || d2 == 0.0D) {
                    if (entity1.getLowestRidingEntity() == entityPlayer.getLowestRidingEntity() && !entityPlayer.canRiderInteract()) {
                        if (d2 == 0.0D) {
                            pointedEntity = entity1;
                        }
                    } else {
                        pointedEntity = entity1;
                        d2 = d3;
                    }
                }
            }
        }
        if (pointedEntity != null){
            EntityLivingBase effectTarget = TargetingUtil.getSelectionTarget(pointedEntity);
            if (effectTarget != null){
                if (!world.isRemote){
                    KaBladeEntityProperties.getPropCompound(effectTarget).setInteger(KaBladeEntityProperties.FALLING_PETALS,100);
                    KaBladeEntityProperties.updateNBTForClient(effectTarget);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event){
        World world = event.getEntity().world;
        if (!world.isRemote){
            NBTTagCompound entityProperties = KaBladeEntityProperties.getPropCompound(event.getEntity());
            if (entityProperties.getInteger(KaBladeEntityProperties.FALLING_PETALS)>0){
                KaBladeEntityProperties.doIntegerLower(entityProperties,KaBladeEntityProperties.FALLING_PETALS);
                KaBladeEntityProperties.updateNBTForClient(event.getEntity());
            }
        }
    }
}


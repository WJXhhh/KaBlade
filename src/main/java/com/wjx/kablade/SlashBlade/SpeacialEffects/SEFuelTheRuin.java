package com.wjx.kablade.SlashBlade.SpeacialEffects;

import com.wjx.kablade.SlashBlade.BladeProxy;
import com.wjx.kablade.util.KaBladePlayerProp;
import com.wjx.kablade.util.TargetingUtil;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.specialeffect.IRemovable;
import mods.flammpfeil.slashblade.specialeffect.ISpecialEffect;
import mods.flammpfeil.slashblade.specialeffect.SpecialEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;
import java.util.UUID;

/** “绯刃残樱”：两秒失血换取五秒攻击强化，并在结算时引爆血焰。 */
public class SEFuelTheRuin implements ISpecialEffect, IRemovable {
    private static final int BLEED_TICKS = 40;
    private static final int BUFF_TICKS = 100;
    private static final double RANGE = 13.0D;
    private static final UUID ATTACK_MODIFIER_ID =
            UUID.fromString("c6c4e511-9db1-4a8f-9f09-65d33e7d3738");

    public static final String BLEED_TIME = "fuel_the_ruin_bleeding";
    public static final String LOST_HEALTH = "fuel_the_ruin_lost_health";
    public static final String BUFF_TIME = "fuel_the_ruin";

    @Override
    public void register() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public int getDefaultRequiredLevel() {
        return -1;
    }

    @Override
    public String getEffectKey() {
        return "kablade.fuel_the_ruin";
    }

    @Override
    public boolean canCopy(ItemStack itemStack) {
        return false;
    }

    @Override
    public boolean canRemoval(ItemStack itemStack) {
        return !itemStack.getTranslationKey().equals("item.wjx.blade.honkai.ruinous_sakura");
    }

    /** SA 释放后开始或重置两秒失血窗口。 */
    public static void trigger(EntityPlayer player) {
        if (player.world.isRemote || !hasEffect(player)) return;
        NBTTagCompound prop = KaBladePlayerProp.getPropCompound(player);
        prop.setInteger(BLEED_TIME, BLEED_TICKS);
        prop.setFloat(LOST_HEALTH, 0.0F);
        prop.setInteger(BUFF_TIME, 0);
        removeAttackModifier(player);
        KaBladePlayerProp.updateNBTForClient(player);
    }

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer) || event.getEntityLiving().world.isRemote
                || event.getAmount() <= 0.0F) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        NBTTagCompound prop = KaBladePlayerProp.getPropCompound(player);
        if (prop.getInteger(BLEED_TIME) > 0) {
            prop.setFloat(LOST_HEALTH, prop.getFloat(LOST_HEALTH) + event.getAmount());
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        EntityPlayer player = event.player;
        NBTTagCompound prop = KaBladePlayerProp.getPropCompound(player);

        int buff = prop.getInteger(BUFF_TIME);
        if (buff > 0) {
            prop.setInteger(BUFF_TIME, buff - 1);
            if (buff == 1) removeAttackModifier(player);
            if (buff == 1 || buff % 10 == 0) KaBladePlayerProp.updateNBTForClient(player);
        }

        int bleeding = prop.getInteger(BLEED_TIME);
        if (bleeding <= 0) return;
        if (!player.isEntityAlive()) {
            endBleed(player, prop);
            return;
        }

        if (bleeding % 10 == 0) applyBleedDamage(player);
        bleeding--;
        prop.setInteger(BLEED_TIME, bleeding);
        if (bleeding == 0) {
            resolveBleed(player, prop.getFloat(LOST_HEALTH));
            endBleed(player, prop);
        } else if (bleeding % 10 == 0) {
            KaBladePlayerProp.updateNBTForClient(player);
        }
    }

    private static void applyBleedDamage(EntityPlayer player) {
        float floor = player.getMaxHealth() * 0.30F;
        float available = player.getHealth() - floor;
        if (available > 0.0F) {
            player.attackEntityFrom(DamageSource.MAGIC,
                    Math.min(player.getMaxHealth() * 0.01F, available));
        }
    }

    private static void resolveBleed(EntityPlayer player, float lostHealth) {
        if (lostHealth <= 0.0F) return;
        IAttributeInstance attack = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if (attack == null) return;

        removeAttackModifier(player);
        attack.applyModifier(new AttributeModifier(ATTACK_MODIFIER_ID, "kablade.fuel_the_ruin",
                lostHealth * 2.0D, 0).setSaved(false));
        NBTTagCompound prop = KaBladePlayerProp.getPropCompound(player);
        prop.setInteger(BUFF_TIME, BUFF_TICKS);

        float damage = (float) (lostHealth * 0.8D * attack.getAttributeValue());
        AxisAlignedBB area = new AxisAlignedBB(player.posX - RANGE, player.posY - 1.25D,
                player.posZ - RANGE, player.posX + RANGE, player.posY + 4.5D, player.posZ + RANGE);
        List<Entity> found = player.world.getEntitiesInAABBexcluding(player, area,
                entity -> TargetingUtil.canSelectForDamage(player, entity));
        Vec3d center = player.getPositionVector().add(0.0D, 1.0D, 0.0D);
        for (Entity receiver : TargetingUtil.getDistinctDamageTargets(found)) {
            EntityLivingBase target = TargetingUtil.getSelectionTarget(receiver);
            Vec3d hitPoint = TargetingUtil.getClosestPointOnDamageBounds(receiver, center);
            if (target == null || hitPoint.squareDistanceTo(center) > RANGE * RANGE) continue;
            target.hurtResistantTime = 0;
            receiver.attackEntityFrom(DamageSource.causePlayerDamage(player), damage);
        }

        WorldServer world = (WorldServer) player.world;
        world.spawnParticle(EnumParticleTypes.FLAME, player.posX, player.posY + 1.0D, player.posZ,
                42, RANGE * 0.52D, 1.3D, RANGE * 0.52D, 0.04D);
        world.playSound(null, player.posX, player.posY + 1.0D, player.posZ,
                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.8F, 1.35F);
    }

    private static void endBleed(EntityPlayer player, NBTTagCompound prop) {
        prop.removeTag(BLEED_TIME);
        prop.removeTag(LOST_HEALTH);
        KaBladePlayerProp.updateNBTForClient(player);
    }

    private static void removeAttackModifier(EntityPlayer player) {
        IAttributeInstance attack = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if (attack == null) return;
        AttributeModifier old = attack.getModifier(ATTACK_MODIFIER_ID);
        if (old != null) attack.removeModifier(old);
    }

    private static boolean hasEffect(EntityPlayer player) {
        ItemStack blade = player.getHeldItemMainhand();
        return blade.getItem() instanceof ItemSlashBlade
                && SpecialEffects.isEffective(player, blade, BladeProxy.FuelTheRuin)
                == SpecialEffects.State.Effective;
    }
}

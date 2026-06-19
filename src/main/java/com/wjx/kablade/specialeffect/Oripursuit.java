package com.wjx.kablade.specialeffect;

import com.wjx.kablade.init.ModSpecialEffects;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.UUID;

/**
 * 源能裁决 —— 源能刃「碎钢」专属特殊效果。
 * 从 1.12.2 {@code SEOripursuit} 移植而来。
 * <p>
 * 用 {@link SlashBladeEvent.ChargeActionEvent} 替代 1.12.2 的 mixin+SaEvent，
 * 用 {@link SlashBladeEvent.HitEvent} 替代 1.12.2 的 mixin+BladeAttackEvent。
 * <ul>
 *   <li>SA 时射线锁定前方 10 格最近敌人，存储 UUID（600 tick 有效）</li>
 *   <li>每次砍中锁定目标时，追加一把召唤剑造成额外伤害</li>
 * </ul>
 */
public class Oripursuit extends SpecialEffect {

    private static final String LOCKED_UUID_KEY = "kablade.oripursuit.locked_uuid";
    private static final String LOCKED_TIMER_KEY = "kablade.oripursuit.locked_timer";
    private static final int LOCK_DURATION = 600;
    private static final double RAY_DISTANCE = 10.0;
    private static final int SWORD_COLOR = 0x00FFFF;

    public Oripursuit() {
        super(-1, true, true);
    }

    // ── SA 时锁定敌人 ──────────────────────────────────────────────

    @SubscribeEvent
    public static void onChargeAction(SlashBladeEvent.ChargeActionEvent event) {
        if (event.getType() == SlashArts.ArtsType.Fail) return;

        LivingEntity user = event.getEntityLiving();
        if (!(user instanceof Player player)) return;
        if (user.level().isClientSide()) return;

        ItemStack blade = player.getMainHandItem();
        if (!isOripursuitBlade(blade)) return;

        LivingEntity target = raycastTarget((ServerLevel) user.level(), player);
        if (target != null) {
            setLockedTarget(player, target);
        }
    }

    // ── 命中锁定目标时追加飞剑 ─────────────────────────────────────

    @SubscribeEvent
    public static void onHit(SlashBladeEvent.HitEvent event) {
        LivingEntity user = event.getUser();
        LivingEntity target = event.getTarget();
        if (!(user instanceof Player player)) return;
        if (user.level().isClientSide()) return;

        ItemStack blade = player.getMainHandItem();
        if (!isOripursuitBlade(blade)) return;

        UUID lockedUuid = getLockedUUID(player);
        if (lockedUuid == null) return;
        if (!target.getUUID().equals(lockedUuid)) return;

        // 追加召唤剑
        float bladeAttack = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float extraDamage = Math.min(bladeAttack, 3.0F);

        EntityAbstractSummonedSword sword = new EntityAbstractSummonedSword(
                SlashBlade.RegistryEvents.SummonedSword, user.level());
        sword.setShooter(player);
        sword.setDamage(4.0F + extraDamage);
        sword.setPos(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ());
        sword.setColor(SWORD_COLOR);
        sword.setHitEntity(target);
        user.level().addFreshEntity(sword);
    }

    // ── Tick 倒计时 ────────────────────────────────────────────────

    @SubscribeEvent
    public static void onUpdate(SlashBladeEvent.UpdateEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;
        if (entity.level().isClientSide()) return;

        ItemStack blade = event.getBlade();
        if (!blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.ORIPURSUIT.getId()))
                .orElse(false)) return;

        CompoundTag persist = player.getPersistentData();
        CompoundTag playerData = persist.contains(Player.PERSISTED_NBT_TAG)
                ? persist.getCompound(Player.PERSISTED_NBT_TAG) : new CompoundTag();

        if (playerData.contains(LOCKED_TIMER_KEY)) {
            int timer = playerData.getInt(LOCKED_TIMER_KEY);
            if (timer > 0) {
                playerData.putInt(LOCKED_TIMER_KEY, timer - 1);
            }
            if (timer <= 0 || !playerData.contains(LOCKED_UUID_KEY)) {
                playerData.remove(LOCKED_UUID_KEY);
                playerData.putInt(LOCKED_TIMER_KEY, 0);
            }
            persist.put(Player.PERSISTED_NBT_TAG, playerData);
        }
    }

    // ── 工具方法 ───────────────────────────────────────────────────

    private static boolean isOripursuitBlade(ItemStack stack) {
        return stack.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.ORIPURSUIT.getId()))
                .orElse(false);
    }

    private static void setLockedTarget(Player player, LivingEntity target) {
        CompoundTag persist = player.getPersistentData();
        CompoundTag playerData = persist.contains(Player.PERSISTED_NBT_TAG)
                ? persist.getCompound(Player.PERSISTED_NBT_TAG) : new CompoundTag();
        playerData.putString(LOCKED_UUID_KEY, target.getUUID().toString());
        playerData.putInt(LOCKED_TIMER_KEY, LOCK_DURATION);
        persist.put(Player.PERSISTED_NBT_TAG, playerData);
    }

    private static UUID getLockedUUID(Player player) {
        CompoundTag persist = player.getPersistentData();
        CompoundTag playerData = persist.contains(Player.PERSISTED_NBT_TAG)
                ? persist.getCompound(Player.PERSISTED_NBT_TAG) : new CompoundTag();
        if (!playerData.contains(LOCKED_UUID_KEY) || playerData.getInt(LOCKED_TIMER_KEY) <= 0) {
            return null;
        }
        try {
            return UUID.fromString(playerData.getString(LOCKED_UUID_KEY));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static LivingEntity raycastTarget(ServerLevel level, LivingEntity user) {
        Vec3 eye = user.getEyePosition();
        Vec3 look = user.getLookAngle();
        Vec3 end = eye.add(look.scale(RAY_DISTANCE));

        AABB scanBox = user.getBoundingBox()
                .expandTowards(look.scale(RAY_DISTANCE))
                .inflate(1.0);

        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class, scanBox, e -> e != user && e.isAlive() && e.isPickable());

        LivingEntity closest = null;
        double closestDist = RAY_DISTANCE;

        for (LivingEntity candidate : candidates) {
            AABB bb = candidate.getBoundingBox().inflate(candidate.getPickRadius());
            var hit = bb.clip(eye, end);
            if (bb.contains(eye)) {
                closest = candidate;
                closestDist = 0;
                break;
            } else if (hit.isPresent()) {
                double dist = eye.distanceTo(hit.get());
                if (dist < closestDist) {
                    closest = candidate;
                    closestDist = dist;
                }
            }
        }

        return closest;
    }
}

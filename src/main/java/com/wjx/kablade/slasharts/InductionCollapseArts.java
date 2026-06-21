package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.ConfinementForceFieldEntity;
import com.wjx.kablade.util.SATool;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/**
 * 高频坍缩 —— 「高周波切割刀」专属 SA。
 * <p>
 * 从 1.12.2 {@code HonkaiInductionCollapse} 移植而来：优先在锁定的目标实体处生成
 * 禁锢力场；若无锁定目标，则在玩家视线方向最近的可攻击实体处生成；
 * 完全没有目标时在玩家前方生成。
 */
public final class InductionCollapseArts extends SlashArts {

    public InductionCollapseArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        ServerLevel level = (ServerLevel) user.level();
        ItemStack blade = user.getMainHandItem();

        // 获取 SlashBlade 锁定的目标（走 capability，1.12.2 的 TargetEntityId 等价物）
        Entity target = null;
        Entity locked = blade.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.getTargetEntity(level))
                .orElse(null);
        if (locked instanceof LivingEntity && locked.isAlive() && locked.distanceTo(user) < 100.0F) {
            target = locked;
        }

        // 没有锁定时取视线方向最近的可攻击实体
        if (target == null) {
            target = SATool.getEntityToWatch(user);
        }

        Vec3 spawnPos;
        if (target instanceof LivingEntity livingTarget) {
            spawnPos = new Vec3(livingTarget.getX(), livingTarget.getY(), livingTarget.getZ());
        } else {
            spawnPos = new Vec3(
                    user.getX() + user.getDeltaMovement().x,
                    user.getY() + user.getDeltaMovement().y,
                    user.getZ() + user.getDeltaMovement().z);
        }

        ConfinementForceFieldEntity field = new ConfinementForceFieldEntity(level, user);
        field.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        level.addFreshEntity(field);

        return super.doArts(type, user);
    }
}

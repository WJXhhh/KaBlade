package com.wjx.kablade.util;

import com.wjx.kablade.Main;
import com.wjx.kablade.config.ModConfig;
import mods.flammpfeil.slashblade.entity.selector.EntitySelectorAttackable;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 拔刀剑及 KaBlade SA 共用的单目标解析器。
 * 世界查询必须由服务端主线程调用。
 */
public final class TargetingUtil {
    public static final double DEFAULT_LOCK_DISTANCE = 20.0D;

    private static final int SNEAK_MASK = 16;
    private static final Map<UUID, LockInputState> LOCK_INPUT_STATES = new HashMap<>();
    private static final Map<UUID, Long> LAST_DEBUG_LOG_TICK = new HashMap<>();

    private static final AtomicLong RESOLVER_CALLS = new AtomicLong();
    private static final AtomicLong RAY_QUERIES = new AtomicLong();
    private static final AtomicLong FALLBACK_QUERIES = new AtomicLong();

    private TargetingUtil() {
    }

    public static Entity getValidLockedTarget(EntityPlayer player, ItemStack blade, double maxDistance) {
        if (player == null || blade == null || blade.isEmpty()) {
            return null;
        }

        NBTTagCompound tag = ItemSlashBlade.getItemTagCompound(blade);
        int entityId = ItemSlashBlade.TargetEntityId.get(tag);
        if (entityId == 0) {
            return null;
        }

        Entity target = player.world.getEntityByID(entityId);
        String invalidReason = getInvalidReason(player, target, maxDistance);
        if (invalidReason != null) {
            ItemSlashBlade.TargetEntityId.set(tag, 0);
            debugInvalidation(player, entityId, invalidReason);
            return null;
        }
        return target;
    }

    public static Entity resolveTarget(EntityPlayer player, ItemStack blade, double rayReach,
                                       double fallbackHorizontal, double fallbackVertical) {
        long started = System.nanoTime();
        RESOLVER_CALLS.incrementAndGet();

        Entity target = getValidLockedTarget(player, blade, rayReach);
        int rayCandidates = 0;
        int fallbackCandidates = 0;

        if (target == null) {
            RaySearchResult ray = rayTraceEntityWithCount(player, rayReach, 0.0F);
            target = ray.target;
            rayCandidates = ray.candidateCount;
        }

        if (target == null) {
            AxisAlignedBB area = player.getEntityBoundingBox()
                    .grow(fallbackHorizontal, fallbackVertical, fallbackHorizontal);
            List<Entity> candidates = player.world.getEntitiesInAABBexcluding(
                    player,
                    area,
                    entity -> isAttackableCandidate(player, entity, true)
            );
            FALLBACK_QUERIES.incrementAndGet();
            fallbackCandidates = candidates.size();

            double bestDistanceSq = Double.MAX_VALUE;
            for (Entity candidate : candidates) {
                double distanceSq = player.getDistanceSq(candidate);
                if (distanceSq < bestDistanceSq) {
                    bestDistanceSq = distanceSq;
                    target = candidate;
                }
            }
        }

        debugResolver(player, target, rayCandidates, fallbackCandidates, System.nanoTime() - started);
        return target;
    }

    public static Entity rayTraceEntity(EntityLivingBase owner, double reach, float extraBorder) {
        return rayTraceEntityWithCount(owner, reach, extraBorder).target;
    }

    private static RaySearchResult rayTraceEntityWithCount(EntityLivingBase owner, double reach, float extraBorder) {
        World world = owner.world;
        Vec3d start = owner.getPositionEyes(1.0F);
        Vec3d look = owner.getLook(1.0F);
        Vec3d end = start.add(look.scale(reach));

        RayTraceResult blockHit = world.rayTraceBlocks(start, end, false, false, true);
        if (blockHit != null && blockHit.hitVec != null) {
            end = blockHit.hitVec;
        }

        // 1.12.2 的 expand 对负方向会 clamp，使用 union 保证抬头、低头和任意水平方向都覆盖完整射线。
        AxisAlignedBB sweep = owner.getEntityBoundingBox()
                .grow(1.0D, 1.0D, 1.0D)
                .union(new AxisAlignedBB(start, end).grow(1.0D + extraBorder));

        List<Entity> candidates = world.getEntitiesInAABBexcluding(
                owner,
                sweep,
                entity -> isAttackableCandidate(owner, entity, false)
        );
        RAY_QUERIES.incrementAndGet();

        Entity best = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (Entity candidate : candidates) {
            float border = candidate.getCollisionBorderSize() + extraBorder;
            AxisAlignedBB box = candidate.getEntityBoundingBox().grow(border);
            RayTraceResult hit = box.calculateIntercept(start, end);
            double distanceSq;

            if (box.contains(start)) {
                distanceSq = 0.0D;
            } else if (hit != null && hit.hitVec != null) {
                distanceSq = start.squareDistanceTo(hit.hitVec);
            } else {
                continue;
            }

            if (candidate == owner.getRidingEntity() && !candidate.canRiderInteract() && distanceSq != 0.0D) {
                continue;
            }
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = candidate;
            }
        }

        return new RaySearchResult(best, candidates.size());
    }

    private static boolean isAttackableCandidate(EntityLivingBase owner, Entity entity, boolean requireVisible) {
        if (entity == null || entity == owner || !entity.isEntityAlive() || !entity.canBeCollidedWith()) {
            return false;
        }
        if (!EntitySelectorAttackable.getInstance().apply(entity)) {
            return false;
        }
        if (owner instanceof EntityPlayer && entity instanceof EntityPlayer
                && !((EntityPlayer) owner).canAttackPlayer((EntityPlayer) entity)) {
            return false;
        }
        return !requireVisible || owner.canEntityBeSeen(entity);
    }

    private static String getInvalidReason(EntityPlayer player, Entity target, double maxDistance) {
        if (target == null) {
            return "unloaded";
        }
        if (!target.isEntityAlive()) {
            return "dead";
        }
        if (target.world != player.world) {
            return "different_world";
        }
        if (maxDistance > 0.0D && player.getDistanceSq(target) > maxDistance * maxDistance) {
            return "out_of_range";
        }
        if (!isAttackableCandidate(player, target, false)) {
            return "not_attackable";
        }
        return null;
    }

    /** Shift 按下边沿调用：允许原版 onUpdate 在当前 Tick 做至多一次兼容重试。 */
    public static void onSneakPressed(EntityPlayer player) {
        LOCK_INPUT_STATES.put(player.getUniqueID(), new LockInputState(player.world.getTotalWorldTime()));
    }

    public static void onSneakReleased(EntityPlayer player) {
        LOCK_INPUT_STATES.remove(player.getUniqueID());
    }

    /** 兼容服务重载或其他模组直接写入 SB.MCS 的情况。 */
    public static void observeMoveCommand(EntityPlayer player) {
        boolean sneaking = (player.getEntityData().getByte("SB.MCS") & SNEAK_MASK) != 0;
        UUID id = player.getUniqueID();
        if (sneaking) {
            if (!LOCK_INPUT_STATES.containsKey(id)) {
                LOCK_INPUT_STATES.put(id, new LockInputState(player.world.getTotalWorldTime()));
            }
        } else {
            LOCK_INPUT_STATES.remove(id);
        }
    }

    /** 仅供 ItemSlashBlade.onUpdate 的目标 ID 读取重定向使用，不修改刀的真实 NBT。 */
    public static boolean shouldSuppressRepeatedVanillaSearch(EntityPlayer player) {
        LockInputState state = LOCK_INPUT_STATES.get(player.getUniqueID());
        return state != null && player.world.getTotalWorldTime() > state.retryThroughTick;
    }

    public static long getResolverCalls() {
        return RESOLVER_CALLS.get();
    }

    public static long getRayQueryCount() {
        return RAY_QUERIES.get();
    }

    public static long getFallbackQueryCount() {
        return FALLBACK_QUERIES.get();
    }

    private static void debugResolver(EntityPlayer player, Entity target, int rayCandidates,
                                      int fallbackCandidates, long durationNanos) {
        if (!ModConfig.GeneralConf.DebugTargeting || Main.logger == null) {
            return;
        }
        long tick = player.world.getTotalWorldTime();
        Long previous = LAST_DEBUG_LOG_TICK.get(player.getUniqueID());
        if (previous != null && tick - previous < 20L) {
            return;
        }
        LAST_DEBUG_LOG_TICK.put(player.getUniqueID(), tick);
        Main.logger.info("[Targeting] player={} tick={} thread={} rayCandidates={} fallbackCandidates={} durationUs={} target={}",
                player.getName(), tick, Thread.currentThread().getName(), rayCandidates, fallbackCandidates,
                durationNanos / 1000L, target == null ? 0 : target.getEntityId());
    }

    private static void debugInvalidation(EntityPlayer player, int entityId, String reason) {
        if (ModConfig.GeneralConf.DebugTargeting && Main.logger != null) {
            Main.logger.info("[Targeting] player={} tick={} invalidTarget={} reason={} thread={}",
                    player.getName(), player.world.getTotalWorldTime(), entityId, reason,
                    Thread.currentThread().getName());
        }
    }

    private static final class LockInputState {
        private final long retryThroughTick;

        private LockInputState(long retryThroughTick) {
            this.retryThroughTick = retryThroughTick;
        }
    }

    private static final class RaySearchResult {
        private final Entity target;
        private final int candidateCount;

        private RaySearchResult(Entity target, int candidateCount) {
            this.target = target;
            this.candidateCount = candidateCount;
        }
    }
}

package com.wjx.kablade.util;

import com.wjx.kablade.Main;
import com.wjx.kablade.config.ModConfig;
import mods.flammpfeil.slashblade.entity.selector.EntitySelectorAttackable;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MultiPartEntityPart;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 拔刀剑及 KaBlade SA 共用的单目标解析器。
 * 世界查询必须由服务端主线程调用。
 */
public final class TargetingUtil {
    public static final double DEFAULT_LOCK_DISTANCE = 20.0D;

    private static final int SNEAK_MASK = 16;
    private static final String[] PARENT_METHOD_NAMES = new String[]{"getParent", "getParentEntity"};
    private static final String[] PARENT_FIELD_NAMES = new String[]{"parent", "hydra", "dragon"};
    private static final Set<String> DEFAULT_MULTIPART_TARGET_CLASSES = new HashSet<>(Arrays.asList(
            "com.github.alexthe666.iceandfire.entity.EntityDragonBase",
            "com.github.alexthe666.iceandfire.entity.EntityDeathWorm"
    ));
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

        Entity target = getSelectionTarget(player.world.getEntityByID(entityId));
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
            Set<Integer> seenTargets = new HashSet<>();
            for (Entity candidate : candidates) {
                EntityLivingBase selection = getSelectionTarget(candidate);
                if (selection == null || !seenTargets.add(selection.getEntityId())) {
                    continue;
                }
                double distanceSq = player.getDistanceSq(candidate);
                if (distanceSq < bestDistanceSq) {
                    bestDistanceSq = distanceSq;
                    target = selection;
                }
            }
        }

        debugResolver(player, target, rayCandidates, fallbackCandidates, System.nanoTime() - started);
        return target;
    }

    public static Entity rayTraceEntity(EntityLivingBase owner, double reach, float extraBorder) {
        return rayTraceEntityWithCount(owner, reach, extraBorder).target;
    }

    /** Final harmful-target check for server-side SA damage. */
    public static boolean canDamage(EntityLivingBase owner, Entity entity) {
        return canSelectForDamage(owner, entity);
    }

    /**
     * 将可命中的 multipart 部件归一化到用于锁定、去重、友伤判断的父级活体。
     * 不要求可选模组在编译期存在。
     */
    public static Entity resolveMultipartParent(Entity raw) {
        if (raw == null) {
            return null;
        }
        Entity current = raw;
        Set<Integer> visited = new HashSet<>();
        for (int depth = 0; depth < 8 && current != null; depth++) {
            if (!visited.add(System.identityHashCode(current))) {
                break;
            }
            Entity parent = resolveMultipartParentOnce(current);
            if (parent == null || parent == current) {
                break;
            }
            current = parent;
        }
        return current;
    }

    public static EntityLivingBase getSelectionTarget(Entity raw) {
        Entity target = resolveMultipartParent(raw);
        return target instanceof EntityLivingBase ? (EntityLivingBase) target : null;
    }

    public static Entity getDamageReceiver(Entity raw) {
        return raw;
    }

    public static boolean canUseEntityCollision(Entity raw) {
        EntityLivingBase selection = getSelectionTarget(raw);
        return raw != null && (raw.canBeCollidedWith() || (selection != null && selection != raw));
    }

    public static boolean canSelectForDamage(EntityLivingBase owner, Entity raw) {
        EntityLivingBase entity = getSelectionTarget(raw);
        if (owner == null || entity == null || entity == owner || raw == owner || !entity.isEntityAlive()) {
            return false;
        }
        if (entity instanceof EntityPlayer) {
            EntityPlayer targetPlayer = (EntityPlayer) entity;
            if (targetPlayer.isCreative() || targetPlayer.isSpectator()) {
                return false;
            }
            if (owner instanceof EntityPlayer && !((EntityPlayer) owner).canAttackPlayer(targetPlayer)) {
                return false;
            }
        }
        if (owner.isOnSameTeam(entity) && owner.getTeam() != null
                && !owner.getTeam().getAllowFriendlyFire()) {
            return false;
        }
        if (entity instanceof EntityTameable && ((EntityTameable) entity).isOwner(owner)) {
            return false;
        }
        return isSlashBladeAttackable(raw, entity) || isCompatibleHostileTarget(raw, entity);
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
                .union(new AxisAlignedBB(start.x, start.y, start.z, end.x, end.y, end.z)
                        .grow(1.0D + extraBorder));

        List<Entity> candidates = world.getEntitiesInAABBexcluding(
                owner,
                sweep,
                entity -> isAttackableCandidate(owner, entity, false)
        );
        RAY_QUERIES.incrementAndGet();

        Entity best = null;
        double bestDistanceSq = Double.MAX_VALUE;
        Set<Integer> seenTargets = new HashSet<>();
        for (Entity candidate : candidates) {
            EntityLivingBase selection = getSelectionTarget(candidate);
            if (selection == null || !seenTargets.add(selection.getEntityId())) {
                continue;
            }
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
                best = selection;
            }
        }

        return new RaySearchResult(best, candidates.size());
    }

    private static boolean isAttackableCandidate(EntityLivingBase owner, Entity entity, boolean requireVisible) {
        EntityLivingBase selection = getSelectionTarget(entity);
        if (!canSelectForDamage(owner, entity) || !canUseEntityCollision(entity)) {
            return false;
        }
        return !requireVisible || owner.canEntityBeSeen(selection) || owner.canEntityBeSeen(entity);
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
        if (!canSelectForDamage(player, target)) {
            return "not_attackable";
        }
        return null;
    }

    private static Entity resolveMultipartParentOnce(Entity raw) {
        if (raw instanceof MultiPartEntityPart) {
            Object parent = ((MultiPartEntityPart) raw).parent;
            if (parent instanceof Entity) {
                return (Entity) parent;
            }
        }
        for (String methodName : PARENT_METHOD_NAMES) {
            Object parent = invokeNoArg(raw, methodName);
            if (parent instanceof Entity && parent != raw) {
                return (Entity) parent;
            }
        }
        for (String fieldName : PARENT_FIELD_NAMES) {
            Object parent = readField(raw, fieldName);
            if (parent instanceof Entity && parent != raw) {
                return (Entity) parent;
            }
        }
        return raw;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        Class<?> clazz = target.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                Method method = clazz.getDeclaredMethod(methodName);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (ReflectiveOperationException ignored) {
                clazz = clazz.getSuperclass();
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Object readField(Object target, String fieldName) {
        Class<?> clazz = target.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (ReflectiveOperationException ignored) {
                clazz = clazz.getSuperclass();
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    private static boolean isSlashBladeAttackable(Entity raw, EntityLivingBase selection) {
        return EntitySelectorAttackable.getInstance().apply(raw)
                || (raw != selection && EntitySelectorAttackable.getInstance().apply(selection));
    }

    private static boolean isCompatibleHostileTarget(Entity raw, EntityLivingBase selection) {
        if (selection instanceof IMob) {
            return true;
        }
        if (!selection.isNonBoss()) {
            return true;
        }
        return isConfiguredCompatibleTarget(selection)
                || (raw != selection && isConfiguredCompatibleTarget(raw));
    }

    private static boolean isConfiguredCompatibleTarget(Entity entity) {
        Set<String> configured = new HashSet<>(DEFAULT_MULTIPART_TARGET_CLASSES);
        if (ModConfig.GeneralConf.ExtraMultipartTargetClasses != null) {
            configured.addAll(Arrays.asList(ModConfig.GeneralConf.ExtraMultipartTargetClasses));
        }
        Class<?> clazz = entity.getClass();
        while (clazz != null && clazz != Object.class) {
            if (configured.contains(clazz.getName())) {
                return true;
            }
            clazz = clazz.getSuperclass();
        }
        String entityName = EntityList.getEntityString(entity);
        return entityName != null && configured.contains(entityName);
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
        Main.logger.info("[Targeting] player={} tick={} thread={} rayCandidates={} fallbackCandidates={} durationUs={} target={} targetClass={}",
                player.getName(), tick, Thread.currentThread().getName(), rayCandidates, fallbackCandidates,
                durationNanos / 1000L, target == null ? 0 : target.getEntityId(),
                target == null ? "null" : target.getClass().getName());
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

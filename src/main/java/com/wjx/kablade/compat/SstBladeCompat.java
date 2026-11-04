package com.wjx.kablade.compat;

import com.wjx.kablade.Main;
import mods.flammpfeil.slashblade.entity.IShootable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.Consumer;

/** Optional Sword Strange Tales integration; no SST classes are linked by Kablade. */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SstBladeCompat {
    private static final String SNAPSHOT_KEY = "sstmodAffinityDamageBonusSnapshot";
    private static final ThreadLocal<CastAffinity> DEFERRED = new ThreadLocal<>();
    private SstBladeCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded("sstmod");
    }

    /** SST 2.1.10 replaces PlayerAttackHelper's rank/experience formula. */
    public static double rankBonus(int rank, int refine) {
        if (rank >= 7) return rank * 4.1D + refine * 0.25D;
        if (rank >= 6) return rank * 2.5D + refine * 0.17D;
        if (rank >= 5) return rank * 1.7D + refine * 0.12D;
        if (rank >= 4) return rank * 2.1D;
        return rank;
    }

    /** Let SST apply its own affinity bonus exactly once, including after switching blades. */
    public static void snapshotAffinity(Entity effect, LivingEntity owner) {
        if (!isLoaded() || !(owner instanceof Player player) || effect.level().isClientSide()) {
            return;
        }
        CastAffinity cast = DEFERRED.get();
        if (cast != null && cast.owner.equals(player.getUUID())) {
            effect.getPersistentData().putDouble(SNAPSHOT_KEY, cast.percent);
            return;
        }
        if (AffinityApi.SNAPSHOT != null) {
            try {
                AffinityApi.SNAPSHOT.invoke(null, effect, owner.getMainHandItem(), player.getUUID());
            } catch (ReflectiveOperationException exception) {
                AffinityApi.report(exception);
            }
        }
    }

    /** A child summon keeps its parent's cast snapshot rather than the current held blade. */
    public static void inheritAffinity(Entity child, Entity parent) {
        if (isLoaded() && parent.getPersistentData().contains(SNAPSHOT_KEY, 99)) {
            child.getPersistentData().putDouble(SNAPSHOT_KEY, parent.getPersistentData().getDouble(SNAPSHOT_KEY));
        }
    }

    public static double affinityBonus(Player player) {
        if (!isLoaded() || AffinityApi.BONUS == null) return 0.0D;
        try {
            double value = (double) AffinityApi.BONUS.invoke(null, player.getMainHandItem(), player.getUUID());
            return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
        } catch (ReflectiveOperationException exception) {
            AffinityApi.report(exception);
            return 0.0D;
        }
    }

    /** Scope the captured affinity to this callback, including recursively scheduled child summons. */
    public static Runnable deferred(LivingEntity owner, Runnable task) {
        if (!isLoaded() || !(owner instanceof Player player)) return task;
        Consumer<Runnable> scope = affinityScope(owner);
        return () -> scope.accept(task);
    }

    /** Long-running cast records retain only a UUID and percentage, not a mutable held stack. */
    public static Consumer<Runnable> affinityScope(LivingEntity owner) {
        if (!isLoaded() || !(owner instanceof Player player)) return Runnable::run;
        CastAffinity previous = DEFERRED.get();
        CastAffinity captured = previous != null && previous.owner.equals(owner.getUUID())
                ? previous : new CastAffinity(owner.getUUID(), affinityBonus(player));
        return scope(captured);
    }

    public static Consumer<Runnable> affinityScope(LivingEntity owner, double percent) {
        if (!isLoaded() || !(owner instanceof Player)) return Runnable::run;
        return scope(new CastAffinity(owner.getUUID(), Double.isFinite(percent) ? Math.max(0.0D, percent) : 0.0D));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void snapshotDeferredNativeSummon(EntityJoinLevelEvent event) {
        if (DEFERRED.get() == null || event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof IShootable shootable && shootable.getShooter() instanceof LivingEntity owner) {
            snapshotAffinity(event.getEntity(), owner);
        }
    }

    private static Consumer<Runnable> scope(CastAffinity captured) {
        return task -> {
            CastAffinity saved = DEFERRED.get();
            DEFERRED.set(captured);
            try {
                task.run();
            } finally {
                if (saved == null) DEFERRED.remove(); else DEFERRED.set(saved);
            }
        };
    }

    public static DamageSource captureSource(DamageSource source) {
        CastAffinity cast = DEFERRED.get();
        if (cast == null || source instanceof AffinitySource || source.getEntity() == null
                || !cast.owner.equals(source.getEntity().getUUID())) return source;
        return new AffinitySource(source, cast.percent);
    }

    /** Used by the optional SST handler mixin; retain its original early-out rules. */
    public static boolean applyCapturedAffinity(LivingDamageEvent event) {
        if (!(event.getSource() instanceof AffinitySource source)) return false;
        if (!event.isCanceled() && event.getAmount() > 0.0F && !event.getEntity().level().isClientSide()) {
            event.setAmount((float) (event.getAmount() * (1.0D + source.percent / 100.0D)));
        }
        return true;
    }

    private record CastAffinity(UUID owner, double percent) {
    }

    private static final class AffinitySource extends DamageSource {
        private final double percent;

        private AffinitySource(DamageSource source, double percent) {
            super(source.typeHolder(), source.getDirectEntity(), source.getEntity());
            this.percent = percent;
        }
    }

    private static final class AffinityApi {
        private static boolean reported;
        private static final Method SNAPSHOT = findSnapshot();
        private static final Method BONUS = findBonus();

        private static Method findBonus() {
            try {
                return Class.forName("cn.star.sstmod.slashblade.BladeAffinity")
                        .getMethod("getDamageBonusPercent", ItemStack.class, UUID.class);
            } catch (ReflectiveOperationException | LinkageError exception) {
                report(exception);
                return null;
            }
        }

        private static Method findSnapshot() {
            try {
                return Class.forName("cn.star.sstmod.slashblade.BladeAffinity")
                        .getMethod("snapshotDamageBonus", Entity.class, ItemStack.class, UUID.class);
            } catch (ReflectiveOperationException | LinkageError exception) {
                report(exception);
                return null;
            }
        }

        private static void report(Throwable exception) {
            if (!reported) {
                reported = true;
                Main.LOGGER.warn("SST affinity snapshot API is unavailable; using SST's normal hit handling", exception);
            }
        }
    }
}

package com.wjx.kablade.mixin;

import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/** Keep selectable multipart bodies available to SlashBlade's melee scan. */
@Mixin(value = TargetSelector.class, remap = false)
public abstract class SlashBladeTargetSelectorMixin {
    @Unique
    private static final String KABLADE_MELEE_SELECTOR =
            "getTargettableEntitiesWithinAABB(Lnet/minecraft/world/level/Level;"
                    + "Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/AABB;D)Ljava/util/List;";

    @ModifyArg(method = KABLADE_MELEE_SELECTOR, at = @At(value = "INVOKE",
            target = "Ljava/util/stream/Stream;flatMap(Ljava/util/function/Function;)Ljava/util/stream/Stream;",
            ordinal = 0, remap = false), index = 0, remap = false)
    private static Function<LivingEntity, Stream<? extends Entity>> kablade$multipartScan(
            Function<LivingEntity, Stream<? extends Entity>> original) {
        return kablade$wrapParts(original, false);
    }

    @ModifyArg(method = KABLADE_MELEE_SELECTOR, at = @At(value = "INVOKE",
            target = "Ljava/util/stream/Stream;flatMap(Ljava/util/function/Function;)Ljava/util/stream/Stream;",
            ordinal = 1, remap = false), index = 0, remap = false)
    private static Function<LivingEntity, Stream<? extends Entity>> kablade$bodyScan(
            Function<LivingEntity, Stream<? extends Entity>> original) {
        return kablade$wrapParts(original, true);
    }

    @Unique
    private static Function<LivingEntity, Stream<? extends Entity>> kablade$wrapParts(
            Function<LivingEntity, Stream<? extends Entity>> original, boolean includeBody) {
        return root -> {
            if (!root.isMultipartEntity()) {
                return original.apply(root);
            }

            // Forge allows getParts() to be null, even for a multipart entity.
            Stream<? extends Entity> parts = root.getParts() == null
                    ? Stream.empty() : original.apply(root).filter(Objects::nonNull);
            if (!root.isPickable()) {
                // Some bosses can only be hurt through their parts, including parts
                // which deliberately opt out of ordinary crosshair targeting.
                return parts;
            }

            Stream<? extends Entity> selectableParts = parts.filter(Entity::isPickable);
            // Only the normal AABB scan may add the body. The expanded scan exists
            // for distant parts and would otherwise extend the body's attack range.
            return includeBody ? Stream.concat(selectableParts, Stream.of(root)) : selectableParts;
        };
    }

    @Inject(method = KABLADE_MELEE_SELECTOR, at = @At("RETURN"), cancellable = true, remap = false)
    private static void kablade$preferSelectedParts(Level level, LivingEntity attacker, AABB area, double reach,
                                                     CallbackInfoReturnable<List<Entity>> cir) {
        List<Entity> targets = cir.getReturnValue();
        Set<Entity> rootsWithParts = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Entity target : targets) {
            if (target instanceof PartEntity<?> part && part.isPickable()
                    && part.getParent() instanceof LivingEntity root
                    && root.isMultipartEntity() && root.isPickable()) {
                rootsWithParts.add(root);
            }
        }

        Set<Entity> bodies = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Entity> result = new ArrayList<>(targets.size());
        for (Entity target : targets) {
            if (target instanceof LivingEntity root && root.isMultipartEntity() && root.isPickable()
                    && (rootsWithParts.contains(root) || !bodies.add(root))) {
                continue;
            }
            result.add(target);
        }
        cir.setReturnValue(result);
    }
}

package com.wjx.kablade.compat;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.util.List;
import java.util.Set;

/** Skip the optional selector patch if a SlashBlade update changes its bytecode. */
public final class KabladeMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("kablade/mixin");
    private static final String SELECTOR_MIXIN = "com.wjx.kablade.mixin.SlashBladeTargetSelectorMixin";
    private static final String SELECTOR_METHOD = "getTargettableEntitiesWithinAABB";
    private static final String SELECTOR_DESC = "(Lnet/minecraft/world/level/Level;"
            + "Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/AABB;D)Ljava/util/List;";
    private static final String FLAT_MAP_DESC = "(Ljava/util/function/Function;)Ljava/util/stream/Stream;";
    private static final String EXPANDED_SCAN_DESC = "(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;"
            + "Ljava/util/function/Predicate;)Ljava/util/List;";
    private static final String BODY_SCAN_DESC =
            "(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!SELECTOR_MIXIN.equals(mixinClassName)) {
            return true;
        }
        try {
            ClassNode target = MixinService.getService().getBytecodeProvider().getClassNode(targetClassName);
            if (supportsSelector(target)) {
                return true;
            }
            LOGGER.warn("SlashBlade's melee selector has changed; multipart targeting compatibility is disabled.");
        } catch (Exception | LinkageError exception) {
            LOGGER.warn("Could not inspect SlashBlade's melee selector; multipart targeting compatibility is disabled.",
                    exception);
        }
        return false;
    }

    static boolean supportsSelector(ClassNode target) {
        MethodNode selector = target.methods.stream()
                .filter(method -> SELECTOR_METHOD.equals(method.name) && SELECTOR_DESC.equals(method.desc)
                        && (method.access & Opcodes.ACC_STATIC) != 0)
                .findFirst().orElse(null);
        if (selector == null) {
            return false;
        }

        int expansions = 0;
        boolean returnsList = false;
        String scan = null;
        for (AbstractInsnNode instruction : selector.instructions) {
            if (instruction.getOpcode() == Opcodes.ARETURN) {
                returnsList = true;
            }
            if (instruction instanceof MethodInsnNode call && "net/minecraft/world/level/Level".equals(call.owner)
                    && (EXPANDED_SCAN_DESC.equals(call.desc) || BODY_SCAN_DESC.equals(call.desc))) {
                scan = call.desc;
            }
            if (instruction instanceof MethodInsnNode call && "java/util/stream/Stream".equals(call.owner)
                    && "flatMap".equals(call.name) && FLAT_MAP_DESC.equals(call.desc)) {
                if (!((expansions == 0 && EXPANDED_SCAN_DESC.equals(scan))
                        || (expansions == 1 && BODY_SCAN_DESC.equals(scan)))) {
                    return false;
                }
                AbstractInsnNode previous = instruction.getPrevious();
                while (previous != null && previous.getOpcode() < 0) {
                    previous = previous.getPrevious();
                }
                if (!(previous instanceof InvokeDynamicInsnNode lambda) || !isPartsExpansion(target, lambda)) {
                    return false;
                }
                expansions++;
                scan = null;
            }
        }
        return returnsList && expansions == 2;
    }

    private static boolean isPartsExpansion(ClassNode target, InvokeDynamicInsnNode lambda) {
        for (Object argument : lambda.bsmArgs) {
            if (!(argument instanceof Handle handle) || !target.name.equals(handle.getOwner())) {
                continue;
            }
            for (MethodNode method : target.methods) {
                if (!method.name.equals(handle.getName()) || !method.desc.equals(handle.getDesc())) {
                    continue;
                }
                boolean multipart = false;
                boolean parts = false;
                boolean streamParts = false;
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof MethodInsnNode call) {
                        multipart |= "isMultipartEntity".equals(call.name) && "()Z".equals(call.desc);
                        parts |= "getParts".equals(call.name)
                                && "()[Lnet/minecraftforge/entity/PartEntity;".equals(call.desc);
                        streamParts |= "java/util/stream/Stream".equals(call.owner) && "of".equals(call.name)
                                && "([Ljava/lang/Object;)Ljava/util/stream/Stream;".equals(call.desc);
                    }
                }
                return multipart && parts && streamParts;
            }
        }
        return false;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}

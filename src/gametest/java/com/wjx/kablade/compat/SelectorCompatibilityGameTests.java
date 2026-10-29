package com.wjx.kablade.compat;

import com.wjx.kablade.Main;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;

@GameTestHolder(Main.MODID)
@PrefixGameTestTemplate(false)
public final class SelectorCompatibilityGameTests {
    private SelectorCompatibilityGameTests() {
    }

    @GameTest(template = "multipart_targeting")
    public static void changedSelectorDoesNotPartiallyApply(GameTestHelper helper) throws IOException {
        ClassNode selector = new ClassNode();
        try (InputStream input = TargetSelector.class.getResourceAsStream("TargetSelector.class")) {
            if (input == null) {
                helper.fail("SlashBlade selector bytecode was not found");
                return;
            }
            new ClassReader(input).accept(selector, 0);
        }
        helper.assertTrue(KabladeMixinPlugin.supportsSelector(selector),
                "the pinned SlashBlade selector must be supported");

        for (MethodNode method : selector.methods) {
            if (!"getTargettableEntitiesWithinAABB".equals(method.name)
                    || !method.desc.contains("Lnet/minecraft/world/phys/AABB;D)")) {
                continue;
            }
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call && "java/util/stream/Stream".equals(call.owner)
                        && "flatMap".equals(call.name)) {
                    call.name = "changedByAnotherVersion";
                    helper.assertFalse(KabladeMixinPlugin.supportsSelector(selector),
                            "a changed selector must disable the whole compatibility mixin");
                    helper.succeed();
                    return;
                }
            }
        }
        helper.fail("the expected multipart expansion was not found");
    }
}

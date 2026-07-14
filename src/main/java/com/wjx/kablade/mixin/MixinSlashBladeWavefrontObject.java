package com.wjx.kablade.mixin;

import com.wjx.kablade.config.ModConfig;
import mods.flammpfeil.slashblade.client.model.obj.GroupObject;
import mods.flammpfeil.slashblade.client.model.obj.WavefrontObject;
import net.minecraft.client.renderer.Tessellator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Mixin(value = WavefrontObject.class, remap = false)
public abstract class MixinSlashBladeWavefrontObject {
    @Shadow public ArrayList<GroupObject> groupObjects;

    @Unique private Map<String, List<GroupObject>> kablade$groupIndex;
    @Unique private int kablade$indexedGroupCount = -1;

    @Inject(method = "renderPart", at = @At("HEAD"), cancellable = true)
    private void kablade$renderIndexedPart(String partName, CallbackInfo ci) {
        if (!ModConfig.GeneralConf.EnableSlashBladeModelWarmup) {
            return;
        }
        for (GroupObject group : kablade$getGroups(partName)) {
            group.render();
        }
        ci.cancel();
    }

    @Inject(method = "tessellatePart", at = @At("HEAD"), cancellable = true)
    private void kablade$tessellateIndexedPart(Tessellator tessellator, String partName, CallbackInfo ci) {
        if (!ModConfig.GeneralConf.EnableSlashBladeModelWarmup) {
            return;
        }
        for (GroupObject group : kablade$getGroups(partName)) {
            group.render(tessellator);
        }
        ci.cancel();
    }

    @Unique
    private List<GroupObject> kablade$getGroups(String partName) {
        if (kablade$groupIndex == null || kablade$indexedGroupCount != groupObjects.size()) {
            kablade$rebuildGroupIndex();
        }
        List<GroupObject> groups = kablade$groupIndex.get(kablade$normalize(partName));
        return groups == null ? java.util.Collections.<GroupObject>emptyList() : groups;
    }

    @Unique
    private void kablade$rebuildGroupIndex() {
        Map<String, List<GroupObject>> rebuilt = new HashMap<String, List<GroupObject>>();
        for (GroupObject group : groupObjects) {
            if (group == null) {
                continue;
            }
            String key = kablade$normalize(group.name);
            List<GroupObject> groups = rebuilt.get(key);
            if (groups == null) {
                groups = new ArrayList<GroupObject>(1);
                rebuilt.put(key, groups);
            }
            groups.add(group);
        }
        kablade$groupIndex = rebuilt;
        kablade$indexedGroupCount = groupObjects.size();
    }

    @Unique
    private static String kablade$normalize(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }
}

package com.wjx.kablade.mixin;

import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.discovery.ModDiscoverer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Mixin(ModDiscoverer.class)
public class MixinModDiscoverer {       // Only used in some special environment where game may load this mod repeatedly
    /**
     * Return-stage fallback for environments which bypass Forge's candidate deduplication.
     *
     * NeonExpansion may already remove relative/absolute aliases in addCandidate(). In that case
     * this method is intentionally a no-op. If duplicates still reach the result, keep the first
     * KaBlade container for each physical source file. Different physical JARs remain visible so
     * Forge can still report a genuine duplicate-mod installation.
     */
    @Inject(method = "identifyMods", at = @At("RETURN"), remap = false)
    private void kablade$deduplicateOwnContainers(
            CallbackInfoReturnable<List<ModContainer>> callbackInfo) {
        List<ModContainer> modList = callbackInfo.getReturnValue();
        if (modList == null || modList.isEmpty()) {
            return;
        }

        Set<String> seenSources = new HashSet<>();
        for (Iterator<ModContainer> iterator = modList.iterator(); iterator.hasNext();) {
            ModContainer container = iterator.next();
            if (container == null || !"kablade".equals(container.getModId()) || container.getSource() == null) {
                continue;
            }
            if (!seenSources.add(sourceIdentity(container.getSource()))) {
                iterator.remove();
            }
        }
    }

    private static String sourceIdentity(File source) {
        try {
            return source.getCanonicalPath();
        } catch (IOException | SecurityException ignored) {
            return source.getAbsoluteFile().toPath().normalize().toFile().getPath();
        }
    }
}

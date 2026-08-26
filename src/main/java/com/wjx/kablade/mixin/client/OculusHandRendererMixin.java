package com.wjx.kablade.mixin.client;

import com.wjx.kablade.client.shader.BladeLuminousHandOculusPipeline;
import com.wjx.kablade.Main;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

/**
 * Flushes deferred blade masks only after Oculus has submitted its fully-buffered hand.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.pathways.HandRenderer", remap = false)
public abstract class OculusHandRendererMixin {
    private static Field kablade$bufferSourceField;
    private static boolean kablade$loggedReflectionFailure;

    @Inject(method = {"renderSolid", "renderTranslucent"}, at = @At(value = "INVOKE",
            target = "Lnet/irisshaders/batchedentityrendering/impl/FullyBufferedMultiBufferSource;endBatch()V",
            shift = At.Shift.AFTER), remap = false, require = 0)
    private void kablade$flushLuminousHand(CallbackInfo ci) {
        try {
            if (kablade$bufferSourceField == null) {
                kablade$bufferSourceField = this.getClass().getDeclaredField("bufferSource");
                kablade$bufferSourceField.setAccessible(true);
            }
            Object buffer = kablade$bufferSourceField.get(this);
            BladeLuminousHandOculusPipeline.flushQueuedHand(
                    (MultiBufferSource.BufferSource) buffer);
        } catch (ReflectiveOperationException | ClassCastException exception) {
            if (!kablade$loggedReflectionFailure) {
                kablade$loggedReflectionFailure = true;
                Main.LOGGER.warn("Could not access Oculus' native hand buffer for the blade "
                        + "luminous replay.", exception);
            }
        }
    }
}

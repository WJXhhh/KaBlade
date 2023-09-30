package com.wjx.kablade.mixin;

import net.minecraftforge.fml.common.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.Proxy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Mixin(value = LoadController.class, remap = false)
public class LoadControllerMixin {
    @Shadow private Loader loader;

    @Inject(method = "distributeStateMessage(Lnet/minecraftforge/fml/common/LoaderState;[Ljava/lang/Object;)V", at = @At("HEAD"))
    private void beforeConstructing(LoaderState state, Object[] eventData, CallbackInfo ci) throws Throwable {
        if (state == LoaderState.CONSTRUCTING) { // This state is where Forge adds mod files to ModClassLoader

            ModClassLoader KaBladeMixinModClassLoader1 = (ModClassLoader) eventData[0];

            Mixins.addConfiguration("mixins.kablade.json");

            for (ModContainer container : this.loader.getActiveModList()) {
                KaBladeMixinModClassLoader1.addFile(container.getSource());
            }

            Field field =  Proxy.class.getDeclaredField("transformer");
            field.setAccessible(true);
            IMixinTransformer transformer = ((IMixinTransformer)field.get(null));
            Field field1 = transformer.getClass().getDeclaredField("processor");
            field1.setAccessible(true);
            Object processor = field1.get(transformer);
            Method selectMethod = processor.getClass().getDeclaredMethod("select", MixinEnvironment.class);
            selectMethod.setAccessible(true);
            selectMethod.invoke(processor, MixinEnvironment.getCurrentEnvironment());
        }
    }

}

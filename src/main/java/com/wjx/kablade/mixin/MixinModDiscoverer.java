package com.wjx.kablade.mixin;

import com.google.common.collect.Lists;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.discovery.ModDiscoverer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mixin(ModDiscoverer.class)
public class MixinModDiscoverer {
    @Inject(method = "identifyMods", at = @At(value = "RETURN"),locals = LocalCapture.CAPTURE_FAILSOFT, remap = false)
    private void injectBeforeReturn(CallbackInfoReturnable<List<ModContainer>> cir,List<ModContainer> modList) {
        String id = "kablade";
        int count = 0;
        boolean flag = false;
        for (ModContainer c1 : modList){
            if (c1.getModId().equals("networkmod")){
                flag = true;
            }
        }
        if (flag){
            for (ModContainer c : modList){
                if (c.getModId().equals(id)){
                    count++;
                }
            }
            if (count >=2){
                for (Iterator<ModContainer> it = modList.iterator();it.hasNext();){
                    ModContainer c = it.next();
                    if (c.getModId().equals(id)){
                        it.remove();
                        break;
                    }
                }
            }
        }
    }
}

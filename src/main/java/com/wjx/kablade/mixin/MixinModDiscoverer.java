package com.wjx.kablade.mixin;

import com.google.common.collect.Maps;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.discovery.ModDiscoverer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@Mixin(ModDiscoverer.class)
public class MixinModDiscoverer {
    @Inject(method = "identifyMods", at = @At(value = "RETURN"),locals = LocalCapture.CAPTURE_FAILSOFT, remap = false)
    private void injectBeforeReturn(CallbackInfoReturnable<List<ModContainer>> cir,List<ModContainer> modList) {
        String id = "kablade";
        HashMap<String,Integer> maps = Maps.newHashMap();
        int count = 0;
        for (ModContainer c : modList) {
            if (c.getModId().equals(id)) {
                if (!maps.containsKey(c.getSource().getName())){
                    maps.put(c.getSource().getName(),1);
                }
                else maps.put(c.getSource().getName(),maps.get(c.getSource().getName())+1);

                //count++;
            }
        }
        String name = null;
        for (String k : maps.keySet()){
            if (maps.get(k)>1){
                name = k;
            }
        }
        if (name!=null){
            for (Iterator<ModContainer> it = modList.iterator();it.hasNext();){
                ModContainer c = it.next();
                if (c.getModId().equals(id)&&c.getSource().getName().equals(name)){
                    it.remove();
                    break;
                }
            }
        }
    }
}

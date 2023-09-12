package com.wjx.kablade;

import com.google.common.collect.Lists;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.List;

public class KaBladeLateMixinInit{
    public List<String> getMixinConfigs() {
        return Lists.newArrayList("mixins.kablade.json");
    }
}

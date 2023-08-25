package com.wjx.kablade;

import com.google.common.collect.Lists;

import java.util.List;

public class KaBladeLateMixinInit{
    public List<String> getMixinConfigs() {
        return Lists.newArrayList("mixins.kablade.json");
    }
}

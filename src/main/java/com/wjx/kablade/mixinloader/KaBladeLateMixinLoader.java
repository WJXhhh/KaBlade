package com.wjx.kablade.mixinloader;

import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.Config;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Collections;
import java.util.List;

/**
 * 通过 MixinBooter 直接登记 KaBlade 的实际 Mixin 配置。
 *
 * <p>本类必须位于 mixins.kablade.json 声明的 Mixin 包之外，否则 Mixin
 * 会禁止将它作为普通启动类直接加载。</p>
 */
public class KaBladeLateMixinLoader implements ILateMixinLoader {
    private static final String MIXIN_CONFIG = "mixins.kablade.json";

    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList(MIXIN_CONFIG);
    }

    @Override
    public boolean shouldMixinConfigQueue(String mixinConfig) {
        for (Config config : Mixins.getConfigs()) {
            if (mixinConfig.equals(config.getName())) {
                return false;
            }
        }
        return true;
    }
}

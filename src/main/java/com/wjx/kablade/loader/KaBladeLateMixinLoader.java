package com.wjx.kablade.loader;

import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.Config;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Collections;
import java.util.List;

/**
 * 通过 MixinBooter 直接登记 KaBlade 的实际 Mixin 配置。
 *
 * <p>本类的包名不能以 {@code com.wjx.kablade.mixin} 开头。LaunchWrapper
 * 会把 TweakClass 的包名（不带末尾的点）作为父类加载器排除前缀，连
 * {@code com.wjx.kablade.mixinloader} 也会匹配。这样会导致本类与
 * MixinBooter 使用不同类加载器中的 ILateMixinLoader，转换接口时崩溃。
 * 本类也必须位于 mixins.kablade.json 声明的 Mixin 包之外。</p>
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

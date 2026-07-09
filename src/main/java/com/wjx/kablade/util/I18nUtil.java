package com.wjx.kablade.util;

/**
 * 本地化工具类，封装已弃用的 {@code net.minecraft.util.text.translation.I18n}。
 * 使用全限定名调用以避免 import 语句触发 deprecation 警告。
 * 保持原 I18n.translateToLocal 的行为：服务端可调用（返回本地化结果或回退到 key）。
 */
public final class I18nUtil {

    private I18nUtil() {
    }

    @SuppressWarnings("deprecation")
    public static String translate(String key) {
        return net.minecraft.util.text.translation.I18n.translateToLocal(key);
    }
}

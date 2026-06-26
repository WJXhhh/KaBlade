package com.wjx.kablade.init;

import com.wjx.kablade.data.IPlayerPropertyData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * KBlade2 的 capability 注册中心。
 */
public final class KabladeCapabilities {

    /** 玩家属性数据 capability。 */
    public static final Capability<IPlayerPropertyData> PLAYER_PROPERTY_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});

    private KabladeCapabilities() {
    }
}

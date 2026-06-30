package com.wjx.kablade.data;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.KabladeCapabilities;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 玩家属性数据 capability 提供者。
 * <p>
 * 通过 {@link com.wjx.kablade.event.PlayerPropertyDataHandler} 在玩家实体上附加。
 */
public class PlayerPropertyDataProvider implements ICapabilitySerializable<CompoundTag> {

    /** capability 附着在玩家实体上的 key。 */
    public static final ResourceLocation KEY =
            ResourceLocation.fromNamespaceAndPath(Main.MODID, "player_property_data");

    private final PlayerPropertyData data = new PlayerPropertyData();
    private final LazyOptional<IPlayerPropertyData> holder = LazyOptional.of(() -> data);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> cap, @Nullable Direction side) {
        return KabladeCapabilities.PLAYER_PROPERTY_DATA.orEmpty(cap, holder);
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.save();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        data.load(nbt);
    }

    /** 如果玩家持有此 capability，快捷获取实现实例。 */
    public static PlayerPropertyData get(Player player) {
        return (PlayerPropertyData) player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                .orElseThrow(() -> new IllegalStateException(
                        "Player " + player.getScoreboardName() + " missing property data cap"));
    }
}

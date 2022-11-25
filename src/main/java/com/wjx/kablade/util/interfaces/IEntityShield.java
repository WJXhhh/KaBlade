package com.wjx.kablade.util.interfaces;

import net.minecraftforge.fml.common.registry.IThrowableEntity;

public interface IEntityShield extends IThrowableEntity {
    float getShieldBlood();
    void setShieldBlood(float blood);
}

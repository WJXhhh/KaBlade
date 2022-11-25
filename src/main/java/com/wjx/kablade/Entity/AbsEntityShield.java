package com.wjx.kablade.Entity;

import com.wjx.kablade.util.interfaces.IEntityShield;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public abstract class AbsEntityShield extends Entity implements IEntityShield {
    public AbsEntityShield(World worldIn) {
        super(worldIn);
    }
}

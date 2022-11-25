package com.wjx.kablade.Entity.ai;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;

public class EntityAIStop extends EntityAIBase {
    EntityLiving living;
    public EntityAIStop(EntityLiving living){
        this.living = living;
        this.setMutexBits(1|2|4);
    }

    @Override
    public boolean shouldExecute() {
        return true;
    }


    @Override
    public void updateTask() {
        living.getNavigator().clearPath();
        super.updateTask();
    }
}

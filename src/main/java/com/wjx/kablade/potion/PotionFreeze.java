package com.wjx.kablade.potion;

import com.wjx.kablade.Entity.ai.EntityAIStop;
import com.wjx.kablade.init.PotionInit;
import com.wjx.kablade.util.Reference;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;

public class PotionFreeze extends Potion {
    public PotionFreeze(int liquidColorIn) {
        super(true, liquidColorIn);
        PotionInit.potions.add(this);
        setRegistryName(new ResourceLocation(Reference.MODID, "freeze"));
        setPotionName("kablade.potion." + "freeze");
    }
    public void removeAttributesModifiersFromEntity(EntityLivingBase entityLivingBaseIn, AbstractAttributeMap attributeMapIn, int amplifier)
    {
        super.removeAttributesModifiersFromEntity(entityLivingBaseIn, attributeMapIn, amplifier);
        if (entityLivingBaseIn instanceof EntityLiving)
        {
            EntityLiving living = (EntityLiving) entityLivingBaseIn;
            for (EntityAITasks.EntityAITaskEntry ai:
                    living.tasks.taskEntries) {
                if (ai.action instanceof EntityAIStop )
                {
                    living.tasks.removeTask(ai.action);
                    return;
                }
            }

//            ((EntityLiving) entityLivingBaseIn).tasks.addTask(0, new EntityAIPerification((EntityLiving) entityLivingBaseIn));
        }
    }

    public void applyAttributesModifiersToEntity(EntityLivingBase entityLivingBaseIn, AbstractAttributeMap attributeMapIn, int amplifier)
    {
        if (entityLivingBaseIn instanceof EntityLiving)
        {
            EntityLiving living = (EntityLiving) entityLivingBaseIn;
            for (EntityAITasks.EntityAITaskEntry ai:
                    living.tasks.taskEntries) {
                if (ai.action instanceof EntityAIStop)
                {
                    return;
                }
            }

            ((EntityLiving) entityLivingBaseIn).tasks.addTask(0, new EntityAIStop((EntityLiving) entityLivingBaseIn));
        }
        super.applyAttributesModifiersToEntity(entityLivingBaseIn, attributeMapIn, amplifier);
    }
}

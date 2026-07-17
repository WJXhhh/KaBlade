package com.wjx.kablade.event;

import com.wjx.kablade.Main;
import com.wjx.kablade.util.KaBladePlayerProp;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.UUID;

/** Applies and removes the non-stacking player buff supplied by a stage light. */
@Mod.EventBusSubscriber(modid = Main.MODID)
public final class StageLightBoostHandler {

    private static final UUID MODIFIER_ID =
            UUID.fromString("7a3b8c9d-0e1f-4a5b-8c7d-9e0f1a2b3c4d");

    private StageLightBoostHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }

        EntityPlayer player = event.player;
        NBTTagCompound properties = KaBladePlayerProp.getPropCompound(player);
        int remaining = properties.getInteger(KaBladePlayerProp.STAGE_LIGHT);
        if (remaining > 0) {
            applyIfMissing(player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE));
            applyIfMissing(player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED));
            properties.setInteger(KaBladePlayerProp.STAGE_LIGHT, remaining - 1);
            KaBladePlayerProp.updateNBTForClient(player);
        } else {
            remove(player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE));
            remove(player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED));
        }
    }

    private static void applyIfMissing(IAttributeInstance attribute) {
        if (attribute == null || attribute.getModifier(MODIFIER_ID) != null) {
            return;
        }
        attribute.applyModifier(new AttributeModifier(
                MODIFIER_ID, "kablade.stage_light", 0.10D, 1).setSaved(false));
    }

    private static void remove(IAttributeInstance attribute) {
        if (attribute != null && attribute.getModifier(MODIFIER_ID) != null) {
            attribute.removeModifier(MODIFIER_ID);
        }
    }
}

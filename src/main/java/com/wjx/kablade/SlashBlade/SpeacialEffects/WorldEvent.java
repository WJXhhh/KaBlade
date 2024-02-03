/*package com.wjx.kablade.SlashBlade.SpeacialEffects;

import com.lmxxc.LMXXC;
import com.lmxxc.NBTSava;
import com.lmxxc.network.LMXXCMessage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;


import static com.lmxxc.LMXXC.PACKET_HANDLER;

@Mod.EventBusSubscriber(modid = LMXXC.Mod_ID)
public class WorldEvent {
    public WorldEvent() {
        MinecraftForge.EVENT_BUS.register(this);
    }


    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntity();
            if (!player.world.isRemote) {
                NBTTagCompound compound = NBTSava.getPlayerPersisted(player).copy();
                PACKET_HANDLER.sendTo(new LMXXCMessage(compound), (EntityPlayerMP) player);
            }
        }
    }

    @SubscribeEvent
    public void PlayerEvent(PlayerEvent.Clone event) {
        EntityPlayer origin = event.getOriginal();
        EntityPlayer player = event.getEntityPlayer();


        if (!player.world.isRemote) {

            NBTTagCompound origin_compound = NBTSava.getPlayerPersistedForLMXXC(origin).copy();

            if (origin_compound.getDouble("attackNum") > 0) {
                AbstractAttributeMap map = player.getAttributeMap();
                IAttributeInstance instance = map.getAttributeInstanceByName(SharedMonsterAttributes.ATTACK_DAMAGE.getName());
                if (instance != null) {
                    instance.applyModifier(new AttributeModifier(LMXXC.ATTACK_NUM, "attack_Num", origin_compound.getDouble("attack_Num"), 0));
                }
            }

            if (origin_compound.getDouble("healthBoost") > 0) {
                AbstractAttributeMap map = player.getAttributeMap();
                IAttributeInstance instance = map.getAttributeInstanceByName(SharedMonsterAttributes.MAX_HEALTH.getName());
                if (instance != null) {
                    instance.applyModifier(new AttributeModifier(LMXXC.HEALTH_BOOST, "healthBoost", origin_compound.getDouble("healthBoost"), 0));
                }
            }
            PACKET_HANDLER.sendTo(new LMXXCMessage(NBTSava.getPlayerPersisted(origin)), (EntityPlayerMP) player);

        }
    }

   public void clearPlayerHealthBoostModifier(EntityPlayer player){
        AbstractAttributeMap map = player.getAttributeMap();
        IAttributeInstance instance = map.getAttributeInstanceByName(SharedMonsterAttributes.MAX_HEALTH.getName());
        if (instance != null) {
            if (instance.getModifier(LMXXC.HEALTH_BOOST) != null) {
                instance.removeModifier(LMXXC.HEALTH_BOOST);
            }
            instance.applyModifier(new AttributeModifier(LMXXC.HEALTH_BOOST,"healthBoost",0,0));
        }
    }
    public void clearPlayerAttackNumModifier(EntityPlayer player){
        AbstractAttributeMap map = player.getAttributeMap();
        IAttributeInstance instance = map.getAttributeInstanceByName(SharedMonsterAttributes.ATTACK_DAMAGE.getName());
        if (instance != null) {
            if (instance.getModifier(LMXXC.ATTACK_NUM) != null) {
                instance.removeModifier(LMXXC.ATTACK_NUM);
            }
            instance.applyModifier(new AttributeModifier(LMXXC.ATTACK_NUM,"attack_Num",0,0));
        }
    }

    @SubscribeEvent
    public void TickEvent(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        NBTTagCompound compound = NBTSava.getPlayerPersistedForLMXXC(player);
        if (compound.getDouble("wudaofen")==720){
            compound.setDouble("wudaotick",compound.getDouble("wudaotick")+1);
            if (compound.getDouble("wudaotick")>=1200){
                compound.setDouble("wudaotick",0);
                compound.setDouble("wudaofen",compound.getDouble("wudaofen")-1);
            }
        }
            compound.setDouble("lmjishiliu", compound.getDouble("lmjishiliu") + 1);
            if (compound.getDouble("shouyuantime") >= 20) {
                compound.setDouble("shouyuan", compound.getDouble("shouyuan") + 1);
                compound.setDouble("lmjishiliu", 0);
                if (compound.getDouble("shouyuan") >=160) {
                    player.sendStatusMessage(new TextComponentString("寿元已尽"), false);
                    if (NBTSava.getPlayerPersisted(player).hasKey("lmxxc")) {
                        NBTSava.getPlayerPersisted(player).removeTag("lmxxc");
                        clearPlayerHealthBoostModifier(player);
                        clearPlayerAttackNumModifier(player);
                        if (!player.world.isRemote){
                            LMXXC.PACKET_HANDLER.sendTo(new LMXXCMessage(NBTSava.getPlayerPersisted(player)), (EntityPlayerMP) player);
                        }
                    }
                    compound.setDouble("shouyuan", compound.getDouble("shouyuan") - 160);
                }
            }
        if (compound.getDouble("healthBoost") != compound.getDouble("healthBoostOrigin")) {
            AbstractAttributeMap map = player.getAttributeMap();
            IAttributeInstance instance = map.getAttributeInstanceByName(SharedMonsterAttributes.MAX_HEALTH.getName());
            if (instance != null) {
                if (instance.getModifier(LMXXC.HEALTH_BOOST) != null) {
                    instance.removeModifier(LMXXC.HEALTH_BOOST);
                }
                compound.setDouble("healthBoostOrigin", compound.getDouble("healthBoost"));
                instance.applyModifier(new AttributeModifier(LMXXC.HEALTH_BOOST, "health_boost", compound.getDouble("healthBoostOrigin"), 0));
            }
        }

        if (compound.getDouble("attackNum") != compound.getDouble("attackNumOrigin")) {
            AbstractAttributeMap map = player.getAttributeMap();
            IAttributeInstance instance = map.getAttributeInstanceByName(SharedMonsterAttributes.ATTACK_DAMAGE.getName());
            if (instance != null) {
                if (instance.getModifier(LMXXC.ATTACK_NUM) != null) {
                    instance.removeModifier(LMXXC.ATTACK_NUM);
                }
                compound.setDouble("attackNumOrigin", compound.getDouble("attackNum"));
                if (!player.world.isRemote) {
                    LMXXC.PACKET_HANDLER.sendTo(new LMXXCMessage(NBTSava.getPlayerPersisted(player)), (EntityPlayerMP) player);
                }
                instance.applyModifier(new AttributeModifier(LMXXC.ATTACK_NUM, "attack_num", compound.getDouble("attackNumOrigin"), 0));
            }
        }

    }


    @SubscribeEvent
    public void LivingHurtEvent(LivingHurtEvent event) {
        Entity entity = event.getEntity();
        if (entity != null) {
            if (entity instanceof EntityLivingBase) {
                NBTTagCompound compound = NBTSava.getPlayerPersistedForLMXXC((EntityLivingBase) entity);
                double fy = compound.getDouble("defense");
                event.setAmount((float) ((event.getAmount()*event.getAmount())/(event.getAmount()+fy)));
            }

        }
    }
}*/

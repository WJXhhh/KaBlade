package com.wjx.kablade.client.renderer;

import com.wjx.kablade.Entity.EntityRaizanCleave;
import com.wjx.kablade.Main;
import com.wjx.kablade.SlashBlade.specialattack.RaizanCleaveTimeline;
import com.wjx.kablade.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.*;
import net.minecraftforge.client.event.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.*;

import java.util.*;

/** 两个新 SA 的持刀隐藏、镜头、FOV 与光敏闪光控制。 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid=Main.MODID,value=Side.CLIENT)
public final class ElectricSkillFeedback {
    private static final Set<Integer> RAIZAN_OWNERS=new HashSet<Integer>();private ElectricSkillFeedback(){}
    public static boolean isRaizanActive(int id){return RAIZAN_OWNERS.contains(id);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase!=TickEvent.Phase.END)return;RAIZAN_OWNERS.clear();Minecraft mc=Minecraft.getMinecraft();if(mc.world==null)return;for(Entity e:mc.world.loadedEntityList)if(e instanceof EntityRaizanCleave&&!e.isDead)RAIZAN_OWNERS.add(((EntityRaizanCleave)e).getOwnerId());}
    @SubscribeEvent public static void hand(RenderSpecificHandEvent event){Minecraft mc=Minecraft.getMinecraft();if(event.getHand()==EnumHand.MAIN_HAND&&mc.player!=null&&isRaizanActive(mc.player.getEntityId()))event.setCanceled(true);}
    @SubscribeEvent public static void camera(EntityViewRenderEvent.CameraSetup event){Feedback r=raizan(event.getEntity(),(float)event.getRenderPartialTicks());RaidenCycloneRenderer.Feedback c=cyclone(event.getEntity(),(float)event.getRenderPartialTicks());if(r!=null&&ModConfig.GeneralConf.RaizanCleaveCameraFeedback){float s=r.impact*r.fade;event.setYaw(event.getYaw()+MathHelper.sin(r.age*.91F+r.phase)*.60F*s);event.setPitch(event.getPitch()+MathHelper.sin(r.age*1.27F+r.phase*1.7F)*.52F*s);event.setRoll(event.getRoll()+MathHelper.sin(r.age*.73F+r.phase*.7F)*.38F*s);}if(c!=null&&ModConfig.GeneralConf.RaidenCycloneCameraFeedback){float s=c.impact*c.fade;event.setYaw(event.getYaw()+MathHelper.sin(c.seconds*18.4F+c.phase)*.78F*s);event.setPitch(event.getPitch()+MathHelper.sin(c.seconds*25.3F+c.phase*1.3F)*.61F*s);event.setRoll(event.getRoll()+MathHelper.sin(c.seconds*15.7F+c.phase*.7F)*.42F*s);}}
    @SubscribeEvent public static void fov(EntityViewRenderEvent.FOVModifier event){Feedback r=raizan(event.getEntity(),(float)event.getRenderPartialTicks());if(r!=null&&ModConfig.GeneralConf.RaizanCleaveCameraFeedback)event.setFOV(event.getFOV()+r.fov*r.fade);RaidenCycloneRenderer.Feedback c=cyclone(event.getEntity(),(float)event.getRenderPartialTicks());if(c!=null&&ModConfig.GeneralConf.RaidenCycloneCameraFeedback){float v=0;for(float h:com.wjx.kablade.SlashBlade.specialattack.RaidenCycloneTimeline.HIT_SECONDS){float a=cinematic(c.seconds,h,2.6F);if(Math.abs(a)>Math.abs(v))v=a;}event.setFOV(event.getFOV()+v*c.fade);}}
    @SubscribeEvent public static void flash(RenderGameOverlayEvent.Post event){if(event.getType()!=RenderGameOverlayEvent.ElementType.ALL)return;Minecraft mc=Minecraft.getMinecraft();if(mc.player==null)return;Feedback r=raizan(mc.player,event.getPartialTicks());RaidenCycloneRenderer.Feedback c=cyclone(mc.player,event.getPartialTicks());float strength=0;if(r!=null)strength=Math.max(strength,r.impact*r.fade*(ModConfig.GeneralConf.RaizanCleaveReducedFlash?.28F:1));if(c!=null)strength=Math.max(strength,c.impact*c.fade*(ModConfig.GeneralConf.RaidenCycloneReducedFlash?.25F:1));int alpha=(int)(MathHelper.clamp(strength*.18F,0,.18F)*255);if(alpha>0){ScaledResolution res=event.getResolution();Gui.drawRect(0,0,res.getScaledWidth(),res.getScaledHeight(),(alpha<<24)|0xEDEBFF);}}
    private static Feedback raizan(Entity viewer,float partial){if(viewer==null||viewer.world==null)return null;Vec3d camera=new Vec3d(viewer.posX,viewer.posY+viewer.getEyeHeight(),viewer.posZ);Feedback best=null;float score=0;for(Entity raw:viewer.world.loadedEntityList){if(!(raw instanceof EntityRaizanCleave)||raw.isDead)continue;EntityRaizanCleave e=(EntityRaizanCleave)raw;float age=e.getRenderAge(partial),impact=0;for(int i=0;i<RaizanCleaveTimeline.HIT_TICKS.length;i++){int h=RaizanCleaveTimeline.HIT_TICKS[i];impact=Math.max(impact,gaussian(age,h,(i==0||i==5||i==8)?.72F:.52F));}float a=cinematic(age,RaizanCleaveTimeline.HIT_TICKS[0],2.8F),b=cinematic(age,RaizanCleaveTimeline.HIT_TICKS[8],4.2F),fov=Math.abs(a)>Math.abs(b)?a:b;float fade=1-smooth((float)((camera.distanceTo(e.getTargetAnchor())-4)/28));if(impact*fade>score){score=impact*fade;best=new Feedback(age,impact,fade,fov,(e.getSeed()&0xFFFF)/65535F*(float)Math.PI*2);}}return best;}
    private static RaidenCycloneRenderer.Feedback cyclone(Entity viewer,float partial){if(viewer==null)return null;return RaidenCycloneRenderer.feedback(new Vec3d(viewer.posX,viewer.posY+viewer.getEyeHeight(),viewer.posZ),partial);}
    private static float cinematic(float age,float hit,float amp){return-amp*.54F*gaussian(age,hit-.62F,.42F)+amp*.72F*gaussian(age,hit+.58F,.78F);}private static float gaussian(float v,float c,float w){float x=(v-c)/w;return(float)Math.exp(-.5*x*x);}private static float smooth(float x){float t=MathHelper.clamp(x,0,1);return t*t*(3-2*t);}private static final class Feedback{final float age,impact,fade,fov,phase;Feedback(float a,float i,float d,float f,float p){age=a;impact=i;fade=d;fov=f;phase=p;}}
}

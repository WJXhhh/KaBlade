package com.wjx.kablade.client.renderer;

import com.wjx.kablade.Entity.EntitySevenThunders;
import com.wjx.kablade.Main;
import com.wjx.kablade.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/** 1.20 客户端反馈在 1.12.2 的镜头、FOV 与全屏闪光实现。 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid=Main.MODID,value=Side.CLIENT)
public final class SevenThundersFeedback {
    private static final double MAX_DISTANCE=48.0D;
    private SevenThundersFeedback(){}

    @SubscribeEvent public static void camera(EntityViewRenderEvent.CameraSetup event){
        if(!ModConfig.GeneralConf.SevenThundersCameraFeedback)return;Feedback f=strongest(event.getEntity(),(float)event.getRenderPartialTicks());
        if(f==null||f.impact<.002F)return;float strength=f.impact*f.distanceFade;
        if(f.mode==EntitySevenThunders.MODE_NARUKAMI_DIVINITY){
            event.setYaw(event.getYaw()+MathHelper.sin(f.frame*2.74F+f.phase)*.58F*strength);
            event.setPitch(event.getPitch()+MathHelper.sin(f.frame*3.16F+f.phase*1.5F)*.52F*strength);
            event.setRoll(event.getRoll()+MathHelper.sin(f.frame*2.21F+f.phase*.8F)*.38F*strength);
        }else{float n1=MathHelper.sin(f.frame*4.731F+f.phase),n2=MathHelper.sin(f.frame*7.913F+f.phase*1.73F);
            event.setYaw(event.getYaw()+n1*.88F*strength);event.setPitch(event.getPitch()+n2*.68F*strength);
            event.setRoll(event.getRoll()+(n1*.46F-n2*.18F)*strength);}
    }

    @SubscribeEvent public static void fov(EntityViewRenderEvent.FOVModifier event){
        if(!ModConfig.GeneralConf.SevenThundersCameraFeedback)return;Feedback f=strongest(event.getEntity(),(float)event.getRenderPartialTicks());if(f==null)return;
        float impulse=fovImpulse(f.frame,f.mode)*f.distanceFade;event.setFOV(event.getFOV()+impulse);
    }

    @SubscribeEvent public static void flash(RenderGameOverlayEvent.Post event){
        if(event.getType()!=RenderGameOverlayEvent.ElementType.ALL)return;Minecraft mc=Minecraft.getMinecraft();if(mc.player==null)return;
        Feedback f=strongest(mc.player,event.getPartialTicks());if(f==null)return;float flash=flashStrength(f.frame,f.mode)*f.distanceFade;
        float limit=f.mode==EntitySevenThunders.MODE_NARUKAMI_DIVINITY?.17F:.26F;
        if(ModConfig.GeneralConf.SevenThundersReducedFlash)limit*=f.mode==EntitySevenThunders.MODE_NARUKAMI_DIVINITY?.25F:(.10F/.26F);
        int alpha=(int)(MathHelper.clamp(flash*limit,0,limit)*255);if(alpha<=0)return;
        int tint=f.mode==EntitySevenThunders.MODE_NARUKAMI_DIVINITY?0xFFF4FF:0xF6E8FF;
        ScaledResolution res=event.getResolution();Gui.drawRect(0,0,res.getScaledWidth(),res.getScaledHeight(),(alpha<<24)|tint);
    }

    private static Feedback strongest(Entity viewer,float partial){
        if(viewer==null||viewer.world==null)return null;Vec3d camera=new Vec3d(viewer.posX,viewer.posY+viewer.getEyeHeight(),viewer.posZ);World world=viewer.world;
        AxisAlignedBB box=new AxisAlignedBB(camera.x-MAX_DISTANCE,camera.y-MAX_DISTANCE,camera.z-MAX_DISTANCE,camera.x+MAX_DISTANCE,camera.y+MAX_DISTANCE,camera.z+MAX_DISTANCE);
        List<EntitySevenThunders> casts=world.getEntitiesWithinAABB(EntitySevenThunders.class,box);Feedback best=null;float score=0;
        for(EntitySevenThunders cast:casts){float frame=(cast.ticksExisted+partial)*40/54,impact=impact(frame,cast.getMode());
            double targetDistance=camera.distanceTo(cast.getTargetAnchor(partial)),ownerDistance=camera.distanceTo(cast.getOwnerAnchor(partial));
            float fade=cast.getMode()==EntitySevenThunders.MODE_NARUKAMI_DIVINITY?1-smooth((float)Math.min(targetDistance,ownerDistance),4,34):1-smooth((float)targetDistance,5,(float)MAX_DISTANCE);
            float current=Math.max(impact,Math.abs(fovImpulse(frame,cast.getMode()))/(cast.getMode()==EntitySevenThunders.MODE_NARUKAMI_DIVINITY?3.1F:3.4F))*fade;
            if(current>score){score=current;best=new Feedback(frame,impact,fade,(cast.getSeed()&0xFFFF)/65535F*(float)Math.PI*2,cast.getMode());}}
        return best;
    }

    private static float impact(float f,int mode){if(mode==EntitySevenThunders.MODE_THUNDERBOLT_CALL)return MathHelper.clamp(
            gaussian(f,3.8F,.58F)*.58F+gaussian(f,5.05F,.92F)*.86F+gaussian(f,11.5F,.62F)*.72F+gaussian(f,12.65F,.86F)+gaussian(f,14.3F,1.45F)*.36F,0,1);
        float opening=gaussian(f,.65F,.5F),cross=gaussian(f,6.7F,.44F)*.76F+gaussian(f,7.35F,.46F)*.82F;
        float cage=Math.max(Math.max(gaussian(f,17.92F,.42F),gaussian(f,18.70F,.42F)),Math.max(gaussian(f,19.62F,.44F),gaussian(f,20.62F,.46F)));cage=Math.max(cage,gaussian(f,21.58F,.48F));cage*=.68F;
        return MathHelper.clamp(Math.max(Math.max(opening,cross),Math.max(cage,gaussian(f,23.08F,.70F)*.88F)),0,1);}
    private static float fovImpulse(float f,int mode){if(mode==EntitySevenThunders.MODE_THUNDERBOLT_CALL){float a=cinematic(f,5,2.25F),b=cinematic(f,12.55F,3.4F);return Math.abs(a)>Math.abs(b)?a:b;}
        float a=narukamiCinematic(f,.65F,2.4F),b=narukamiCinematic(f,7.15F,3.1F),c=narukamiCinematic(f,23.08F,2.7F),v=Math.abs(a)>Math.abs(b)?a:b;return Math.abs(v)>Math.abs(c)?v:c;}
    private static float cinematic(float f,float hit,float amp){return-amp*.5F*gaussian(f,hit-.42F,.32F)+amp*.70F*gaussian(f,hit+.50F,.64F);}
    private static float narukamiCinematic(float f,float hit,float amp){return-amp*.44F*gaussian(f,hit-.30F,.26F)+amp*.68F*gaussian(f,hit+.44F,.58F);}
    private static float flashStrength(float f,int mode){return mode==EntitySevenThunders.MODE_THUNDERBOLT_CALL?MathHelper.clamp(gaussian(f,5.05F,.48F)+gaussian(f,12.65F,.55F),0,1):MathHelper.clamp(gaussian(f,.65F,.38F)+gaussian(f,7.15F,.42F)+gaussian(f,23.08F,.52F),0,1);}
    private static float gaussian(float v,float c,float w){float x=(v-c)/w;return(float)Math.exp(-x*x);}private static float smooth(float v,float a,float b){float x=MathHelper.clamp((v-a)/(b-a),0,1);return x*x*(3-2*x);}
    private static final class Feedback{final float frame,impact,distanceFade,phase;final int mode;Feedback(float f,float i,float d,float p,int m){frame=f;impact=i;distanceFade=d;phase=p;mode=m;}}
}

package com.wjx.kablade.network;

import com.sun.javafx.geom.Vec3f;
import com.wjx.kablade.particle.ParticleColorfulSmoke;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MessageHandlerSpawnColorfulSmoke implements IMessageHandler<MessageSpawnColorfulSmoke, IMessage> {
    @Override
    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MessageSpawnColorfulSmoke message, MessageContext ctx) {
        if(ctx.side == Side.CLIENT)
        {
            World world = Minecraft.getMinecraft().world;
            Minecraft.getMinecraft().addScheduledTask(() -> Minecraft.getMinecraft().effectRenderer.addEffect(new ParticleColorfulSmoke(world, message.x, message.y,message.z,new Vec3f(message.r, message.g,message.b), message.life)));
        }
        return null;
    }
}

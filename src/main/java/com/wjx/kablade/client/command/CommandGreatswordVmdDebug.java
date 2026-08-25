package com.wjx.kablade.client.command;

import com.wjx.kablade.client.renderer.GreatswordVmdAnimation;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

/** 仅在反混淆开发客户端注册，用于逐帧检查 1.20 移植动作。 */
public final class CommandGreatswordVmdDebug extends CommandBase {
    @Override
    public String getName() {
        return "kabladevmd";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/kabladevmd <nuclear|valkyrie> <0-67> 或 /kabladevmd off";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        if (args.length == 1 && "off".equalsIgnoreCase(args[0])) {
            GreatswordVmdAnimation.INSTANCE.clearDebugFrame();
            sender.sendMessage(new TextComponentString("大剑 VMD 定帧已关闭"));
            return;
        }
        if (args.length != 2) throw new CommandException(getUsage(sender));

        GreatswordVmdAnimation.Skill skill;
        if ("nuclear".equalsIgnoreCase(args[0])) {
            skill = GreatswordVmdAnimation.Skill.NUCLEAR;
        } else if ("valkyrie".equalsIgnoreCase(args[0])) {
            skill = GreatswordVmdAnimation.Skill.VALKYRIE;
        } else {
            throw new CommandException(getUsage(sender));
        }
        int frame = parseInt(args[1], 0, 67);
        GreatswordVmdAnimation.INSTANCE.setDebugFrame(skill, frame);
        sender.sendMessage(new TextComponentString("大剑 VMD 定帧：" + args[0] + " / " + frame));
    }
}

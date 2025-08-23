package com.pppopipupu.combp.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

import com.pppopipupu.combp.Config;

public class CommandTick extends CommandBase {

    @Override
    public String getCommandName() {
        return Config.command_prefix + "tick";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "commands.combp.tick.usage";

    }
    @Override
    public int getRequiredPermissionLevel() {
        return 3;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {

    }
}

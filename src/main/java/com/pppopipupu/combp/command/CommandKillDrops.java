package com.pppopipupu.combp.command;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;

import com.pppopipupu.combp.Config;

public class CommandKillDrops extends CommandBase {

    @Override
    public String getCommandName() {
        return Config.command_prefix + "killdrops";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "commands.combp.killdrops.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 3;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length > 0) {
            throw new WrongUsageException("commands.combp.killdrops.usage");
        }
        int count = 0;
        List<Entity> list = sender.getEntityWorld().loadedEntityList;
        // 直接for，防止CME
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) instanceof EntityItem entityItem) {
                entityItem.setDead();
                count++;
            }
        }
        func_152373_a(sender, this, "commands.killdrops.fill.success", count);

    }
}

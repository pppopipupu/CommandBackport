package com.pppopipupu.combp.command;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentTranslation;

import com.pppopipupu.combp.Config;
import com.pppopipupu.combp.TickManager;

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
        if (args.length == 0) throw new WrongUsageException(getCommandUsage(sender));

        switch (args[0].toLowerCase()) {
            case "query":
                handleQuery(sender);
                break;
            case "rate":
                handleRate(sender, args);
                break;
            case "sprint":
                handleSprint(sender, args);
                break;
            case "freeze":
                handleFreeze(sender, args);
                break;
            case "unfreeze":
                handleUnfreeze(sender, args);
                break;
            default:
                throw new WrongUsageException(getCommandUsage(sender));
        }
    }

    private void handleQuery(ICommandSender sender) {
        ChatComponentTranslation statusMessage;
        if (TickManager.isGameFrozen()) {
            statusMessage = new ChatComponentTranslation("commands.combp.tick.query.status.frozen");
        } else if (TickManager.isSprinting()) {
            statusMessage = new ChatComponentTranslation(
                "commands.combp.tick.query.status.sprinting",
                String.format("%.1f", TickManager.getPreviousTickRate()));
        } else {
            statusMessage = new ChatComponentTranslation(
                "commands.combp.tick.query.status.normal",
                String.format("%.1f", TickManager.getTargetTickRate()));
        }
        sender.addChatMessage(statusMessage);

        TickManager.getCurrentAverageMspt()
            .ifPresent(
                mspt -> sender.addChatMessage(
                    new ChatComponentTranslation("commands.combp.tick.query.mspt", String.format("%.3f", mspt))));
    }

    private void handleRate(ICommandSender sender, String[] args) {
        if (args.length != 2) throw new WrongUsageException("commands.combp.tick.rate.usage");
        float rate = (float) parseDoubleBounded(sender, args[1], 1.0, 1000.0);
        TickManager.setTickRate(rate);
        func_152373_a(sender, this, "commands.combp.tick.rate.success", rate);
    }

    private void handleSprint(ICommandSender sender, String[] args) {
        if (args.length > 1 && "stop".equalsIgnoreCase(args[1])) {
            if (!TickManager.isSprinting()) {
                throw new WrongUsageException("commands.combp.tick.sprint.not_sprinting");
            }
            TickManager.stopSprint();
            return;
        }

        if (TickManager.isSprinting()) {
            throw new WrongUsageException("commands.combp.tick.sprint.already_sprinting");
        }

        int duration = 0;
        if (args.length == 2) {
            duration = parseIntBounded(sender, args[1], 1, 3600);
        }
        TickManager.startSprint(duration, sender);
        if (duration > 0) {
            func_152373_a(sender, this, "commands.combp.tick.sprint.success_duration", duration);
        } else {
            func_152373_a(sender, this, "commands.combp.tick.sprint.success_infinite");
        }
    }

    private void handleFreeze(ICommandSender sender, String[] args) {
        if (args.length != 1) throw new WrongUsageException("commands.combp.tick.freeze.usage");
        if (TickManager.isGameFrozen()) {
            throw new WrongUsageException("commands.combp.tick.freeze.already_frozen");
        }
        TickManager.freezeGame();
        func_152373_a(sender, this, "commands.combp.tick.freeze.success");
    }

    private void handleUnfreeze(ICommandSender sender, String[] args) {
        if (args.length != 1) throw new WrongUsageException("commands.combp.tick.unfreeze.usage");
        if (!TickManager.isGameFrozen()) {
            throw new WrongUsageException("commands.combp.tick.unfreeze.not_frozen");
        }
        TickManager.unfreezeGame();
        func_152373_a(sender, this, "commands.combp.tick.unfreeze.success");
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "query", "rate", "sprint", "freeze", "unfreeze");
        }
        if (args.length == 2 && "sprint".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "stop");
        }
        return null;
    }
}

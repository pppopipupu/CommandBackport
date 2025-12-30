package com.pppopipupu.combp.command;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;

import com.pppopipupu.combp.Config;
import com.pppopipupu.combp.TickManager;
import net.minecraftforge.event.entity.player.PlayerUseItemEvent;

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
            case "sprint" -> handleSprint(sender, args);
            case "freeze" -> handleFreeze(sender, args);
            case "unfreeze" -> handleUnfreeze(sender, args);
            case "rate" -> handleRate(sender, args);
            case "step" -> handleStep(sender, args);
            default -> throw new WrongUsageException(getCommandUsage(sender));
        }
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

        if (TickManager.isSprinting())
            throw new WrongUsageException("commands.combp.tick.sprint.already_sprinting");
        if (TickManager.isGameFrozen())
            throw new WrongUsageException("commands.combp.tick.sprint.frozen_sprint");

        int duration = 0;
        if (args.length == 2) {
            duration = parseIntBounded(sender, args[1], 1, 32767);
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

    private void handleStep(ICommandSender sender, String[] args) {
        if (args.length != 2) throw new WrongUsageException("commands.combp.tick.step.usage");
        if (args[1].equalsIgnoreCase("stop")) {
            TickManager.stepTick = 0;
            func_152373_a(sender, this, "commands.combp.tick.stop.step.success");
        } else if(TickManager.isGameFrozen()) {
            int duration = parseInt(sender, args[1]);
            TickManager.stepTick = duration;
            func_152373_a(sender, this, "commands.combp.tick.step.success", duration);
        }
        else throw new WrongUsageException("commands.combp.tick.unfreeze.not_frozen");


    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "rate", "sprint", "freeze", "unfreeze", "step");
        }
        if (args.length == 2 && "sprint".equalsIgnoreCase(args[0]) || "step".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "stop");
        }
        return null;
    }
}

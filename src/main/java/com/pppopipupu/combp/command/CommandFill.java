package com.pppopipupu.combp.command;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.pppopipupu.combp.Config;

public class CommandFill extends CommandBase {

    @Override
    public String getCommandName() {
        return Config.command_prefix + "fill";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "commands.combp.fill.usage";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 7) {
            throw new WrongUsageException("commands.combp.fill.usage");
        } else {
            EntityPlayerMP entityplayermp = getCommandSenderAsPlayer(sender);
            int i = 0;
            int fX = MathHelper.floor_double(func_110666_a(sender, entityplayermp.posX, args[i++]));
            int fY = MathHelper.floor_double(func_110666_a(sender, entityplayermp.posY, args[i++]));
            int fZ = MathHelper.floor_double(func_110666_a(sender, entityplayermp.posZ, args[i++]));
            int tX = MathHelper.floor_double(func_110666_a(sender, entityplayermp.posX, args[i++]));
            int tY = MathHelper.floor_double(func_110666_a(sender, entityplayermp.posY, args[i++]));
            int tZ = MathHelper.floor_double(func_110666_a(sender, entityplayermp.posZ, args[i++]));
            if (tX < fX) {
                int tmp = tX;
                tX = fX;
                fX = tmp;
            }
            if (tY < fY) {
                int tmp = tY;
                tY = fY;
                fY = tmp;
            }
            if (tZ < fZ) {
                int tmp = tZ;
                tZ = fZ;
                fZ = tmp;
            }
            Block block = CommandBase.getBlockByText(sender, args[i++]);
            Block replace = null;
            int meta = 0;
            int rmeta = 0;
            String mode = "destroy";
            if (i < args.length) {
                meta = CommandBase.parseIntBounded(sender, args[i++], 0, 15);
            }
            if (i < args.length) {
                mode = args[i++];
            }
            if (i < args.length) {
                replace = CommandBase.getBlockByText(sender, args[i++]);
            }
            if (i < args.length) {
                rmeta = CommandBase.parseIntBounded(sender, args[i], 0, 15);
            }
            if (mode.equalsIgnoreCase("replace") && replace == null) {
                mode = "destroy";
            }
            if (fY <= 0 || tY >= 255) {
                throw new CommandException("commands.combp.fill.outOfRange");
            }
            World world = sender.getEntityWorld();
            if (!world.checkChunksExist(fX, fY, fZ, tX, tY, tZ)) {
                throw new CommandException("commands.combp.fill.outOfWorld");
            }
            int volume = 0;
            for (int z = fZ; z <= tZ; z++) {
                for (int y = fY; y <= tY; y++) {
                    for (int x = fX; x <= tX; x++) {
                        if (mode.equalsIgnoreCase("destroy")) {
                            world.setBlock(x, y, z, block, meta, 2);
                            volume++;
                        }
                        boolean isOuter = x == tX || y == tY || z == tZ || x == fX || y == fY || z == fZ;
                        if (mode.equalsIgnoreCase("hollow")) {
                            if (isOuter) {
                                world.setBlock(x, y, z, block, meta, 2);
                                volume++;
                            } else {
                                world.setBlock(x, y, z, Blocks.air);
                            }
                        }
                        if (mode.equalsIgnoreCase("outline")) {
                            if (isOuter) {
                                world.setBlock(x, y, z, block, meta, 2);
                                volume++;
                            }
                        }
                        if (mode.equalsIgnoreCase("keep")) {
                            if (world.getBlock(x, y, z) == Blocks.air) {
                                world.setBlock(x, y, z, block, meta, 2);
                                volume++;
                            }
                        }
                        if (mode.equalsIgnoreCase("replace")) {
                            if (world.getBlock(x, y, z) == replace) {
                                world.setBlock(x, y, z, block, rmeta, 2);
                                volume++;
                            }
                        }
                    }
                }
            }
            func_152373_a(sender, this, "commands.combp.fill.success", volume);
        }
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 9) {
            return getListOfStringsMatchingLastWord(args, "replace", "destroy", "hollow", "outline", "keep");
        }
        return null;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }
}

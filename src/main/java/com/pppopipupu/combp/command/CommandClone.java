package com.pppopipupu.combp.command;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import com.pppopipupu.combp.Config;

public class CommandClone extends CommandBase {

    private enum MaskMode {
        REPLACE,
        MASKED
    }

    private enum CloneMode {
        NORMAL,
        MOVE,
        FORCE
    }

    @Override
    public String getCommandName() {
        return Config.command_prefix + "clone";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "commands.combp.clone.usage";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 9) {
            throw new WrongUsageException("commands.combp.clone.usage");
        }

        World world = sender.getEntityWorld();

        ChunkCoordinates begin = parseCoordinates(sender, args, 0);
        ChunkCoordinates end = parseCoordinates(sender, args, 3);
        ChunkCoordinates destination = parseCoordinates(sender, args, 6);

        MaskMode maskMode = MaskMode.REPLACE;
        CloneMode cloneMode = CloneMode.NORMAL;

        if (args.length >= 10) {
            String maskArg = args[9].toLowerCase();
            if ("masked".equals(maskArg)) {
                maskMode = MaskMode.MASKED;
            } else if (!"replace".equals(maskArg)) {
                throw new WrongUsageException("commands.combp.clone.invalidMaskMode", args[9]);
            }
        }

        if (args.length >= 11) {
            String cloneArg = args[10].toLowerCase();
            if ("force".equals(cloneArg)) {
                cloneMode = CloneMode.FORCE;
            } else if ("move".equals(cloneArg)) {
                cloneMode = CloneMode.MOVE;
            } else if (!"normal".equals(cloneArg)) {
                throw new WrongUsageException("commands.combp.clone.invalidCloneMode", args[10]);
            }
        }

        int minX = Math.min(begin.posX, end.posX);
        int minY = Math.min(begin.posY, end.posY);
        int minZ = Math.min(begin.posZ, end.posZ);
        int maxX = Math.max(begin.posX, end.posX);
        int maxY = Math.max(begin.posY, end.posY);
        int maxZ = Math.max(begin.posZ, end.posZ);

        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;

        if (cloneMode != CloneMode.FORCE) {
            int destMinX = destination.posX;
            int destMinY = destination.posY;
            int destMinZ = destination.posZ;
            int destMaxX = destMinX + sizeX - 1;
            int destMaxY = destMinY + sizeY - 1;
            int destMaxZ = destMinZ + sizeZ - 1;

            if ((minX <= destMaxX && maxX >= destMinX) && (minY <= destMaxY && maxY >= destMinY)
                && (minZ <= destMaxZ && maxZ >= destMinZ)) {
                throw new CommandException("commands.combp.clone.noOverlap");
            }
        }
        List<BlockInfo> blocksToClone = new ArrayList<BlockInfo>();
        for (int z = minZ; z <= maxZ; ++z) {
            for (int y = minY; y <= maxY; ++y) {
                for (int x = minX; x <= maxX; ++x) {
                    Block block = world.getBlock(x, y, z);

                    if (maskMode == MaskMode.MASKED && block.isAir(world, x, y, z)) {
                        continue;
                    }

                    int meta = world.getBlockMetadata(x, y, z);
                    NBTTagCompound nbt = null;
                    TileEntity tileEntity = world.getTileEntity(x, y, z);
                    if (tileEntity != null) {
                        nbt = new NBTTagCompound();
                        tileEntity.writeToNBT(nbt);
                    }
                    blocksToClone.add(new BlockInfo(x, y, z, block, meta, nbt));
                }
            }
        }

        int blocksAffected = 0;

        for (BlockInfo info : blocksToClone) {
            int destX = info.posX - minX + destination.posX;
            int destY = info.posY - minY + destination.posY;
            int destZ = info.posZ - minZ + destination.posZ;

            boolean success = world.setBlock(destX, destY, destZ, info.block, info.metadata, 2);

            if (success) {
                blocksAffected++;
                if (info.nbtData != null) {
                    TileEntity newTileEntity = world.getTileEntity(destX, destY, destZ);
                    if (newTileEntity != null) {
                        info.nbtData.setInteger("x", destX);
                        info.nbtData.setInteger("y", destY);
                        info.nbtData.setInteger("z", destZ);
                        newTileEntity.readFromNBT(info.nbtData);
                        newTileEntity.markDirty();
                    }
                }
            }
        }

        if (cloneMode == CloneMode.MOVE) {
            for (BlockInfo info : blocksToClone) {
                world.setBlock(info.posX, info.posY, info.posZ, Blocks.air, 0, 2);
            }
        }

        func_152373_a(sender, this, "commands.combp.clone.success", blocksAffected); // notifyAdmins
    }

    private ChunkCoordinates parseCoordinates(ICommandSender sender, String[] args, int index) {
        ChunkCoordinates baseCoords = sender.getPlayerCoordinates();
        int x = (int) Math.floor(func_110666_a(sender, baseCoords.posX, args[index]));
        int y = (int) Math.floor(func_110666_a(sender, baseCoords.posY, args[index + 1]));
        int z = (int) Math.floor(func_110666_a(sender, baseCoords.posZ, args[index + 2]));
        return new ChunkCoordinates(x, y, z);
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 10) {
            return getListOfStringsMatchingLastWord(args, "replace", "masked");
        }
        if (args.length == 11) {
            return getListOfStringsMatchingLastWord(args, "force", "move", "normal");
        }
        return null;
    }

    private static class BlockInfo {

        public final int posX, posY, posZ;
        public final Block block;
        public final int metadata;
        public final NBTTagCompound nbtData;

        public BlockInfo(int x, int y, int z, Block block, int meta, NBTTagCompound nbt) {
            this.posX = x;
            this.posY = y;
            this.posZ = z;
            this.block = block;
            this.metadata = meta;
            this.nbtData = nbt;
        }
    }
}

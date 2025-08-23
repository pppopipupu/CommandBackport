package com.pppopipupu.combp.command;

import java.util.*;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.structure.*;

import com.pppopipupu.combp.Config;
import com.pppopipupu.combp.mixin.MixinMapGenStructureAccessor;

public class CommandLocate extends CommandBase {

    private static final Map<Class<? extends IChunkProvider>, Map<String, MapGenStructure>> structureCache = new WeakHashMap<>();

    @Override
    public String getCommandName() {
        return Config.command_prefix + "locate";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "commands.combp.locate.usage";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        String type = args[0].toLowerCase();
        String targetName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        World world = sender.getEntityWorld();

        switch (type) {
            case "biome":
                sender.addChatMessage(new ChatComponentTranslation("commands.combp.locate.start.biome", targetName));
                searchForBiome(sender, world, targetName);
                break;
            case "structure":
                sender
                    .addChatMessage(new ChatComponentTranslation("commands.combp.locate.start.structure", targetName));
                searchForStructure(sender, world, targetName);
                break;
            default:
                throw new WrongUsageException(getCommandUsage(sender));
        }
    }

    private void searchForBiome(ICommandSender sender, World world, String biomeName) {
        BiomeGenBase targetBiome = null;
        for (BiomeGenBase biome : BiomeGenBase.getBiomeGenArray()) {
            if (biome != null && biome.biomeName.equalsIgnoreCase(biomeName)) {
                targetBiome = biome;
                break;
            }
        }

        if (targetBiome == null) {
            sendFailure(sender, "commands.combp.locate.biome.invalid", biomeName);
            return;
        }
        ChunkPosition result = world.getWorldChunkManager()
            .findBiomePosition(
                sender.getPlayerCoordinates().posX,
                sender.getPlayerCoordinates().posZ,
                6400,
                Collections.singletonList(targetBiome),
                new Random());

        if (result != null) {
            sendSuccess(sender, targetBiome.biomeName, result.chunkPosX, result.chunkPosZ);
        } else {
            sendFailure(sender, "commands.combp.locate.fail", biomeName);
        }
    }

    private void searchForStructure(ICommandSender sender, World world, String structureName) {
        MapGenStructure structureGenerator = getStructureGeneratorFromWorld(world, structureName);
        if (structureGenerator == null) {
            sendFailure(sender, "commands.combp.locate.structure.invalid", structureName);
            return;
        }

        MixinMapGenStructureAccessor accessor = (MixinMapGenStructureAccessor) structureGenerator;

        accessor.invoke_setupStructureSeed(world);

        ChunkPosition result = findStructureUncached(
            accessor,
            sender.getPlayerCoordinates().posX,
            sender.getPlayerCoordinates().posZ);
        if (result != null) {
            sendSuccess(sender, structureName, result.chunkPosX, result.chunkPosZ);
        } else {
            sendFailure(sender, "commands.combp.locate.fail", structureName);
        }
    }

    private ChunkPosition findStructureUncached(MixinMapGenStructureAccessor accessor, int startX, int startZ) {

        int searchRadius = 128;

        int startChunkX = startX >> 4; // startX / 16
        int startChunkZ = startZ >> 4; // startZ / 16

        for (int radius = 0; radius <= searchRadius; ++radius) {
            for (int dx = -radius; dx <= radius; ++dx) {
                boolean isEdge = (Math.abs(dx) == radius);
                for (int dz = -radius; dz <= radius; ++dz) {
                    if (!isEdge && Math.abs(dz) != radius) {
                        continue;
                    }

                    int chunkX = startChunkX + dx;
                    int chunkZ = startChunkZ + dz;

                    if (accessor.invoke_canSpawnStructureAtCoords(chunkX, chunkZ)) {

                        StructureStart structureStart = accessor.invoke_getStructureStart(chunkX, chunkZ);

                        if (structureStart != null && structureStart.getComponents() != null
                            && !structureStart.getComponents()
                                .isEmpty()) {

                            StructureBoundingBox boundingBox = structureStart.getBoundingBox();
                            if (boundingBox != null) {
                                return new ChunkPosition(boundingBox.getCenterX(), 64, boundingBox.getCenterZ());
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private MapGenStructure getStructureGeneratorFromWorld(World world, String name) {
        IChunkProvider provider = world.getChunkProvider();
        if (provider instanceof ChunkProviderServer) {
            provider = ((ChunkProviderServer) provider).currentChunkProvider;
        }

        Class<? extends IChunkProvider> providerClass = provider.getClass();
        String lowerCaseName = name.toLowerCase();

        if (!structureCache.containsKey(providerClass)) {
            Map<String, MapGenStructure> foundStructures = new HashMap<>();
            final IChunkProvider finalProvider = provider;

            for (Class<?> c = providerClass; c != null; c = c.getSuperclass()) {
                for (java.lang.reflect.Field field : c.getDeclaredFields()) {
                    try {
                        field.setAccessible(true);
                        Object fieldValue = field.get(finalProvider);
                        if (fieldValue == null) {
                            continue;
                        }

                        if (fieldValue instanceof MapGenStructure) {
                            MapGenStructure structure = (MapGenStructure) fieldValue;
                            foundStructures.putIfAbsent(
                                structure.func_143025_a()
                                    .toLowerCase(),
                                structure);
                        } else if (fieldValue instanceof Collection) {
                            for (Object element : (Collection<?>) fieldValue) {
                                if (element instanceof MapGenStructure) {
                                    MapGenStructure structure = (MapGenStructure) element;
                                    foundStructures.putIfAbsent(
                                        structure.func_143025_a()
                                            .toLowerCase(),
                                        structure);
                                }
                            }
                        } else if (fieldValue instanceof Map) {
                            for (Object mapValue : ((Map<?, ?>) fieldValue).values()) {
                                if (mapValue instanceof MapGenStructure) {
                                    MapGenStructure structure = (MapGenStructure) mapValue;
                                    foundStructures.putIfAbsent(
                                        structure.func_143025_a()
                                            .toLowerCase(),
                                        structure);
                                }
                            }
                        }
                    } catch (Exception e) {}
                }
            }
            structureCache.put(providerClass, foundStructures);
        }

        return structureCache.get(providerClass)
            .get(lowerCaseName);
    }

    private void sendSuccess(ICommandSender sender, String name, int x, int z) {
        sender.addChatMessage(new ChatComponentTranslation("commands.combp.locate.success", name, x, z));
    }

    private void sendFailure(ICommandSender sender, String translationKey, String name) {
        ChatComponentTranslation component = new ChatComponentTranslation(translationKey, name);
        component.getChatStyle()
            .setColor(EnumChatFormatting.RED);
        sender.addChatMessage(component);
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "biome", "structure");
        }
        if (args.length >= 2) {
            if ("biome".equalsIgnoreCase(args[0])) {
                List<String> biomeNames = new ArrayList<>();
                for (BiomeGenBase biome : BiomeGenBase.getBiomeGenArray()) {
                    if (biome != null) biomeNames.add(biome.biomeName);
                }
                return getListOfStringsMatchingLastWord(args, biomeNames.toArray(new String[0]));
            }
            if ("structure".equalsIgnoreCase(args[0])) {
                return getListOfStringsMatchingLastWord(
                    args,
                    getAvailableStructures(sender.getEntityWorld()).toArray(new String[0]));
            }
        }
        return null;
    }

    private List<String> getAvailableStructures(World world) {
        IChunkProvider provider = world.getChunkProvider();
        if (provider instanceof ChunkProviderServer) {
            provider = ((ChunkProviderServer) provider).currentChunkProvider;
        }

        getStructureGeneratorFromWorld(world, "dummy_to_populate_cache");

        Map<String, MapGenStructure> structures = structureCache.get(provider.getClass());
        if (structures == null || structures.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> names = new ArrayList<>();
        for (MapGenStructure structure : structures.values()) {
            names.add(structure.func_143025_a());
        }
        return names;
    }
}

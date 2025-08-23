package com.pppopipupu.combp.mixin;

import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenStructure;

import net.minecraft.world.gen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = MapGenStructure.class)
public interface MixinMapGenStructureAccessor {

    @Invoker("func_151545_a")
    ChunkPosition invoke_findNearestStructure(World world, int x, int y, int z);
    @Invoker("canSpawnStructureAtCoords")
    boolean invoke_canSpawnStructureAtCoords(int chunkX, int chunkZ);

    @Invoker("getStructureStart")
    StructureStart invoke_getStructureStart(int chunkX, int chunkZ);
    @Invoker("func_143027_a")
    void invoke_setupStructureSeed(World world);
}

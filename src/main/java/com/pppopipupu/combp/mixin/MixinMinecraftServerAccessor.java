package com.pppopipupu.combp.mixin;

import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftServer.class)
public interface MixinMinecraftServerAccessor {

    @Invoker("updateTimeLightAndEntities")
    void invokeUpdateTimeLightAndEntities();
}

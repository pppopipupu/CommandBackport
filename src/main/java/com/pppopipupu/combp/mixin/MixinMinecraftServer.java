package com.pppopipupu.combp.mixin;

import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.pppopipupu.combp.TickManager;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer {

    @Redirect(
        method = "Lnet/minecraft/server/MinecraftServer;tick()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;updateTimeLightAndEntities()V"))
    private void redirectUpdateTimeLightAndEntities(MinecraftServer server) {
        if (TickManager.targetTickRate != 20.0f || TickManager.isSprinting() || TickManager.isGameFrozen() || TickManager.isStep) {
            TickManager.runControlledTicks(server);
        } else {
            server.updateTimeLightAndEntities();
        }
    }
}

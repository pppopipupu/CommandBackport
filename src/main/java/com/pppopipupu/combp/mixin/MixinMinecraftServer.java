package com.pppopipupu.combp.mixin;

import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pppopipupu.combp.TickManager;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer {

    @Inject(
        method = "Lnet/minecraft/server/MinecraftServer;tick()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;updateTimeLightAndEntities()V"),
        cancellable = true)
    private void onUpdateTimeLightAndEntities(CallbackInfo ci) {
        ci.cancel();
        TickManager.runControlledTicks((MinecraftServer) (Object) this);
    }
}

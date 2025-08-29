package com.pppopipupu.combp.mixin;

import net.minecraft.server.management.PlayerManager;
import net.minecraft.world.WorldServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pppopipupu.combp.TickManager;

@Mixin(WorldServer.class)
public abstract class MixinWorldServer {

    @Shadow
    private PlayerManager thePlayerManager;

    @Shadow
    protected void func_147456_g() {}

    @Inject(method = "Lnet/minecraft/world/WorldServer;tick()V", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        if (TickManager.isGameFrozen()) {
            ci.cancel();
            func_147456_g();
            thePlayerManager.updatePlayerInstances();
        }
    }
}

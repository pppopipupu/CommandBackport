package com.pppopipupu.combp.mixin;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pppopipupu.combp.TickManager;

@Mixin(World.class)
public abstract class MixinWorld {

    @Shadow
    public List loadedEntityList;
    @Shadow
    public Profiler theProfiler;

    @Shadow
    public abstract void updateEntity(Entity p_72939_1_);

    @Inject(method = "Lnet/minecraft/world/World;updateEntities()V", at = @At("HEAD"), cancellable = true)
    private void onUpdateEntities(CallbackInfo ci) {
        if (TickManager.isGameFrozen()) {
            ci.cancel();
            this.theProfiler.startSection("entities");
            this.theProfiler.startSection("regular");

            for (int i = 0; i < this.loadedEntityList.size(); ++i) {
                Entity entity = (Entity) this.loadedEntityList.get(i);

                if (entity.ridingEntity != null) {
                    if (entity.ridingEntity.isDead || entity.ridingEntity.riddenByEntity != entity) {
                        entity.ridingEntity.riddenByEntity = null;
                        entity.ridingEntity = null;
                    } else {
                        continue;
                    }
                }

                boolean shouldUpdate = entity instanceof EntityPlayer
                    || (entity.riddenByEntity != null && entity.riddenByEntity instanceof EntityPlayer);
                if (shouldUpdate) {
                    this.theProfiler.startSection("tick");
                    if (!entity.isDead) {
                        try {
                            this.updateEntity(entity);
                        } catch (Throwable ignored) {}
                    }
                    this.theProfiler.endSection();
                }
            }
            this.theProfiler.endSection(); // regular
            this.theProfiler.endStartSection("blockEntities");
            this.theProfiler.endSection(); // blockEntities
            this.theProfiler.endSection(); // entities
        }
    }

    @Inject(method = "Lnet/minecraft/world/World;tick()V", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        if (TickManager.isGameFrozen()) {
            ci.cancel();
        }
    }
}

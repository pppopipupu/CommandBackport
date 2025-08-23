package com.pppopipupu.combp.mixin;

import net.minecraft.util.Timer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Timer.class)
public interface MixinTimerAccessor {

    @Accessor("ticksPerSecond")
    void setTicksPerSecond(float value);
}

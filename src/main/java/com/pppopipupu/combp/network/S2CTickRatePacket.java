package com.pppopipupu.combp.network;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;

import com.pppopipupu.combp.mixin.MixinMinecraftAccessor;
import com.pppopipupu.combp.mixin.MixinTimerAccessor;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class S2CTickRatePacket implements IMessage {

    private float tickRate;

    public S2CTickRatePacket() {}

    public S2CTickRatePacket(float tickRate) {
        this.tickRate = tickRate;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.tickRate = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeFloat(this.tickRate);
    }

    public static class Handler implements IMessageHandler<S2CTickRatePacket, IMessage> {

        @Override
        public IMessage onMessage(S2CTickRatePacket message, MessageContext ctx) {
            Minecraft.getMinecraft()
                .func_152344_a(() -> {
                    Timer timer = ((MixinMinecraftAccessor) Minecraft.getMinecraft()).getTimer();

                    if (timer != null) {
                        ((MixinTimerAccessor) timer).setTicksPerSecond(message.tickRate);
                    }
                });
            return null;
        }
    }
}

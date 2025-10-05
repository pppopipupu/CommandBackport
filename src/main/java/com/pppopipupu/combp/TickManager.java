package com.pppopipupu.combp;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;

import com.pppopipupu.combp.mixin.MixinMinecraftServerAccessor;
import com.pppopipupu.combp.network.S2CTickRatePacket;

public class TickManager {

    private static float targetTickRate = 20.0f;
    private static float previousTickRate = 20.0f;
    private static boolean isSprinting = false;
    private static long sprintStopTime = -1;
    private static double tickAccumulator = 0.0;
    private static ICommandSender sprintInitiator = null;
    private static boolean isGameFrozen = false;



    public static boolean isGameFrozen() {
        return isGameFrozen;
    }

    public static void freezeGame() {
        if (isSprinting()) {
            if (sprintInitiator != null) {
                forceStopSprintWithoutReport();
            }
        }
        isGameFrozen = true;
    }

    public static void unfreezeGame() {
        isGameFrozen = false;
    }

    public static boolean isSprinting() {
        return isSprinting;
    }

    public static void runControlledTicks(MinecraftServer server) {
        long tickLogicStartTime = System.nanoTime();
        long timeBudget = 40_000_000L;

        if (isSprinting) {
            if (sprintStopTime > 0 && System.currentTimeMillis() > sprintStopTime) {
                stopSprint();
            } else {
                while ((System.nanoTime() - tickLogicStartTime) < timeBudget) {
                    ((MixinMinecraftServerAccessor) server).invokeUpdateTimeLightAndEntities();
                }
                return;
            }
        }
        double ticksToRunThisPeriod = targetTickRate / 20.0;
        tickAccumulator += ticksToRunThisPeriod;

        double accumulatorMax = ticksToRunThisPeriod * 40.0;
        if (tickAccumulator > accumulatorMax) {
            tickAccumulator = accumulatorMax;
        }

        while (tickAccumulator >= 1.0 && (System.nanoTime() - tickLogicStartTime) < timeBudget) {
            ((MixinMinecraftServerAccessor) server).invokeUpdateTimeLightAndEntities();
            tickAccumulator -= 1.0;
        }
    }


    public static void setTickRate(float rate) {
        targetTickRate = Math.max(1.0f, rate);
        isSprinting = false;
        sprintStopTime = -1;
        tickAccumulator = 0;
        CommonProxy.network.sendToAll(new S2CTickRatePacket(targetTickRate));
    }

    public static void startSprint(int durationSeconds, ICommandSender initiator) {
        if (!isSprinting) {
            previousTickRate = targetTickRate;
        }
        isSprinting = true;
        sprintInitiator = initiator;
        if (durationSeconds > 0) {
            sprintStopTime = System.currentTimeMillis() + durationSeconds * 1000L;
        } else {
            sprintStopTime = -1;
        }
        tickAccumulator = 0;
        CommonProxy.network.sendToAll(new S2CTickRatePacket(20.0f));
    }

    public static void stopSprint() {
        if (!isSprinting) return;


        if (sprintInitiator != null) {
            ChatComponentTranslation stopMessage = new ChatComponentTranslation(
                "commands.combp.tick.sprint.stop.success");
            stopMessage.getChatStyle()
                .setColor(EnumChatFormatting.GREEN);
            sprintInitiator.addChatMessage(stopMessage);

        }

        forceStopSprintWithoutReport();
    }

    public static void forceStopSprintWithoutReport() {
        isSprinting = false;
        sprintStopTime = -1;
        sprintInitiator = null;
        setTickRate(previousTickRate);
    }
}

package com.pppopipupu.combp;

import java.util.Arrays;
import java.util.OptionalDouble;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;

import com.pppopipupu.combp.mixin.MixinMinecraftServerAccessor;
import com.pppopipupu.combp.network.S2CTickRatePacket;

public class TickManager {

    private static final int PERFORMANCE_DATA_CAPACITY = 1200;
    private static float targetTickRate = 20.0f;
    private static float previousTickRate = 20.0f;
    private static boolean isSprinting = false;
    private static long sprintStopTime = -1;
    private static double tickAccumulator = 0.0;
    private static final long[] tickTimes = new long[PERFORMANCE_DATA_CAPACITY];
    private static int tickTimesIndex = 0;
    private static int tickTimesCount = 0;
    private static ICommandSender sprintInitiator = null;
    private static long tickTimeSum = 0;
    private static boolean isGameFrozen = false;

    public static float getTargetTickRate() {
        return targetTickRate;
    }

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

    public static float getPreviousTickRate() {
        return previousTickRate;
    }

    public static OptionalDouble getCurrentAverageMspt() {
        if (tickTimesCount == 0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of((double) tickTimeSum / tickTimesCount / 1_000_000.0);
    }

    public static void runControlledTicks(MinecraftServer server) {
        long tickLogicStartTime = System.nanoTime();
        long timeBudget = 40_000_000L;

        if (isSprinting) {
            if (sprintStopTime > 0 && System.currentTimeMillis() > sprintStopTime) {
                stopSprint();
            } else {
                while ((System.nanoTime() - tickLogicStartTime) < timeBudget) {
                    tickOnce(server);
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
            tickOnce(server);
            tickAccumulator -= 1.0;
        }
    }

    private static void tickOnce(MinecraftServer server) {
        long startTime = System.nanoTime();
        ((MixinMinecraftServerAccessor) server).invokeUpdateTimeLightAndEntities();
        long endTime = System.nanoTime();

        long newTickTime = endTime - startTime;

        if (tickTimesCount == PERFORMANCE_DATA_CAPACITY) {
            tickTimeSum -= tickTimes[tickTimesIndex];
        }

        tickTimes[tickTimesIndex] = newTickTime;
        tickTimeSum += newTickTime;

        tickTimesIndex = (tickTimesIndex + 1) % PERFORMANCE_DATA_CAPACITY;
        if (tickTimesCount < PERFORMANCE_DATA_CAPACITY) {
            tickTimesCount++;
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

        PerformanceData report = getPerformanceData();

        if (sprintInitiator != null) {
            ChatComponentTranslation stopMessage = new ChatComponentTranslation(
                "commands.combp.tick.sprint.stop.success");
            stopMessage.getChatStyle()
                .setColor(EnumChatFormatting.GREEN);
            sprintInitiator.addChatMessage(stopMessage);

            if (report != null && report.hasData) {
                sprintInitiator.addChatMessage(
                    new ChatComponentTranslation(
                        "commands.combp.tick.report.header",
                        report.tickCount,
                        String.format("%.3f", report.averageMspt)));
                sprintInitiator.addChatMessage(
                    new ChatComponentTranslation(
                        "commands.combp.tick.report.body",
                        String.format("%.3f", report.p50),
                        String.format("%.3f", report.p90),
                        String.format("%.3f", report.p99)));
            } else {
                sprintInitiator.addChatMessage(new ChatComponentTranslation("commands.combp.tick.report.no_data"));
            }
        }

        forceStopSprintWithoutReport();
    }

    public static void forceStopSprintWithoutReport() {
        isSprinting = false;
        sprintStopTime = -1;
        sprintInitiator = null;
        setTickRate(previousTickRate);
    }

    private static PerformanceData getPerformanceData() {
        if (tickTimesCount == 0) {
            return new PerformanceData(false, 0, 0, 0, 0, 0);
        }

        long[] sortedTimes = new long[tickTimesCount];
        System.arraycopy(tickTimes, 0, sortedTimes, 0, tickTimesCount);

        tickTimeSum = 0;
        tickTimesCount = 0;
        tickTimesIndex = 0;

        Arrays.sort(sortedTimes);
        double averageMs = Arrays.stream(sortedTimes)
            .average()
            .orElse(0) / 1_000_000.0;

        double p50Ms = sortedTimes[sortedTimes.length / 2] / 1_000_000.0;
        double p90Ms = sortedTimes[(int) (sortedTimes.length * 0.9)] / 1_000_000.0;
        double p99Ms = sortedTimes[(int) (sortedTimes.length * 0.99)] / 1_000_000.0;

        return new PerformanceData(true, sortedTimes.length, averageMs, p50Ms, p90Ms, p99Ms);
    }

    public static class PerformanceData {

        public final boolean hasData;
        public final int tickCount;
        public final double averageMspt;
        public final double p50, p90, p99;

        public PerformanceData(boolean hasData, int tickCount, double averageMspt, double p50, double p90, double p99) {
            this.hasData = hasData;
            this.tickCount = tickCount;
            this.averageMspt = averageMspt;
            this.p50 = p50;
            this.p90 = p90;
            this.p99 = p99;
        }
    }
}

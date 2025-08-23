package com.pppopipupu.combp;

import java.util.Arrays;
import java.util.LinkedList;
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
    private static final LinkedList<Long> tickTimes = new LinkedList<>();
    private static ICommandSender sprintInitiator = null;

    public static float getTargetTickRate() {
        return targetTickRate;
    }

    public static boolean isSprinting() {
        return isSprinting;
    }

    public static float getPreviousTickRate() {
        return previousTickRate;
    }

    public static OptionalDouble getCurrentAverageMspt() {
        synchronized (tickTimes) {
            if (tickTimes.isEmpty()) {
                return OptionalDouble.empty();
            }
            return OptionalDouble.of(
                tickTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0) / 1_000_000.0);
        }
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

        synchronized (tickTimes) {
            tickTimes.add(endTime - startTime);
            if (tickTimes.size() > PERFORMANCE_DATA_CAPACITY) {
                tickTimes.removeFirst();
            }
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

    private static void forceStopSprintWithoutReport() {
        isSprinting = false;
        sprintStopTime = -1;
        sprintInitiator = null;
        setTickRate(previousTickRate);
    }

    private static PerformanceData getPerformanceData() {
        if (tickTimes.isEmpty()) {
            return new PerformanceData(false, 0, 0, 0, 0, 0);
        }
        long[] times;
        synchronized (tickTimes) {
            times = tickTimes.stream()
                .mapToLong(Long::longValue)
                .toArray();
            tickTimes.clear();
        }
        Arrays.sort(times);
        double averageMs = Arrays.stream(times)
            .average()
            .orElse(0) / 1_000_000.0;
        double p50Ms = times[times.length / 2] / 1_000_000.0;
        double p90Ms = times[(int) (times.length * 0.9)] / 1_000_000.0;
        double p99Ms = times[(int) (times.length * 0.99)] / 1_000_000.0;
        return new PerformanceData(true, times.length, averageMs, p50Ms, p90Ms, p99Ms);
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

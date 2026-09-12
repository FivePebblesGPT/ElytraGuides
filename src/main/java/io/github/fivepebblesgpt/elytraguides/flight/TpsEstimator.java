package io.github.fivepebblesgpt.elytraguides.flight;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Estimates effective TPS from the server's periodic game-time synchronization packets.
 * Client tick spacing is retained as a fallback until enough server samples exist, and
 * also covers local client/integrated-server slowdowns before the next time sync arrives.
 */
public final class TpsEstimator {
    private static final int MAX_CLIENT_SAMPLES = 21;
    private static final int MAX_SERVER_SAMPLES = 6;
    private static final long SERVER_SAMPLE_STALE_NANOS = 30_000_000_000L;
    private static final double MIN_TPS = 1.0;
    private static final double MAX_TPS = 20.0;

    private final Deque<Long> clientTickTimesNanos = new ArrayDeque<>(MAX_CLIENT_SAMPLES);
    private final Deque<ServerSample> serverSamples = new ArrayDeque<>(MAX_SERVER_SAMPLES);

    public void onClientTick(long nowNanos) {
        clientTickTimesNanos.addLast(nowNanos);
        while (clientTickTimesNanos.size() > MAX_CLIENT_SAMPLES) {
            clientTickTimesNanos.removeFirst();
        }
    }

    public synchronized void onServerTimeSync(long gameTime, long nowNanos) {
        ServerSample previous = serverSamples.peekLast();
        if (previous != null) {
            long tickDelta = gameTime - previous.gameTime();

            // Extra time packets can be emitted by clock/time mutations. The routine sync
            // cadence is 20 server ticks, so very small deltas are not useful TPS samples.
            if (tickDelta > 0 && tickDelta < 10) {
                return;
            }

            if (tickDelta <= 0 || tickDelta > 200) {
                serverSamples.clear();
            }
        }

        serverSamples.addLast(new ServerSample(gameTime, nowNanos));
        while (serverSamples.size() > MAX_SERVER_SAMPLES) {
            serverSamples.removeFirst();
        }
    }

    public synchronized double estimatedTps() {
        long nowNanos = System.nanoTime();
        if (serverSamples.size() >= 2
                && nowNanos - serverSamples.getLast().arrivalNanos() <= SERVER_SAMPLE_STALE_NANOS) {
            ServerSample first = serverSamples.getFirst();
            ServerSample last = serverSamples.getLast();
            long tickDelta = last.gameTime() - first.gameTime();
            double seconds = (last.arrivalNanos() - first.arrivalNanos()) / 1_000_000_000.0;

            if (tickDelta > 0 && seconds > 0.0) {
                return clampTps(tickDelta / seconds);
            }
        }

        return estimatedClientTps();
    }

    public synchronized void reset() {
        clientTickTimesNanos.clear();
        serverSamples.clear();
    }

    private double estimatedClientTps() {
        if (clientTickTimesNanos.size() < 3) {
            return MAX_TPS;
        }

        long first = clientTickTimesNanos.getFirst();
        long last = clientTickTimesNanos.getLast();
        double seconds = (last - first) / 1_000_000_000.0;
        if (seconds <= 0.0) {
            return MAX_TPS;
        }

        return clampTps((clientTickTimesNanos.size() - 1) / seconds);
    }

    private static double clampTps(double tps) {
        return Math.clamp(tps, MIN_TPS, MAX_TPS);
    }

    private record ServerSample(long gameTime, long arrivalNanos) {}
}

package io.github.fivepebblesgpt.elytraguides.flight;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Estimates the effective client simulation tick rate from wall-clock tick spacing.
 * This catches local/integrated-server slowdowns. A remote server's authoritative TPS
 * is not directly observable from a vanilla client, so it cannot be measured exactly.
 */
public final class TpsEstimator {
    private static final int MAX_SAMPLES = 21;
    private static final double MIN_TPS = 1.0;
    private static final double MAX_TPS = 20.0;

    private final Deque<Long> tickTimesNanos = new ArrayDeque<>(MAX_SAMPLES);

    public void onTick(long nowNanos) {
        tickTimesNanos.addLast(nowNanos);
        while (tickTimesNanos.size() > MAX_SAMPLES) {
            tickTimesNanos.removeFirst();
        }
    }

    public double estimatedTps() {
        if (tickTimesNanos.size() < 3) {
            return MAX_TPS;
        }

        long first = tickTimesNanos.getFirst();
        long last = tickTimesNanos.getLast();
        double seconds = (last - first) / 1_000_000_000.0;
        if (seconds <= 0.0) {
            return MAX_TPS;
        }

        double tps = (tickTimesNanos.size() - 1) / seconds;
        return Math.clamp(tps, MIN_TPS, MAX_TPS);
    }

    public void reset() {
        tickTimesNanos.clear();
    }
}

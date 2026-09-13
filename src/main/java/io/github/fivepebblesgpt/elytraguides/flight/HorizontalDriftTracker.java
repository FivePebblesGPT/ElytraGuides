package io.github.fivepebblesgpt.elytraguides.flight;

import java.util.ArrayDeque;
import java.util.Deque;

public final class HorizontalDriftTracker {
    private static final long WINDOW_NANOS = 2_000_000_000L;

    private final Deque<YawSample> samples = new ArrayDeque<>();

    public void tick(boolean isFlying, float yawDegrees, long nowNanos) {
        if (!isFlying) {
            reset();
            return;
        }

        samples.addLast(new YawSample(nowNanos, yawDegrees));
        long cutoff = nowNanos - WINDOW_NANOS;
        while (!samples.isEmpty() && samples.getFirst().timeNanos() < cutoff) {
            samples.removeFirst();
        }
    }

    public boolean hasHistory() {
        return samples.size() >= 2;
    }

    public double averageYawDegrees() {
        if (samples.isEmpty()) {
            return 0.0;
        }

        double sumSin = 0.0;
        double sumCos = 0.0;
        for (YawSample sample : samples) {
            double radians = Math.toRadians(sample.yawDegrees());
            sumSin += Math.sin(radians);
            sumCos += Math.cos(radians);
        }

        if (Math.abs(sumSin) < 1.0e-12 && Math.abs(sumCos) < 1.0e-12) {
            return samples.getLast().yawDegrees();
        }

        return Math.toDegrees(Math.atan2(sumSin, sumCos));
    }

    public void reset() {
        samples.clear();
    }

    private record YawSample(long timeNanos, double yawDegrees) {
    }
}

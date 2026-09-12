package io.github.fivepebblesgpt.elytraguides.flight;

import io.github.fivepebblesgpt.elytraguides.config.ElytraGuidesConfig;

public final class ManeuverGuide {
    private final ElytraGuidesConfig config;

    private boolean flying;
    private boolean hasPreviousPitch;
    private float previousPitch;

    private boolean targetActive;
    private double targetPitch;
    private long lastTargetUpdateNanos;

    public ManeuverGuide(ElytraGuidesConfig config) {
        this.config = config;
    }

    public void tick(boolean isFlying, float currentPitch, long nowNanos) {
        if (!isFlying || !config.enabled()) {
            reset();
            return;
        }

        if (!flying) {
            flying = true;
            previousPitch = currentPitch;
            hasPreviousPitch = true;
            return;
        }

        if (hasPreviousPitch && crossedSnapPitch(previousPitch, currentPitch)) {
            resetTarget(nowNanos);
        }

        previousPitch = currentPitch;
        hasPreviousPitch = true;
    }

    public void advanceTarget(long nowNanos, double estimatedTps) {
        if (!targetActive) {
            return;
        }

        if (lastTargetUpdateNanos == 0L) {
            lastTargetUpdateNanos = nowNanos;
            return;
        }

        double elapsedSeconds = Math.max(0.0, (nowNanos - lastTargetUpdateNanos) / 1_000_000_000.0);
        lastTargetUpdateNanos = nowNanos;

        double tpsScale = config.compensateForTps() ? Math.clamp(estimatedTps / 20.0, 0.05, 1.0) : 1.0;
        double degreesToMove = config.returnRateDegreesPerSecond() * tpsScale * elapsedSeconds;
        targetPitch = moveToward(targetPitch, config.approachPitch(), degreesToMove);
    }

    public boolean isTargetActive() {
        return targetActive;
    }

    public double targetPitch() {
        return targetPitch;
    }

    public boolean isOnTarget(float currentPitch) {
        return targetActive && Math.abs(currentPitch - targetPitch) <= config.targetTolerance();
    }

    public double targetError(float currentPitch) {
        return currentPitch - targetPitch;
    }

    public void reset() {
        flying = false;
        hasPreviousPitch = false;
        targetActive = false;
        targetPitch = config.snapPitch();
        lastTargetUpdateNanos = 0L;
    }

    private void resetTarget(long nowNanos) {
        targetActive = true;
        targetPitch = config.snapPitch();
        lastTargetUpdateNanos = nowNanos;
    }

    private boolean crossedSnapPitch(float previous, float current) {
        double approachToSnap = config.snapPitch() - config.approachPitch();
        if (approachToSnap >= 0.0) {
            return previous < config.snapPitch() && current >= config.snapPitch();
        }
        return previous > config.snapPitch() && current <= config.snapPitch();
    }

    private static double moveToward(double current, double target, double maxDelta) {
        double delta = target - current;
        if (Math.abs(delta) <= maxDelta) {
            return target;
        }
        return current + Math.copySign(maxDelta, delta);
    }
}

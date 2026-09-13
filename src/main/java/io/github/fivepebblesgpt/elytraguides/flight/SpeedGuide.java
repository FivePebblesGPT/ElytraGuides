package io.github.fivepebblesgpt.elytraguides.flight;

import io.github.fivepebblesgpt.elytraguides.config.ElytraGuidesConfig;

public final class SpeedGuide {
    private static final double TICKS_PER_SECOND = 20.0;

    private final ElytraGuidesConfig config;

    private boolean hasSample;
    private boolean optimal;
    private double horizontalSpeed;
    private double verticalSpeed;

    public SpeedGuide(ElytraGuidesConfig config) {
        this.config = config;
    }

    public void tick(boolean isFlying, double velocityX, double velocityY, double velocityZ) {
        if (!isFlying) {
            reset();
            return;
        }

        horizontalSpeed = Math.hypot(velocityX, velocityZ) * TICKS_PER_SECOND;
        verticalSpeed = velocityY * TICKS_PER_SECOND;
        hasSample = true;
        optimal = isInsideOptimalEnvelope(horizontalSpeed, verticalSpeed);
    }

    public void reset() {
        hasSample = false;
        optimal = false;
        horizontalSpeed = 0.0;
        verticalSpeed = 0.0;
    }

    public boolean hasSample() {
        return hasSample;
    }

    public boolean isOptimal() {
        return optimal;
    }

    public double horizontalSpeed() {
        return horizontalSpeed;
    }

    public double verticalSpeed() {
        return verticalSpeed;
    }

    public double optimalHorizontalMin() {
        return Math.min(config.optimalHorizontalSpeedMin(), config.optimalHorizontalSpeedMax());
    }

    public double optimalHorizontalMax() {
        return Math.max(config.optimalHorizontalSpeedMin(), config.optimalHorizontalSpeedMax());
    }

    private boolean isInsideOptimalEnvelope(double horizontal, double vertical) {
        return horizontal >= optimalHorizontalMin()
                && horizontal <= optimalHorizontalMax()
                && Math.abs(vertical - config.optimalVerticalSpeed()) <= config.optimalVerticalSpeedTolerance();
    }
}

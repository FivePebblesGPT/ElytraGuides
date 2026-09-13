package io.github.fivepebblesgpt.elytraguides.flight;

import io.github.fivepebblesgpt.elytraguides.config.ElytraGuidesConfig;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class SpeedGuide {
    private static final double TICKS_PER_SECOND = 20.0;
    private static final double HORIZONTAL_REARM_MARGIN = 0.5;
    private static final double VERTICAL_REARM_MARGIN = 0.5;

    private final ElytraGuidesConfig config;
    private final NotificationService notifications;

    private boolean hasSample;
    private boolean optimal;
    private boolean notificationArmed = true;
    private double horizontalSpeed;
    private double verticalSpeed;

    public SpeedGuide(ElytraGuidesConfig config, NotificationService notifications) {
        this.config = config;
        this.notifications = notifications;
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
        if (optimal && notificationArmed && config.notifyOptimalSpeed()) {
            notifications.send(optimalSpeedMessage(), config.optimalSpeedDestination());
            notificationArmed = false;
        } else if (!optimal && isClearlyOutsideOptimalEnvelope(horizontalSpeed, verticalSpeed)) {
            notificationArmed = true;
        }
    }

    public void reset() {
        hasSample = false;
        optimal = false;
        notificationArmed = true;
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

    private boolean isClearlyOutsideOptimalEnvelope(double horizontal, double vertical) {
        return horizontal < optimalHorizontalMin() - HORIZONTAL_REARM_MARGIN
                || horizontal > optimalHorizontalMax() + HORIZONTAL_REARM_MARGIN
                || Math.abs(vertical - config.optimalVerticalSpeed())
                > config.optimalVerticalSpeedTolerance() + VERTICAL_REARM_MARGIN;
    }

    private Component optimalSpeedMessage() {
        int color = config.onTargetColor().argb() & 0x00FFFFFF;
        return Component.literal(String.format(
                Locale.ROOT,
                "Optimal speed  •  H %.2f m/s  •  V %.2f m/s",
                horizontalSpeed,
                verticalSpeed
        )).withStyle(style -> style.withColor(color));
    }
}

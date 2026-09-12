package io.github.fivepebblesgpt.elytraguides.flight;

import io.github.fivepebblesgpt.elytraguides.config.ElytraGuidesConfig;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class FlightAltitudeTracker {
    private static final double MIN_REPORTABLE_SEGMENT = 0.01;

    private final ElytraGuidesConfig config;
    private final NotificationService notifications;

    private boolean inFlight;
    private double startY;
    private double maxY;
    private double minY;
    private double totalAscent;

    private double lastTurnY;
    private TurnType lastTurnType = TurnType.START;

    private boolean hasSignedSample;
    private int lastSign;
    private double lastSignedY;
    private double lastSignedVelocity;

    public FlightAltitudeTracker(ElytraGuidesConfig config, NotificationService notifications) {
        this.config = config;
        this.notifications = notifications;
    }

    public void tick(boolean isFlying, double y, double verticalVelocity) {
        if (!isFlying) {
            if (inFlight) {
                endFlight(y);
            }
            return;
        }

        if (!inFlight) {
            beginFlight(y);
        }

        maxY = Math.max(maxY, y);
        minY = Math.min(minY, y);

        int sign = velocitySign(verticalVelocity, config.verticalSpeedDeadzone());
        if (sign == 0) {
            return;
        }

        if (!hasSignedSample) {
            hasSignedSample = true;
            lastSign = sign;
            lastSignedY = y;
            lastSignedVelocity = verticalVelocity;
            return;
        }

        if (sign != lastSign) {
            double crossingY = zeroCrossingY(lastSignedY, lastSignedVelocity, y, verticalVelocity);
            maxY = Math.max(maxY, crossingY);
            minY = Math.min(minY, crossingY);

            if (lastSign > 0 && sign < 0) {
                onPeak(crossingY);
            } else if (lastSign < 0 && sign > 0) {
                onTrough(crossingY);
            }

            lastSign = sign;
        }

        lastSignedY = y;
        lastSignedVelocity = verticalVelocity;
    }

    public void reset() {
        inFlight = false;
        hasSignedSample = false;
        lastTurnType = TurnType.START;
        totalAscent = 0.0;
    }

    private void beginFlight(double y) {
        inFlight = true;
        startY = y;
        maxY = y;
        minY = y;
        totalAscent = 0.0;
        lastTurnY = y;
        lastTurnType = TurnType.START;
        hasSignedSample = false;
    }

    private void onPeak(double peakY) {
        double ascent = peakY - lastTurnY;
        if (ascent > 0.0) {
            totalAscent += ascent;
        }

        if (config.logAscentAtPeak() && ascent >= MIN_REPORTABLE_SEGMENT) {
            notifications.send(
                    Component.literal(String.format(Locale.ROOT, "Ascent +%.2f blocks  •  peak Y %.2f", ascent, peakY)),
                    config.turnPointDestination()
            );
        }

        lastTurnY = peakY;
        lastTurnType = TurnType.PEAK;
    }

    private void onTrough(double troughY) {
        double descent = lastTurnY - troughY;
        if (config.logDescentAtTrough() && descent >= MIN_REPORTABLE_SEGMENT) {
            notifications.send(
                    Component.literal(String.format(Locale.ROOT, "Descent -%.2f blocks  •  trough Y %.2f", descent, troughY)),
                    config.turnPointDestination()
            );
        }

        lastTurnY = troughY;
        lastTurnType = TurnType.TROUGH;
    }

    private void endFlight(double endY) {
        double finalTotalAscent = totalAscent;
        if (hasSignedSample && lastSign > 0) {
            finalTotalAscent += Math.max(0.0, endY - lastTurnY);
        }

        maxY = Math.max(maxY, endY);
        minY = Math.min(minY, endY);

        if (config.flightEndSummary()) {
            notifications.send(
                    Component.literal(String.format(
                            Locale.ROOT,
                            "Flight ended  •  max Y %.2f  •  total gain +%.2f blocks",
                            maxY,
                            finalTotalAscent
                    )),
                    config.flightEndDestination()
            );
        }

        reset();
    }

    private static int velocitySign(double velocity, double deadzone) {
        if (velocity > deadzone) {
            return 1;
        }
        if (velocity < -deadzone) {
            return -1;
        }
        return 0;
    }

    private static double zeroCrossingY(double y0, double velocity0, double y1, double velocity1) {
        double denominator = Math.abs(velocity0) + Math.abs(velocity1);
        if (denominator <= 1.0e-9) {
            return (y0 + y1) * 0.5;
        }

        double fraction = Math.abs(velocity0) / denominator;
        return y0 + (y1 - y0) * fraction;
    }

    private enum TurnType {
        START,
        PEAK,
        TROUGH
    }
}

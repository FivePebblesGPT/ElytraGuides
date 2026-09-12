package io.github.fivepebblesgpt.elytraguides.flight;

import io.github.fivepebblesgpt.elytraguides.config.ElytraGuidesConfig;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class FlightAltitudeTracker {
    private static final double MIN_REPORTABLE_AMPLITUDE = 0.01;

    private final ElytraGuidesConfig config;
    private final NotificationService notifications;

    private boolean inFlight;
    private double maxY;

    private double lastTurnY;
    private TurnType lastTurnType = TurnType.START;

    private int peakCount;
    private double firstPeakY;
    private double previousPeakY;

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
        peakCount = 0;
    }

    private void beginFlight(double y) {
        inFlight = true;
        maxY = y;
        lastTurnY = y;
        lastTurnType = TurnType.START;
        peakCount = 0;
        hasSignedSample = false;
    }

    private void onPeak(double peakY) {
        if (peakCount == 0) {
            firstPeakY = peakY;
        } else if (config.logAscentAtPeak()) {
            double cycleGain = peakY - previousPeakY;
            String amplitudePart = cycleAmplitudePart(peakY);

            notifications.send(
                    Component.literal(String.format(
                            Locale.ROOT,
                            "Cycle gain %+.2f blocks%s  •  peak Y %.2f",
                            cycleGain,
                            amplitudePart,
                            peakY
                    )),
                    config.turnPointDestination()
            );
        }

        previousPeakY = peakY;
        peakCount++;
        lastTurnY = peakY;
        lastTurnType = TurnType.PEAK;
    }

    private String cycleAmplitudePart(double currentPeakY) {
        if (!config.logDescentAtTrough() || lastTurnType != TurnType.TROUGH) {
            return "";
        }

        double descentAmplitude = previousPeakY - lastTurnY;
        double ascentAmplitude = currentPeakY - lastTurnY;
        if (descentAmplitude < MIN_REPORTABLE_AMPLITUDE && ascentAmplitude < MIN_REPORTABLE_AMPLITUDE) {
            return "";
        }

        return String.format(
                Locale.ROOT,
                "  •  amplitude ↓%.2f / ↑%.2f",
                Math.max(0.0, descentAmplitude),
                Math.max(0.0, ascentAmplitude)
        );
    }

    private void onTrough(double troughY) {
        lastTurnY = troughY;
        lastTurnType = TurnType.TROUGH;
    }

    private void endFlight(double endY) {
        maxY = Math.max(maxY, endY);

        if (config.flightEndSummary()) {
            Component summary;
            if (peakCount >= 2) {
                double netPeakGain = previousPeakY - firstPeakY;
                summary = Component.literal(String.format(
                        Locale.ROOT,
                        "Flight ended  •  max Y %.2f  •  net peak gain %+.2f blocks",
                        maxY,
                        netPeakGain
                ));
            } else {
                summary = Component.literal(String.format(
                        Locale.ROOT,
                        "Flight ended  •  max Y %.2f  •  net peak gain n/a",
                        maxY
                ));
            }

            notifications.send(summary, config.flightEndDestination());
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

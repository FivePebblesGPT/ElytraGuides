package io.github.fivepebblesgpt.elytraguides.flight;

import io.github.fivepebblesgpt.elytraguides.config.ElytraGuidesConfig;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class FlightAltitudeTracker {
    private static final double MIN_REPORTABLE_AMPLITUDE = 0.01;
    private static final int NEGATIVE_GAIN_COLOR = 0xFF5555;
    private static final int NEUTRAL_GAIN_COLOR = 0xFFFFFF;

    private final ElytraGuidesConfig config;
    private final NotificationService notifications;

    private boolean inFlight;
    private double maxY;

    private double lastTurnY;
    private TurnType lastTurnType = TurnType.START;

    private int peakCount;
    private double firstPeakY;
    private double previousPeakY;

    private long flightStartedNanos;
    private double traveledDistance;
    private double horizontalDistance;
    private boolean hasPositionSample;
    private double lastX;
    private double lastY;
    private double lastZ;

    private boolean hasSignedSample;
    private int lastSign;
    private double lastSignedY;
    private double lastSignedVelocity;

    public FlightAltitudeTracker(ElytraGuidesConfig config, NotificationService notifications) {
        this.config = config;
        this.notifications = notifications;
    }

    public void tick(
            boolean isFlying,
            double x,
            double y,
            double z,
            double verticalVelocity,
            long nowNanos
    ) {
        if (!isFlying) {
            if (inFlight) {
                updateDistance(x, y, z);
                endFlight(y, nowNanos);
            }
            return;
        }

        if (!inFlight) {
            beginFlight(x, y, z, nowNanos);
        } else {
            updateDistance(x, y, z);
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
        hasPositionSample = false;
        lastTurnType = TurnType.START;
        peakCount = 0;
        traveledDistance = 0.0;
        horizontalDistance = 0.0;
        flightStartedNanos = 0L;
    }

    private void beginFlight(double x, double y, double z, long nowNanos) {
        inFlight = true;
        maxY = y;
        lastTurnY = y;
        lastTurnType = TurnType.START;
        peakCount = 0;
        hasSignedSample = false;

        flightStartedNanos = nowNanos;
        traveledDistance = 0.0;
        horizontalDistance = 0.0;
        hasPositionSample = true;
        lastX = x;
        lastY = y;
        lastZ = z;
    }

    private void updateDistance(double x, double y, double z) {
        if (!hasPositionSample) {
            hasPositionSample = true;
            lastX = x;
            lastY = y;
            lastZ = z;
            return;
        }

        double dx = x - lastX;
        double dy = y - lastY;
        double dz = z - lastZ;

        traveledDistance += Math.sqrt(dx * dx + dy * dy + dz * dz);
        horizontalDistance += Math.sqrt(dx * dx + dz * dz);

        lastX = x;
        lastY = y;
        lastZ = z;
    }

    private void onPeak(double peakY) {
        if (peakCount == 0) {
            firstPeakY = peakY;
        } else if (config.logAscentAtPeak()) {
            double cycleGain = peakY - previousPeakY;
            String amplitudePart = cycleAmplitudePart(peakY);

            var message = Component.literal("Cycle gain ");
            message.append(
                    Component.literal(String.format(Locale.ROOT, "%+.2f blocks", cycleGain))
                            .withStyle(style -> style.withColor(gainColor(cycleGain)))
            );
            message.append(Component.literal(String.format(
                    Locale.ROOT,
                    "%s  •  peak Y %.2f",
                    amplitudePart,
                    peakY
            )));

            notifications.send(message, config.turnPointDestination());
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

    private void endFlight(double endY, long nowNanos) {
        maxY = Math.max(maxY, endY);

        if (config.flightEndSummary()) {
            double elapsedSeconds = Math.max(0.001, (nowNanos - flightStartedNanos) / 1_000_000_000.0);
            double averageHorizontalSpeed = horizontalDistance / elapsedSeconds;

            var summary = Component.literal(String.format(
                    Locale.ROOT,
                    "Flight ended  •  max Y %.2f  •  ",
                    maxY
            ));

            if (peakCount >= 2) {
                double netPeakGain = previousPeakY - firstPeakY;
                summary.append(Component.literal("net peak gain "));
                summary.append(
                        Component.literal(String.format(Locale.ROOT, "%+.2f blocks", netPeakGain))
                                .withStyle(style -> style.withColor(gainColor(netPeakGain)))
                );
            } else {
                summary.append(Component.literal("net peak gain n/a"));
            }

            summary.append(Component.literal(String.format(
                    Locale.ROOT,
                    "  •  distance %s  •  horiz %s  •  avg horiz %.2f blocks/s  •  time %.1fs",
                    formatDistance(traveledDistance),
                    formatDistance(horizontalDistance),
                    averageHorizontalSpeed,
                    elapsedSeconds
            )));

            notifications.send(summary, config.flightEndDestination());
        }

        reset();
    }

    private int gainColor(double gain) {
        int positiveColor = config.onTargetColor().argb() & 0x00FFFFFF;
        if (gain <= -10.0) {
            return NEGATIVE_GAIN_COLOR;
        }
        if (gain >= 10.0) {
            return positiveColor;
        }
        if (gain < 0.0) {
            return interpolateRgb(NEGATIVE_GAIN_COLOR, NEUTRAL_GAIN_COLOR, (gain + 10.0) / 10.0);
        }
        return interpolateRgb(NEUTRAL_GAIN_COLOR, positiveColor, gain / 10.0);
    }

    private static int interpolateRgb(int from, int to, double fraction) {
        double t = Math.clamp(fraction, 0.0, 1.0);

        int fromR = (from >> 16) & 0xFF;
        int fromG = (from >> 8) & 0xFF;
        int fromB = from & 0xFF;
        int toR = (to >> 16) & 0xFF;
        int toG = (to >> 8) & 0xFF;
        int toB = to & 0xFF;

        int r = (int) Math.round(fromR + (toR - fromR) * t);
        int g = (int) Math.round(fromG + (toG - fromG) * t);
        int b = (int) Math.round(fromB + (toB - fromB) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static String formatDistance(double blocks) {
        if (blocks >= 1000.0) {
            return String.format(Locale.ROOT, "%.2f km", blocks / 1000.0);
        }
        return String.format(Locale.ROOT, "%.1f blocks", blocks);
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

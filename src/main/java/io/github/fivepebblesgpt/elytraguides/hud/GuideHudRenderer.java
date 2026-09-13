package io.github.fivepebblesgpt.elytraguides.hud;

import io.github.fivepebblesgpt.elytraguides.config.ElytraGuidesConfig;
import io.github.fivepebblesgpt.elytraguides.flight.HorizontalDriftTracker;
import io.github.fivepebblesgpt.elytraguides.flight.ManeuverGuide;
import io.github.fivepebblesgpt.elytraguides.flight.SpeedGuide;
import io.github.fivepebblesgpt.elytraguides.flight.TpsEstimator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Locale;
import java.util.function.BooleanSupplier;

public final class GuideHudRenderer {
    private static final int TARGET_BAR_WIDTH = 18;
    private static final int TARGET_BAR_OFFSET = 3;
    private static final int DRIFT_GUIDE_Y_OFFSET = -12;
    private static final int SPEEDOMETER_Y_OFFSET = 25;
    private static final int SPEEDOMETER_WIDTH = 96;
    private static final int SPEEDOMETER_HEIGHT = 4;

    private final ElytraGuidesConfig config;
    private final ManeuverGuide maneuverGuide;
    private final TpsEstimator tpsEstimator;
    private final HorizontalDriftTracker horizontalDriftTracker;
    private final SpeedGuide speedGuide;
    private final BooleanSupplier functionalityEnabled;

    public GuideHudRenderer(
            ElytraGuidesConfig config,
            ManeuverGuide maneuverGuide,
            TpsEstimator tpsEstimator,
            HorizontalDriftTracker horizontalDriftTracker,
            SpeedGuide speedGuide,
            BooleanSupplier functionalityEnabled
    ) {
        this.config = config;
        this.maneuverGuide = maneuverGuide;
        this.tpsEstimator = tpsEstimator;
        this.horizontalDriftTracker = horizontalDriftTracker;
        this.speedGuide = speedGuide;
        this.functionalityEnabled = functionalityEnabled;
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!functionalityEnabled.getAsBoolean() || minecraft.player == null || !minecraft.player.isFallFlying()) {
            return;
        }

        long nowNanos = System.nanoTime();
        maneuverGuide.advanceTarget(nowNanos, tpsEstimator.estimatedTps());

        float currentPitch = minecraft.player.getXRot();
        float currentYaw = minecraft.player.getYRot();
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        double fovDegrees = minecraft.options.fov().get();

        drawPitchBar(
                graphics,
                centerX,
                screenHeight,
                currentPitch,
                config.approachPitch(),
                config.approachColor().argb(),
                fovDegrees
        );
        drawPitchBar(
                graphics,
                centerX,
                screenHeight,
                currentPitch,
                config.snapPitch(),
                config.snapColor().argb(),
                fovDegrees
        );

        if (config.showHorizontalDriftGuide() && horizontalDriftTracker.hasHistory()) {
            drawHorizontalDriftGuide(
                    graphics,
                    screenWidth,
                    screenHeight,
                    centerX,
                    centerY,
                    currentYaw,
                    fovDegrees
            );
        }

        if (config.showSpeedometer() && speedGuide.hasSample()) {
            drawSpeedometer(graphics, centerX, centerY);
        }

        if (maneuverGuide.isTargetActive()) {
            drawTarget(graphics, centerX, screenHeight, currentPitch, fovDegrees);
        }
    }

    private void drawPitchBar(
            GuiGraphicsExtractor graphics,
            int centerX,
            int screenHeight,
            float currentPitch,
            double targetPitch,
            int color,
            double fovDegrees
    ) {
        int y = pitchToScreenY(targetPitch, currentPitch, screenHeight, fovDegrees);
        if (y < -8 || y > screenHeight + 8) {
            return;
        }

        int halfWidth = config.guideBarWidth() / 2;
        int thickness = config.guideBarThickness();
        int top = y - thickness / 2;
        int bottom = top + thickness;
        int left = centerX - halfWidth;
        int right = centerX + halfWidth;

        graphics.fill(left, top, right, bottom, color);

        if (config.showPitchLabels()) {
            String label = String.format(Locale.ROOT, "%+.1f°", targetPitch);
            int labelWidth = Minecraft.getInstance().font.width(label);
            graphics.text(Minecraft.getInstance().font, label, left - labelWidth - 5, y - 4, color, true);
        }
    }

    private void drawTarget(
            GuiGraphicsExtractor graphics,
            int centerX,
            int screenHeight,
            float currentPitch,
            double fovDegrees
    ) {
        double targetPitch = maneuverGuide.targetPitch();
        int y = pitchToScreenY(targetPitch, currentPitch, screenHeight, fovDegrees);
        if (y < -12 || y > screenHeight + 12) {
            return;
        }

        boolean onTarget = maneuverGuide.isOnTarget(currentPitch);
        int color = onTarget ? config.onTargetColor().argb() : config.targetColor().argb();
        int thickness = Math.max(1, config.guideBarThickness());
        int halfWidth = TARGET_BAR_WIDTH / 2;
        int left = centerX - halfWidth;
        int right = centerX + halfWidth;

        graphics.fill(
                left,
                y - TARGET_BAR_OFFSET - thickness,
                right,
                y - TARGET_BAR_OFFSET,
                color
        );
        graphics.fill(
                left,
                y + TARGET_BAR_OFFSET,
                right,
                y + TARGET_BAR_OFFSET + thickness,
                color
        );

        if (config.showTargetError()) {
            String error = String.format(Locale.ROOT, "Δ%+.1f°", maneuverGuide.targetError(currentPitch));
            graphics.centeredText(Minecraft.getInstance().font, error, centerX, y + TARGET_BAR_OFFSET + thickness + 3, color);
        }
    }

    private void drawHorizontalDriftGuide(
            GuiGraphicsExtractor graphics,
            int screenWidth,
            int screenHeight,
            int centerX,
            int centerY,
            float currentYaw,
            double fovDegrees
    ) {
        double averageYaw = horizontalDriftTracker.averageYawDegrees();
        int targetX = yawToScreenX(averageYaw, currentYaw, screenWidth, screenHeight, fovDegrees);
        targetX = Math.clamp(targetX, 4, screenWidth - 4);

        int y = centerY + DRIFT_GUIDE_Y_OFFSET;
        int color = config.horizontalDriftColor().argb();
        int delta = targetX - centerX;

        if (Math.abs(delta) <= 1) {
            graphics.fill(centerX - 2, y, centerX + 3, y + 1, color);
            return;
        }

        int startX = centerX + Integer.signum(delta) * 3;
        int left = Math.min(startX, targetX);
        int right = Math.max(startX, targetX);
        graphics.fill(left, y, right + 1, y + 1, color);
        graphics.fill(targetX, y - 2, targetX + 1, y + 3, color);
    }

    private void drawSpeedometer(GuiGraphicsExtractor graphics, int centerX, int centerY) {
        int left = centerX - SPEEDOMETER_WIDTH / 2;
        int right = left + SPEEDOMETER_WIDTH;
        int top = centerY + SPEEDOMETER_Y_OFFSET;
        int bottom = top + SPEEDOMETER_HEIGHT;

        double scaleMax = Math.max(50.0, speedGuide.optimalHorizontalMax() + 7.0);
        double fillFraction = Math.clamp(speedGuide.horizontalSpeed() / scaleMax, 0.0, 1.0);
        int filledRight = left + (int) Math.round(SPEEDOMETER_WIDTH * fillFraction);

        boolean optimal = speedGuide.isOptimal();
        boolean brightFlash = ((System.nanoTime() / 200_000_000L) & 1L) == 0L;
        int onTargetColor = config.onTargetColor().argb();
        int fillColor = optimal
                ? withAlpha(onTargetColor, brightFlash ? 0xFF : 0x88)
                : config.speedometerColor().argb();

        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, 0x88000000);
        if (filledRight > left) {
            graphics.fill(left, top, filledRight, bottom, fillColor);
        }

        int optimalMinX = left + (int) Math.round(
                SPEEDOMETER_WIDTH * Math.clamp(speedGuide.optimalHorizontalMin() / scaleMax, 0.0, 1.0)
        );
        int optimalMaxX = left + (int) Math.round(
                SPEEDOMETER_WIDTH * Math.clamp(speedGuide.optimalHorizontalMax() / scaleMax, 0.0, 1.0)
        );
        int markerColor = withAlpha(onTargetColor, 0xCC);
        graphics.fill(optimalMinX, top - 2, optimalMinX + 1, bottom + 2, markerColor);
        graphics.fill(optimalMaxX, top - 2, optimalMaxX + 1, bottom + 2, markerColor);

        String speedText = optimal
                ? String.format(
                        Locale.ROOT,
                        "↑ H %.2f m/s • V %.2f m/s ↑",
                        speedGuide.horizontalSpeed(),
                        speedGuide.verticalSpeed()
                )
                : String.format(
                        Locale.ROOT,
                        "H %.2f m/s • V %.2f m/s",
                        speedGuide.horizontalSpeed(),
                        speedGuide.verticalSpeed()
                );
        int textColor = optimal ? withAlpha(onTargetColor, brightFlash ? 0xFF : 0x99) : 0xFFFFFFFF;
        graphics.centeredText(Minecraft.getInstance().font, speedText, centerX, bottom + 3, textColor);
    }

    private static int pitchToScreenY(double targetPitch, double currentPitch, int screenHeight, double fovDegrees) {
        double pitchDelta = targetPitch - currentPitch;
        if (pitchDelta <= -89.0) {
            return -10_000;
        }
        if (pitchDelta >= 89.0) {
            return 10_000;
        }

        double safeFov = Math.clamp(fovDegrees, 30.0, 120.0);
        double focalLength = screenHeight / (2.0 * Math.tan(Math.toRadians(safeFov) / 2.0));
        double offset = Math.tan(Math.toRadians(pitchDelta)) * focalLength;
        return (int) Math.round(screenHeight / 2.0 + offset);
    }

    private static int yawToScreenX(
            double targetYaw,
            double currentYaw,
            int screenWidth,
            int screenHeight,
            double fovDegrees
    ) {
        double yawDelta = wrapDegrees(targetYaw - currentYaw);
        yawDelta = Math.clamp(yawDelta, -85.0, 85.0);

        double safeFov = Math.clamp(fovDegrees, 30.0, 120.0);
        double focalLength = screenHeight / (2.0 * Math.tan(Math.toRadians(safeFov) / 2.0));
        double offset = Math.tan(Math.toRadians(yawDelta)) * focalLength;
        return (int) Math.round(screenWidth / 2.0 + offset);
    }

    private static double wrapDegrees(double degrees) {
        double wrapped = degrees % 360.0;
        if (wrapped >= 180.0) {
            wrapped -= 360.0;
        }
        if (wrapped < -180.0) {
            wrapped += 360.0;
        }
        return wrapped;
    }

    private static int withAlpha(int argb, int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | (argb & 0x00FFFFFF);
    }
}

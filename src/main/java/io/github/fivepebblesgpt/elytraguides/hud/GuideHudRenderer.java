package io.github.fivepebblesgpt.elytraguides.hud;

import io.github.fivepebblesgpt.elytraguides.config.ElytraGuidesConfig;
import io.github.fivepebblesgpt.elytraguides.flight.ManeuverGuide;
import io.github.fivepebblesgpt.elytraguides.flight.TpsEstimator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Locale;
import java.util.function.BooleanSupplier;

public final class GuideHudRenderer {
    private final ElytraGuidesConfig config;
    private final ManeuverGuide maneuverGuide;
    private final TpsEstimator tpsEstimator;
    private final BooleanSupplier functionalityEnabled;

    public GuideHudRenderer(
            ElytraGuidesConfig config,
            ManeuverGuide maneuverGuide,
            TpsEstimator tpsEstimator,
            BooleanSupplier functionalityEnabled
    ) {
        this.config = config;
        this.maneuverGuide = maneuverGuide;
        this.tpsEstimator = tpsEstimator;
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
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int centerX = screenWidth / 2;
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

        drawCircle(graphics, centerX, y, 5, color);
        if (onTarget) {
            graphics.fill(centerX - 1, y - 1, centerX + 1, y + 1, color);
        }

        if (config.showTargetError()) {
            String error = String.format(Locale.ROOT, "Δ%+.1f°", maneuverGuide.targetError(currentPitch));
            graphics.centeredText(Minecraft.getInstance().font, error, centerX, y + 8, color);
        }
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

    private static void drawCircle(GuiGraphicsExtractor graphics, int centerX, int centerY, int radius, int color) {
        int x = radius;
        int y = 0;
        int error = 1 - x;

        while (x >= y) {
            plotCircleOctants(graphics, centerX, centerY, x, y, color);
            y++;
            if (error < 0) {
                error += 2 * y + 1;
            } else {
                x--;
                error += 2 * (y - x) + 1;
            }
        }
    }

    private static void plotCircleOctants(
            GuiGraphicsExtractor graphics,
            int centerX,
            int centerY,
            int x,
            int y,
            int color
    ) {
        pixel(graphics, centerX + x, centerY + y, color);
        pixel(graphics, centerX + y, centerY + x, color);
        pixel(graphics, centerX - y, centerY + x, color);
        pixel(graphics, centerX - x, centerY + y, color);
        pixel(graphics, centerX - x, centerY - y, color);
        pixel(graphics, centerX - y, centerY - x, color);
        pixel(graphics, centerX + y, centerY - x, color);
        pixel(graphics, centerX + x, centerY - y, color);
    }

    private static void pixel(GuiGraphicsExtractor graphics, int x, int y, int color) {
        graphics.fill(x, y, x + 1, y + 1, color);
    }
}

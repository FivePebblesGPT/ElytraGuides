package io.github.fivepebblesgpt.elytraguides.config;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;
import io.wispforest.owo.config.annotation.RangeConstraint;
import io.wispforest.owo.config.annotation.SectionHeader;
import io.wispforest.owo.ui.core.Color;

@Modmenu(modId = "elytraguides")
@Config(name = "elytraguides", wrapperName = "ElytraGuidesConfig")
public class ElytraGuidesConfigModel {

    @SectionHeader("guide")
    public boolean enabled = true;

    @RangeConstraint(min = -90.0, max = 90.0, decimalPlaces = 1)
    public float approachPitch = 32.5f;

    @RangeConstraint(min = -90.0, max = 90.0, decimalPlaces = 1)
    public float snapPitch = -49.0f;

    @RangeConstraint(min = 0.1, max = 60.0, decimalPlaces = 1)
    public float returnRateDegreesPerSecond = 10.0f;

    public boolean compensateForTps = true;

    @RangeConstraint(min = 0.1, max = 10.0, decimalPlaces = 1)
    public float targetTolerance = 1.0f;

    @RangeConstraint(min = 20, max = 240)
    public int guideBarWidth = 72;

    @RangeConstraint(min = 1, max = 6)
    public int guideBarThickness = 2;

    public boolean showPitchLabels = true;
    public boolean showTargetError = true;

    public Color approachColor = Color.ofArgb(0xDD55C7FF);
    public Color snapColor = Color.ofArgb(0xDDFFB347);
    public Color targetColor = Color.ofArgb(0xFFFFFFFF);
    public Color onTargetColor = Color.ofArgb(0xFF55FF88);

    @SectionHeader("crosshair")
    public CrosshairStyle crosshairStyle = CrosshairStyle.DOT;
    public Color crosshairColor = Color.ofArgb(0xFFFFFFFF);
    public boolean customCrosshairOnlyWhileFlying = true;

    @SectionHeader("altitude")
    public boolean logAscentAtPeak = true;
    public boolean logDescentAtTrough = true;
    public LogDestination turnPointDestination = LogDestination.HUD_TOAST;

    public boolean flightEndSummary = true;
    public LogDestination flightEndDestination = LogDestination.CHAT;

    @RangeConstraint(min = 0.0, max = 0.1, decimalPlaces = 4)
    public double verticalSpeedDeadzone = 0.0025;

    @RangeConstraint(min = 500, max = 10000)
    public int toastDurationMs = 2200;
}

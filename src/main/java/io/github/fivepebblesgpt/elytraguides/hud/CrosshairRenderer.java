package io.github.fivepebblesgpt.elytraguides.hud;

import io.github.fivepebblesgpt.elytraguides.config.CrosshairStyle;
import io.github.fivepebblesgpt.elytraguides.config.ElytraGuidesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.BooleanSupplier;

public final class CrosshairRenderer {
    private final ElytraGuidesConfig config;
    private final BooleanSupplier functionalityEnabled;

    public CrosshairRenderer(ElytraGuidesConfig config, BooleanSupplier functionalityEnabled) {
        this.config = config;
        this.functionalityEnabled = functionalityEnabled;
    }

    public boolean shouldReplaceVanilla() {
        if (!functionalityEnabled.getAsBoolean() || config.crosshairStyle() == CrosshairStyle.VANILLA) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.options.getCameraType().isFirstPerson()) {
            return false;
        }

        return !config.customCrosshairOnlyWhileFlying() || minecraft.player.isFallFlying();
    }

    public void render(GuiGraphicsExtractor graphics) {
        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;
        int color = config.crosshairColor().argb();

        switch (config.crosshairStyle()) {
            case DOT -> graphics.fill(centerX - 1, centerY - 1, centerX + 1, centerY + 1, color);
            case VANILLA -> {
                // The caller delegates to the original vanilla HUD element for this mode.
            }
        }
    }
}

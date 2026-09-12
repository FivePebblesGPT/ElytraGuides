package io.github.fivepebblesgpt.elytraguides.hud;

import io.github.fivepebblesgpt.elytraguides.config.CrosshairStyle;
import io.github.fivepebblesgpt.elytraguides.config.ElytraGuidesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class CrosshairRenderer {
    private final ElytraGuidesConfig config;

    public CrosshairRenderer(ElytraGuidesConfig config) {
        this.config = config;
    }

    public boolean shouldReplaceVanilla() {
        if (config.crosshairStyle() == CrosshairStyle.VANILLA) {
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
            case THIN_CROSS -> drawThinCross(graphics, centerX, centerY, config.crosshairArmLength(), color);
            case DOT -> graphics.fill(centerX - 1, centerY - 1, centerX + 1, centerY + 1, color);
            case VANILLA -> {
                // The caller delegates to the original vanilla HUD element for this mode.
            }
        }
    }

    private static void drawThinCross(
            GuiGraphicsExtractor graphics,
            int centerX,
            int centerY,
            int armLength,
            int color
    ) {
        // Two-pixel-thick arms around a 2x2 empty square centered on screen.
        graphics.fill(centerX - 1, centerY - 1 - armLength, centerX + 1, centerY - 1, color);
        graphics.fill(centerX - 1, centerY + 1, centerX + 1, centerY + 1 + armLength, color);
        graphics.fill(centerX - 1 - armLength, centerY - 1, centerX - 1, centerY + 1, color);
        graphics.fill(centerX + 1, centerY - 1, centerX + 1 + armLength, centerY + 1, color);
    }
}

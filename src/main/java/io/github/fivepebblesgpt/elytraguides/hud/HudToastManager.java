package io.github.fivepebblesgpt.elytraguides.hud;

import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class HudToastManager {
    private Component message;
    private long expiresAtMs;

    public void show(Component message, int durationMs) {
        this.message = message;
        this.expiresAtMs = Util.getMillis() + durationMs;
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (message == null) {
            return;
        }

        if (Util.getMillis() >= expiresAtMs) {
            message = null;
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int centerX = graphics.guiWidth() / 2;
        int y = 22;
        int textWidth = minecraft.font.width(message);
        int left = centerX - textWidth / 2 - 6;
        int right = centerX + textWidth / 2 + 6;

        graphics.fill(left, y - 4, right, y + 13, 0xB0000000);
        graphics.centeredText(minecraft.font, message, centerX, y, 0xFFFFFFFF);
    }

    public void clear() {
        message = null;
        expiresAtMs = 0L;
    }
}

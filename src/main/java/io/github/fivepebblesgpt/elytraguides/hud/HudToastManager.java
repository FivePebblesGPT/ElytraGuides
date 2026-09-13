package io.github.fivepebblesgpt.elytraguides.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

public final class HudToastManager {
    private static final int MIN_DURATION_MS = 5000;
    private static final int BACKGROUND_ALPHA = 0x88;

    private Component message;
    private long shownAtMs;
    private long expiresAtMs;

    public void show(Component message, int durationMs) {
        int effectiveDuration = Math.max(MIN_DURATION_MS, durationMs);
        this.message = message;
        this.shownAtMs = Util.getMillis();
        this.expiresAtMs = shownAtMs + effectiveDuration;
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (message == null) {
            return;
        }

        long nowMs = Util.getMillis();
        if (nowMs >= expiresAtMs) {
            clear();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int centerX = graphics.guiWidth() / 2;
        int y = 22;
        int textWidth = minecraft.font.width(message);
        int left = centerX - textWidth / 2 - 6;
        int right = centerX + textWidth / 2 + 6;

        float opacity = opacityAt(nowMs);
        int backgroundAlpha = Math.clamp(Math.round(BACKGROUND_ALPHA * opacity), 0, 255);
        int textAlpha = Math.clamp(Math.round(255.0f * opacity), 0, 255);

        graphics.fill(left, y - 4, right, y + 13, backgroundAlpha << 24);
        graphics.centeredText(minecraft.font, message, centerX, y, (textAlpha << 24) | 0x00FFFFFF);
    }

    public void clear() {
        message = null;
        shownAtMs = 0L;
        expiresAtMs = 0L;
    }

    private float opacityAt(long nowMs) {
        long duration = Math.max(1L, expiresAtMs - shownAtMs);
        long fadeDuration = Math.max(3000L, Math.round(duration * 0.65));
        fadeDuration = Math.min(fadeDuration, duration);
        long fadeStart = expiresAtMs - fadeDuration;

        if (nowMs <= fadeStart) {
            return 1.0f;
        }

        double remaining = Math.max(0L, expiresAtMs - nowMs);
        double linear = remaining / fadeDuration;
        return (float) (linear * linear * (3.0 - 2.0 * linear));
    }
}

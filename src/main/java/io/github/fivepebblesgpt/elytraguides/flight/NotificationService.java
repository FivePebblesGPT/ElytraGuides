package io.github.fivepebblesgpt.elytraguides.flight;

import io.github.fivepebblesgpt.elytraguides.config.ElytraGuidesConfig;
import io.github.fivepebblesgpt.elytraguides.config.LogDestination;
import io.github.fivepebblesgpt.elytraguides.hud.HudToastManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class NotificationService {
    private final ElytraGuidesConfig config;
    private final HudToastManager toastManager;

    public NotificationService(ElytraGuidesConfig config, HudToastManager toastManager) {
        this.config = config;
        this.toastManager = toastManager;
    }

    public void send(Component message, LogDestination destination) {
        if (destination == LogDestination.OFF) {
            return;
        }

        if (destination == LogDestination.HUD_TOAST || destination == LogDestination.BOTH) {
            toastManager.show(message, config.toastDurationMs());
        }

        if (destination == LogDestination.CHAT || destination == LogDestination.BOTH) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                minecraft.player.sendSystemMessage(message);
            }
        }
    }
}

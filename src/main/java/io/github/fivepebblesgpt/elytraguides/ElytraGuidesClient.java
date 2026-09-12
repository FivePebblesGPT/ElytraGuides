package io.github.fivepebblesgpt.elytraguides;

import io.github.fivepebblesgpt.elytraguides.config.ElytraGuidesConfig;
import io.github.fivepebblesgpt.elytraguides.flight.FlightAltitudeTracker;
import io.github.fivepebblesgpt.elytraguides.flight.ManeuverGuide;
import io.github.fivepebblesgpt.elytraguides.flight.NotificationService;
import io.github.fivepebblesgpt.elytraguides.flight.TpsEstimator;
import io.github.fivepebblesgpt.elytraguides.hud.CrosshairRenderer;
import io.github.fivepebblesgpt.elytraguides.hud.GuideHudRenderer;
import io.github.fivepebblesgpt.elytraguides.hud.HudToastManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class ElytraGuidesClient implements ClientModInitializer {
    public static final String MOD_ID = "elytraguides";
    public static final ElytraGuidesConfig CONFIG = ElytraGuidesConfig.createAndLoad();
    public static final TpsEstimator TPS_ESTIMATOR = new TpsEstimator();

    private final ManeuverGuide maneuverGuide = new ManeuverGuide(CONFIG);
    private final HudToastManager toastManager = new HudToastManager();
    private final NotificationService notifications = new NotificationService(CONFIG, toastManager);
    private final FlightAltitudeTracker altitudeTracker = new FlightAltitudeTracker(CONFIG, notifications);
    private final GuideHudRenderer guideHudRenderer = new GuideHudRenderer(CONFIG, maneuverGuide, TPS_ESTIMATOR);
    private final CrosshairRenderer crosshairRenderer = new CrosshairRenderer(CONFIG);

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CROSSHAIR,
                Identifier.fromNamespaceAndPath(MOD_ID, "flight_guides"),
                guideHudRenderer::render
        );

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "flight_toast"),
                toastManager::render
        );

        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, vanillaCrosshair ->
                (graphics, deltaTracker) -> {
                    if (crosshairRenderer.shouldReplaceVanilla()) {
                        crosshairRenderer.render(graphics);
                    } else {
                        vanillaCrosshair.extractRenderState(graphics, deltaTracker);
                    }
                }
        );
    }

    private void onEndClientTick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            maneuverGuide.reset();
            altitudeTracker.reset();
            TPS_ESTIMATOR.reset();
            toastManager.clear();
            return;
        }

        long nowNanos = System.nanoTime();
        TPS_ESTIMATOR.onClientTick(nowNanos);

        boolean flying = minecraft.player.isFallFlying();
        maneuverGuide.tick(flying, minecraft.player.getXRot(), nowNanos);
        altitudeTracker.tick(flying, minecraft.player.getY(), minecraft.player.getDeltaMovement().y);
    }
}

package io.github.fivepebblesgpt.elytraguides;

import com.mojang.blaze3d.platform.InputConstants;
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
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class ElytraGuidesClient implements ClientModInitializer {
    public static final String MOD_ID = "elytraguides";
    public static final ElytraGuidesConfig CONFIG = ElytraGuidesConfig.createAndLoad();
    public static final TpsEstimator TPS_ESTIMATOR = new TpsEstimator();

    private static KeyMapping toggleFunctionalityKey;

    private boolean runtimeEnabled = true;

    private final ManeuverGuide maneuverGuide = new ManeuverGuide(CONFIG);
    private final HudToastManager toastManager = new HudToastManager();
    private final NotificationService notifications = new NotificationService(CONFIG, toastManager);
    private final FlightAltitudeTracker altitudeTracker = new FlightAltitudeTracker(CONFIG, notifications);
    private final GuideHudRenderer guideHudRenderer = new GuideHudRenderer(
            CONFIG,
            maneuverGuide,
            TPS_ESTIMATOR,
            this::isFunctionalityEnabled
    );
    private final CrosshairRenderer crosshairRenderer = new CrosshairRenderer(CONFIG, this::isFunctionalityEnabled);

    @Override
    public void onInitializeClient() {
        migrateIncorrectPitchDefaults();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "controls")
        );
        toggleFunctionalityKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.elytraguides.toggle",
                        InputConstants.Type.KEYSYM,
                        InputConstants.KEY_G,
                        category
                )
        );

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

    public static KeyMapping toggleFunctionalityKey() {
        if (toggleFunctionalityKey == null) {
            throw new IllegalStateException("Toggle key mapping has not been registered yet");
        }
        return toggleFunctionalityKey;
    }

    private void onEndClientTick(Minecraft minecraft) {
        handleToggleKey(minecraft);

        if (minecraft.player == null || minecraft.level == null) {
            resetRuntimeState();
            return;
        }

        if (!isFunctionalityEnabled()) {
            resetRuntimeState();
            return;
        }

        long nowNanos = System.nanoTime();
        TPS_ESTIMATOR.onClientTick(nowNanos);

        boolean flying = minecraft.player.isFallFlying();
        maneuverGuide.tick(flying, minecraft.player.getXRot(), nowNanos);
        altitudeTracker.tick(flying, minecraft.player.getY(), minecraft.player.getDeltaMovement().y);
    }

    private void handleToggleKey(Minecraft minecraft) {
        if (toggleFunctionalityKey == null) {
            return;
        }

        while (toggleFunctionalityKey.consumeClick()) {
            runtimeEnabled = !runtimeEnabled;
            resetRuntimeState();

            if (minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.translatable(
                        runtimeEnabled
                                ? "message.elytraguides.toggle.enabled"
                                : "message.elytraguides.toggle.disabled"
                ));
            }
        }
    }

    private boolean isFunctionalityEnabled() {
        return runtimeEnabled && CONFIG.enabled();
    }

    private void resetRuntimeState() {
        maneuverGuide.reset();
        altitudeTracker.reset();
        TPS_ESTIMATOR.reset();
        toastManager.clear();
    }

    private static void migrateIncorrectPitchDefaults() {
        if (Float.compare(CONFIG.approachPitch(), -32.5f) == 0
                && Float.compare(CONFIG.snapPitch(), 49.0f) == 0) {
            CONFIG.approachPitch(32.5f);
            CONFIG.snapPitch(-49.0f);
        }
    }
}

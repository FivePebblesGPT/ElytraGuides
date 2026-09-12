package io.github.fivepebblesgpt.elytraguides.mixin;

import io.github.fivepebblesgpt.elytraguides.ElytraGuidesClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Inject(method = "handleSetTime", at = @At("TAIL"))
    private void elytraguides$sampleServerTime(ClientboundSetTimePacket packet, CallbackInfo ci) {
        ElytraGuidesClient.TPS_ESTIMATOR.onServerTimeSync(packet.gameTime(), System.nanoTime());
    }
}

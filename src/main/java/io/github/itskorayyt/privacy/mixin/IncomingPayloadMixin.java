package io.github.itskorayyt.privacy.mixin;

import io.github.itskorayyt.privacy.ClientPrivacy;
import io.github.itskorayyt.privacy.PrivacyLog;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientCommonPacketListenerImpl.class, priority = 1100)
abstract class IncomingPayloadMixin {
    @Shadow @Final protected Connection connection;

    @Inject(method = "handleCustomPayload(Lnet/minecraft/network/protocol/common/ClientboundCustomPayloadPacket;)V",
            at = @At("HEAD"))
    private void privacyObserveRequest(ClientboundCustomPayloadPacket packet, CallbackInfo callback) {
        var config = ClientPrivacy.settings();
        if (config.debugLogging()) {
            var id = packet.payload().type().id();
            if (id.getPath().equals("register") && (id.getNamespace().equals("minecraft") || id.getNamespace().equals("c"))) {
                PrivacyLog.trace(connection, PrivacyLog.Event.REGISTRATION_RECEIVED);
            }
        }
    }
}

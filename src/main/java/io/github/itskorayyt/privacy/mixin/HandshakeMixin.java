package io.github.itskorayyt.privacy.mixin;

import io.github.itskorayyt.privacy.ClientPrivacy;
import io.github.itskorayyt.privacy.PrivacyLog;
import io.github.itskorayyt.privacy.network.ChannelFilter;
import io.github.itskorayyt.privacy.network.LoginQueryApproval;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.custom.DiscardedQueryPayload;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientHandshakePacketListenerImpl.class)
abstract class HandshakeMixin {
    @Shadow @Final private Connection connection;

    @ModifyVariable(method = "handleCustomQuery", at = @At("HEAD"), argsOnly = true)
    private ClientboundCustomQueryPacket privacyDeclineQuery(ClientboundCustomQueryPacket packet) {
        var settings = ClientPrivacy.settings();
        var payload = packet.payload();
        if (!settings.enabled() || !settings.blockUnapprovedCustomPayloads()
                || connection.isMemoryConnection() || payload instanceof DiscardedQueryPayload) {
            return packet;
        }
        if (ChannelFilter.allowed(payload.id(), settings)) {
            ((LoginQueryApproval) connection).privacy$allowLoginAnswer(packet.transactionId());
            return packet;
        }
        if (settings.debugLogging()) {
            PrivacyLog.loginQueryDeclined();
        }
        // Skip Fabric's handler and let vanilla send its normal empty answer.
        return new ClientboundCustomQueryPacket(packet.transactionId(), new DiscardedQueryPayload(payload.id()));
    }
}

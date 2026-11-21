package io.github.itskorayyt.privacy.mixin;

import io.github.itskorayyt.privacy.ClientPrivacy;
import io.github.itskorayyt.privacy.PrivacyLog;
import io.github.itskorayyt.privacy.network.ChannelFilter;
import io.github.itskorayyt.privacy.network.LoginQueryApproval;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket;
import net.minecraft.network.protocol.login.custom.DiscardedQueryPayload;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientHandshakePacketListenerImpl.class)
abstract class HandshakeMixin {
    @Shadow @Final private Connection connection;

    @Inject(method = "handleLoginFinished", at = @At("HEAD"))
    private void privacyTraceLogin(ClientboundLoginFinishedPacket packet, CallbackInfo callback) {
        PrivacyLog.trace(connection, PrivacyLog.Event.LOGIN_FINISHED);
    }

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
        PrivacyLog.trace(connection, PrivacyLog.Event.LOGIN_QUERY_DECLINED);
        // Skip Fabric's handler and let vanilla send its normal empty answer.
        return new ClientboundCustomQueryPacket(packet.transactionId(), new DiscardedQueryPayload(payload.id()));
    }
}

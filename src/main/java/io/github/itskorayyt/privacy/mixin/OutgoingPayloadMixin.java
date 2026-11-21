package io.github.itskorayyt.privacy.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.itskorayyt.privacy.ClientPrivacy;
import io.github.itskorayyt.privacy.PrivacyLog;
import io.github.itskorayyt.privacy.network.ChannelFilter;
import io.github.itskorayyt.privacy.network.LoginQueryApproval;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Connection.class)
abstract class OutgoingPayloadMixin implements LoginQueryApproval {
    @Shadow private Channel channel;
    @Shadow public abstract PacketFlow getSending();
    @Shadow public abstract boolean isMemoryConnection();

    // Fabric login handlers may complete on a different thread.
    @Unique private final Set<Integer> privacy$approvedLoginQueries = ConcurrentHashMap.newKeySet();

    @Override
    public void privacy$allowLoginAnswer(int transactionId) {
        privacy$approvedLoginQueries.add(transactionId);
    }

    @WrapMethod(method = "doSendPacket")
    private void privacySend(Packet<?> packet, ChannelFutureListener callback, boolean flush, Operation<Void> original) {
        if (!(packet instanceof ServerboundCustomPayloadPacket || packet instanceof ServerboundCustomQueryAnswerPacket)
                || getSending() != PacketFlow.SERVERBOUND || isMemoryConnection()) {
            original.call(packet, callback, flush);
            return;
        }

        var settings = ClientPrivacy.settings();
        if (packet instanceof ServerboundCustomQueryAnswerPacket answer
                && settings.enabled() && settings.blockUnapprovedCustomPayloads()) {
            boolean approved = privacy$approvedLoginQueries.remove(answer.transactionId());
            if (!approved && answer.payload() != null) {
                privacy$drop(callback, flush, settings.debugLogging());
                return;
            }
        } else if (packet instanceof ServerboundCustomPayloadPacket custom) {
            var filtered = ChannelFilter.outgoing(custom.payload(), settings, ClientPrivacy.hasChannelAdapter());
            if (filtered == null) {
                privacy$drop(callback, flush, settings.debugLogging());
                return;
            }
            if (filtered != custom.payload()) {
                packet = new ServerboundCustomPayloadPacket(filtered);
            }
        }
        original.call(packet, callback, flush);
    }

    @Unique
    private void privacy$drop(ChannelFutureListener callback, boolean flush, boolean debugLogging) {
        if (debugLogging) {
            PrivacyLog.payloadBlocked();
        }
        if (flush) {
            channel.flush();
        }
        if (callback != null) {
            channel.newFailedFuture(new IllegalStateException("Custom payload blocked by Fabric Client Privacy"))
                    .addListener(callback);
        }
    }
}

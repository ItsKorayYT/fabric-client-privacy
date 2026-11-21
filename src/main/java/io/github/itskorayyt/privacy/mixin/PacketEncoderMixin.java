package io.github.itskorayyt.privacy.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.itskorayyt.privacy.ClientPrivacy;
import io.github.itskorayyt.privacy.PrivacyLog;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PacketEncoder.class)
abstract class PacketEncoderMixin {
    @Shadow @Final private ProtocolInfo<?> protocolInfo;
    @Unique private Set<String> privacy$encodedTypes;

    @Inject(method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;Lio/netty/buffer/ByteBuf;)V",
            at = @At("RETURN"))
    private void privacyTraceEncoded(ChannelHandlerContext context, Packet<?> packet, ByteBuf output,
                                     CallbackInfo callback, @Local PacketType<?> type) {
        if (!ClientPrivacy.settings().debugLogging() || protocolInfo.flow() != PacketFlow.SERVERBOUND) {
            return;
        }
        if (privacy$encodedTypes == null) {
            var connection = context.pipeline().get(Connection.class);
            if (connection == null || connection.isMemoryConnection() || connection.getSending() != PacketFlow.SERVERBOUND) {
                return;
            }
            privacy$encodedTypes = new HashSet<>();
        }
        PrivacyLog.traceEncoded(protocolInfo.id(), type, packet, privacy$encodedTypes);
    }
}

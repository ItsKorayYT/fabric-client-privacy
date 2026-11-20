package io.github.itskorayyt.privacy.mixin;

import io.github.itskorayyt.privacy.ClientPrivacy;
import io.github.itskorayyt.privacy.network.ChannelFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ServerboundCustomPayloadPacket.class)
abstract class OutgoingPayloadMixin {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static CustomPacketPayload privacyChannels(CustomPacketPayload payload) {
        var config = ClientPrivacy.settings();
        if (config.enabled() && config.filterModChannels() && ClientPrivacy.hasChannelAdapter()
                && !Minecraft.getInstance().hasSingleplayerServer()) {
            return ChannelFilter.apply(payload, config.allowedChannels());
        }
        return payload;
    }
}

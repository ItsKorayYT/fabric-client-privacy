package io.github.itskorayyt.privacy.mixin;

import io.github.itskorayyt.privacy.ClientPrivacy;
import io.github.itskorayyt.privacy.network.ChannelFilter;
import java.util.Collection;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "net.fabricmc.fabric.impl.networking.AbstractChanneledNetworkAddon")
abstract class LegacyAdvertisementMixin {
    // Fabric skips an empty collection here. Older peers can decode an empty wire
    // advertisement as a channel with an empty path.
    @ModifyVariable(method = "createRegistrationPayload(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload$Type;Ljava/util/Collection;)Lnet/fabricmc/fabric/impl/networking/RegistrationPayload;",
            at = @At("HEAD"), argsOnly = true, require = 0)
    private Collection<Identifier> privacyLegacyChannels(Collection<Identifier> channels) {
        var config = ClientPrivacy.settings();
        if (ClientPrivacy.hasChannelAdapter() && config.enabled() && config.filterModChannels()
                && !Minecraft.getInstance().hasSingleplayerServer()) {
            return ChannelFilter.legacyChannels(channels, config.allowedChannels());
        }
        return channels;
    }
}

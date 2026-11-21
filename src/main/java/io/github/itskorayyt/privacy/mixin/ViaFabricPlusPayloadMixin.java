package io.github.itskorayyt.privacy.mixin;

import io.github.itskorayyt.privacy.ClientPrivacy;
import io.github.itskorayyt.privacy.network.ChannelFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.viaversion.viafabricplus.protocoltranslator.impl.platform.ViaFabricPlusViaVersionPlatform", remap = false)
abstract class ViaFabricPlusPayloadMixin {
    // ViaVersion sends these optional payloads below Minecraft's Connection.send path.
    @Inject(method = "sendCustomPayload(Lcom/viaversion/viaversion/api/connection/UserConnection;Ljava/lang/String;[B)V",
            at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void privacyFilterViaPayload(@Coerce Object connection, String channel, byte[] body, CallbackInfo callback) {
        var settings = ClientPrivacy.settings();
        if (!settings.enabled() || !settings.blockUnapprovedCustomPayloads()) {
            return;
        }

        var client = Minecraft.getInstance();
        if (client != null && client.hasSingleplayerServer()) {
            return;
        }

        var id = Identifier.tryParse(channel);
        if (id == null || !ChannelFilter.allowed(id, settings)) {
            callback.cancel();
        }
    }
}

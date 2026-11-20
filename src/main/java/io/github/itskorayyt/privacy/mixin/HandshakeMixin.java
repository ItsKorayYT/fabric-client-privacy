package io.github.itskorayyt.privacy.mixin;

import io.github.itskorayyt.privacy.ClientPrivacy;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ClientHandshakePacketListenerImpl.class)
abstract class HandshakeMixin {
    @ModifyArg(method = "handleLoginFinished", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/common/custom/BrandPayload;<init>(Ljava/lang/String;)V"), index = 0)
    private String privacyBrand(String brand) {
        var config = ClientPrivacy.settings();
        return config.enabled() && config.spoofClientBrand() && !Minecraft.getInstance().hasSingleplayerServer()
                ? ClientBrandRetriever.VANILLA_NAME : brand;
    }
}

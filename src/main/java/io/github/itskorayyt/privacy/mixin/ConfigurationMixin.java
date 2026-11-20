package io.github.itskorayyt.privacy.mixin;

import io.github.itskorayyt.privacy.ClientPrivacy;
import io.github.itskorayyt.privacy.network.PackFilter;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientConfigurationPacketListenerImpl;
import net.minecraft.server.packs.repository.KnownPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ClientConfigurationPacketListenerImpl.class)
abstract class ConfigurationMixin {
    @ModifyArg(method = "handleSelectKnownPacks", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/KnownPacksManager;trySelectingPacks(Ljava/util/List;)Ljava/util/List;"), index = 0)
    private List<KnownPack> privacyOffers(List<KnownPack> offered) {
        var config = ClientPrivacy.settings();
        return config.enabled() && config.filterKnownPacks() && !Minecraft.getInstance().hasSingleplayerServer()
                ? PackFilter.offers(offered) : offered;
    }
}

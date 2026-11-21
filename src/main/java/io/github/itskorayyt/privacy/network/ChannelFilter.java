package io.github.itskorayyt.privacy.network;

import io.github.itskorayyt.privacy.config.Settings;
import java.util.Set;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class ChannelFilter {
    private ChannelFilter() {}

    static boolean advertised(Identifier id, Set<String> allowed) {
        return switch (id.toString()) {
            case "minecraft:brand", "minecraft:register", "minecraft:unregister",
                    "c:version", "c:register", "fabric:split" -> true;
            default -> allowed.contains(id.toString());
        };
    }

    public static boolean allowed(Identifier id, Settings settings) {
        return !settings.blockUnapprovedCustomPayloads() || settings.allowedChannels().contains(id.toString());
    }

    public static CustomPacketPayload outgoing(CustomPacketPayload payload, Settings settings, boolean hasChannelAdapter) {
        if (!settings.enabled()) {
            return payload;
        }
        if (payload instanceof BrandPayload brand) {
            return settings.spoofClientBrand() && !ClientBrandRetriever.VANILLA_NAME.equals(brand.brand())
                    ? new BrandPayload(ClientBrandRetriever.VANILLA_NAME) : brand;
        }

        Identifier id = payload.type().id();
        return switch (id.toString()) {
            case "minecraft:register", "minecraft:unregister", "c:register" -> {
                if (!settings.filterModChannels()) {
                    yield payload;
                }
                yield hasChannelAdapter ? FabricChannelFilter.registration(payload, settings.allowedChannels()) : null;
            }
            case "c:version" -> (hasChannelAdapter && FabricChannelFilter.isVersion(payload)) || allowed(id, settings)
                    ? payload : null;
            default -> allowed(id, settings) ? payload : null;
        };
    }
}

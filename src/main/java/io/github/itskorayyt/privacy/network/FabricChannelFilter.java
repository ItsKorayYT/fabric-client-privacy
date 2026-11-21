package io.github.itskorayyt.privacy.network;

import java.util.LinkedHashSet;
import java.util.Set;
import net.fabricmc.fabric.impl.networking.CommonRegisterPayload;
import net.fabricmc.fabric.impl.networking.CommonVersionPayload;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

final class FabricChannelFilter {
    private FabricChannelFilter() {}

    static CustomPacketPayload registration(CustomPacketPayload payload, Set<String> allowed) {
        if (payload instanceof RegistrationPayload legacy) {
            var channels = legacy.channels();
            var kept = channels.stream().filter(id -> ChannelFilter.advertised(id, allowed)).toList();
            // Legacy peers can decode an empty advertisement as a channel with an empty path.
            if (kept.isEmpty()) {
                return null;
            }
            return kept.size() == channels.size() ? legacy : new RegistrationPayload(legacy.type(), kept);
        }
        if (payload instanceof CommonRegisterPayload common) {
            var kept = new LinkedHashSet<>(common.channels());
            kept.removeIf(id -> !ChannelFilter.advertised(id, allowed));
            return kept.size() == common.channels().size() ? common
                    : new CommonRegisterPayload(common.version(), common.protocol(), kept);
        }
        return null;
    }

    static boolean isVersion(CustomPacketPayload payload) {
        return payload instanceof CommonVersionPayload;
    }
}

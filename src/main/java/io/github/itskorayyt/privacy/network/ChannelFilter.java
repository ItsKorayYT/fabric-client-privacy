package io.github.itskorayyt.privacy.network;

import java.util.LinkedHashSet;
import java.util.Set;
import net.fabricmc.fabric.impl.networking.CommonRegisterPayload;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class ChannelFilter {
    private ChannelFilter() {}

    public static boolean advertised(Identifier id, Set<String> allowed) {
        // Keep common negotiation and Fabric's split-packet transport available.
        return id.getNamespace().equals("minecraft") || switch (id.toString()) {
            case "c:version", "c:register", "fabric:split" -> true;
            default -> allowed.contains(id.toString());
        };
    }

    public static CustomPacketPayload apply(CustomPacketPayload payload, Set<String> allowed) {
        if (payload instanceof RegistrationPayload p && p.channels().stream().anyMatch(id -> !advertised(id, allowed))) {
            return new RegistrationPayload(p.type(), p.channels().stream().filter(id -> advertised(id, allowed)).toList());
        }
        if (payload instanceof CommonRegisterPayload p && p.channels().stream().anyMatch(id -> !advertised(id, allowed))) {
            var kept = new LinkedHashSet<Identifier>();
            for (var id : p.channels()) if (advertised(id, allowed)) kept.add(id);
            return new CommonRegisterPayload(p.version(), p.protocol(), kept);
        }
        return payload;
    }
}

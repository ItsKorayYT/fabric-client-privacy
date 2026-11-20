package io.github.itskorayyt.privacy.network;

import java.util.List;
import java.util.Set;
import net.minecraft.server.packs.repository.KnownPack;

public final class PackFilter {
    private static final Set<String> BUILT_INS = Set.of("core", "minecart_improvements", "redstone_experiments", "trade_rebalance");

    private PackFilter() {}

    private static boolean vanilla(KnownPack pack) {
        return pack.isVanilla() && BUILT_INS.contains(pack.id()) && pack.version().equals("26.3");
    }

    public static List<KnownPack> offers(List<KnownPack> offered) {
        for (KnownPack pack : offered) {
            if (!vanilla(pack)) return offered.stream().filter(PackFilter::vanilla).toList();
        }
        return offered;
    }
}

package io.github.itskorayyt.privacy;

import io.github.itskorayyt.privacy.config.*;
import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class ClientPrivacy implements ClientModInitializer {
    // Exact networking builds shipped in Fabric API releases for the final 26.3 client.
    private static final Set<String> NETWORKING_BUILDS = Set.of(
            "6.3.8+fcdff87f02", "6.3.8+fcdff87f5d", "6.3.9+b9b88df45d",
            "6.3.10+5c1fee4d5d", "6.4.0+a21c9eb65d", "6.4.1+c58129785d");

    private static volatile Settings settings = Settings.DEFAULT;
    private static volatile boolean channelAdapter;

    @Override
    public void onInitializeClient() {
        var loader = FabricLoader.getInstance();
        settings = ConfigFile.load(loader.getConfigDir().resolve("fabric-client-privacy.json"));
        // The optional typed adapter uses Fabric Networking implementation records.
        channelAdapter = supportsInstalledNetworking();
        if (settings.enabled() && settings.filterModChannels() && !channelAdapter
                && loader.isModLoaded("fabric-networking-api-v1")) {
            PrivacyLog.unsupportedNetworking();
        }
    }

    static boolean supportsInstalledNetworking() {
        return FabricLoader.getInstance().getModContainer("fabric-networking-api-v1")
                .map(mod -> NETWORKING_BUILDS.contains(mod.getMetadata().getVersion().getFriendlyString()))
                .orElse(false);
    }

    public static Settings settings() { return settings; }
    public static boolean hasChannelAdapter() { return channelAdapter; }
}

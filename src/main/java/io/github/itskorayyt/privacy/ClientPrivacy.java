package io.github.itskorayyt.privacy;

import io.github.itskorayyt.privacy.config.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class ClientPrivacy implements ClientModInitializer {
    private static volatile Settings settings = Settings.DEFAULT;
    private static volatile boolean channelAdapter;

    @Override
    public void onInitializeClient() {
        var loader = FabricLoader.getInstance();
        settings = ConfigFile.load(loader.getConfigDir().resolve("fabric-client-privacy.json"));
        // The optional typed adapter uses Fabric Networking implementation records.
        channelAdapter = loader.getModContainer("fabric-networking-api-v1")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString().equals("6.4.1+c58129785d"))
                .orElse(false);
    }

    public static Settings settings() { return settings; }
    public static boolean hasChannelAdapter() { return channelAdapter; }
}

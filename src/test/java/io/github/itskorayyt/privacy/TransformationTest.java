package io.github.itskorayyt.privacy;

import io.github.itskorayyt.privacy.network.SignPrivacy;
import java.util.List;
import java.util.Arrays;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TransformationTest {
    @Test void loadedFabricApiEnablesChannelFiltering() {
        var api = FabricLoader.getInstance().getModContainer("fabric-api").orElseThrow();
        assertEquals(System.getProperty("test.fabricApiVersion"), api.getMetadata().getVersion().getFriendlyString());
        assertTrue(ClientPrivacy.supportsInstalledNetworking());
    }

    @Test void allClientTargetsTransform() throws Exception {
        assertTrue(FabricLoader.getInstance().isModLoaded("fabric_client_privacy"));
        var addon = Class.forName("net.fabricmc.fabric.impl.networking.AbstractChanneledNetworkAddon");
        assertTrue(Arrays.stream(addon.getDeclaredMethods()).anyMatch(method -> method.getName().contains("privacy")));
        for (String target : List.of("client.multiplayer.ClientHandshakePacketListenerImpl",
                "client.multiplayer.ClientConfigurationPacketListenerImpl",
                "client.multiplayer.ClientCommonPacketListenerImpl",
                "client.gui.screens.inventory.AbstractSignEditScreen",
                "network.protocol.common.ServerboundCustomPayloadPacket")) {
            var type = Class.forName("net.minecraft." + target, false, getClass().getClassLoader());
            assertTrue(Arrays.stream(type.getDeclaredMethods())
                    .anyMatch(method -> method.getName().contains("privacy")), target);
        }
    }

    @Test void simpleModSignUsesVanillaFallback() {
        String key = "item.fabric_client_privacy.sample";
        assertEquals(key, SignPrivacy.replacement(Component.translatable(key)));
        assertEquals("fallback", SignPrivacy.replacement(Component.translatableWithFallback(key, "fallback")));
        assertNull(SignPrivacy.replacement(Component.literal("unchanged")));
        assertNull(SignPrivacy.replacement(Component.translatable("item.minecraft.apple")));
        assertNull(SignPrivacy.replacement(Component.translatable(key).append(" more")));
        assertNull(SignPrivacy.replacement(Component.translatable(key, "argument")));
        assertNull(SignPrivacy.replacement(Component.translatableWithFallback(key, "%s")));
    }
}

package io.github.itskorayyt.privacy;

import io.github.itskorayyt.privacy.network.LoginQueryApproval;
import io.github.itskorayyt.privacy.network.SignPrivacy;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.impl.networking.payload.FriendlyByteBufLoginQueryRequestPayload;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.core.RegistryAccess;
import net.minecraft.locale.Language;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.VanillaPackResourcesBuilder;
import net.minecraft.server.packs.repository.PackSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class TransformationTest {
    @Test void loadedFabricApiEnablesChannelFiltering() {
        var api = FabricLoader.getInstance().getModContainer("fabric-api").orElseThrow();
        assertEquals(System.getProperty("test.fabricApiVersion"), api.getMetadata().getVersion().getFriendlyString());
        assertTrue(ClientPrivacy.supportsInstalledNetworking());
    }

    @Test void allClientTargetsTransform() throws Exception {
        assertTrue(FabricLoader.getInstance().isModLoaded("fabric_client_privacy"));
        for (String target : List.of("client.multiplayer.ClientHandshakePacketListenerImpl",
                "client.multiplayer.ClientConfigurationPacketListenerImpl",
                "client.multiplayer.ClientCommonPacketListenerImpl",
                "client.gui.screens.inventory.AbstractSignEditScreen",
                "client.gui.screens.options.OptionsScreen",
                "network.chat.contents.TranslatableContents",
                "network.Connection")) {
            var type = Class.forName("net.minecraft." + target, false, getClass().getClassLoader());
            assertTrue(Arrays.stream(type.getDeclaredMethods())
                    .anyMatch(method -> method.getName().contains("privacy")), target);
        }
    }

    @Test void connectionFiltersBeforeEncodingAndPreservesOrdinaryPackets() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var connection = new Connection(PacketFlow.CLIENTBOUND);
        var channel = new EmbeddedChannel(new PacketEncoder<>(ConfigurationProtocols.SERVERBOUND), connection);
        try {
            var blocked = new DiscardedPayload(Identifier.parse("example:optional_data"));
            var sendResult = new AtomicReference<Throwable>();
            connection.send(new ServerboundCustomPayloadPacket(blocked), future -> sendResult.set(future.cause()));
            assertInstanceOf(IllegalStateException.class, sendResult.get());
            assertNull(channel.readOutbound());

            connection.send(new ServerboundCustomPayloadPacket(new BrandPayload("fabric")));
            try (var encoded = new PacketBuffer(channel.readOutbound())) {
                encoded.buffer.readVarInt();
                assertEquals(new BrandPayload("vanilla"),
                        ServerboundCustomPayloadPacket.STREAM_CODEC.decode(encoded.buffer).payload());
            }

            connection.send(new ServerboundKeepAlivePacket(91));
            try (var encoded = new PacketBuffer(channel.readOutbound())) {
                encoded.buffer.readVarInt();
                assertEquals(91L, ServerboundKeepAlivePacket.STREAM_CODEC.decode(encoded.buffer).getId());
            }
            assertNull(channel.readOutbound());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test void playConnectionBlocksARegisteredFabricModPayload() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        PayloadTypeRegistry.serverboundPlay().register(ModData.TYPE, ModData.CODEC);
        var context = new GameProtocols.Context() {
            @Override public boolean hasInfiniteMaterials() { return false; }
            @Override public boolean canUseCommandBlocks() { return false; }
        };
        var protocol = GameProtocols.SERVERBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(RegistryAccess.EMPTY), context);
        var connection = new Connection(PacketFlow.CLIENTBOUND);
        var channel = new EmbeddedChannel(new PacketEncoder<>(protocol), connection);
        try {
            connection.send(ClientPlayNetworking.createServerboundPacket(new ModData(42)));
            assertNull(channel.readOutbound());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test void directLoginAnswerMaySendDataOnlyOnceForAnApprovedQuery() {
        var connection = new Connection(PacketFlow.CLIENTBOUND);
        var channel = new EmbeddedChannel(new PacketEncoder<>(LoginProtocols.SERVERBOUND), connection);
        try {
            var answer = new ServerboundCustomQueryAnswerPacket(29, buffer -> buffer.writeByte(42));
            connection.send(answer);
            assertNull(channel.readOutbound());

            ((LoginQueryApproval) connection).privacy$allowLoginAnswer(29);
            connection.send(answer);
            try (var encoded = new PacketBuffer(channel.readOutbound())) {
                encoded.buffer.readVarInt();
                assertEquals(29, encoded.buffer.readVarInt());
                assertTrue(encoded.buffer.readBoolean());
                assertEquals(42, encoded.buffer.readUnsignedByte());
                assertFalse(encoded.buffer.isReadable());
            }

            connection.send(answer);
            assertNull(channel.readOutbound());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test void unapprovedLoginQuerySendsEmptyAnswerWithoutCallingFabricReceiver() {
        var id = Identifier.parse("example:login_probe");
        assertTrue(ClientLoginNetworking.registerGlobalReceiver(id, (client, listener, buffer, sender) -> {
            fail("Unapproved login query reached the Fabric receiver");
            return null;
        }));
        var connection = new Connection(PacketFlow.CLIENTBOUND);
        var channel = new EmbeddedChannel(new PacketEncoder<>(LoginProtocols.SERVERBOUND), connection);
        var requestData = new FriendlyByteBuf(Unpooled.buffer().writeByte(42));
        try {
            var listener = new ClientHandshakePacketListenerImpl(connection, null, null, null,
                    false, null, status -> {}, null, null);
            listener.handleCustomQuery(new ClientboundCustomQueryPacket(17,
                    new FriendlyByteBufLoginQueryRequestPayload(id, requestData)));

            try (var answer = new PacketBuffer(channel.readOutbound())) {
                answer.buffer.readVarInt();
                assertEquals(17, answer.buffer.readVarInt());
                assertFalse(answer.buffer.readBoolean());
                assertFalse(answer.buffer.isReadable());
            }
            assertNull(channel.readOutbound());
        } finally {
            requestData.release();
            ClientLoginNetworking.unregisterGlobalReceiver(id);
            channel.finishAndReleaseAll();
        }
    }

    @Test void signEditorUsesVanillaTextAndDoesNotLookUpModKeys() {
        var vanilla = Language.DEFAULT_INSTANCE;
        assertEquals("plain text", SignPrivacy.editorLine(Component.literal("plain text"), vanilla));
        assertEquals("Apple", SignPrivacy.editorLine(Component.translatable("item.minecraft.apple"), vanilla));
        assertEquals("example.settings", SignPrivacy.editorLine(Component.translatable("example.settings"), vanilla));
        assertEquals("config.fabric_client_privacy.title", SignPrivacy.editorLine(
                Component.translatable("config.fabric_client_privacy.title"), vanilla));
        assertEquals("fallback", SignPrivacy.editorLine(
                Component.translatableWithFallback("example.settings", "fallback"), vanilla));
        assertEquals("key.example.toggle", SignPrivacy.editorLine(Component.keybind("key.example.toggle"), vanilla));
    }

    @Test void signEditorSanitizesNestedFormattingAndObjectFallbacks() {
        var vanilla = Language.DEFAULT_INSTANCE;
        var object = MutableComponent.create(new ObjectContents(
                new AtlasSprite(AtlasSprite.DEFAULT_ATLAS, Identifier.withDefaultNamespace("example")),
                Optional.of(Component.translatable("example.private"))));
        var line = Component.translatableWithFallback("example.formatted", "%2$s : %1$s %%", object,
                Component.literal("before ").append(Component.keybind("key.example.toggle")))
                .append(" after");
        assertEquals("before key.example.toggle : example.private % after", SignPrivacy.editorLine(line, vanilla));
        assertEquals("invalid %s %q", SignPrivacy.editorLine(
                Component.translatableWithFallback("example.invalid", "invalid %s %q", "argument"), vanilla));
        assertEquals(383, SignPrivacy.editorLine(
                Component.translatableWithFallback("example.long", "a".repeat(383) + "😀"), vanilla).length());
    }

    @Test void signEditorKeepsBuiltInNonEnglishTranslations(@TempDir Path directory) throws Exception {
        var languages = Files.createDirectories(directory.resolve("assets/minecraft/lang"));
        Files.writeString(languages.resolve("de_de.json"), """
                {"item.minecraft.apple":"Apfel"}
                """);
        var pack = new VanillaPackResourcesBuilder().exposeNamespace("minecraft")
                .pushJarResources().pushUniversalPath(directory)
                .build(new PackLocationInfo("vanilla", Component.literal("Vanilla"), PackSource.BUILT_IN, Optional.empty()));

        var german = SignPrivacy.vanillaLanguage(pack, "de_de");
        assertEquals("Apfel", SignPrivacy.editorLine(Component.translatable("item.minecraft.apple"), german));
        var activeLanguages = Files.createDirectories(directory.resolve("active/assets/minecraft/lang"));
        Files.writeString(activeLanguages.resolve("de_de.json"), """
                {"item.minecraft.apple":"Pack override","example.settings":"Local add-on text"}
                """);
        var activePack = new VanillaPackResourcesBuilder().exposeNamespace("minecraft")
                .pushUniversalPath(directory.resolve("active"))
                .build(new PackLocationInfo("active", Component.literal("Active"), PackSource.SERVER, Optional.empty()));
        var active = ClientLanguage.loadFrom(activePack.asResourceManager(), List.of("de_de"), false);
        var earlier = Language.getInstance();
        Language.inject(active);
        try {
            var line = Component.translatable("example.settings");
            assertEquals("Local add-on text", line.getString());
            assertEquals("example.settings", SignPrivacy.editorLine(line, german));
            assertEquals("Apfel", SignPrivacy.editorLine(Component.translatable("item.minecraft.apple"), german));
        } finally {
            Language.inject(earlier);
        }
        assertSame(Language.DEFAULT_INSTANCE, SignPrivacy.vanillaLanguage(pack, "example_missing"));
        assertSame(Language.DEFAULT_INSTANCE, SignPrivacy.vanillaLanguage(pack, Language.DEFAULT));
    }

    private record ModData(int value) implements CustomPacketPayload {
        private static final Type<ModData> TYPE = new Type<>(Identifier.parse("example:play_data"));
        private static final StreamCodec<RegistryFriendlyByteBuf, ModData> CODEC =
                StreamCodec.of((buffer, data) -> buffer.writeInt(data.value()), buffer -> new ModData(buffer.readInt()));

        @Override public Type<ModData> type() {
            return TYPE;
        }
    }

    private static final class PacketBuffer implements AutoCloseable {
        private final FriendlyByteBuf buffer;

        PacketBuffer(ByteBuf bytes) {
            buffer = new FriendlyByteBuf(assertNotNullBuffer(bytes));
        }

        private static ByteBuf assertNotNullBuffer(ByteBuf bytes) {
            assertNotNull(bytes, "No packet was written");
            return bytes;
        }

        @Override public void close() {
            buffer.release();
        }
    }
}

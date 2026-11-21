package io.github.itskorayyt.privacy;

import io.github.itskorayyt.privacy.config.*;
import io.github.itskorayyt.privacy.network.*;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.impl.networking.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.KnownPack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PrivacyTest {
    @TempDir Path directory;

    @Test void firstRunAndDocumentedExample() throws Exception {
        Path file = directory.resolve("config/privacy.json");
        assertEquals(Settings.DEFAULT, ConfigFile.load(file));
        assertEquals(Files.readString(Path.of("examples/fabric-client-privacy.json")), Files.readString(file));
        assertEquals(Settings.DEFAULT, ConfigFile.load(file));
    }

    @Test void malformedInputSurvivesRecovery() throws Exception {
        Path file = directory.resolve("privacy.json");
        for (String invalid : List.of("null", "[]", "{", "{}", "true", "{} junk")) {
            Files.writeString(file, invalid);
            assertEquals(Settings.DEFAULT, ConfigFile.load(file));
            assertEquals(invalid, Files.readString(file));
        }
    }

    @Test void migrationAndFieldValidation() throws Exception {
        Path file = directory.resolve("privacy.json");
        Files.writeString(file, """
                {"enabled":false,"spoofClientBrand":"false","futureFeature":9,
                 "allowedChannels":["example:maps/v1","example:maps/v1",12,"*","BAD:Id","missing_namespace"]}
                """);
        Settings settings = ConfigFile.load(file);
        assertFalse(settings.enabled());
        assertTrue(settings.spoofClientBrand());
        assertTrue(settings.filterKnownPacks());
        assertTrue(settings.blockUnapprovedCustomPayloads());
        assertEquals(Set.of("example:maps/v1"), settings.allowedChannels());
    }

    @Test void savesEditsAndPreservesExtraFields() throws Exception {
        Path file = directory.resolve("config/privacy.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
                {"enabled":true,"futureFeature":{"value":9}}
                """);
        var edited = new Settings(false, true, false, true, false, true, true,
                Set.of("example:map", "other:sync"));
        ConfigFile.save(file, edited);

        assertEquals(edited, ConfigFile.load(file));
        var saved = com.google.gson.JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        assertEquals(9, saved.getAsJsonObject("futureFeature").get("value").getAsInt());
    }

    @Test void failedSavePreservesExistingFile() throws Exception {
        Path file = directory.resolve("config/privacy.json");
        var original = ConfigFile.load(file);
        var originalText = Files.readString(file);
        var oversized = new HashSet<String>();
        for (int index = 0; index < 1200; index++) {
            oversized.add("example:" + "a".repeat(240) + index);
        }

        assertThrows(java.io.IOException.class, () -> ConfigFile.save(file, new Settings(
                false, true, true, true, true, true, false, oversized)));
        assertEquals(originalText, Files.readString(file));
        assertEquals(original, ConfigFile.load(file));
    }

    private static final Identifier MAP = Identifier.parse("example:map");
    private static final Identifier BRAND = Identifier.parse("minecraft:brand");
    private static final Identifier CONTROL = Identifier.parse("c:register");
    private static final Identifier VANILLA_NAMESPACE_PROBE = Identifier.parse("minecraft:mod_probe");

    private Settings settings(boolean filterModChannels, boolean blockUnapprovedCustomPayloads, String... channels) {
        return new Settings(true, true, filterModChannels, true, true, blockUnapprovedCustomPayloads, false, Set.of(channels));
    }

    @Test void legacyAdvertisementRoundTrip() {
        for (var type : List.of(RegistrationPayload.REGISTER, RegistrationPayload.UNREGISTER)) {
            var original = new RegistrationPayload(type, List.of(MAP, BRAND, CONTROL));
            var namespacedProbe = new RegistrationPayload(type, List.of(VANILLA_NAMESPACE_PROBE, BRAND));
            var safe = (RegistrationPayload) ChannelFilter.outgoing(namespacedProbe, Settings.DEFAULT, true);
            assertEquals(List.of(BRAND), safe.channels());
            assertSame(original, ChannelFilter.outgoing(original, settings(true, true, "example:map"), true));
            var filtered = (RegistrationPayload) ChannelFilter.outgoing(original, Settings.DEFAULT, true);
            assertEquals(type, filtered.type());
            var codec = type.equals(RegistrationPayload.REGISTER) ? RegistrationPayload.REGISTER_CODEC : RegistrationPayload.UNREGISTER_CODEC;
            var buf = new FriendlyByteBuf(Unpooled.buffer());
            try {
                codec.encode(buf, filtered);
                assertEquals(List.of(BRAND, CONTROL), codec.decode(buf).channels());
                assertFalse(buf.isReadable());
                assertNull(ChannelFilter.outgoing(new RegistrationPayload(type, List.of(MAP)), Settings.DEFAULT, true));
                assertNull(ChannelFilter.outgoing(original, Settings.DEFAULT, false));
                assertSame(original, ChannelFilter.outgoing(original, settings(false, true), false));
            } finally {
                buf.release();
            }
        }
    }

    @Test void commonRegistrationPreservesProtocol() {
        var original = new CommonRegisterPayload(1, "play", Set.of(MAP, CONTROL));
        var result = (CommonRegisterPayload) ChannelFilter.outgoing(original, Settings.DEFAULT, true);
        assertEquals(new CommonRegisterPayload(1, "play", Set.of(CONTROL)), result);
        assertSame(original, ChannelFilter.outgoing(original, settings(true, true, "example:map"), true));
    }

    @Test void outboundCustomPayloadsRequireExplicitApproval() {
        var brand = new BrandPayload("fabric");
        assertEquals(new BrandPayload("vanilla"), ChannelFilter.outgoing(brand, Settings.DEFAULT, false));
        var optional = new DiscardedPayload(MAP);
        assertNull(ChannelFilter.outgoing(optional, Settings.DEFAULT, false));
        assertNull(ChannelFilter.outgoing(optional, settings(true, true, "example:maps"), true));
        assertSame(optional, ChannelFilter.outgoing(optional, settings(true, true, "example:map"), true));
        assertSame(optional, ChannelFilter.outgoing(optional, settings(true, false), false));
        assertSame(optional, ChannelFilter.outgoing(optional,
                new Settings(false, true, true, true, true, true, false, Set.of()), true));
        assertNull(ChannelFilter.outgoing(new DiscardedPayload(VANILLA_NAMESPACE_PROBE), Settings.DEFAULT, true));
        assertNull(ChannelFilter.outgoing(new DiscardedPayload(Identifier.parse("fabric:split")), Settings.DEFAULT, true));

        var negotiation = new CommonVersionPayload(new int[] {1});
        assertSame(negotiation, ChannelFilter.outgoing(negotiation, Settings.DEFAULT, true));
        assertNull(ChannelFilter.outgoing(new DiscardedPayload(negotiation.type().id()), Settings.DEFAULT, true));
    }

    @Test void knownPacksPreserveIdentityAndOrder() {
        var core = new KnownPack("minecraft", "core", "26.3");
        var experiment = new KnownPack("minecraft", "trade_rebalance", "26.3");
        var vanilla = List.of(experiment, core);
        assertSame(vanilla, PackFilter.offers(vanilla));
        assertEquals(vanilla, PackFilter.offers(List.of(new KnownPack("minecraft", "extra", "26.3"), experiment,
                new KnownPack("minecraft", "core", "other"), new KnownPack("tools", "core", "26.3"), core)));
    }
}

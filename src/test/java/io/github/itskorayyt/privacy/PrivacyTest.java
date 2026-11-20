package io.github.itskorayyt.privacy;

import io.github.itskorayyt.privacy.config.*;
import io.github.itskorayyt.privacy.network.*;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.impl.networking.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.BrandPayload;
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
        assertEquals(Set.of("example:maps/v1"), settings.allowedChannels());
    }

    private static final Identifier MAP = Identifier.parse("example:map");
    private static final Identifier BRAND = Identifier.parse("minecraft:brand");
    private static final Identifier CONTROL = Identifier.parse("c:register");

    @Test void legacyAdvertisementRoundTrip() {
        for (var type : List.of(RegistrationPayload.REGISTER, RegistrationPayload.UNREGISTER)) {
            var original = new RegistrationPayload(type, List.of(MAP, BRAND, CONTROL));
            assertSame(original, ChannelFilter.apply(original, Set.of("example:map")));
            var filtered = (RegistrationPayload) ChannelFilter.apply(original, Set.of("example:maps"));
            assertEquals(type, filtered.type());
            var codec = type.equals(RegistrationPayload.REGISTER) ? RegistrationPayload.REGISTER_CODEC : RegistrationPayload.UNREGISTER_CODEC;
            var buf = new FriendlyByteBuf(Unpooled.buffer());
            try {
                codec.encode(buf, filtered);
                assertEquals(List.of(BRAND, CONTROL), codec.decode(buf).channels());
                assertFalse(buf.isReadable());
                var empty = (RegistrationPayload) ChannelFilter.apply(new RegistrationPayload(type, List.of(MAP)), Set.of());
                codec.encode(buf, empty);
                assertTrue(codec.decode(buf).channels().isEmpty());
            } finally {
                buf.release();
            }
        }
    }

    @Test void commonRegistrationPreservesProtocol() {
        var original = new CommonRegisterPayload(1, "play", Set.of(MAP, CONTROL));
        var result = (CommonRegisterPayload) ChannelFilter.apply(original, Set.of());
        assertEquals(new CommonRegisterPayload(1, "play", Set.of(CONTROL)), result);
        assertSame(original, ChannelFilter.apply(original, Set.of("example:map")));
    }

    @Test void otherPayloadsPassByIdentity() {
        var brand = new BrandPayload("fabric");
        assertSame(brand, ChannelFilter.apply(brand, Set.of()));
        var unknown = new net.minecraft.network.protocol.common.custom.DiscardedPayload(MAP);
        assertSame(unknown, ChannelFilter.apply(unknown, Set.of()));
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

package io.github.itskorayyt.privacy;

import io.github.itskorayyt.privacy.config.Settings;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PacketTraceTest {
    @Test void encodedTypesAreOptInDeduplicatedAndRedacted() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var connection = new Connection(PacketFlow.CLIENTBOUND);
        var channel = new EmbeddedChannel(new PacketEncoder<>(ConfigurationProtocols.SERVERBOUND), connection);
        var login = new Connection(PacketFlow.CLIENTBOUND);
        var loginChannel = new EmbeddedChannel(new PacketEncoder<>(LoginProtocols.SERVERBOUND), login);
        var settings = ClientPrivacy.class.getDeclaredField("settings");
        settings.setAccessible(true);
        var previousSettings = ClientPrivacy.settings();
        var logger = (Logger) LogManager.getLogger("fabric-client-privacy");
        var previousLevel = logger.getLevel();
        var messages = new ArrayList<String>();
        var appender = new AbstractAppender("Packet trace test", null, null, true, Property.EMPTY_ARRAY) {
            @Override public void append(LogEvent event) {
                messages.add(event.getMessage().getFormattedMessage());
            }
        };
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
        try {
            settings.set(null, Settings.DEFAULT);
            connection.send(new ServerboundKeepAlivePacket(123456789));
            assertTrue(messages.isEmpty());

            settings.set(null, new Settings(true, true, true, true, true, true, true, Set.of("example:private_allowed")));
            connection.send(new ServerboundKeepAlivePacket(123456789));
            connection.send(new ServerboundKeepAlivePacket(123456789));
            connection.send(new ServerboundCustomPayloadPacket(new DiscardedPayload(Identifier.parse("example:private"))));
            connection.send(new ServerboundCustomPayloadPacket(new BrandPayload("private brand")));
            var allowed = new DiscardedPayload(Identifier.parse("example:private_allowed"));
            connection.send(new ServerboundCustomPayloadPacket(allowed));
            connection.send(new ServerboundCustomPayloadPacket(allowed));
            login.send(new ServerboundCustomQueryAnswerPacket(123456789, null));
            for (var id : List.of("example:private", "minecraft:private")) {
                assertEquals("other", PrivacyLog.builtInTypeLabel(new PacketType<>(PacketFlow.SERVERBOUND, Identifier.parse(id))));
            }
            assertEquals(List.of(
                    "[Privacy trace] Minecraft encoded serverbound packet keep_alive in configuration.",
                    "[Privacy trace] Minecraft encoded serverbound packet custom_payload (brand) in configuration.",
                    "[Privacy trace] Minecraft encoded serverbound packet custom_payload (other) in configuration.",
                    "[Privacy trace] Minecraft encoded serverbound packet custom_query_answer (empty) in login."),
                    messages.stream().filter(s -> s.contains("encoded serverbound")).toList());
            assertTrue(messages.stream().noneMatch(s -> s.contains("private") || s.contains("123456789")));
        } finally {
            settings.set(null, previousSettings);
            channel.finishAndReleaseAll();
            loginChannel.finishAndReleaseAll();
            logger.removeAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }
    }
}

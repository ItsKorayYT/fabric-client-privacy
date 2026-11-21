package io.github.itskorayyt.privacy;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PrivacyLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("fabric-client-privacy");
    private static final Map<Connection, EnumSet<Event>> OBSERVED = new WeakHashMap<>();

    public enum Event {
        LOGIN_FINISHED("Minecraft processed login completion."),
        REGISTRATION_RECEIVED("Minecraft handled an incoming channel registration."),
        LOGIN_QUERY_DECLINED("The privacy check declined a custom login query."),
        PAYLOAD_BLOCKED("The privacy check blocked an outgoing custom payload or login answer."),
        BRAND_REWRITTEN("The privacy check rewrote the outgoing client brand."),
        REGISTRATION_FILTERED("The privacy check filtered an outgoing channel advertisement."),
        SIGN_REQUEST_RECEIVED("Minecraft received a sign editor request."),
        SIGN_EDITOR_PREFILLED("Minecraft built sign editor prefill."),
        SIGN_PREFILL_SANITIZED("The privacy check sanitized structured sign prefill."),
        SIGN_UPDATE_QUEUED("A sign update reached Minecraft's outgoing packet hook.");

        private final String message;

        Event(String message) {
            this.message = message;
        }
    }

    private PrivacyLog() {}

    public static void trace(Connection connection, Event event) {
        var settings = ClientPrivacy.settings();
        if (!settings.debugLogging() || connection.getSending() != PacketFlow.SERVERBOUND || connection.isMemoryConnection()) {
            return;
        }
        synchronized (OBSERVED) {
            var events = OBSERVED.computeIfAbsent(connection, ignored -> {
                LOGGER.info("[Privacy trace] Connection settings enabled={} brand={} channels={} knownPacks={} signs={} payloads={} allowedChannelCount={} fabricAdapter={}",
                        settings.enabled(), settings.spoofClientBrand(), settings.filterModChannels(), settings.filterKnownPacks(),
                        settings.protectTranslationProbes(), settings.blockUnapprovedCustomPayloads(),
                        settings.allowedChannels().size(), ClientPrivacy.hasChannelAdapter());
                return EnumSet.noneOf(Event.class);
            });
            if (events.add(event)) {
                LOGGER.info("[Privacy trace] {}", event.message);
            }
        }
    }

    public static void unsupportedNetworking() {
        LOGGER.warn("Unrecognized Fabric Networking version. Channel advertisements will be blocked while channel filtering is enabled.");
    }

    public static void configSaveFailed(IOException cause) {
        LOGGER.warn("Could not save privacy configuration.", cause);
    }

    public static void configUnavailable() {
        LOGGER.warn("Could not load privacy configuration. Using defaults for this session.");
    }
}

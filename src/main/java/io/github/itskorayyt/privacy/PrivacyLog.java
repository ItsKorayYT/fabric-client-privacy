package io.github.itskorayyt.privacy;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PrivacyLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("fabric-client-privacy");
    private static final AtomicBoolean SEEN_REGISTER = new AtomicBoolean();

    private static final AtomicBoolean BLOCKED_PAYLOAD = new AtomicBoolean();
    private static final AtomicBoolean DECLINED_QUERY = new AtomicBoolean();

    private PrivacyLog() {}

    public static void payloadBlocked() {
        if (BLOCKED_PAYLOAD.compareAndSet(false, true)) LOGGER.info("Blocked an outgoing custom payload.");
    }

    public static void loginQueryDeclined() {
        if (DECLINED_QUERY.compareAndSet(false, true)) LOGGER.info("Declined an unapproved custom login query.");
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

    public static void registrationSeen() {
        if (SEEN_REGISTER.compareAndSet(false, true)) LOGGER.info("Received a channel registration request.");
    }
}

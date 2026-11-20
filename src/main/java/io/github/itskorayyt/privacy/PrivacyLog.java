package io.github.itskorayyt.privacy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PrivacyLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("fabric-client-privacy");
    private static final AtomicBoolean SEEN_REGISTER = new AtomicBoolean();

    private PrivacyLog() {}

    public static void configUnavailable() {
        LOGGER.warn("Could not load privacy configuration. Using defaults for this session.");
    }

    public static void registrationSeen() {
        if (SEEN_REGISTER.compareAndSet(false, true)) LOGGER.info("Received a channel registration request.");
    }
}

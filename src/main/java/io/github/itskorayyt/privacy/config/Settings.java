package io.github.itskorayyt.privacy.config;

import java.util.Set;

public record Settings(boolean enabled, boolean spoofClientBrand, boolean filterModChannels,
                       boolean filterKnownPacks, boolean protectTranslationProbes,
                       boolean blockUnapprovedCustomPayloads, boolean debugLogging, Set<String> allowedChannels) {
    public static final Settings DEFAULT = new Settings(true, true, true, true, true, true, false, Set.of());

    public Settings {
        allowedChannels = Set.copyOf(allowedChannels);
    }
}

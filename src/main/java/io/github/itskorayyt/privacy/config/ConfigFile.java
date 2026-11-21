package io.github.itskorayyt.privacy.config;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import io.github.itskorayyt.privacy.PrivacyLog;

public final class ConfigFile {
    private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).setPrettyPrinting().create();
    private static final Pattern CHANNEL = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

    private ConfigFile() {}

    public static Settings load(Path file) {
        try {
            if (Files.notExists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, JSON.toJson(Settings.DEFAULT) + "\n", StandardOpenOption.CREATE_NEW);
            }
            if (Files.size(file) > 262144) throw new JsonParseException("Size limit");
            JsonObject root = JSON.fromJson(Files.readString(file), JsonObject.class);
            if (root == null) throw new JsonParseException("Missing object");
            Set<String> channels = new LinkedHashSet<>();
            JsonElement entries = root.get("allowedChannels");
            if (entries != null && entries.isJsonArray()) {
                for (JsonElement entry : entries.getAsJsonArray()) {
                    if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) continue;
                    String id = entry.getAsString();
                    if (id.length() <= 256 && CHANNEL.matcher(id).matches()) channels.add(id);
                }
            }
            Settings d = Settings.DEFAULT;
            return new Settings(flag(root, "enabled", d.enabled()), flag(root, "spoofClientBrand", d.spoofClientBrand()),
                    flag(root, "filterModChannels", d.filterModChannels()), flag(root, "filterKnownPacks", d.filterKnownPacks()),
                    flag(root, "protectTranslationProbes", d.protectTranslationProbes()),
                    flag(root, "blockUnapprovedCustomPayloads", d.blockUnapprovedCustomPayloads()),
                    flag(root, "debugLogging", d.debugLogging()), channels);
        } catch (IOException | JsonParseException ex) {
            PrivacyLog.configUnavailable();
            return Settings.DEFAULT;
        }
    }

    private static boolean flag(JsonObject root, String name, boolean fallback) {
        JsonElement value = root.get(name);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()
                ? value.getAsBoolean() : fallback;
    }
}

package io.github.itskorayyt.privacy.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;
import io.github.itskorayyt.privacy.PrivacyLog;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

public final class ConfigFile {
    public static final int MAX_FILE_BYTES = 262144;

    private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).setPrettyPrinting().create();
    private static final Pattern CHANNEL = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

    private ConfigFile() {}

    public static Settings load(Path file) {
        try {
            if (Files.notExists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, JSON.toJson(Settings.DEFAULT) + "\n", StandardOpenOption.CREATE_NEW);
            }
            JsonObject root = readObject(file);
            Set<String> channels = new LinkedHashSet<>();
            JsonElement entries = root.get("allowedChannels");
            if (entries != null && entries.isJsonArray()) {
                for (JsonElement entry : entries.getAsJsonArray()) {
                    if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) continue;
                    String id = entry.getAsString();
                    if (isChannelId(id)) channels.add(id);
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

    public static boolean isChannelId(String id) {
        return id.length() <= 256 && CHANNEL.matcher(id).matches();
    }

    public static void save(Path file, Settings settings) throws IOException {
        JsonObject root = new JsonObject();
        if (Files.exists(file)) {
            try {
                root = readObject(file);
            } catch (JsonParseException ignored) {
                // An explicit save can repair an unreadable JSON structure.
            }
        }
        JsonObject updated = JSON.toJsonTree(settings).getAsJsonObject();
        updated.add("allowedChannels", JSON.toJsonTree(settings.allowedChannels().stream().sorted().toList()));
        for (var entry : updated.entrySet()) {
            root.add(entry.getKey(), entry.getValue());
        }
        byte[] contents = (JSON.toJson(root) + "\n").getBytes(StandardCharsets.UTF_8);
        if (contents.length > MAX_FILE_BYTES) {
            throw new IOException("Privacy configuration exceeds the size limit");
        }

        Path destination = file.toAbsolutePath();
        Path directory = destination.getParent();
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "fabric-client-privacy-", ".tmp");
        try {
            Files.write(temporary, contents);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static JsonObject readObject(Path file) throws IOException {
        if (Files.size(file) > MAX_FILE_BYTES) {
            throw new JsonParseException("Size limit");
        }
        JsonObject root = JSON.fromJson(Files.readString(file), JsonObject.class);
        if (root == null) {
            throw new JsonParseException("Missing object");
        }
        return root;
    }

    private static boolean flag(JsonObject root, String name, boolean fallback) {
        JsonElement value = root.get(name);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()
                ? value.getAsBoolean() : fallback;
    }
}

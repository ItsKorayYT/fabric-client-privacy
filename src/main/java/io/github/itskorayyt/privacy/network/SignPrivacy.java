package io.github.itskorayyt.privacy.network;

import io.github.itskorayyt.privacy.mixin.TranslatableContentsInvoker;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.contents.TranslatableFormatException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.VanillaPackResources;

public final class SignPrivacy {
    private static final int MAX_LINE_LENGTH = 384;
    private static final int MAX_DEPTH = 64;
    private static final int MAX_COMPONENTS = 2048;

    private static String cachedLocale = Language.DEFAULT;
    private static Language cachedLanguage = Language.DEFAULT_INSTANCE;

    private SignPrivacy() {}

    // Called on the render thread. VanillaPackResources excludes the profile's active resource packs.
    public static Language vanillaLanguage(VanillaPackResources vanilla, String locale) {
        if (!cachedLocale.equals(locale)) {
            var resources = vanilla.asResourceManager();
            var location = Identifier.tryParse("minecraft:lang/" + locale + ".json");
            cachedLanguage = !Language.DEFAULT.equals(locale) && location != null && resources.getResource(location).isPresent()
                    ? ClientLanguage.loadFrom(resources, List.of(Language.DEFAULT, locale), false)
                    : Language.DEFAULT_INSTANCE;
            cachedLocale = locale;
        }
        return cachedLanguage;
    }

    public static String editorLine(Component line, Language vanillaLanguage) {
        if (line.getContents() instanceof PlainTextContents text && line.getSiblings().isEmpty()) {
            return text.text();
        }
        var output = new SignLine(vanillaLanguage);
        output.append(line, 0);
        int length = output.text.length();
        if (length == MAX_LINE_LENGTH && Character.isHighSurrogate(output.text.charAt(length - 1))) {
            output.text.setLength(length - 1);
        }
        return output.text.toString();
    }

    private static final class SignLine {
        private final Language language;
        private final StringBuilder text = new StringBuilder();
        private int remainingComponents = MAX_COMPONENTS;

        private SignLine(Language language) {
            this.language = language;
        }

        private void append(Component component, int depth) {
            if (depth >= MAX_DEPTH || remainingComponents <= 0 || text.length() >= MAX_LINE_LENGTH) {
                return;
            }
            remainingComponents--;
            switch (component.getContents()) {
                case TranslatableContents translation -> appendTranslation(translation, depth);
                case KeybindContents keybind -> appendText(keybind.getName());
                case ObjectContents object -> {
                    if (object.fallback().isPresent()) {
                        append(object.fallback().orElseThrow(), depth + 1);
                    } else {
                        appendText(object.contents().defaultFallback());
                    }
                }
                default -> component.getContents().visit(this::appendText);
            }
            for (var sibling : component.getSiblings()) {
                append(sibling, depth + 1);
            }
        }

        private void appendTranslation(TranslatableContents translation, int depth) {
            String fallback = translation.getFallback() == null ? translation.getKey() : translation.getFallback();
            String template = language.getOrDefault(translation.getKey(), fallback);
            int start = text.length();
            try {
                ((TranslatableContentsInvoker) translation).privacyDecomposeTemplate(template, part -> {
                    if (part instanceof Component component) {
                        append(component, depth + 1);
                    } else {
                        part.visit(this::appendText);
                    }
                });
            } catch (TranslatableFormatException ex) {
                text.setLength(start);
                appendText(template);
            }
        }

        private Optional<Void> appendText(String part) {
            int length = Math.min(part.length(), MAX_LINE_LENGTH - text.length());
            text.append(part, 0, length);
            return Optional.empty();
        }
    }
}

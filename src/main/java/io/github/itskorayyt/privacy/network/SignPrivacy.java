package io.github.itskorayyt.privacy.network;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class SignPrivacy {
    private SignPrivacy() {}

    // Only unformatted, standalone keys with a loaded mod's identifier as the
    // second segment (e.g. item.example.widget), in the built-in English locale.
    public static String replacement(Component line) {
        if (line.getContents() instanceof TranslatableContents text && line.getSiblings().isEmpty()
                && text.getArgs().length == 0 && !Language.DEFAULT_INSTANCE.has(text.getKey())) {
            String key = text.getKey();
            int first = key.indexOf('.');
            int second = key.indexOf('.', first + 1);
            String fallback = text.getFallback() == null ? key : text.getFallback();
            if (first > 0 && second > first + 1 && fallback.indexOf('%') < 0
                    && FabricLoader.getInstance().isModLoaded(key.substring(first + 1, second))) {
                return fallback;
            }
        }
        return null;
    }
}

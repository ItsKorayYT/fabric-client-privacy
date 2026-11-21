package io.github.itskorayyt.privacy.mixin;

import java.util.function.Consumer;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TranslatableContents.class)
public interface TranslatableContentsInvoker {
    @Invoker("decomposeTemplate")
    void privacyDecomposeTemplate(String template, Consumer<FormattedText> parts);
}

package io.github.itskorayyt.privacy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.itskorayyt.privacy.ClientPrivacy;
import io.github.itskorayyt.privacy.PrivacyLog;
import io.github.itskorayyt.privacy.network.EditorTextPrivacy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AnvilScreen.class)
abstract class AnvilScreenMixin {
    // The slot update initializes the field. Its responder compares the same name before sending a rename.
    @WrapOperation(method = {"slotChanged", "onNameChanged"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;"), require = 2)
    private String privacyResolveItemName(Component name, Operation<String> original) {
        var settings = ClientPrivacy.settings();
        var client = Minecraft.getInstance();
        if (!settings.enabled() || !settings.protectTranslationProbes() || client.hasSingleplayerServer()
                || !EditorTextPrivacy.isStructured(name)) {
            return original.call(name);
        }
        var listener = settings.debugLogging() ? client.getConnection() : null;
        if (listener != null) {
            PrivacyLog.trace(listener.getConnection(), PrivacyLog.Event.ANVIL_PREFILL_SANITIZED);
        }
        return EditorTextPrivacy.editorLine(name, client);
    }
}

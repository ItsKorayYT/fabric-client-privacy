package io.github.itskorayyt.privacy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.itskorayyt.privacy.ClientPrivacy;
import io.github.itskorayyt.privacy.PrivacyLog;
import io.github.itskorayyt.privacy.network.SignPrivacy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractSignEditScreen.class)
abstract class SignEditorMixin {
    @WrapOperation(method = "<init>(Lnet/minecraft/world/level/block/entity/SignBlockEntity;Lnet/minecraft/world/level/block/entity/SignTextSlot;ZLnet/minecraft/network/chat/Component;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;"))
    private String privacyEditText(Component line, Operation<String> original) {
        var config = ClientPrivacy.settings();
        var client = Minecraft.getInstance();
        var listener = config.debugLogging() ? client.getConnection() : null;
        if (listener != null) {
            PrivacyLog.trace(listener.getConnection(), PrivacyLog.Event.SIGN_EDITOR_PREFILLED);
        }
        if (config.enabled() && config.protectTranslationProbes() && !client.hasSingleplayerServer()) {
            var language = SignPrivacy.vanillaLanguage(client.getVanillaPackResources(), client.getLanguageManager().getSelected());
            String text = SignPrivacy.editorLine(line, language);
            if (listener != null && SignPrivacy.isStructured(line)) {
                PrivacyLog.trace(listener.getConnection(), PrivacyLog.Event.SIGN_PREFILL_SANITIZED);
            }
            return text;
        }
        return original.call(line);
    }
}

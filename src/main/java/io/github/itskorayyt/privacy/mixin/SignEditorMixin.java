package io.github.itskorayyt.privacy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.itskorayyt.privacy.ClientPrivacy;
import io.github.itskorayyt.privacy.network.SignPrivacy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractSignEditScreen.class)
abstract class SignEditorMixin {
    @WrapOperation(method = "<init>(Lnet/minecraft/world/level/block/entity/SignBlockEntity;Lnet/minecraft/world/level/block/entity/SignTextSlot;ZLnet/minecraft/network/chat/Component;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;"))
    private String privacyEditText(Component line, Operation<String> original) {
        var config = ClientPrivacy.settings();
        var client = Minecraft.getInstance();
        if (config.enabled() && config.protectTranslationProbes() && !client.hasSingleplayerServer()
                && Language.DEFAULT.equals(client.getLanguageManager().getSelected())) {
            var replacement = SignPrivacy.replacement(line);
            if (replacement != null && client.getResourcePackRepository().getSelectedPacks().stream()
                    .noneMatch(pack -> pack.getPackSource() == PackSource.SERVER || pack.getId().startsWith("file/"))) {
                return replacement;
            }
        }
        return original.call(line);
    }
}

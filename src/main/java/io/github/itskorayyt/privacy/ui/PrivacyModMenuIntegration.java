package io.github.itskorayyt.privacy.ui;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class PrivacyModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<PrivacyOptionsScreen> getModConfigScreenFactory() {
        return PrivacyOptionsScreen::new;
    }
}

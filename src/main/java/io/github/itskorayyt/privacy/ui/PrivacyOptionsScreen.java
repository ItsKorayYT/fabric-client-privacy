package io.github.itskorayyt.privacy.ui;

import io.github.itskorayyt.privacy.ClientPrivacy;
import io.github.itskorayyt.privacy.PrivacyLog;
import io.github.itskorayyt.privacy.config.Settings;
import java.io.IOException;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class PrivacyOptionsScreen extends Screen {
    private static final String KEY = "config.fabric_client_privacy.";
    private static final int GAP = 6;
    private static final int BUTTON_HEIGHT = 20;

    private final Screen parent;
    private Settings draft = ClientPrivacy.settings();
    private boolean saveFailed;
    private CycleButton<Boolean> enabled;
    private CycleButton<Boolean> brand;
    private CycleButton<Boolean> channels;
    private CycleButton<Boolean> packs;
    private CycleButton<Boolean> signs;
    private CycleButton<Boolean> payloads;
    private CycleButton<Boolean> debug;

    public PrivacyOptionsScreen(Screen parent) {
        super(Component.translatable(KEY + "title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(358, width - 20);
        int optionWidth = (contentWidth - GAP) / 2;
        int left = (width - contentWidth) / 2;
        int right = left + optionWidth + GAP;
        int top = Math.max(30, (height - 142) / 2);
        int row = BUTTON_HEIGHT + GAP;

        enabled = toggle("enabled", draft.enabled(), (width - optionWidth) / 2, top, optionWidth);
        brand = toggle("brand", draft.spoofClientBrand(), left, top + row, optionWidth);
        channels = toggle("channels", draft.filterModChannels(), right, top + row, optionWidth);
        packs = toggle("packs", draft.filterKnownPacks(), left, top + row * 2, optionWidth);
        signs = toggle("signs", draft.protectTranslationProbes(), right, top + row * 2, optionWidth);
        payloads = toggle("payloads", draft.blockUnapprovedCustomPayloads(), left, top + row * 3, optionWidth);
        debug = toggle("debug", draft.debugLogging(), right, top + row * 3, optionWidth);
        addRenderableWidget(Button.builder(Component.translatable(KEY + "allowlist.button", draft.allowedChannels().size()),
                button -> minecraft.gui.setScreen(new AllowedChannelsScreen(this, draft.allowedChannels(), this::updateDraft)))
                .bounds(left, top + row * 4, contentWidth, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable(KEY + "allowlist.tooltip"))).build());

        int footerWidth = (contentWidth - GAP * 2) / 3;
        int bottom = height - 28;
        addRenderableWidget(Button.builder(Component.translatable(KEY + "defaults"), button -> {
            draft = Settings.DEFAULT;
            saveFailed = false;
            rebuildWidgets();
        }).bounds(left, bottom, footerWidth, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds((width - footerWidth) / 2, bottom, footerWidth, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable(KEY + "save"), button -> save())
                .bounds(left + contentWidth - footerWidth, bottom, footerWidth, BUTTON_HEIGHT).build());
    }

    private CycleButton<Boolean> toggle(String name, boolean value, int x, int y, int width) {
        return addRenderableWidget(CycleButton.onOffBuilder(value)
                .withTooltip(current -> Tooltip.create(Component.translatable(KEY + name + ".tooltip")))
                .create(x, y, width, BUTTON_HEIGHT, Component.translatable(KEY + name),
                        (button, current) -> updateDraft(draft.allowedChannels())));
    }

    private void updateDraft(Set<String> allowed) {
        draft = new Settings(enabled.getValue(), brand.getValue(), channels.getValue(), packs.getValue(),
                signs.getValue(), payloads.getValue(), debug.getValue(), allowed);
        saveFailed = false;
    }

    private void save() {
        try {
            ClientPrivacy.saveSettings(draft);
            onClose();
        } catch (IOException ex) {
            PrivacyLog.configSaveFailed(ex);
            saveFailed = true;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 16, 0xFFFFFFFF);
        Component status = Component.translatable(KEY + (saveFailed ? "save_failed" : "rejoin"));
        graphics.centeredText(font, status, width / 2, height - 43, saveFailed ? 0xFFFF7777 : 0xFFBBBBBB);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}

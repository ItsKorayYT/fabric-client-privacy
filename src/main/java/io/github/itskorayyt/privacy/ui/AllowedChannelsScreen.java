package io.github.itskorayyt.privacy.ui;

import io.github.itskorayyt.privacy.config.ConfigFile;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

final class AllowedChannelsScreen extends Screen {
    private static final String KEY = "config.fabric_client_privacy.allowlist.";

    private final Screen parent;
    private final Consumer<Set<String>> update;
    private String text;
    private int invalidLine;

    AllowedChannelsScreen(Screen parent, Set<String> allowed, Consumer<Set<String>> update) {
        super(Component.translatable(KEY + "title"));
        this.parent = parent;
        this.update = update;
        text = String.join("\n", allowed.stream().sorted().toList());
    }

    @Override
    protected void init() {
        int editorWidth = Math.min(358, width - 20);
        int left = (width - editorWidth) / 2;
        var editor = MultiLineEditBox.builder()
                .setX(left).setY(46).setShowDecorations(false).setPlaceholder(Component.translatable(KEY + "placeholder"))
                .build(font, editorWidth, Math.max(16, height - 105), title);
        editor.setCharacterLimit(ConfigFile.MAX_FILE_BYTES);
        editor.setValue(text);
        editor.setValueListener(value -> {
            text = value;
            invalidLine = 0;
        });
        addRenderableWidget(editor);

        int buttonWidth = (editorWidth - 6) / 2;
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(left, height - 28, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> apply())
                .bounds(left + editorWidth - buttonWidth, height - 28, buttonWidth, 20).build());
    }

    private void apply() {
        var channels = new LinkedHashSet<String>();
        var lines = text.lines().iterator();
        for (int number = 1; lines.hasNext(); number++) {
            var channel = lines.next().strip();
            if (channel.isEmpty()) {
                continue;
            }
            if (!ConfigFile.isChannelId(channel)) {
                invalidLine = number;
                return;
            }
            channels.add(channel);
        }
        update.accept(channels);
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 16, 0xFFFFFFFF);
        graphics.centeredText(font, Component.translatable(KEY + "line_hint"), width / 2, 32, 0xFFBBBBBB);
        if (invalidLine > 0) {
            graphics.centeredText(font, Component.translatable(KEY + "invalid", invalidLine),
                    width / 2, height - 44, 0xFFFF7777);
        }
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}

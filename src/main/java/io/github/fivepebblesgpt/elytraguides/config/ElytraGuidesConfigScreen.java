package io.github.fivepebblesgpt.elytraguides.config;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.fivepebblesgpt.elytraguides.ElytraGuidesClient;
import io.wispforest.owo.config.ui.ConfigScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public final class ElytraGuidesConfigScreen extends ConfigScreen {
    private ButtonComponent toggleKeyButton;
    private boolean waitingForToggleKey;

    public ElytraGuidesConfigScreen(@Nullable Screen parent) {
        super(DEFAULT_MODEL_ID, ElytraGuidesClient.CONFIG, parent);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        super.build(rootComponent);

        FlowLayout optionPanel = rootComponent.childById(FlowLayout.class, "option-panel");
        if (optionPanel == null) {
            return;
        }

        FlowLayout row = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.fixed(28));
        row.padding(Insets.of(5));
        row.verticalAlignment(VerticalAlignment.CENTER);

        row.child(UIComponents.label(Component.translatable("text.config.elytraguides.option.toggleKey")));

        FlowLayout controls = UIContainers.horizontalFlow(Sizing.content(), Sizing.fill(100));
        controls.horizontalAlignment(HorizontalAlignment.RIGHT);
        controls.verticalAlignment(VerticalAlignment.CENTER);

        toggleKeyButton = UIComponents.button(currentToggleKeyMessage(), button -> {
            waitingForToggleKey = true;
            button.setMessage(Component.translatable("text.config.elytraguides.option.toggleKey.listening"));
        });
        toggleKeyButton.horizontalSizing(Sizing.fixed(100));

        controls.child(toggleKeyButton);
        controls.child(UIComponents.button(
                Component.translatable("text.config.elytraguides.option.toggleKey.reset"),
                button -> bindToggleKey(ElytraGuidesClient.toggleFunctionalityKey().getDefaultKey())
        ).horizontalSizing(Sizing.fixed(55)).margins(Insets.left(4)));

        row.child(controls);
        optionPanel.child(0, row);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (!waitingForToggleKey) {
            return super.keyPressed(input);
        }

        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            waitingForToggleKey = false;
            refreshToggleKeyButton();
            return true;
        }

        bindToggleKey(InputConstants.getKey(input));
        return true;
    }

    private void bindToggleKey(InputConstants.Key key) {
        KeyMapping mapping = ElytraGuidesClient.toggleFunctionalityKey();
        mapping.setKey(key);
        KeyMapping.resetMapping();
        this.minecraft.options.save();
        waitingForToggleKey = false;
        refreshToggleKeyButton();
    }

    private Component currentToggleKeyMessage() {
        return ElytraGuidesClient.toggleFunctionalityKey().getTranslatedKeyMessage();
    }

    private void refreshToggleKeyButton() {
        if (toggleKeyButton != null) {
            toggleKeyButton.setMessage(currentToggleKeyMessage());
        }
    }
}

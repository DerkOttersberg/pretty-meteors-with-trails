package com.derko.prettymeteors.client;

import com.derko.prettymeteors.PrettyMeteorsConfig;
import com.derko.prettymeteors.PrettyMeteorsConfig.Settings;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PrettyMeteorsConfigScreen extends Screen {
    private final Screen parent;
    private boolean nightEventsEnabled;
    private Button enabledButton;
    private EditBox starsField;
    private EditBox noneChanceField;
    private EditBox smallChanceField;
    private EditBox mediumChanceField;
    private EditBox largeChanceField;
    private Component errorText = Component.empty();

    public PrettyMeteorsConfigScreen(Screen parent) {
        super(Component.literal("Pretty Meteors Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Settings settings = PrettyMeteorsConfig.snapshot();
        this.nightEventsEnabled = settings.nightEventsEnabled();
        int centerX = this.width / 2;

        this.enabledButton = this.addRenderableWidget(Button.builder(enabledText(), button -> {
            this.nightEventsEnabled = !this.nightEventsEnabled;
            button.setMessage(enabledText());
        }).bounds(centerX + 10, 58, 145, 20).build());

        this.starsField = addField(centerX + 10, 88, 74, Integer.toString(settings.nightStarCount()));
        this.noneChanceField = addField(centerX - 60, 120, 58, Integer.toString(settings.nightNoneChance()));
        this.smallChanceField = addField(centerX + 98, 120, 58, Integer.toString(settings.nightSmallChance()));
        this.mediumChanceField = addField(centerX - 60, 150, 58, Integer.toString(settings.nightMediumChance()));
        this.largeChanceField = addField(centerX + 98, 150, 58, Integer.toString(settings.nightLargeChance()));

        int buttonY = Math.min(this.height - 28, 204);
        this.addRenderableWidget(Button.builder(Component.literal("Reset Defaults"), button -> resetDefaults())
            .bounds(centerX - 155, buttonY, 100, 20)
            .build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
            .bounds(centerX - 50, buttonY, 100, 20)
            .build());
        this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> saveAndClose())
            .bounds(centerX + 55, buttonY, 100, 20)
            .build());
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parent);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xCC101014);
        int centerX = this.width / 2;
        graphics.centeredText(this.font, this.title, centerX, 16, 0xFFFFFF);
        graphics.centeredText(
            this.font,
            Component.literal("Night settings affect newly scheduled events"),
            centerX,
            33,
            0xAFAFAF
        );
        graphics.text(this.font, Component.literal("Nightly Events"), centerX - 155, 64, 0xFFFFFF, true);
        graphics.text(this.font, Component.literal("Shooting Stars per Night (0-20)"), centerX - 155, 94, 0xFFFFFF, true);
        graphics.text(this.font, Component.literal("No Shower %"), centerX - 155, 126, 0xFFFFFF, true);
        graphics.text(this.font, Component.literal("Small %"), centerX + 10, 126, 0xFFFFFF, true);
        graphics.text(this.font, Component.literal("Medium %"), centerX - 155, 156, 0xFFFFFF, true);
        graphics.text(this.font, Component.literal("Large %"), centerX + 10, 156, 0xFFFFFF, true);
        graphics.centeredText(this.font, Component.literal(chanceSummary()), centerX, 179, chanceTotal() == 100 ? 0x78E08F : 0xFF6B6B);
        if (!this.errorText.getString().isEmpty()) {
            graphics.centeredText(this.font, this.errorText, centerX, 191, 0xFF6B6B);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private EditBox addField(int x, int y, int width, String value) {
        EditBox field = new EditBox(this.font, x, y, width, 20, Component.empty());
        field.setValue(value);
        this.addRenderableWidget(field);
        return field;
    }

    private void resetDefaults() {
        Settings defaults = Settings.defaults();
        this.nightEventsEnabled = defaults.nightEventsEnabled();
        this.enabledButton.setMessage(enabledText());
        this.starsField.setValue(Integer.toString(defaults.nightStarCount()));
        this.noneChanceField.setValue(Integer.toString(defaults.nightNoneChance()));
        this.smallChanceField.setValue(Integer.toString(defaults.nightSmallChance()));
        this.mediumChanceField.setValue(Integer.toString(defaults.nightMediumChance()));
        this.largeChanceField.setValue(Integer.toString(defaults.nightLargeChance()));
        this.errorText = Component.empty();
    }

    private void saveAndClose() {
        try {
            Settings settings = new Settings(
                this.nightEventsEnabled,
                parseInt(this.starsField, 0, 20, "Shooting stars"),
                parseInt(this.noneChanceField, 0, 100, "No-shower chance"),
                parseInt(this.smallChanceField, 0, 100, "Small chance"),
                parseInt(this.mediumChanceField, 0, 100, "Medium chance"),
                parseInt(this.largeChanceField, 0, 100, "Large chance")
            );
            if (settings.chanceTotal() != 100) {
                throw new IllegalArgumentException("The four shower chances must total 100%.");
            }
            PrettyMeteorsConfig.update(settings);
            onClose();
        } catch (IllegalArgumentException exception) {
            this.errorText = Component.literal(exception.getMessage());
        }
    }

    private int chanceTotal() {
        try {
            return Integer.parseInt(this.noneChanceField.getValue().trim())
                + Integer.parseInt(this.smallChanceField.getValue().trim())
                + Integer.parseInt(this.mediumChanceField.getValue().trim())
                + Integer.parseInt(this.largeChanceField.getValue().trim());
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private String chanceSummary() {
        int total = chanceTotal();
        return total < 0 ? "Shower chance total: invalid" : "Shower chance total: " + total + "% (must be 100%)";
    }

    private static int parseInt(EditBox field, int min, int max, String label) {
        try {
            int value = Integer.parseInt(field.getValue().trim());
            if (value < min || value > max) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " must be a whole number from " + min + " to " + max + ".");
        }
    }

    private Component enabledText() {
        return Component.literal(this.nightEventsEnabled ? "Enabled" : "Disabled");
    }
}

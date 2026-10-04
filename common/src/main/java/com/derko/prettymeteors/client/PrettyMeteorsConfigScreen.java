package com.derko.prettymeteors.client;

import com.derko.prettymeteors.PrettyMeteorsConfig;
import com.derko.prettymeteors.PrettyMeteorsConfig.Settings;
import io.github.derkottersberg.prettymeteors.internal.client.SettingsScreen;
import net.minecraft.client.gui.screens.Screen;

public final class PrettyMeteorsConfigScreen extends SettingsScreen {
    private boolean enabled;
    private String stars, none, small, medium, large;

    public PrettyMeteorsConfigScreen(Screen parent) {
        super(parent, "Pretty Meteors Settings", "New nightly events in singleplayer. Multiplayer uses server settings.");
        setDraft(PrettyMeteorsConfig.snapshot());
    }

    private void setDraft(Settings settings) {
        this.enabled = settings.nightEventsEnabled();
        this.stars = Integer.toString(settings.nightStarCount());
        this.none = Integer.toString(settings.nightNoneChance());
        this.small = Integer.toString(settings.nightSmallChance());
        this.medium = Integer.toString(settings.nightMediumChance());
        this.large = Integer.toString(settings.nightLargeChance());
    }

    @Override
    protected void buildSettings() {
        toggleSetting("Nightly events", "Automatic night events", "Schedule shooting stars and a possible shower each night. Manual /prettymeteors commands still work.",
            this.enabled, v -> this.enabled = v);
        textSetting("Nightly events", "Single stars per night", "0-20 individual shooting stars, in addition to any meteor shower. Set 0 to disable these stars.",
            this.stars, v -> this.stars = v);
        textSetting("Shower probability", "No shower (%)", "0-100%. Chance that a night has no shower. The four probabilities must add up to 100%.",
            this.none, v -> this.none = v);
        textSetting("Shower probability", "Small shower (%)", "0-100%. Chance of a small shower that night. This is probability, not meteor size.",
            this.small, v -> this.small = v);
        textSetting("Shower probability", "Medium shower (%)", "0-100%. Chance of a medium shower that night. Changes apply when the next night is scheduled.",
            this.medium, v -> this.medium = v);
        textSetting("Shower probability", "Large shower (%)", "0-100%. Chance of a large shower that night. Set 100 here and 0 elsewhere for nightly large showers.",
            this.large, v -> this.large = v);
    }

    @Override
    protected void resetDraft() { setDraft(Settings.defaults()); }

    @Override
    protected void saveDraft() {
        Settings settings = new Settings(this.enabled,
            integer(this.stars, 0, 20, "Single stars per night"),
            integer(this.none, 0, 100, "No shower (%)"),
            integer(this.small, 0, 100, "Small shower (%)"),
            integer(this.medium, 0, 100, "Medium shower (%)"),
            integer(this.large, 0, 100, "Large shower (%)"));
        if (settings.chanceTotal() != 100) throw new IllegalArgumentException("Shower probabilities must total 100%. Currently " + settings.chanceTotal() + "%.");
        PrettyMeteorsConfig.update(settings);
    }

    @Override
    protected boolean validDraft() {
        try {
            return integer(this.none, 0, 100, "No shower") + integer(this.small, 0, 100, "Small")
                + integer(this.medium, 0, 100, "Medium") + integer(this.large, 0, 100, "Large") == 100;
        } catch (IllegalArgumentException ignored) { return false; }
    }

    @Override
    protected String summary() {
        try {
            int total = Integer.parseInt(this.none) + Integer.parseInt(this.small)
                + Integer.parseInt(this.medium) + Integer.parseInt(this.large);
            return "Shower probabilities: " + total + "% / 100%. Save applies all pages.";
        } catch (NumberFormatException ignored) {
            return "Enter whole-number probabilities. Their total must be 100%.";
        }
    }
}

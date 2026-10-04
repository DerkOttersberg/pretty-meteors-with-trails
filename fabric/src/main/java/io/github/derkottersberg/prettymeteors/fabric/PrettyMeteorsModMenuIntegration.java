package io.github.derkottersberg.prettymeteors.fabric;

import com.derko.prettymeteors.client.PrettyMeteorsConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class PrettyMeteorsModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return PrettyMeteorsConfigScreen::new;
    }
}

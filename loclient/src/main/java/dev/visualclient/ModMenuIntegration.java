package dev.visualclient;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Adds a config button for VisualClient in the Mod Menu mod list (if Mod Menu is installed). */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new SettingsScreen(parent);
    }
}

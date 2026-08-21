package eu.stefanbraun612.smartautoattack.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import eu.stefanbraun612.smartautoattack.client.config.SmartAutoAttackConfigScreen;

public class SmartAutoAttackModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return SmartAutoAttackConfigScreen::build;
	}
}

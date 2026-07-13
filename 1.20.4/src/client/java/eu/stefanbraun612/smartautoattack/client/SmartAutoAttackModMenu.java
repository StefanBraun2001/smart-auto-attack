package eu.stefanbraun612.smartautoattack.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import eu.stefanbraun612.smartautoattack.client.config.SmartAutoAttackConfig;
import me.shedaniel.autoconfig.AutoConfig;

public class SmartAutoAttackModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> AutoConfig.getConfigScreen(SmartAutoAttackConfig.class, parent).get();
	}
}

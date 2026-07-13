package eu.stefanbraun612.smartautoattack.client;

import eu.stefanbraun612.smartautoattack.client.config.SmartAutoAttackConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SmartAutoAttackClient implements ClientModInitializer {
	public static final String MOD_ID = "smartautoattack";

	private static KeyBinding toggleKey;
	private static boolean enabled = false;

	@Override
	public void onInitializeClient() {
		AutoConfig.register(SmartAutoAttackConfig.class, GsonConfigSerializer::new);

		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.smartautoattack.toggle",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_J,
				"key.categories.smartautoattack"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.wasPressed()) {
				setEnabled(!enabled, client);
			}
			if (enabled) {
				AutoEatLogic.tick(client);
				// Don't attack while mid-chew: attacking cancels the vanilla eat-use
				// action, which would otherwise cause an endless "switch to food,
				// get interrupted, switch back" loop without ever finishing a bite.
				if (!AutoEatLogic.isEating()) {
					AutoAttackLogic.tick(client);
				}
			}
		});
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean value, MinecraftClient client) {
		enabled = value;
		AutoAttackLogic.reset();
		SmartAutoAttackConfig config = AutoConfig.getConfigHolder(SmartAutoAttackConfig.class).getConfig();
		FeedbackUtil.send(client, config, value ? "Smart Auto Attack: ON" : "Smart Auto Attack: OFF");
	}
}

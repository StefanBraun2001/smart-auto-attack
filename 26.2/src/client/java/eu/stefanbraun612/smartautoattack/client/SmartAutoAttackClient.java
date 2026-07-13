package eu.stefanbraun612.smartautoattack.client;

import com.mojang.blaze3d.platform.InputConstants;
import eu.stefanbraun612.smartautoattack.client.config.SmartAutoAttackConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class SmartAutoAttackClient implements ClientModInitializer {
	public static final String MOD_ID = "smartautoattack";

	private static KeyMapping toggleKey;
	private static boolean enabled = false;

	@Override
	public void onInitializeClient() {
		AutoConfig.register(SmartAutoAttackConfig.class, GsonConfigSerializer::new);

		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath(MOD_ID, "main"));

		toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.smartautoattack.toggle",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_J,
				category
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.consumeClick()) {
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

	public static void setEnabled(boolean value, Minecraft client) {
		enabled = value;
		AutoAttackLogic.reset();
		SmartAutoAttackConfig config = AutoConfig.getConfigHolder(SmartAutoAttackConfig.class).getConfig();
		FeedbackUtil.send(client, config, value ? "Smart Auto Attack: ON" : "Smart Auto Attack: OFF");
	}
}

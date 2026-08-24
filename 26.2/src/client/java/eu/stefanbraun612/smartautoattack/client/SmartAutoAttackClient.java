package eu.stefanbraun612.smartautoattack.client;

import com.mojang.blaze3d.platform.InputConstants;
import eu.stefanbraun612.smartautoattack.client.config.SmartAutoAttackConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class SmartAutoAttackClient implements ClientModInitializer {
	public static final String MOD_ID = "smartautoattack";

	// Fully-qualified name of the (optional, separate) Smart Auto Reconnect mod's
	// signal class - checked via reflection so this mod builds and runs fine whether
	// or not that mod is installed, with no compile-time dependency between them.
	private static final String RECONNECT_SIGNAL_CLASS = "eu.stefanbraun612.smartautoreconnect.client.ReconnectSignal";
	private static final long RECONNECT_SIGNAL_WINDOW_MILLIS = 30000;

	private static KeyMapping toggleKey;
	private static boolean enabled = false;
	// Sat for a few ticks after (re)joining a world before AutoEat/AutoAttack are
	// allowed to act, so nothing swings/eats against a still-loading world state.
	private static int joinSettleTicksLeft = 0;

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

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (!enabled) {
				return;
			}
			if (wasScriptedReconnect()) {
				// Smart Auto Reconnect just handled this - always resume, no matter
				// what "resume after manual reconnect" is set to.
				joinSettleTicksLeft = 60;
				return;
			}
			SmartAutoAttackConfig config = AutoConfig.getConfigHolder(SmartAutoAttackConfig.class).getConfig();
			if (config.resumeAfterManualReconnect) {
				joinSettleTicksLeft = 60;
			} else {
				setEnabled(false, client);
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.consumeClick()) {
				setEnabled(!enabled, client);
			}
			if (joinSettleTicksLeft > 0) {
				joinSettleTicksLeft--;
				return;
			}
			if (enabled) {
				AutoEatLogic.tick(client, AutoAttackLogic.isCriticalHealthPauseActive());
				// Don't attack while mid-chew: attacking cancels the vanilla eat-use
				// action, which would otherwise cause an endless "switch to food,
				// get interrupted, switch back" loop without ever finishing a bite.
				if (!AutoEatLogic.isEating()) {
					AutoAttackLogic.tick(client);
				}
			}
		});
	}

	private static boolean wasScriptedReconnect() {
		try {
			Class<?> signalClass = Class.forName(RECONNECT_SIGNAL_CLASS);
			long timestamp = (long) signalClass.getField("lastAutoReconnectAtMillis").get(null);
			return timestamp > 0 && (System.currentTimeMillis() - timestamp) < RECONNECT_SIGNAL_WINDOW_MILLIS;
		} catch (Throwable t) {
			return false; // Smart Auto Reconnect not installed, or any reflection issue - treat as manual
		}
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean value, Minecraft client) {
		enabled = value;
		AutoAttackLogic.reset();
		AutoEatLogic.reset();
		SmartAutoAttackConfig config = AutoConfig.getConfigHolder(SmartAutoAttackConfig.class).getConfig();
		FeedbackUtil.send(client, config, value ? "Smart Auto Attack: ON" : "Smart Auto Attack: OFF");
	}
}

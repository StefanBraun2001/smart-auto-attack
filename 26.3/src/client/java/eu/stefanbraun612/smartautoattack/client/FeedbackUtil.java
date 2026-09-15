package eu.stefanbraun612.smartautoattack.client;

import eu.stefanbraun612.smartautoattack.client.config.SmartAutoAttackConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class FeedbackUtil {
	public static void send(Minecraft client, SmartAutoAttackConfig config, String message) {
		if (client.player == null || config.feedbackMode == SmartAutoAttackConfig.FeedbackMode.SILENT) {
			return;
		}
		if (config.feedbackMode == SmartAutoAttackConfig.FeedbackMode.ACTION_BAR) {
			client.gui.hud.setOverlayMessage(Component.literal(message), false);
		} else {
			client.player.sendSystemMessage(Component.literal(message));
		}
	}
}

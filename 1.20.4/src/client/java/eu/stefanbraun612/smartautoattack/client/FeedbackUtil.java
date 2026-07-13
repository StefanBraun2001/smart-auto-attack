package eu.stefanbraun612.smartautoattack.client;

import eu.stefanbraun612.smartautoattack.client.config.SmartAutoAttackConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class FeedbackUtil {
	public static void send(MinecraftClient client, SmartAutoAttackConfig config, String message) {
		if (client.player == null || config.feedbackMode == SmartAutoAttackConfig.FeedbackMode.SILENT) {
			return;
		}
		boolean actionBar = config.feedbackMode == SmartAutoAttackConfig.FeedbackMode.ACTION_BAR;
		client.player.sendMessage(Text.of(message), actionBar);
	}
}

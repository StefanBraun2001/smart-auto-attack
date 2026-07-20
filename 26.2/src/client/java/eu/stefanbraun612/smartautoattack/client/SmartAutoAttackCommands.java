package eu.stefanbraun612.smartautoattack.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import eu.stefanbraun612.smartautoattack.client.config.SmartAutoAttackConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class SmartAutoAttackCommands {
	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
				ClientCommands.literal("smartautoattack")
						.then(ClientCommands.literal("preset")
								.then(ClientCommands.literal("list")
										.executes(ctx -> listPresets(ctx.getSource())))
								.then(ClientCommands.literal("apply")
										.then(ClientCommands.<String>argument("name", StringArgumentType.word())
												.executes(ctx -> applyPreset(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
								.then(ClientCommands.literal("save")
										.then(ClientCommands.<String>argument("name", StringArgumentType.word())
												.executes(ctx -> savePreset(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
								.then(ClientCommands.literal("delete")
										.then(ClientCommands.<String>argument("name", StringArgumentType.word())
												.executes(ctx -> deletePreset(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))))));
	}

	private static int listPresets(FabricClientCommandSource source) {
		Map<String, AttackPreset> presets = PresetManager.all();
		if (presets.isEmpty()) {
			source.sendFeedback(Component.literal("Smart Auto Attack: no presets saved."));
			return 1;
		}
		source.sendFeedback(Component.literal("Smart Auto Attack presets: " + String.join(", ", presets.keySet())));
		return 1;
	}

	private static int applyPreset(FabricClientCommandSource source, String name) {
		AttackPreset preset = PresetManager.get(name);
		if (preset == null) {
			source.sendError(Component.literal("Smart Auto Attack: no preset named '" + name + "'."));
			return 0;
		}
		SmartAutoAttackConfig config = AutoConfig.getConfigHolder(SmartAutoAttackConfig.class).getConfig();
		preset.applyTo(config);
		AutoConfig.getConfigHolder(SmartAutoAttackConfig.class).save();
		source.sendFeedback(Component.literal("Smart Auto Attack: applied preset '" + name + "'."));
		return 1;
	}

	private static int savePreset(FabricClientCommandSource source, String name) {
		SmartAutoAttackConfig config = AutoConfig.getConfigHolder(SmartAutoAttackConfig.class).getConfig();
		PresetManager.save(name, AttackPreset.fromConfig(config));
		source.sendFeedback(Component.literal("Smart Auto Attack: saved current settings as preset '" + name + "'."));
		return 1;
	}

	private static int deletePreset(FabricClientCommandSource source, String name) {
		if (PresetManager.delete(name)) {
			source.sendFeedback(Component.literal("Smart Auto Attack: deleted preset '" + name + "'."));
			return 1;
		}
		source.sendError(Component.literal("Smart Auto Attack: no preset named '" + name + "'."));
		return 0;
	}
}

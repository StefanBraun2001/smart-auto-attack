package eu.stefanbraun612.smartautoattack.client;

import eu.stefanbraun612.smartautoattack.client.config.SmartAutoAttackConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.concurrent.ThreadLocalRandom;

public class AutoAttackLogic {
	private static int hitCount = 0;

	// Ticks the attack has actually been "live" for - used for the max-duration limit.
	// Only frozen when freezeDurationDuringDay pauses it (see tick()); otherwise it
	// behaves just like a plain elapsed-time counter since being enabled.
	private static long elapsedActiveTicks = 0;

	// Only used by FIXED_INTERVAL / RANDOM_INTERVAL cadence. Counts down every tick
	// regardless of target presence, and is only ever re-armed right after a swing -
	// if it reaches 0 with no valid target it just stays "ready" until one appears.
	private static long ticksUntilNextAttack = 0;

	public static void reset() {
		hitCount = 0;
		elapsedActiveTicks = 0;
		ticksUntilNextAttack = 0; // ready to attack immediately once enabled
	}

	public static void tick(Minecraft client) {
		SmartAutoAttackConfig config = AutoConfig.getConfigHolder(SmartAutoAttackConfig.class).getConfig();
		Player player = client.player;
		ClientLevel world = client.level;
		if (player == null || world == null || client.gameMode == null) {
			return;
		}

		boolean isDaytime = false;
		if (config.nightOnly) {
			long timeOfDay = world.getOverworldClockTime() % 24000;
			isDaytime = timeOfDay < 13000 || timeOfDay > 23000;
		}

		boolean skipNightGate = config.nightOnly && config.skipNightCheckInDimensionsWithoutCycle
				&& !dimensionHasDayNightCycle(world);
		boolean nightGatePasses = !config.nightOnly || skipNightGate || !isDaytime;

		boolean pausedForDuration = config.freezeDurationDuringDay && config.nightOnly && isDaytime && !skipNightGate;
		if (!pausedForDuration) {
			elapsedActiveTicks++;
		}

		if (!checkDurability(player, config)) {
			stop(client, config, "Smart Auto Attack: stopped (low durability)");
			return;
		}

		if (!passesHungerSafety(player, config)) {
			stop(client, config, "Smart Auto Attack: stopped (hunger too low)");
			return;
		}

		long maxDurationTicks = DurationParser.parseTicks(config.maxDuration);
		if (maxDurationTicks > 0 && elapsedActiveTicks >= maxDurationTicks) {
			stop(client, config, "Smart Auto Attack: stopped (time limit reached)");
			return;
		}

		if (!nightGatePasses) {
			return;
		}

		Entity target = resolveTarget(client, config);

		if (config.attackCadenceMode == SmartAutoAttackConfig.AttackCadenceMode.DEFAULT) {
			if (target == null && config.requireTargetDetected) {
				return;
			}
			if (player.getAttackStrengthScale(0f) < 1.0f) {
				return;
			}
			performAttack(client, player, config, target);
		} else {
			if (ticksUntilNextAttack > 0) {
				ticksUntilNextAttack--;
				return;
			}
			if (target == null && config.requireTargetDetected) {
				return; // stays at 0 ("ready"); attacks the instant a valid target reappears
			}
			if (config.alwaysFullyCharge && player.getAttackStrengthScale(0f) < 1.0f) {
				return; // interval elapsed but weapon isn't fully charged yet - wait, don't consume it
			}
			performAttack(client, player, config, target);
			ticksUntilNextAttack = nextIntervalTicks(config);
		}
	}

	// Nether and End have no day/night cycle (their clock is fixed/meaningless even
	// though the underlying world-time counter keeps advancing globally).
	private static boolean dimensionHasDayNightCycle(Level world) {
		return world.dimension() != Level.NETHER && world.dimension() != Level.END;
	}

	private static void performAttack(Minecraft client, Player player, SmartAutoAttackConfig config, Entity target) {
		if (target != null) {
			client.gameMode.attack(player, target);
		}
		player.swing(InteractionHand.MAIN_HAND);

		hitCount++;
		if (config.maxHits > 0 && hitCount >= config.maxHits) {
			stop(client, config, "Smart Auto Attack: stopped (hit limit reached)");
		}
	}

	private static void stop(Minecraft client, SmartAutoAttackConfig config, String message) {
		SmartAutoAttackClient.setEnabled(false, client);
		FeedbackUtil.send(client, config, message);
		playAutoStopSound(client, config);
	}

	// Only reached from auto-stop paths (this method) - never from the player
	// manually pressing the toggle key, since that goes straight to setEnabled()
	// without a sound, on the assumption the player already knows they stopped it.
	private static void playAutoStopSound(Minecraft client, SmartAutoAttackConfig config) {
		if (!config.playSoundOnAutoStop) {
			return;
		}
		Identifier id = Identifier.tryParse(config.autoStopSound);
		if (id == null) {
			return;
		}
		SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getValue(id);
		if (sound == null) {
			return;
		}
		client.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0f));
	}

	private static long nextIntervalTicks(SmartAutoAttackConfig config) {
		double unitTicks = config.intervalUnit == SmartAutoAttackConfig.IntervalUnit.SECONDS ? 20.0 : 1.0;
		double ticks;
		if (config.attackCadenceMode == SmartAutoAttackConfig.AttackCadenceMode.FIXED_INTERVAL) {
			ticks = config.fixedIntervalValue * unitTicks;
		} else {
			double min = Math.min(config.randomIntervalMin, config.randomIntervalMax) * unitTicks;
			double max = Math.max(config.randomIntervalMin, config.randomIntervalMax) * unitTicks;
			ticks = min + ThreadLocalRandom.current().nextDouble() * (max - min);
		}
		return Math.max(1, Math.round(ticks));
	}

	private static Entity resolveTarget(Minecraft client, SmartAutoAttackConfig config) {
		if (client.hitResult == null || client.hitResult.getType() != HitResult.Type.ENTITY) {
			return null;
		}
		Entity target = ((EntityHitResult) client.hitResult).getEntity();
		if (config.requireTargetDetected && !passesFilter(target, config)) {
			return null;
		}
		return target;
	}

	private static boolean checkDurability(Player player, SmartAutoAttackConfig config) {
		if (config.minDurability <= 0 && config.minDurabilityPercent <= 0) {
			return true;
		}
		ItemStack stack = player.getMainHandItem();
		int maxDamage = stack.getMaxDamage();
		if (maxDamage <= 0) {
			return true; // item has no durability (e.g. bare hand, unbreakable tool)
		}
		int remaining = maxDamage - stack.getDamageValue();
		// <= (not <): minDurability/minDurabilityPercent represent uses left to
		// preserve, so the guard must trip *at* the threshold, before that last
		// use is spent - otherwise the weapon consumes its final durability
		// point and breaks despite the configured floor.
		if (config.minDurability > 0 && remaining <= config.minDurability) {
			return false;
		}
		if (config.minDurabilityPercent > 0) {
			float percent = (remaining * 100f) / maxDamage;
			if (percent <= config.minDurabilityPercent) {
				return false;
			}
		}
		return true;
	}

	private static boolean passesHungerSafety(Player player, SmartAutoAttackConfig config) {
		if (!config.hungerSafetyStopEnabled) {
			return true;
		}
		return player.getFoodData().getFoodLevel() >= config.hungerSafetyStopThreshold;
	}

	private static boolean passesFilter(Entity target, SmartAutoAttackConfig config) {
		Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
		boolean listed = config.targetList.contains(id.toString());
		return switch (config.filterMode) {
			case BLACKLIST -> !listed;
			case WHITELIST -> listed;
		};
	}
}

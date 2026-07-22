package eu.stefanbraun612.smartautoattack.client;

import eu.stefanbraun612.smartautoattack.client.config.SmartAutoAttackConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Locale;
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

	// Tracks the night-only gate's state across ticks so a feedback message can fire
	// only on the transition (start paused/attacking, or flip at dawn/dusk) instead of
	// spamming every tick. Null means "not yet evaluated this run" - guarantees a
	// message fires right away even if the mod is enabled already inside the paused
	// window, so leaving night-only on by accident doesn't look like it silently
	// broke instead of behaving as configured.
	private static Boolean nightGatePassedLastTick = null;

	public static void reset() {
		hitCount = 0;
		elapsedActiveTicks = 0;
		ticksUntilNextAttack = 0; // ready to attack immediately once enabled
		nightGatePassedLastTick = null;
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
			// Matches the Creaking Heart's own awake window (Minecraft Wiki), not the
			// textbook 13000/23000 generic night thresholds - creakings specifically
			// wake up earlier and go dormant at the same point as regular night ends.
			isDaytime = timeOfDay < 12600 || timeOfDay > 23400;
		}

		boolean skipNightGate = config.nightOnly && config.skipNightCheckInDimensionsWithoutCycle
				&& !dimensionHasDayNightCycle(world);
		boolean nightGatePasses = !config.nightOnly || skipNightGate || !isDaytime;

		if (config.nightOnly) {
			if (nightGatePassedLastTick == null || nightGatePassedLastTick != nightGatePasses) {
				FeedbackUtil.send(client, config, nightGatePasses
						? "Smart Auto Attack: night has begun - attacking"
						: "Smart Auto Attack: paused (night only - waiting for night)");
			}
			nightGatePassedLastTick = nightGatePasses;
		} else {
			nightGatePassedLastTick = null; // stale state - re-evaluate cleanly if night only gets turned back on
		}

		boolean pausedForDuration = config.freezeDurationDuringDay && config.nightOnly && isDaytime && !skipNightGate;
		if (!pausedForDuration) {
			elapsedActiveTicks++;
		}

		if (!ensureUsableTool(client, player, config)) {
			stop(client, config, "Smart Auto Attack: stopped (no usable tool left)");
			return;
		}

		if (!passesHungerSafety(player, config)) {
			stop(client, config, "Smart Auto Attack: stopped (hunger too low)");
			return;
		}

		if (!passesHealthSafety(player, config)) {
			stop(client, config, "Smart Auto Attack: stopped (health too low)");
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
		// UNCENSORED BUILD ("_AP" - "attacks players"): unlike the Modrinth-distributed
		// build, this branch does NOT hardcode a player exclusion here - blacklist/
		// whitelist fully governs targeting, same as pre-A0.3H2. GitHub-only distribution;
		// never upload builds from this branch to Modrinth (Content Rules 3.3d).
		if (config.requireTargetDetected && !passesFilter(target, config)) {
			return null;
		}
		return target;
	}

	// Returns true if the main hand currently holds a weapon with enough durability to
	// keep going (rotating to another matching weapon first if "use more tools" is on
	// and the current one just dropped below the threshold). Returns false only when
	// there's nothing left usable and the mod should stop.
	private static boolean ensureUsableTool(Minecraft client, Player player, SmartAutoAttackConfig config) {
		if (hasEnoughDurability(player.getMainHandItem(), config)) {
			return true;
		}
		if (!config.useMoreTools) {
			return false;
		}

		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < 9; slot++) {
			if (slot == inventory.getSelectedSlot()) {
				continue;
			}
			ItemStack candidate = inventory.getItem(slot);
			if (candidate.isEmpty() || !matchesKeyword(candidate, config.toolKeyword)) {
				continue;
			}
			if (!hasEnoughDurability(candidate, config)) {
				continue;
			}
			selectSlot(client, slot);
			return true;
		}
		return false;
	}

	private static boolean matchesKeyword(ItemStack stack, String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return false;
		}
		String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
		return id.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
	}

	private static boolean hasEnoughDurability(ItemStack stack, SmartAutoAttackConfig config) {
		if (stack.isEmpty()) {
			// Unlike Auto Mine, punching bare-handed is a legitimate (if slow) way to
			// attack, so leave that untouched when "use more tools" is off - matches
			// pre-existing behavior. Only once that toggle is on do we treat an empty
			// main hand as "not enough" so it actively searches for a real weapon
			// instead of settling for bare fists.
			return !config.useMoreTools;
		}
		if (config.minDurability <= 0 && config.minDurabilityPercent <= 0) {
			return true;
		}
		int maxDamage = stack.getMaxDamage();
		if (maxDamage <= 0) {
			return true; // item has no durability (e.g. bare hand, unbreakable tool)
		}
		int remaining = maxDamage - stack.getDamageValue();
		// <= (not <): minDurability/minDurabilityPercent represent uses left to
		// preserve, so the guard must trip *at* the threshold, before that last
		// use is spent - otherwise the weapon consumes its final durability
		// point and breaks (or, for "use more tools", vanishes to an empty stack
		// that then falsely reads as "no durability restriction" and never rotates).
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

	// Changing Inventory.selectedSlot alone only updates the client's local view -
	// the server keeps tracking whatever slot it last heard about, so any attack
	// packets sent afterward would act on the wrong item server-side unless we also
	// send this packet, same as vanilla does on scroll/number-key input.
	private static void selectSlot(Minecraft client, int slotIndex) {
		client.player.getInventory().setSelectedSlot(slotIndex);
		if (client.getConnection() != null) {
			client.getConnection().send(new ServerboundSetCarriedItemPacket(slotIndex));
		}
	}

	private static boolean passesHungerSafety(Player player, SmartAutoAttackConfig config) {
		if (!config.hungerSafetyStopEnabled) {
			return true;
		}
		return player.getFoodData().getFoodLevel() >= config.hungerSafetyStopThreshold;
	}

	private static boolean passesHealthSafety(Player player, SmartAutoAttackConfig config) {
		if (!config.healthSafetyStopEnabled) {
			return true;
		}
		return player.getHealth() >= config.healthSafetyStopThreshold;
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

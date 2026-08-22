package eu.stefanbraun612.smartautoattack.client;

import eu.stefanbraun612.smartautoattack.client.config.SmartAutoAttackConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
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

	// Tracks the "Adjust to Creakings" override's state across ticks, same transition-only-
	// message purpose as nightGatePassedLastTick above.
	private static Boolean creakingOverrideActiveLastTick = null;

	// Throttle: alternates between an "attacking" phase and a "paused" phase for the
	// configured durations. Mutually exclusive with Night only (only engages while Night
	// only is effectively off - see tick()), which also naturally disengages it during the
	// Adjust to Creakings override (that override always forces Night only on).
	private static boolean throttleAttackingPhase = true;
	private static long throttlePhaseTicksRemaining = 0;
	// Whether the throttle mechanism itself was engaged last tick - used to (re)start on a
	// fresh attack phase the moment it becomes engaged, distinct from throttlePhasePassedLastTick
	// below (which tracks the attack/pause phase transitions for feedback messages).
	private static Boolean throttleEngagedLastTick = null;
	private static Boolean throttlePhasePassedLastTick = null;

	// Remembers the last non-empty main-hand item, for SAME_TYPE/EXACT_MATCH tool rotation.
	// Needed because once a weapon actually breaks (no durability floor set to catch it
	// first), the main hand reads as empty/air by the time the rotation search runs - by
	// then the item's own class/identity is gone, so without this there'd be nothing left
	// to compare candidates against for those two modes.
	private static ItemStack lastKnownMainHandItem = ItemStack.EMPTY;

	// Health-guard pause (only when Eat food to regenerate health is on): freezes everything
	// (including timers) once health drops below the configured threshold, force-feeds via
	// AutoEatLogic (see isCriticalHealthPauseActive()) until hunger is full and health has
	// climbed back to threshold+4, then resumes normally. Gives up and stops the mod if that
	// recovery doesn't happen within the timeout below.
	private static boolean criticalHealthPauseActive = false;
	private static int criticalHealthPauseTicks = 0;
	private static final float CRITICAL_HEALTH_RESUME_MARGIN = 4.0f; // 2 hearts above the threshold
	private static final int CRITICAL_HEALTH_PAUSE_TIMEOUT_TICKS = 900; // 45 seconds

	public static void reset() {
		hitCount = 0;
		elapsedActiveTicks = 0;
		ticksUntilNextAttack = 0; // ready to attack immediately once enabled
		nightGatePassedLastTick = null;
		creakingOverrideActiveLastTick = null;
		throttleAttackingPhase = true;
		throttlePhaseTicksRemaining = 0;
		throttleEngagedLastTick = null;
		throttlePhasePassedLastTick = null;
		lastKnownMainHandItem = ItemStack.EMPTY;
		criticalHealthPauseActive = false;
		criticalHealthPauseTicks = 0;
	}

	// Read by SmartAutoAttackClient to tell AutoEatLogic to force-feed past its normal
	// threshold while a critical-health pause is in progress.
	public static boolean isCriticalHealthPauseActive() {
		return criticalHealthPauseActive;
	}

	public static void tick(Minecraft client) {
		SmartAutoAttackConfig config = AutoConfig.getConfigHolder(SmartAutoAttackConfig.class).getConfig();
		Player player = client.player;
		ClientLevel world = client.level;
		if (player == null || world == null || client.gameMode == null) {
			return;
		}

		if (handleHealthGuard(client, player, config)) {
			return;
		}

		boolean creakingOverrideActive = config.adjustToCreakings && isCrosshairOnCreaking(client);
		if (creakingOverrideActiveLastTick == null || creakingOverrideActiveLastTick != creakingOverrideActive) {
			if (creakingOverrideActive) {
				FeedbackUtil.send(client, config,
						"Smart Auto Attack: Creaking detected - adjusting to Creaking preset (100-tick interval, night only, ignoring Nether/End day-night)");
			} else if (creakingOverrideActiveLastTick != null) {
				FeedbackUtil.send(client, config, "Smart Auto Attack: Creaking no longer detected - reverting to configured settings");
			}
		}
		creakingOverrideActiveLastTick = creakingOverrideActive;

		// While active, overrides only these three settings for this tick - never written back
		// to config, so the player's actual preset/settings are untouched once the Creaking
		// leaves reach.
		// Priority order: Creaking override > Throttle > Night only. Throttle disregards Night
		// only entirely while it's on (see throttleWouldApply below) - the Creaking override
		// still wins over both, forcing Night only on regardless.
		boolean effectiveNightOnly = creakingOverrideActive || (config.nightOnly && !config.throttleEnabled);
		boolean effectiveSkipNightCheckInDimensionsWithoutCycle = creakingOverrideActive || config.skipNightCheckInDimensionsWithoutCycle;
		boolean effectiveFreezeDurationDuringDay = creakingOverrideActive || config.freezeDurationDuringDay;

		boolean isDaytime = false;
		if (effectiveNightOnly) {
			long timeOfDay = world.getOverworldClockTime() % 24000;
			// Matches the Creaking Heart's own awake window (Minecraft Wiki), not the
			// textbook 13000/23000 generic night thresholds - creakings specifically
			// wake up earlier and go dormant at the same point as regular night ends.
			isDaytime = timeOfDay < 12600 || timeOfDay > 23400;
		}

		boolean skipNightGate = effectiveNightOnly && effectiveSkipNightCheckInDimensionsWithoutCycle
				&& !dimensionHasDayNightCycle(world);
		boolean nightGatePasses = !effectiveNightOnly || skipNightGate || !isDaytime;

		if (effectiveNightOnly) {
			if (nightGatePassedLastTick == null || nightGatePassedLastTick != nightGatePasses) {
				FeedbackUtil.send(client, config, nightGatePasses
						? "Smart Auto Attack: night has begun - attacking"
						: "Smart Auto Attack: paused (night only - waiting for night)");
			}
			nightGatePassedLastTick = nightGatePasses;
		} else {
			nightGatePassedLastTick = null; // stale state - re-evaluate cleanly if night only gets turned back on
		}

		// Takes priority over Night only (see effectiveNightOnly above, which already backs
		// off when Throttle is on) - only the Creaking override still bypasses Throttle.
		boolean throttleWouldApply = config.throttleEnabled && !creakingOverrideActive;
		if (throttleWouldApply && (throttleEngagedLastTick == null || !throttleEngagedLastTick)) {
			// Just became engaged - always (re)start on a fresh attack phase.
			throttleAttackingPhase = true;
			throttlePhaseTicksRemaining = DurationParser.parseTicks(config.throttleAttackDuration);
		}
		throttleEngagedLastTick = throttleWouldApply;

		boolean throttleGatePasses = true;
		if (throttleWouldApply) {
			long throttleAttackTicks = DurationParser.parseTicks(config.throttleAttackDuration);
			long throttlePauseTicks = DurationParser.parseTicks(config.throttlePauseDuration);
			// Misconfigured (unparseable/zero duration) - don't gate at all rather than risk
			// getting stuck permanently paused.
			if (throttleAttackTicks > 0 && throttlePauseTicks > 0) {
				if (throttlePhaseTicksRemaining <= 0) {
					throttleAttackingPhase = !throttleAttackingPhase;
					throttlePhaseTicksRemaining = throttleAttackingPhase ? throttleAttackTicks : throttlePauseTicks;
				}
				throttleGatePasses = throttleAttackingPhase;
				throttlePhaseTicksRemaining--;

				if (throttlePhasePassedLastTick == null || throttlePhasePassedLastTick != throttleGatePasses) {
					FeedbackUtil.send(client, config, throttleGatePasses
							? "Smart Auto Attack: throttle - attacking"
							: "Smart Auto Attack: throttle - pausing");
				}
				throttlePhasePassedLastTick = throttleGatePasses;
			}
		} else {
			throttlePhasePassedLastTick = null; // stale state - re-evaluate cleanly once re-engaged
		}

		boolean pausedForThrottlePause = throttleWouldApply && !throttleGatePasses && config.freezeDurationDuringThrottlePause;
		boolean pausedForDuration = (effectiveFreezeDurationDuringDay && effectiveNightOnly && isDaytime && !skipNightGate)
				|| pausedForThrottlePause;
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

		long maxDurationTicks = DurationParser.parseTicks(config.maxDuration);
		if (maxDurationTicks > 0 && elapsedActiveTicks >= maxDurationTicks) {
			stop(client, config, "Smart Auto Attack: stopped (time limit reached)");
			return;
		}

		if (!nightGatePasses || !throttleGatePasses) {
			return;
		}

		Entity target = resolveTarget(client, config);
		// Independent of requireTargetDetected: in blind-swing mode (that toggle off),
		// resolveTarget()'s player exclusion still blocks the actual attack packet, but
		// blind swinging isn't gated on having a target at all, so the arm would still
		// visibly swing while aimed at a player even though nothing gets hit. Checked
		// separately here so the swing itself is suppressed too, not just the damage.
		boolean crosshairOnPlayer = isCrosshairOnPlayer(client);
		PiercingWeapon piercingWeapon = player.getMainHandItem().get(DataComponents.PIERCING_WEAPON);
		// Unlike other weapons, vanilla only lands a spear thrust once the attack cooldown
		// is fully recharged - a partially-charged stab just does nothing. Force the full
		// charge wait for spears regardless of "Always fully charge", since skipping it
		// isn't a "weaker hit" tradeoff for them like it is for everything else - it's a
		// wasted swing.
		boolean requireFullCharge = config.alwaysFullyCharge || piercingWeapon != null;

		SmartAutoAttackConfig.AttackCadenceMode effectiveCadenceMode = creakingOverrideActive
				? SmartAutoAttackConfig.AttackCadenceMode.FIXED_INTERVAL
				: config.attackCadenceMode;

		if (effectiveCadenceMode == SmartAutoAttackConfig.AttackCadenceMode.DEFAULT) {
			if (crosshairOnPlayer) {
				return;
			}
			if (target == null && config.requireTargetDetected) {
				return;
			}
			if (player.getAttackStrengthScale(0f) < 1.0f) {
				return;
			}
			performAttack(client, player, config, target, piercingWeapon);
		} else {
			if (ticksUntilNextAttack > 0) {
				ticksUntilNextAttack--;
				return;
			}
			if (crosshairOnPlayer) {
				return; // stays at 0 ("ready"), same as the no-target case below
			}
			if (target == null && config.requireTargetDetected) {
				return; // stays at 0 ("ready"); attacks the instant a valid target reappears
			}
			if (requireFullCharge && player.getAttackStrengthScale(0f) < 1.0f) {
				return; // interval elapsed but weapon isn't fully charged yet - wait, don't consume it
			}
			performAttack(client, player, config, target, piercingWeapon);
			// Creakings only regenerate/form resin at their heart once every 5 seconds (100
			// ticks), so the override always uses exactly that instead of a config-derived value.
			ticksUntilNextAttack = creakingOverrideActive ? 100 : nextIntervalTicks(config);
		}
	}

	// Separate from resolveTarget() because that method also runs when the crosshair is
	// on nothing at all (blind-swing mode) - this specifically distinguishes "nothing
	// there" (still swings blindly) from "a player is there" (never swings, full stop).
	private static boolean isCrosshairOnPlayer(Minecraft client) {
		return client.hitResult != null && client.hitResult.getType() == HitResult.Type.ENTITY
				&& ((EntityHitResult) client.hitResult).getEntity() instanceof Player;
	}

	// Reuses the exact same crosshair pick vanilla itself uses to decide what a real attack
	// would hit - simpler and more reliable than any custom reach/distance check, and avoids
	// the false negatives a line-of-sight raycast hit constantly in a Creaking's actual habitat
	// (Pale Gardens are dense with leaves, which have solid collision and blocked the ray almost
	// all the time, making that approach detect essentially nothing).
	private static boolean isCrosshairOnCreaking(Minecraft client) {
		return client.hitResult != null && client.hitResult.getType() == HitResult.Type.ENTITY
				&& ((EntityHitResult) client.hitResult).getEntity() instanceof Creaking;
	}

	// Nether and End have no day/night cycle (their clock is fixed/meaningless even
	// though the underlying world-time counter keeps advancing globally).
	private static boolean dimensionHasDayNightCycle(Level world) {
		return world.dimension() != Level.NETHER && world.dimension() != Level.END;
	}

	// Spears (and anything else carrying a PiercingWeapon data component) don't use the
	// normal single-target attack at all - vanilla's own Minecraft.startAttack() skips
	// the crosshair/hitResult path entirely for them and calls gameMode.piercingAttack()
	// instead, which sends a different packet and lets the server resolve a line-thrust
	// hit itself (and swings on its own). Calling the regular gameMode.attack() on a
	// spear sends the wrong packet, so nothing lands even though the arm swings.
	private static void performAttack(Minecraft client, Player player, SmartAutoAttackConfig config, Entity target,
			PiercingWeapon piercingWeapon) {
		if (piercingWeapon != null) {
			client.gameMode.piercingAttack(piercingWeapon);
			player.swing(InteractionHand.MAIN_HAND);
		} else {
			if (target != null) {
				client.gameMode.attack(player, target);
			}
			player.swing(InteractionHand.MAIN_HAND);
		}

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
		// Hardcoded, not configurable, and checked unconditionally - deliberately
		// outside the requireTargetDetected branch below, since that branch is
		// skipped entirely in "blind swing" mode (requireTargetDetected off) and
		// would otherwise let a player slip through unfiltered. Required for
		// Modrinth Content Rules 3.3d (automatic/assisted PvP combat needs a
		// server-side opt-in, which this mod does not implement) - see the
		// separate "_AP" ("attacks players") build on GitHub only for anyone who
		// wants this removed at their own risk.
		if (target instanceof Player) {
			return null;
		}
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
		ItemStack currentTool = player.getMainHandItem();
		if (!currentTool.isEmpty()) {
			lastKnownMainHandItem = currentTool; // still equipped - remember it in case it breaks entirely
		}
		if (hasEnoughDurability(currentTool, config)) {
			return true;
		}
		if (!config.useMoreTools) {
			return false;
		}

		// If the weapon has already broken to empty/air (no durability floor set to catch it
		// first), fall back to the last item we saw equipped so SAME_TYPE/EXACT_MATCH still
		// have something meaningful to compare against - matching against air's own item
		// class would otherwise match almost anything, including food.
		ItemStack referenceTool = currentTool.isEmpty() ? lastKnownMainHandItem : currentTool;

		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < 9; slot++) {
			if (slot == inventory.getSelectedSlot()) {
				continue;
			}
			ItemStack candidate = inventory.getItem(slot);
			if (candidate.isEmpty() || !matchesRotationCriteria(candidate, referenceTool, config)) {
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

	// Swords (and most other vanilla weapons/tools as of this MC version) no longer have
	// their own Item subclass - they're plain Item instances distinguished only by data
	// components/tags, so comparing getClass() lumps them in with everything else that's
	// also just a plain Item (including food). Use vanilla's own tool-category tags
	// instead, which is also more correct for modded weapons that register into them.
	private static final List<TagKey<Item>> TOOL_TYPE_TAGS = List.of(
			ItemTags.PICKAXES, ItemTags.AXES, ItemTags.SHOVELS, ItemTags.HOES, ItemTags.SWORDS, ItemTags.SPEARS);

	private static boolean sameToolType(ItemStack candidate, ItemStack referenceTool) {
		for (TagKey<Item> tag : TOOL_TYPE_TAGS) {
			if (referenceTool.is(tag)) {
				return candidate.is(tag);
			}
		}
		// referenceTool isn't in any known tool-category tag (e.g. a trident, or a modded
		// weapon that doesn't register into one) - fall back to class equality, which still
		// works for vanilla's remaining single-item-per-category weapons.
		return candidate.getItem().getClass() == referenceTool.getItem().getClass();
	}

	// referenceTool is the weapon that just ran low (or, if it already broke to empty, the
	// last non-empty item we saw equipped - see ensureUsableTool).
	private static boolean matchesRotationCriteria(ItemStack candidate, ItemStack referenceTool, SmartAutoAttackConfig config) {
		if (referenceTool.isEmpty() && config.toolRotationMode != SmartAutoAttackConfig.ToolRotationMode.KEYWORD) {
			// Never saw a weapon equipped this run - nothing to compare against, so SAME_TYPE/
			// EXACT_MATCH can't mean anything yet (KEYWORD doesn't need a reference at all).
			return false;
		}
		return switch (config.toolRotationMode) {
			case KEYWORD -> matchesKeyword(candidate, config.toolKeyword);
			case SAME_TYPE -> sameToolType(candidate, referenceTool);
			case EXACT_MATCH -> candidate.getItem() == referenceTool.getItem();
		};
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
		if (config.ignoreHungerSafetyWhileRegenerating && player.hasEffect(MobEffects.REGENERATION)) {
			return true;
		}
		return player.getFoodData().getFoodLevel() >= config.hungerSafetyStopThreshold;
	}

	// Returns true if tick() should return immediately (either a hard stop just fired, or
	// we're mid-recovery-pause). Merges the old "hard stop on low health" and "critical-health
	// panic pause" into a single threshold: Eat food to regenerate health decides which of the
	// two happens once health drops below healthSafetyStopThreshold.
	private static boolean handleHealthGuard(Minecraft client, Player player, SmartAutoAttackConfig config) {
		if (criticalHealthPauseActive && (!config.healthSafetyStopEnabled || !config.eatToRegenerateHealth)) {
			criticalHealthPauseActive = false; // guard/eat-to-recover turned off mid-pause - resume immediately
		}
		if (!config.healthSafetyStopEnabled) {
			return false;
		}
		if (isRegenerationBypassActive(player, config)) {
			if (criticalHealthPauseActive) {
				criticalHealthPauseActive = false; // regen kicked in mid-pause - trust it, resume
			}
			return false;
		}

		if (!criticalHealthPauseActive && player.getHealth() < config.healthSafetyStopThreshold) {
			if (!config.eatToRegenerateHealth) {
				stop(client, config, "Smart Auto Attack: stopped (health too low)");
				return true;
			}
			criticalHealthPauseActive = true;
			criticalHealthPauseTicks = 0;
			FeedbackUtil.send(client, config, "Smart Auto Attack: paused (health critical - recovering)");
		}
		if (!criticalHealthPauseActive) {
			return false;
		}

		// Clamped to max health: a high threshold (e.g. 18) plus the margin must never target
		// above what the player can actually reach, or the pause would never resolve on its own.
		float resumeThreshold = Math.min(config.healthSafetyStopThreshold + CRITICAL_HEALTH_RESUME_MARGIN, player.getMaxHealth());
		if (player.getHealth() >= resumeThreshold) {
			criticalHealthPauseActive = false;
			FeedbackUtil.send(client, config, "Smart Auto Attack: resuming (health recovered)");
			return false;
		}
		criticalHealthPauseTicks++;
		if (criticalHealthPauseTicks >= CRITICAL_HEALTH_PAUSE_TIMEOUT_TICKS) {
			stop(client, config, "Smart Auto Attack: stopped (health failed to recover in time)");
			return true;
		}
		return true; // stays paused - AutoEatLogic force-feeds independently, see isCriticalHealthPauseActive()
	}

	// The Paranoia switch overrides the regen bypass specifically for the eat-to-recover path:
	// it never trusts Regeneration alone to keep hunger topped up, only ever relevant while
	// auto-eat can actually act on it.
	private static boolean isRegenerationBypassActive(Player player, SmartAutoAttackConfig config) {
		if (!config.ignoreHealthSafetyWhileRegenerating || !player.hasEffect(MobEffects.REGENERATION)) {
			return false;
		}
		if (config.paranoiaSwitchEnabled && config.eatToRegenerateHealth && config.autoEatEnabled) {
			return false;
		}
		return true;
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

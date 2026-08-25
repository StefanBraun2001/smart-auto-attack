package eu.stefanbraun612.smartautoattack.client.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@Config(name = "smartautoattack")
public class SmartAutoAttackConfig implements ConfigData {

	// --- Attack cadence ---

	public enum AttackCadenceMode {
		DEFAULT,
		FIXED_INTERVAL,
		RANDOM_INTERVAL
	}

	public enum IntervalUnit {
		TICKS,
		SECONDS
	}

	public AttackCadenceMode attackCadenceMode = AttackCadenceMode.DEFAULT;

	public IntervalUnit intervalUnit = IntervalUnit.TICKS;

	public double fixedIntervalValue = 20;

	public double randomIntervalMin = 15;

	public double randomIntervalMax = 30;

	public boolean requireTargetDetected = true;

	public boolean alwaysFullyCharge = true;

	// --- Timing ---

	public boolean nightOnly = false;

	public boolean skipNightCheckInDimensionsWithoutCycle = false;

	public boolean freezeDurationDuringDay = false;

	public boolean adjustToCreakings = false;

	public int maxHits = 0; // 0 = unlimited

	public String maxDuration = ""; // e.g. "90m", "1.5h", "5400s", "1h30m" - empty = unlimited

	// --- Throttle ---
	// Alternates between an "attacking" phase and a "paused" phase for the configured
	// durations. Mutually exclusive with Night only (only engages while Night only is
	// effectively off), which also naturally disengages it during the Adjust to Creakings
	// override (that override always forces Night only on).

	public boolean throttleEnabled = false;

	public String throttleAttackDuration = "5m"; // same free-text format as maxDuration

	public String throttlePauseDuration = "1m";

	public boolean freezeDurationDuringThrottlePause = true; // pauses the max-duration timer while throttle is paused

	// --- Safety ---

	public int minDurability = 0; // 0 = disabled

	public int minDurabilityPercent = 0; // 0 = disabled

	public boolean useMoreTools = false;

	// How "use more tools" decides whether a hotbar item is a valid replacement.
	public enum ToolRotationMode {
		// Substring match (case-insensitive) against the item's registry ID - see toolKeyword.
		KEYWORD,
		// Same weapon class as the one that just ran low (e.g. any sword replaces any
		// sword), regardless of material - no keyword needed.
		SAME_TYPE,
		// Exact same item as the one that just ran low (e.g. only another diamond sword
		// replaces a diamond sword) - the strictest mode.
		EXACT_MATCH
	}

	public ToolRotationMode toolRotationMode = ToolRotationMode.KEYWORD;

	public String toolKeyword = "sword"; // substring match against the item's registry ID - only used in KEYWORD mode

	public boolean hungerSafetyStopEnabled = true;

	public int hungerSafetyStopThreshold = 6; // hunger points, 0-20 scale

	public boolean ignoreHungerSafetyWhileRegenerating = false;

	public boolean healthSafetyStopEnabled = true;

	public float healthSafetyStopThreshold = 6; // health points, 0-20 scale (each heart = 2 points)

	public boolean eatToRegenerateHealth = false;

	public boolean ignoreHealthSafetyWhileRegenerating = false;

	public boolean paranoiaSwitchEnabled = false;

	// --- Durability warning ---
	// Runs independently of the toggle above - an always-on watchdog for accidentally
	// hand-using a tool that would already fail the Min durability/% guard, even while
	// this mod isn't attacking with it. Reuses that same threshold rather than a separate
	// one. Mutually exclusive with Smart Auto Mine's equivalent feature if installed - see
	// SmartAutoAttackConfigScreen's error supplier on durabilityWarningEnabled.

	public boolean durabilityWarningEnabled = false;

	public List<String> durabilityWarningKeywords = new ArrayList<>(); // e.g. "pickaxe", "axe" - substring match, same as toolKeyword

	public String durabilityWarningSound = "minecraft:block.bell.use"; // full sound event ID

	// Separate toggle: checks all 4 armor slots (+ elytra, which occupies the chest slot)
	// for durability regardless of item type - no keyword list, since "is this a helmet"
	// isn't a meaningful question the way "is this a pickaxe" is. Shares durabilityWarningSound.
	public boolean armorDurabilityWarningEnabled = false;

	// --- Targeting ---

	public enum TargetFilterMode {
		BLACKLIST,
		WHITELIST
	}

	public TargetFilterMode filterMode = TargetFilterMode.BLACKLIST;

	public List<String> targetList = new ArrayList<>();

	// Blacklist mode only - excludes every boat/raft/chest-boat/chest-raft variant and
	// every minecart variant (storage/furnace/TNT/hopper/command-block/spawner) via an
	// AbstractBoat/AbstractMinecart instanceof check, not a vanilla tag - the vanilla
	// "boat" tag omits chest boats/rafts entirely, and there's no vanilla minecart tag at all.
	public boolean excludeBoatsAndMinecarts = false;

	// --- General / feedback ---

	public enum FeedbackMode {
		CHAT,
		ACTION_BAR,
		SILENT
	}

	public FeedbackMode feedbackMode = FeedbackMode.ACTION_BAR;

	public boolean playSoundOnAutoStop = true;

	public String autoStopSound = "minecraft:block.bell.use"; // full sound event ID

	public boolean resumeAfterManualReconnect = false; // scripted reconnects (Smart Auto Reconnect) always resume regardless of this

	// --- Auto-eat ---

	public enum FoodSafetyPreset {
		LIGHT,
		FOOD_INSPECTOR,
		RAT
	}

	public boolean autoEatEnabled = true;

	public boolean autoEatSearchAnySlot = false;

	public int autoEatSlot = 0; // 0 = disabled, 1-9 hotbar slot - only used when autoEatSearchAnySlot is off

	public int autoEatHungerThreshold = 20; // hunger points, 0-20 (matches the vanilla hunger bar: 20 = full, each drumstick icon = 2 points)

	public enum AutoEatAmountMode {
		// Eats exactly one bite per dip below the threshold, then waits for hunger to rise
		// back above it before it's willing to eat again - even if one bite wasn't enough.
		EAT_ONCE,
		// Keeps eating bite after bite, but skips/stops the moment a bite's nutrition
		// would push hunger past the 20-point cap, so it never wastes food.
		DONT_OVEREAT,
		// Keeps eating bite after bite until hunger is fully at 20, no matter how much of
		// a bite's nutrition would go to waste.
		FILL_HUNGER
	}

	public AutoEatAmountMode autoEatAmountMode = AutoEatAmountMode.EAT_ONCE;

	public FoodSafetyPreset foodSafetyPreset = FoodSafetyPreset.LIGHT;
}

package eu.stefanbraun612.smartautoattack.client.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

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

	@ConfigEntry.Gui.Tooltip
	@ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
	public AttackCadenceMode attackCadenceMode = AttackCadenceMode.DEFAULT;

	@ConfigEntry.Gui.Tooltip
	@ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
	public IntervalUnit intervalUnit = IntervalUnit.TICKS;

	@ConfigEntry.Gui.Tooltip
	public double fixedIntervalValue = 20;

	@ConfigEntry.Gui.Tooltip
	public double randomIntervalMin = 15;

	@ConfigEntry.Gui.Tooltip
	public double randomIntervalMax = 30;

	@ConfigEntry.Gui.Tooltip
	public boolean requireTargetDetected = true;

	@ConfigEntry.Gui.Tooltip
	public boolean alwaysFullyCharge = true;

	// --- Timing ---

	@ConfigEntry.Gui.Tooltip
	public boolean nightOnly = false;

	@ConfigEntry.Gui.Tooltip
	public boolean skipNightCheckInDimensionsWithoutCycle = false;

	@ConfigEntry.Gui.Tooltip
	public boolean freezeDurationDuringDay = false;

	@ConfigEntry.Gui.Tooltip
	public boolean adjustToCreakings = false;

	@ConfigEntry.Gui.Tooltip
	public int maxHits = 0; // 0 = unlimited

	@ConfigEntry.Gui.Tooltip
	public String maxDuration = ""; // e.g. "90m", "1.5h", "5400s", "1h30m" - empty = unlimited

	// --- Safety ---

	@ConfigEntry.Gui.Tooltip
	public int minDurability = 0; // 0 = disabled

	@ConfigEntry.Gui.Tooltip
	public int minDurabilityPercent = 0; // 0 = disabled

	@ConfigEntry.Gui.Tooltip
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

	@ConfigEntry.Gui.Tooltip
	@ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
	public ToolRotationMode toolRotationMode = ToolRotationMode.KEYWORD;

	@ConfigEntry.Gui.Tooltip
	public String toolKeyword = "sword"; // substring match against the item's registry ID - only used in KEYWORD mode

	@ConfigEntry.Gui.Tooltip
	public boolean hungerSafetyStopEnabled = true;

	@ConfigEntry.Gui.Tooltip
	public int hungerSafetyStopThreshold = 6; // hunger points, 0-20 scale

	@ConfigEntry.Gui.Tooltip
	public boolean ignoreHungerSafetyWhileRegenerating = false;

	@ConfigEntry.Gui.Tooltip
	public boolean healthSafetyStopEnabled = true;

	@ConfigEntry.Gui.Tooltip
	public float healthSafetyStopThreshold = 6; // health points, 0-20 scale (each heart = 2 points)

	@ConfigEntry.Gui.Tooltip
	public boolean eatToRegenerateHealth = false;

	@ConfigEntry.Gui.Tooltip
	public boolean ignoreHealthSafetyWhileRegenerating = false;

	@ConfigEntry.Gui.Tooltip
	public boolean paranoiaSwitchEnabled = false;

	// --- Targeting ---

	public enum TargetFilterMode {
		BLACKLIST,
		WHITELIST
	}

	@ConfigEntry.Gui.Tooltip
	@ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
	public TargetFilterMode filterMode = TargetFilterMode.BLACKLIST;

	@ConfigEntry.Gui.Tooltip
	public List<String> targetList = new ArrayList<>();

	// --- General / feedback ---

	public enum FeedbackMode {
		CHAT,
		ACTION_BAR,
		SILENT
	}

	@ConfigEntry.Gui.Tooltip
	@ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
	public FeedbackMode feedbackMode = FeedbackMode.ACTION_BAR;

	@ConfigEntry.Gui.Tooltip
	public boolean playSoundOnAutoStop = true;

	@ConfigEntry.Gui.Tooltip
	public String autoStopSound = "minecraft:block.bell.use"; // full sound event ID

	@ConfigEntry.Gui.Tooltip
	public boolean resumeAfterManualReconnect = false; // scripted reconnects (Smart Auto Reconnect) always resume regardless of this

	// --- Auto-eat ---

	public enum FoodSafetyPreset {
		LIGHT,
		FOOD_INSPECTOR,
		RAT
	}

	@ConfigEntry.Gui.Tooltip
	public boolean autoEatEnabled = true;

	@ConfigEntry.Gui.Tooltip
	public boolean autoEatSearchAnySlot = false;

	@ConfigEntry.Gui.Tooltip
	public int autoEatSlot = 0; // 0 = disabled, 1-9 hotbar slot - only used when autoEatSearchAnySlot is off

	@ConfigEntry.Gui.Tooltip
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

	@ConfigEntry.Gui.Tooltip
	@ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
	public AutoEatAmountMode autoEatAmountMode = AutoEatAmountMode.EAT_ONCE;

	@ConfigEntry.Gui.Tooltip
	@ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
	public FoodSafetyPreset foodSafetyPreset = FoodSafetyPreset.LIGHT;
}

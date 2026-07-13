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
	public int maxHits = 0; // 0 = unlimited

	@ConfigEntry.Gui.Tooltip
	public String maxDuration = ""; // e.g. "90m", "1.5h", "5400s", "1h30m" - empty = unlimited

	// --- Safety ---

	@ConfigEntry.Gui.Tooltip
	public int minDurability = 0; // 0 = disabled

	@ConfigEntry.Gui.Tooltip
	public int minDurabilityPercent = 0; // 0 = disabled

	@ConfigEntry.Gui.Tooltip
	public boolean hungerSafetyStopEnabled = true;

	@ConfigEntry.Gui.Tooltip
	public int hungerSafetyStopThreshold = 6; // hunger points, 0-20 scale

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

	// --- Auto-eat ---

	public enum FoodSafetyPreset {
		LIGHT,
		FOOD_INSPECTOR,
		RAT
	}

	@ConfigEntry.Gui.Tooltip
	public int autoEatSlot = 0; // 0 = disabled, 1-9 hotbar slot

	@ConfigEntry.Gui.Tooltip
	public int autoEatHungerThreshold = 20; // hunger points, 0-20 (matches the vanilla hunger bar: 20 = full, each drumstick icon = 2 points)

	@ConfigEntry.Gui.Tooltip
	@ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
	public FoodSafetyPreset foodSafetyPreset = FoodSafetyPreset.LIGHT;
}

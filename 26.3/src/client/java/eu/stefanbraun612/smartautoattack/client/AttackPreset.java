package eu.stefanbraun612.smartautoattack.client;

import eu.stefanbraun612.smartautoattack.client.config.SmartAutoAttackConfig;

// Deliberately scoped to non-player-specific "technique" settings only: duration/hit
// limits, durability, and the auto-eat/food-safety cluster. Never touches hotbar
// slots, keybinds, or feedback style, since those depend on the player's own setup.
public class AttackPreset {
	public String maxDuration = "";
	public int maxHits = 0;
	public int minDurability = 0;
	public int minDurabilityPercent = 0;
	public boolean useMoreTools = false;
	public SmartAutoAttackConfig.ToolRotationMode toolRotationMode = SmartAutoAttackConfig.ToolRotationMode.KEYWORD;
	public String toolKeyword = "sword";
	public boolean nightOnly = false;
	public boolean skipNightCheckInDimensionsWithoutCycle = false;
	public boolean freezeDurationDuringDay = false;
	public boolean autoEatEnabled = true;
	public int autoEatHungerThreshold = 20;
	public boolean hungerSafetyStopEnabled = true;
	public int hungerSafetyStopThreshold = 6;
	public SmartAutoAttackConfig.AttackCadenceMode attackCadenceMode = SmartAutoAttackConfig.AttackCadenceMode.DEFAULT;
	public SmartAutoAttackConfig.IntervalUnit intervalUnit = SmartAutoAttackConfig.IntervalUnit.TICKS;
	public double fixedIntervalValue = 20;
	public double randomIntervalMin = 15;
	public double randomIntervalMax = 30;

	public static AttackPreset fromConfig(SmartAutoAttackConfig config) {
		AttackPreset preset = new AttackPreset();
		preset.maxDuration = config.maxDuration;
		preset.maxHits = config.maxHits;
		preset.minDurability = config.minDurability;
		preset.minDurabilityPercent = config.minDurabilityPercent;
		preset.useMoreTools = config.useMoreTools;
		preset.toolRotationMode = config.toolRotationMode;
		preset.toolKeyword = config.toolKeyword;
		preset.nightOnly = config.nightOnly;
		preset.skipNightCheckInDimensionsWithoutCycle = config.skipNightCheckInDimensionsWithoutCycle;
		preset.freezeDurationDuringDay = config.freezeDurationDuringDay;
		preset.autoEatEnabled = config.autoEatEnabled;
		preset.autoEatHungerThreshold = config.autoEatHungerThreshold;
		preset.hungerSafetyStopEnabled = config.hungerSafetyStopEnabled;
		preset.hungerSafetyStopThreshold = config.hungerSafetyStopThreshold;
		preset.attackCadenceMode = config.attackCadenceMode;
		preset.intervalUnit = config.intervalUnit;
		preset.fixedIntervalValue = config.fixedIntervalValue;
		preset.randomIntervalMin = config.randomIntervalMin;
		preset.randomIntervalMax = config.randomIntervalMax;
		return preset;
	}

	public void applyTo(SmartAutoAttackConfig config) {
		config.maxDuration = maxDuration;
		config.maxHits = maxHits;
		config.minDurability = minDurability;
		config.minDurabilityPercent = minDurabilityPercent;
		config.useMoreTools = useMoreTools;
		config.toolRotationMode = toolRotationMode;
		config.toolKeyword = toolKeyword;
		config.nightOnly = nightOnly;
		config.skipNightCheckInDimensionsWithoutCycle = skipNightCheckInDimensionsWithoutCycle;
		config.freezeDurationDuringDay = freezeDurationDuringDay;
		config.autoEatEnabled = autoEatEnabled;
		config.autoEatHungerThreshold = autoEatHungerThreshold;
		config.hungerSafetyStopEnabled = hungerSafetyStopEnabled;
		config.hungerSafetyStopThreshold = hungerSafetyStopThreshold;
		config.attackCadenceMode = attackCadenceMode;
		config.intervalUnit = intervalUnit;
		config.fixedIntervalValue = fixedIntervalValue;
		config.randomIntervalMin = randomIntervalMin;
		config.randomIntervalMax = randomIntervalMax;
	}

	public static AttackPreset regularTpAehp() {
		AttackPreset preset = new AttackPreset();
		preset.minDurability = 10;
		preset.minDurabilityPercent = 5;
		preset.autoEatEnabled = true;
		preset.autoEatHungerThreshold = 7;
		preset.hungerSafetyStopEnabled = true;
		preset.hungerSafetyStopThreshold = 3;
		return preset;
	}

	public static AttackPreset creakingFtTpAehp() {
		AttackPreset preset = new AttackPreset();
		preset.minDurability = 10;
		preset.minDurabilityPercent = 5;
		preset.nightOnly = true;
		preset.freezeDurationDuringDay = true;
		// Creakings only regenerate/form resin at their heart once every 5 seconds
		// (100 ticks), so hitting faster than that is wasted swings.
		preset.attackCadenceMode = SmartAutoAttackConfig.AttackCadenceMode.FIXED_INTERVAL;
		preset.intervalUnit = SmartAutoAttackConfig.IntervalUnit.TICKS;
		preset.fixedIntervalValue = 100;
		preset.autoEatEnabled = true;
		preset.autoEatHungerThreshold = 7;
		preset.hungerSafetyStopEnabled = true;
		preset.hungerSafetyStopThreshold = 3;
		return preset;
	}

	public static AttackPreset regularMtTpAehp() {
		AttackPreset preset = regularTpAehp();
		preset.useMoreTools = true;
		return preset;
	}

	public static AttackPreset creakingMtFtTpAehp() {
		AttackPreset preset = creakingFtTpAehp();
		preset.useMoreTools = true;
		return preset;
	}
}

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
	public boolean nightOnly = false;
	public boolean skipNightCheckInDimensionsWithoutCycle = false;
	public boolean freezeDurationDuringDay = false;
	public boolean autoEatEnabled = true;
	public int autoEatHungerThreshold = 20;
	public boolean hungerSafetyStopEnabled = true;
	public int hungerSafetyStopThreshold = 6;

	public static AttackPreset fromConfig(SmartAutoAttackConfig config) {
		AttackPreset preset = new AttackPreset();
		preset.maxDuration = config.maxDuration;
		preset.maxHits = config.maxHits;
		preset.minDurability = config.minDurability;
		preset.minDurabilityPercent = config.minDurabilityPercent;
		preset.nightOnly = config.nightOnly;
		preset.skipNightCheckInDimensionsWithoutCycle = config.skipNightCheckInDimensionsWithoutCycle;
		preset.freezeDurationDuringDay = config.freezeDurationDuringDay;
		preset.autoEatEnabled = config.autoEatEnabled;
		preset.autoEatHungerThreshold = config.autoEatHungerThreshold;
		preset.hungerSafetyStopEnabled = config.hungerSafetyStopEnabled;
		preset.hungerSafetyStopThreshold = config.hungerSafetyStopThreshold;
		return preset;
	}

	public void applyTo(SmartAutoAttackConfig config) {
		config.maxDuration = maxDuration;
		config.maxHits = maxHits;
		config.minDurability = minDurability;
		config.minDurabilityPercent = minDurabilityPercent;
		config.nightOnly = nightOnly;
		config.skipNightCheckInDimensionsWithoutCycle = skipNightCheckInDimensionsWithoutCycle;
		config.freezeDurationDuringDay = freezeDurationDuringDay;
		config.autoEatEnabled = autoEatEnabled;
		config.autoEatHungerThreshold = autoEatHungerThreshold;
		config.hungerSafetyStopEnabled = hungerSafetyStopEnabled;
		config.hungerSafetyStopThreshold = hungerSafetyStopThreshold;
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
		preset.autoEatEnabled = true;
		preset.autoEatHungerThreshold = 7;
		preset.hungerSafetyStopEnabled = true;
		preset.hungerSafetyStopThreshold = 3;
		return preset;
	}
}

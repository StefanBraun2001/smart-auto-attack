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
	public boolean autoEatEnabled = true;
	public int autoEatHungerThreshold = 20;
	public SmartAutoAttackConfig.FoodSafetyPreset foodSafetyPreset = SmartAutoAttackConfig.FoodSafetyPreset.LIGHT;
	public boolean hungerSafetyStopEnabled = true;
	public int hungerSafetyStopThreshold = 6;

	public static AttackPreset fromConfig(SmartAutoAttackConfig config) {
		AttackPreset preset = new AttackPreset();
		preset.maxDuration = config.maxDuration;
		preset.maxHits = config.maxHits;
		preset.minDurability = config.minDurability;
		preset.minDurabilityPercent = config.minDurabilityPercent;
		preset.useMoreTools = config.useMoreTools;
		preset.toolRotationMode = config.toolRotationMode;
		preset.toolKeyword = config.toolKeyword;
		preset.autoEatEnabled = config.autoEatEnabled;
		preset.autoEatHungerThreshold = config.autoEatHungerThreshold;
		preset.foodSafetyPreset = config.foodSafetyPreset;
		preset.hungerSafetyStopEnabled = config.hungerSafetyStopEnabled;
		preset.hungerSafetyStopThreshold = config.hungerSafetyStopThreshold;
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
		config.autoEatEnabled = autoEatEnabled;
		config.autoEatHungerThreshold = autoEatHungerThreshold;
		config.foodSafetyPreset = foodSafetyPreset;
		config.hungerSafetyStopEnabled = hungerSafetyStopEnabled;
		config.hungerSafetyStopThreshold = hungerSafetyStopThreshold;
	}

	// Ships as a starting example - night-only and the auto-eat hotbar slot are
	// deliberately left for the player to set manually alongside applying this.
	public static AttackPreset creakingTpAehp() {
		AttackPreset preset = new AttackPreset();
		preset.minDurability = 30;
		preset.autoEatEnabled = true;
		preset.autoEatHungerThreshold = 4;
		preset.foodSafetyPreset = SmartAutoAttackConfig.FoodSafetyPreset.LIGHT;
		preset.hungerSafetyStopEnabled = true;
		preset.hungerSafetyStopThreshold = 2;
		return preset;
	}

	public static AttackPreset creakingMtTpAehp() {
		AttackPreset preset = creakingTpAehp();
		preset.useMoreTools = true;
		return preset;
	}
}

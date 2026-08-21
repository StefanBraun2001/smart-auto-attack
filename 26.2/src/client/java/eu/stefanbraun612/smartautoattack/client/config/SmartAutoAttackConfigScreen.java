package eu.stefanbraun612.smartautoattack.client.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.Requirement;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Hand-built (not annotation-generated) Cloth Config screen, so that fields can be
 * grouped into tabs and dependent fields can be hidden via Requirement - neither is
 * possible with AutoConfig's reflection-based screen generation.
 */
public class SmartAutoAttackConfigScreen {

	private static final String PREFIX = "text.autoconfig.smartautoattack.";

	private static Component option(String field) {
		return Component.translatable(PREFIX + "option." + field);
	}

	private static Component tooltip(String field) {
		return Component.translatable(PREFIX + "option." + field + ".@Tooltip");
	}

	private static Component category(String key) {
		return Component.translatable(PREFIX + "category." + key);
	}

	public static Screen build(Screen parent) {
		ConfigHolder<SmartAutoAttackConfig> holder = AutoConfig.getConfigHolder(SmartAutoAttackConfig.class);
		SmartAutoAttackConfig config = holder.getConfig();
		SmartAutoAttackConfig defaults = new SmartAutoAttackConfig();

		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.translatable(PREFIX + "title"))
				.setSavingRunnable(holder::save);
		ConfigEntryBuilder entryBuilder = builder.entryBuilder();

		// --- Cadence tab ---

		ConfigCategory cadence = builder.getOrCreateCategory(category("cadence"));

		EnumListEntry<SmartAutoAttackConfig.AttackCadenceMode> attackCadenceMode = entryBuilder
				.startEnumSelector(option("attackCadenceMode"), SmartAutoAttackConfig.AttackCadenceMode.class, config.attackCadenceMode)
				.setDefaultValue(defaults.attackCadenceMode)
				.setTooltip(tooltip("attackCadenceMode"))
				.setSaveConsumer(v -> config.attackCadenceMode = v)
				.build();
		cadence.addEntry(attackCadenceMode);

		Requirement notDefaultCadence = Requirement.not(Requirement.isValue(attackCadenceMode, SmartAutoAttackConfig.AttackCadenceMode.DEFAULT));

		cadence.addEntry(entryBuilder
				.startEnumSelector(option("intervalUnit"), SmartAutoAttackConfig.IntervalUnit.class, config.intervalUnit)
				.setDefaultValue(defaults.intervalUnit)
				.setTooltip(tooltip("intervalUnit"))
				.setSaveConsumer(v -> config.intervalUnit = v)
				.setDisplayRequirement(notDefaultCadence)
				.build());

		cadence.addEntry(entryBuilder
				.startDoubleField(option("fixedIntervalValue"), config.fixedIntervalValue)
				.setDefaultValue(defaults.fixedIntervalValue)
				.setTooltip(tooltip("fixedIntervalValue"))
				.setSaveConsumer(v -> config.fixedIntervalValue = v)
				.setDisplayRequirement(Requirement.isValue(attackCadenceMode, SmartAutoAttackConfig.AttackCadenceMode.FIXED_INTERVAL))
				.build());

		cadence.addEntry(entryBuilder
				.startDoubleField(option("randomIntervalMin"), config.randomIntervalMin)
				.setDefaultValue(defaults.randomIntervalMin)
				.setTooltip(tooltip("randomIntervalMin"))
				.setSaveConsumer(v -> config.randomIntervalMin = v)
				.setDisplayRequirement(Requirement.isValue(attackCadenceMode, SmartAutoAttackConfig.AttackCadenceMode.RANDOM_INTERVAL))
				.build());

		cadence.addEntry(entryBuilder
				.startDoubleField(option("randomIntervalMax"), config.randomIntervalMax)
				.setDefaultValue(defaults.randomIntervalMax)
				.setTooltip(tooltip("randomIntervalMax"))
				.setSaveConsumer(v -> config.randomIntervalMax = v)
				.setDisplayRequirement(Requirement.isValue(attackCadenceMode, SmartAutoAttackConfig.AttackCadenceMode.RANDOM_INTERVAL))
				.build());

		cadence.addEntry(entryBuilder
				.startBooleanToggle(option("requireTargetDetected"), config.requireTargetDetected)
				.setDefaultValue(defaults.requireTargetDetected)
				.setTooltip(tooltip("requireTargetDetected"))
				.setSaveConsumer(v -> config.requireTargetDetected = v)
				.build());

		cadence.addEntry(entryBuilder
				.startBooleanToggle(option("alwaysFullyCharge"), config.alwaysFullyCharge)
				.setDefaultValue(defaults.alwaysFullyCharge)
				.setTooltip(tooltip("alwaysFullyCharge"))
				.setSaveConsumer(v -> config.alwaysFullyCharge = v)
				.setDisplayRequirement(notDefaultCadence)
				.build());

		// --- Timing tab ---

		ConfigCategory timing = builder.getOrCreateCategory(category("timing"));

		BooleanListEntry nightOnly = entryBuilder
				.startBooleanToggle(option("nightOnly"), config.nightOnly)
				.setDefaultValue(defaults.nightOnly)
				.setTooltip(tooltip("nightOnly"))
				.setSaveConsumer(v -> config.nightOnly = v)
				.build();
		timing.addEntry(nightOnly);

		timing.addEntry(entryBuilder
				.startBooleanToggle(option("skipNightCheckInDimensionsWithoutCycle"), config.skipNightCheckInDimensionsWithoutCycle)
				.setDefaultValue(defaults.skipNightCheckInDimensionsWithoutCycle)
				.setTooltip(tooltip("skipNightCheckInDimensionsWithoutCycle"))
				.setSaveConsumer(v -> config.skipNightCheckInDimensionsWithoutCycle = v)
				.setDisplayRequirement(Requirement.isTrue(nightOnly))
				.build());

		timing.addEntry(entryBuilder
				.startBooleanToggle(option("freezeDurationDuringDay"), config.freezeDurationDuringDay)
				.setDefaultValue(defaults.freezeDurationDuringDay)
				.setTooltip(tooltip("freezeDurationDuringDay"))
				.setSaveConsumer(v -> config.freezeDurationDuringDay = v)
				.setDisplayRequirement(Requirement.isTrue(nightOnly))
				.build());

		timing.addEntry(entryBuilder
				.startBooleanToggle(option("adjustToCreakings"), config.adjustToCreakings)
				.setDefaultValue(defaults.adjustToCreakings)
				.setTooltip(tooltip("adjustToCreakings"))
				.setSaveConsumer(v -> config.adjustToCreakings = v)
				.build());

		timing.addEntry(entryBuilder
				.startIntField(option("maxHits"), config.maxHits)
				.setDefaultValue(defaults.maxHits)
				.setTooltip(tooltip("maxHits"))
				.setSaveConsumer(v -> config.maxHits = v)
				.build());

		timing.addEntry(entryBuilder
				.startStrField(option("maxDuration"), config.maxDuration)
				.setDefaultValue(defaults.maxDuration)
				.setTooltip(tooltip("maxDuration"))
				.setSaveConsumer(v -> config.maxDuration = v)
				.build());

		// --- Safety tab ---

		ConfigCategory safety = builder.getOrCreateCategory(category("safety"));

		safety.addEntry(entryBuilder
				.startIntField(option("minDurability"), config.minDurability)
				.setDefaultValue(defaults.minDurability)
				.setTooltip(tooltip("minDurability"))
				.setSaveConsumer(v -> config.minDurability = v)
				.build());

		safety.addEntry(entryBuilder
				.startIntField(option("minDurabilityPercent"), config.minDurabilityPercent)
				.setDefaultValue(defaults.minDurabilityPercent)
				.setTooltip(tooltip("minDurabilityPercent"))
				.setSaveConsumer(v -> config.minDurabilityPercent = v)
				.build());

		BooleanListEntry useMoreTools = entryBuilder
				.startBooleanToggle(option("useMoreTools"), config.useMoreTools)
				.setDefaultValue(defaults.useMoreTools)
				.setTooltip(tooltip("useMoreTools"))
				.setSaveConsumer(v -> config.useMoreTools = v)
				.build();
		safety.addEntry(useMoreTools);

		EnumListEntry<SmartAutoAttackConfig.ToolRotationMode> toolRotationMode = entryBuilder
				.startEnumSelector(option("toolRotationMode"), SmartAutoAttackConfig.ToolRotationMode.class, config.toolRotationMode)
				.setDefaultValue(defaults.toolRotationMode)
				.setTooltip(tooltip("toolRotationMode"))
				.setSaveConsumer(v -> config.toolRotationMode = v)
				.setDisplayRequirement(Requirement.isTrue(useMoreTools))
				.build();
		safety.addEntry(toolRotationMode);

		safety.addEntry(entryBuilder
				.startStrField(option("toolKeyword"), config.toolKeyword)
				.setDefaultValue(defaults.toolKeyword)
				.setTooltip(tooltip("toolKeyword"))
				.setSaveConsumer(v -> config.toolKeyword = v)
				.setDisplayRequirement(Requirement.all(
						Requirement.isTrue(useMoreTools),
						Requirement.isValue(toolRotationMode, SmartAutoAttackConfig.ToolRotationMode.KEYWORD)))
				.build());

		BooleanListEntry hungerSafetyStopEnabled = entryBuilder
				.startBooleanToggle(option("hungerSafetyStopEnabled"), config.hungerSafetyStopEnabled)
				.setDefaultValue(defaults.hungerSafetyStopEnabled)
				.setTooltip(tooltip("hungerSafetyStopEnabled"))
				.setSaveConsumer(v -> config.hungerSafetyStopEnabled = v)
				.build();
		safety.addEntry(hungerSafetyStopEnabled);

		safety.addEntry(entryBuilder
				.startIntField(option("hungerSafetyStopThreshold"), config.hungerSafetyStopThreshold)
				.setDefaultValue(defaults.hungerSafetyStopThreshold)
				.setTooltip(tooltip("hungerSafetyStopThreshold"))
				.setSaveConsumer(v -> config.hungerSafetyStopThreshold = v)
				.setDisplayRequirement(Requirement.isTrue(hungerSafetyStopEnabled))
				.build());

		safety.addEntry(entryBuilder
				.startBooleanToggle(option("ignoreHungerSafetyWhileRegenerating"), config.ignoreHungerSafetyWhileRegenerating)
				.setDefaultValue(defaults.ignoreHungerSafetyWhileRegenerating)
				.setTooltip(tooltip("ignoreHungerSafetyWhileRegenerating"))
				.setSaveConsumer(v -> config.ignoreHungerSafetyWhileRegenerating = v)
				.setDisplayRequirement(Requirement.isTrue(hungerSafetyStopEnabled))
				.build());

		BooleanListEntry healthSafetyStopEnabled = entryBuilder
				.startBooleanToggle(option("healthSafetyStopEnabled"), config.healthSafetyStopEnabled)
				.setDefaultValue(defaults.healthSafetyStopEnabled)
				.setTooltip(tooltip("healthSafetyStopEnabled"))
				.setSaveConsumer(v -> config.healthSafetyStopEnabled = v)
				.build();
		safety.addEntry(healthSafetyStopEnabled);

		safety.addEntry(entryBuilder
				.startFloatField(option("healthSafetyStopThreshold"), config.healthSafetyStopThreshold)
				.setDefaultValue(defaults.healthSafetyStopThreshold)
				.setTooltip(tooltip("healthSafetyStopThreshold"))
				.setSaveConsumer(v -> config.healthSafetyStopThreshold = v)
				.setDisplayRequirement(Requirement.isTrue(healthSafetyStopEnabled))
				.build());

		BooleanListEntry eatToRegenerateHealth = entryBuilder
				.startBooleanToggle(option("eatToRegenerateHealth"), config.eatToRegenerateHealth)
				.setDefaultValue(defaults.eatToRegenerateHealth)
				.setTooltip(tooltip("eatToRegenerateHealth"))
				.setSaveConsumer(v -> config.eatToRegenerateHealth = v)
				.setDisplayRequirement(Requirement.isTrue(healthSafetyStopEnabled))
				.build();
		safety.addEntry(eatToRegenerateHealth);

		safety.addEntry(entryBuilder
				.startBooleanToggle(option("ignoreHealthSafetyWhileRegenerating"), config.ignoreHealthSafetyWhileRegenerating)
				.setDefaultValue(defaults.ignoreHealthSafetyWhileRegenerating)
				.setTooltip(tooltip("ignoreHealthSafetyWhileRegenerating"))
				.setSaveConsumer(v -> config.ignoreHealthSafetyWhileRegenerating = v)
				.setDisplayRequirement(Requirement.isTrue(healthSafetyStopEnabled))
				.build());

		safety.addEntry(entryBuilder
				.startBooleanToggle(option("paranoiaSwitchEnabled"), config.paranoiaSwitchEnabled)
				.setDefaultValue(defaults.paranoiaSwitchEnabled)
				.setTooltip(tooltip("paranoiaSwitchEnabled"))
				.setSaveConsumer(v -> config.paranoiaSwitchEnabled = v)
				.setDisplayRequirement(Requirement.all(
						Requirement.isTrue(healthSafetyStopEnabled),
						Requirement.isTrue(eatToRegenerateHealth)))
				.build());

		// --- Targeting tab ---

		ConfigCategory targeting = builder.getOrCreateCategory(category("targeting"));

		targeting.addEntry(entryBuilder
				.startEnumSelector(option("filterMode"), SmartAutoAttackConfig.TargetFilterMode.class, config.filterMode)
				.setDefaultValue(defaults.filterMode)
				.setTooltip(tooltip("filterMode"))
				.setSaveConsumer(v -> config.filterMode = v)
				.build());

		targeting.addEntry(entryBuilder
				.startStrList(option("targetList"), config.targetList)
				.setDefaultValue(defaults.targetList)
				.setTooltip(tooltip("targetList"))
				.setSaveConsumer(v -> config.targetList = v)
				.build());

		// --- General tab ---

		ConfigCategory general = builder.getOrCreateCategory(category("general"));

		general.addEntry(entryBuilder
				.startEnumSelector(option("feedbackMode"), SmartAutoAttackConfig.FeedbackMode.class, config.feedbackMode)
				.setDefaultValue(defaults.feedbackMode)
				.setTooltip(tooltip("feedbackMode"))
				.setSaveConsumer(v -> config.feedbackMode = v)
				.build());

		BooleanListEntry playSoundOnAutoStop = entryBuilder
				.startBooleanToggle(option("playSoundOnAutoStop"), config.playSoundOnAutoStop)
				.setDefaultValue(defaults.playSoundOnAutoStop)
				.setTooltip(tooltip("playSoundOnAutoStop"))
				.setSaveConsumer(v -> config.playSoundOnAutoStop = v)
				.build();
		general.addEntry(playSoundOnAutoStop);

		general.addEntry(entryBuilder
				.startStrField(option("autoStopSound"), config.autoStopSound)
				.setDefaultValue(defaults.autoStopSound)
				.setTooltip(tooltip("autoStopSound"))
				.setSaveConsumer(v -> config.autoStopSound = v)
				.setDisplayRequirement(Requirement.isTrue(playSoundOnAutoStop))
				.build());

		general.addEntry(entryBuilder
				.startBooleanToggle(option("resumeAfterManualReconnect"), config.resumeAfterManualReconnect)
				.setDefaultValue(defaults.resumeAfterManualReconnect)
				.setTooltip(tooltip("resumeAfterManualReconnect"))
				.setSaveConsumer(v -> config.resumeAfterManualReconnect = v)
				.build());

		// --- Auto-eat tab ---

		ConfigCategory autoEat = builder.getOrCreateCategory(category("autoeat"));

		BooleanListEntry autoEatEnabled = entryBuilder
				.startBooleanToggle(option("autoEatEnabled"), config.autoEatEnabled)
				.setDefaultValue(defaults.autoEatEnabled)
				.setTooltip(tooltip("autoEatEnabled"))
				.setSaveConsumer(v -> config.autoEatEnabled = v)
				.build();
		autoEat.addEntry(autoEatEnabled);

		BooleanListEntry autoEatSearchAnySlot = entryBuilder
				.startBooleanToggle(option("autoEatSearchAnySlot"), config.autoEatSearchAnySlot)
				.setDefaultValue(defaults.autoEatSearchAnySlot)
				.setTooltip(tooltip("autoEatSearchAnySlot"))
				.setSaveConsumer(v -> config.autoEatSearchAnySlot = v)
				.setDisplayRequirement(Requirement.isTrue(autoEatEnabled))
				.build();
		autoEat.addEntry(autoEatSearchAnySlot);

		autoEat.addEntry(entryBuilder
				.startIntField(option("autoEatSlot"), config.autoEatSlot)
				.setDefaultValue(defaults.autoEatSlot)
				.setTooltip(tooltip("autoEatSlot"))
				.setSaveConsumer(v -> config.autoEatSlot = v)
				.setDisplayRequirement(Requirement.all(
						Requirement.isTrue(autoEatEnabled),
						Requirement.isFalse(autoEatSearchAnySlot)))
				.build());

		autoEat.addEntry(entryBuilder
				.startIntField(option("autoEatHungerThreshold"), config.autoEatHungerThreshold)
				.setDefaultValue(defaults.autoEatHungerThreshold)
				.setTooltip(tooltip("autoEatHungerThreshold"))
				.setSaveConsumer(v -> config.autoEatHungerThreshold = v)
				.setDisplayRequirement(Requirement.isTrue(autoEatEnabled))
				.build());

		autoEat.addEntry(entryBuilder
				.startEnumSelector(option("autoEatAmountMode"), SmartAutoAttackConfig.AutoEatAmountMode.class, config.autoEatAmountMode)
				.setDefaultValue(defaults.autoEatAmountMode)
				.setTooltip(tooltip("autoEatAmountMode"))
				.setSaveConsumer(v -> config.autoEatAmountMode = v)
				.setDisplayRequirement(Requirement.isTrue(autoEatEnabled))
				.build());

		autoEat.addEntry(entryBuilder
				.startEnumSelector(option("foodSafetyPreset"), SmartAutoAttackConfig.FoodSafetyPreset.class, config.foodSafetyPreset)
				.setDefaultValue(defaults.foodSafetyPreset)
				.setTooltip(tooltip("foodSafetyPreset"))
				.setSaveConsumer(v -> config.foodSafetyPreset = v)
				.setDisplayRequirement(Requirement.isTrue(autoEatEnabled))
				.build());

		return builder.build();
	}
}

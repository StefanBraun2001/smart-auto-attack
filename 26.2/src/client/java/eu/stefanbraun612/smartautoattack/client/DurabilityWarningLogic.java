package eu.stefanbraun612.smartautoattack.client;

import eu.stefanbraun612.smartautoattack.client.config.SmartAutoAttackConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Always-on watchdog, independent of the mod's own enabled toggle: warns (by sound) when
 * the held item matches one of the user's keywords and is already below the Min
 * durability/% threshold that would make Auto Attack itself refuse to use it. Plays once
 * on equip, then loops (capped at twice a second) while the attack/mine key is held.
 */
public class DurabilityWarningLogic {
	private static final int SOUND_COOLDOWN_TICKS = 10; // 20 ticks/sec / 2 plays per sec

	// Tracked by slot + item identity, not the ItemStack itself, so durability ticking
	// down on the same held tool doesn't look like a fresh equip every tick.
	private static int lastEquipSlot = -1;
	private static Item lastEquipItem = null;
	private static int soundCooldownTicks = 0;

	public static void tick(Minecraft client) {
		SmartAutoAttackConfig config = AutoConfig.getConfigHolder(SmartAutoAttackConfig.class).getConfig();
		Player player = client.player;
		if (!config.durabilityWarningEnabled || player == null) {
			lastEquipSlot = -1;
			lastEquipItem = null;
			soundCooldownTicks = 0;
			return;
		}
		if (soundCooldownTicks > 0) {
			soundCooldownTicks--;
		}

		int selectedSlot = player.getInventory().getSelectedSlot();
		ItemStack held = player.getMainHandItem();
		boolean justEquipped = selectedSlot != lastEquipSlot || (held.isEmpty() ? null : held.getItem()) != lastEquipItem;
		lastEquipSlot = selectedSlot;
		lastEquipItem = held.isEmpty() ? null : held.getItem();

		if (!matchesAnyKeyword(held, config) || AutoAttackLogic.hasEnoughDurability(held, config)) {
			return;
		}

		if (justEquipped) {
			SoundUtil.play(client, config.durabilityWarningSound);
			soundCooldownTicks = SOUND_COOLDOWN_TICKS; // still starts the cooldown, so an immediate re-equip loop can't double-fire
		} else if (soundCooldownTicks <= 0 && client.options.keyAttack.isDown()) {
			SoundUtil.play(client, config.durabilityWarningSound);
			soundCooldownTicks = SOUND_COOLDOWN_TICKS;
		}
	}

	private static boolean matchesAnyKeyword(ItemStack stack, SmartAutoAttackConfig config) {
		for (String keyword : config.durabilityWarningKeywords) {
			if (AutoAttackLogic.matchesKeyword(stack, keyword)) {
				return true;
			}
		}
		return false;
	}
}

# Smart Auto Attack - Detailed Guide

Client-side Fabric mod for MC 26.2. Auto-attacks whatever's under your
crosshair, with conditions on top so it doesn't overflow farms, break your
sword, or hit things you didn't mean to.

**This mod cannot attack players at all** - see "Player targeting" below.

## Installation

1. Install Fabric Loader for MC 26.2.
2. Install **Fabric API**.
3. Install **Cloth Config API** (required - the config screen depends on it).
4. Optionally install **Mod Menu** for an in-game config entry point;
   without it, edit `config/smartautoattack.json` directly.
5. Drop the mod jar from [Releases](../../../releases) into your `mods/` folder.

## Toggling it on/off

Default key: **J** (rebind under Options > Controls > Key Binds > Smart
Auto Attack). Toggling sends a feedback message per the "Feedback message"
setting and resets all counters (hit count, elapsed duration, interval
timer, health-guard state), so re-enabling always starts fresh.

## Attack cadence

- **DEFAULT**: attacks whenever the vanilla attack cooldown is at 100% -
  same timing as a perfectly-timed manual left-click.
- **FIXED_INTERVAL**: ignores the vanilla cooldown, attacks once every X
  ticks/seconds instead.
- **RANDOM_INTERVAL**: same, but a random value between X and Y each time.

"Interval unit" switches the X/Y values between ticks (20/sec, default) and
seconds.

**Always fully charge** (default: on, FIXED/RANDOM interval only): also
waits for the vanilla cooldown to hit 100% before swinging even if the
interval elapsed first, avoiding weak/discharged hits from an interval
shorter than your weapon's real cooldown.

**Spears** are a special case: vanilla gives them an entirely different
attack mechanic (a piercing thrust), and an uncharged spear stab does
nothing at all rather than landing a weaker hit. The mod detects a spear
automatically and always forces the full-charge wait for it, regardless of
"Always fully charge".

## Night only, and Adjust to Creakings

- **Night only**: only attacks during the Creaking Heart's actual awake
  window (world time 12600-23400 - creakings wake up earlier than regular
  monsters), not the generic 13000-23000 night range. Sends a feedback
  message whenever it pauses/resumes so a forgotten toggle doesn't look
  like a bug.
- **Ignore night-only in Nether/End** (default: off): skips the night
  check entirely in dimensions with no real day-night cycle.
- **Freeze duration timer during day** (default: off): with Night only on,
  pauses the max-duration timer during the day so it only counts actual
  attacking time.
- **Adjust to Creakings** (default: off): while your crosshair is on a
  Creaking, temporarily overrides Attack cadence/Night only/Freeze-during-
  day/Nether-End to the ideal Creaking setup (100-tick fixed interval,
  night only, freeze during day, ignore Nether/End) for as long as the
  crosshair stays on it, then reverts. Your saved settings are never
  actually changed - only this tick's effective behavior is. Announces
  both when it kicks in and when it reverts.

  This reuses the exact same crosshair pick vanilla itself uses to decide
  what a real attack would hit, rather than a custom reach/distance check
  - simpler, and avoids false negatives in a Creaking's actual habitat
  (Pale Gardens are dense with leaves, which would otherwise block a
  line-of-sight check almost constantly).

## Stop conditions (stack - whichever hits first wins)

- **Max hits** (0 = unlimited).
- **Max duration** (empty = unlimited) - free text, e.g. `90m`, `1.5h`,
  `1h30m`, or a bare number of seconds.
- **Min durability** / **min durability %** (0 = disabled) - stops (or
  switches weapons, if Use more tools is on) before durability runs out.
- **Hunger safety stop** (default: on) / threshold (default: 6) -
  independent safety net on the 0-20 hunger scale, regardless of Auto-eat.
- **Health safety stop** (default: on) / threshold (default: 6) - see
  below, since this one has two very different modes of reacting.

**Play sound on auto-stop** (default: on) plays the configured sound
(default `minecraft:block.bell.use`) whenever the mod stops *itself* -
never on a manual toggle.

## Health safety: stop, or pause and recover

Health safety stop reacts once health drops below the threshold. What it
does depends on **Eat food to regenerate health**:

- **Off** (default): hard auto-disable, same as the hunger safety stop.
- **On**: instead, pauses everything (including all timers), force-feeds
  from your Auto-eat food source until hunger is full, then waits for
  health to climb 2 hearts above the threshold before resuming normally -
  re-topping-up hunger the whole time if it dips again. Clamped to your
  actual max health, so a high threshold (e.g. 18) can never target above
  what's reachable. Gives up and auto-stops if health hasn't recovered
  within 45 seconds. Needs an auto-eat food source configured (see below)
  - without one, it just waits out the timeout every time.

**Ignore hunger/health safety while regenerating** (default: off, one
toggle each): skips the respective safety check entirely while you have
the Regeneration effect (e.g. standing near a beacon).

**Paranoia switch** (default: off): overrides the regeneration-ignore
toggle specifically for the eat-to-recover path - even while regenerating,
the pause-and-eat cycle still triggers, so hunger never goes untended just
because health is being propped up by an effect that could end at any
time. Only has any effect with both Eat food to regenerate health and
Auto-eat on.

## Use more tools

Toggle (default: off) + **Tool rotation mode**:

- **KEYWORD** (default, keyword `sword`): substring match against the
  item ID, case-insensitive - matches any modded sword too.
- **SAME_TYPE**: any weapon in the same vanilla weapon-category tag
  (sword/axe/pickaxe/shovel/hoe/spear) as the one that just ran low,
  regardless of material.
- **EXACT_MATCH**: only the exact same item.

When the current weapon's durability guard would trigger, the mod scans
your hotbar (slots 1-9) for another match with enough durability left and
switches to it instead of stopping. Only stops once nothing usable remains
anywhere in the hotbar. Fighting bare-handed is left alone when this
toggle is off (punching is a legitimate, if slow, attack) - only once it's
on does an empty main hand count as "not enough".

## Target filter (blacklist/whitelist)

BLACKLIST (attack anything except the list) or WHITELIST (only attack
what's listed). Add full namespaced entity IDs via the **+** button, e.g.
`minecraft:villager`, `minecraft:drowned`, `minecraft:zombified_piglin`.

## Player targeting

This mod **cannot attack players**, full stop - not configurable, and the
blacklist/whitelist can't override it. This is a Modrinth Content Rules
requirement (3.3d: "automatic or assisted PvP combat" needs a genuine
server-side opt-in, which this project doesn't implement). The exclusion
is checked unconditionally, including in blind-swing mode (Require target
detected off) - no toggle combination brings player targeting back in this
build, and the mod won't even swing while your crosshair is on a player.

Past `_AP` ("attacks players") releases without this restriction remain on
the [Releases](../../../releases) page for historical use, but that variant
is no longer maintained. Read the warnings on those release notes before
using one - automated combat against other players is very likely to
violate a typical server's rules and get flagged by anti-cheat regardless
of what the mod is called.

## Auto-eat

**Auto-eat enabled** (default: on) is a master switch, separate from the
food source below - lets you disable eating without losing your
configuration (also what presets toggle, since they don't touch hotbar
slots).

**Search any hotbar slot** (default: off): off eats only from the
configured **Auto-eat hotbar slot** (1-9, 0 = disabled); on searches the
whole hotbar for the first eligible food instead, ignoring that slot.

**Auto-eat hunger threshold** (0-20, default 20): starts eating once
hunger drops below this.

**Auto-eat amount** decides how much gets eaten once triggered:

- **EAT_ONCE** (default): exactly one bite per dip below the threshold,
  then waits for hunger to rise back above it before eating again - even
  if that one bite wasn't enough to clear the threshold itself.
- **DONT_OVEREAT**: keeps eating bite after bite past the threshold, but
  stops the moment the next bite's nutrition would push hunger past the
  20-point cap, so nothing gets wasted.
- **FILL_HUNGER**: keeps eating bite after bite until hunger is fully at
  20, regardless of how much of a bite's nutrition would go to waste.

**Auto-eat food safety** guards against eating something regrettable while
AFK:

- **LIGHT** (default): blocks enchanted golden apple, pufferfish.
- **FOOD_INSPECTOR**: Light + rotten flesh, spider eye, raw chicken,
  poisonous potato.
- **RAT**: no extra blocks beyond the hardcoded ones below.

Cake and chorus fruit are **always** blocked regardless of preset (cake
can't be eaten via right-click; chorus fruit randomly teleports you).

## Reconnecting after a disconnect

**Resume after manual reconnect** (default: off) controls what happens if
you reconnect yourself (not through the separate
[Smart Auto Reconnect](https://github.com/StefanBraun2001/smart-auto-reconnect)
mod) after a disconnect while this mod was running. Off: turns itself off
rather than silently resuming unnoticed. On: resumes after a brief settle
buffer.

If Smart Auto Reconnect handles the reconnect for you after an
*involuntary* disconnect, this mod always resumes automatically once the
world finishes loading, regardless of the setting above.

## Presets

Named bundles of "technique" settings - never your hotbar slot, keybind,
or feedback style, since those depend on your own setup. Managed via
client-side commands (work on any server, no OP needed):

```
/smartautoattack preset list
/smartautoattack preset apply <name>
/smartautoattack preset save <name>
/smartautoattack preset delete <name>
```

Ships with four built-in presets - `Regular_TP_AEHP`, `Regular_MT_TP_AEHP`,
`Creaking_FT_TP_AEHP`, `Creaking_MT_FT_TP_AEHP` - see the repo README for
what each one sets.

## Troubleshooting

- **It won't attack a Creaking at all.** Adjust to Creakings only kicks in
  while your crosshair is actually on the Creaking - it's not an area
  check. Confirm Require target detected is on if you want it gated on a
  real target at all.
- **It keeps pausing to eat but never resumes.** Check an Auto-eat food
  source is actually configured (slot set, or Search any hotbar slot on)
  and that Eat food to regenerate health's resume target (threshold + 2
  hearts) is actually reachable given your max health.
- **The health/hunger safety stop never fires even at low health.** Check
  whether you have Regeneration and the matching "Ignore ... while
  regenerating" toggle on - that's an intentional bypass. Turn on the
  Paranoia switch if you still want food management to happen anyway.

## License

MIT - see [LICENSE](../LICENSE).

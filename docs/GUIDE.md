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

## Throttle

**Throttle** (default: off) alternates the mod between an attacking phase
and a paused phase, each lasting the configured **Attack for** / **Then
pause for** duration (same free-text format as Max duration, e.g. `90m`,
`1.5h`, `1h30m`). Enabling it always (re)starts on a fresh attack phase.
An unparseable or zero-length duration on either field disables the gate
entirely (attacks continuously) rather than risk getting stuck paused
forever.

**Freeze duration timer during throttle pause** (default: on): the
max-duration timer above doesn't advance while throttle is in its paused
phase, so it only counts actual attacking time. Turn it off if you want
the max-duration timer to keep counting through pauses too.

**Priority order: Adjust to Creakings > Throttle > Night only.**

- Throttle takes priority over Night only. Turning Throttle on disregards
  Night only completely - not just its own gate, but its two companion
  settings (Ignore night-only in Nether/End, Freeze duration timer during
  day) as well - as if Night only were off, regardless of what its toggle
  is actually set to. To make this obvious rather than silently doing
  nothing, the whole Night only cluster (the toggle and its two companion
  settings) **hides itself in the config screen** while Throttle is on.
  Turning Throttle back off makes it reappear with its previous value
  intact - nothing gets reset.
- Adjust to Creakings still overrides both. While your crosshair is
  actually on a Creaking, the Creaking preset forces Night only on
  regardless of Throttle or Night only's own toggle - so Throttle is
  ignored for as long as that override is active, then resumes normally
  the moment it isn't.

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

## Durability warning

An always-on watchdog, separate from everything else on this page - it
runs whether or not Auto Attack itself is toggled on, since its whole
point is catching you *manually* using a tool the mod would already
refuse to touch.

### Tool warning

Toggle (default: off) + **Warn for tools**: a list of keywords (e.g.
`pickaxe`, `axe`), matched the same way as **Use more tools**'s KEYWORD
mode - substring match against the item ID, case-insensitive. Checks
**both your main hand and offhand** independently. Whenever a held item
matches one of these keywords *and* its durability is already below the
**Min durability** / **Min durability %** threshold from the Safety tab
(the same values the auto-stop/rotation logic uses - there's no separate
threshold for this), the mod plays **Warning sound** (default
`minecraft:block.bell.use`, same free-text sound-event-ID format as
Auto-stop sound, and shared with Armor durability warning below):

- **Once**, the moment that item becomes held in that hand (switching to
  it, or its durability dropping below the threshold while already
  held).
- **Then repeatedly**, capped at twice a second, for as long as you keep
  holding down **either** attack/mine (left-click) **or** use
  (right-click) with it - so shearing a sheep, tilling dirt, or making
  farmland with a low-durability tool warns you too, not just breaking
  blocks or attacking.

### Armor warning

Separate toggle (default: off), **Armor durability warning**: checks all
four armor slots plus the elytra (which occupies the chest slot) for
durability - no keyword list, since any equipped armor piece counts
regardless of type. Uses the same Min durability/% threshold and Warning
sound as the tool warning above. Plays once the moment a piece drops
below the threshold or gets equipped already below it, then repeats
(capped at twice a second) for as long as it stays equipped and low -
there's no interaction key tied to wearing armor, so unlike the tool
warning this one isn't gated on attack/use being held.

### Mutual exclusion with Smart Auto Mine

Both toggles above are **independently** mutually exclusive with Smart
Auto Mine's equivalent features, if you have both mods installed -
enabling either one here while its counterpart in Smart Auto Mine is
already enabled shows an error on the toggle and blocks the config
screen's Save & Done, so the same low-durability item never triggers a
double warning from two mods at once. You can, however, have (for
example) the tool warning enabled here and the armor warning enabled in
Smart Auto Mine at the same time - only matching toggles conflict.

## Target filter (blacklist/whitelist)

BLACKLIST (attack anything except the list) or WHITELIST (only attack
what's listed). Add full namespaced entity IDs via the **+** button, e.g.
`minecraft:villager`, `minecraft:drowned`, `minecraft:zombified_piglin`.

**Exclude boats/minecarts** (default: off, BLACKLIST mode only - hidden
under WHITELIST): blocks every boat/raft/chest-boat/chest-raft and every
minecart variant (storage/furnace/TNT/hopper/command-block/spawner),
without needing to list each one individually. Uses an actual type check
rather than a vanilla tag, since vanilla's own "boat" tag omits chest
boats/rafts and there's no vanilla minecart tag at all - both would
silently under-cover if this used tags instead.

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
or feedback style, since those depend on your own setup. Managed from the
**Presets** tab, not commands - it's the one tab that reads live (a text
line lists every currently saved preset by name) but only *acts* when you
press the config screen's own **Save & Done**, since Cloth Config has no
clickable-button entries to act on immediately:

- **Apply preset**: type an exact saved name, then Save & Done. Overwrites
  whatever you changed elsewhere on the same screen, since it applies
  last, right before the screen actually saves.
- **Save current settings as**: type a name, then Save & Done - saves the
  full current settings (including anything else you changed on the same
  screen) under that name, creating it or overwriting an existing preset
  of the same name.
- **Delete preset**: type an exact saved name, then Save & Done.

All three are independent and optional - leave any of them blank to skip
that action. If you fill in more than one at once, apply runs first, then
save, then delete, so e.g. typing the same name into both Apply and Save
just re-saves that preset's own values back under itself (a no-op).

Ships with four built-in presets:

- **Regular_TP_AEHP**: general-purpose weapon protection (durability floor
  10 / 5%) plus auto-eat with hunger-safety protection (eat below 7,
  hard-stop below 3). No night restriction, default cadence, no tool
  rotation.
- **Regular_MT_TP_AEHP**: same as above, plus "Use more tools" tool
  rotation (keyword `sword`).
- **Creaking_FT_TP_AEHP**: same protection as Regular_TP_AEHP, plus night
  only with the duration timer frozen during the day, and a fixed
  100-tick (5s) attack interval - Creakings only regenerate resin at
  their heart once every 5 seconds, so hitting faster wastes swings.
- **Creaking_MT_FT_TP_AEHP**: same as Creaking_FT_TP_AEHP, plus "Use more
  tools" tool rotation.

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

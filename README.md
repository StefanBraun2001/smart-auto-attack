# Smart Auto Attack

Client-side Fabric mod. Auto-attacks whatever's under your crosshair, with
conditions on top so it doesn't overflow farms, break your sword, or hit
things you didn't mean to.

Current build: **B0.5** (beta, tested), **MC 26.2 only** (1.20.4 support
was dropped as of A0.4). Grab a built jar from the
[Releases](../../releases) page, or build from source with
`./gradlew build` inside `26.2/`. See [docs/GUIDE.md](docs/GUIDE.md) for
a full walkthrough of every setting.

**This mod cannot attack players** (see "Player targeting" below) - this was
done to comply with ModRinth's rules. Even after removal from ModRinth,
there is no plan to restore the ability to attack players, as this mod
isn't designed as a PVP aid, but rather a tool to make AFK-sessions at
mob farms easier and safer to use.

**GitHub-only, not on Modrinth.** A subsequent, silent change to
Modrinth's rules disqualifies this mod from that platform, so GitHub
Releases is the only distribution channel going forward.

## Install

Needs Fabric Loader + **Fabric API**. Also install **Cloth Config API**
(required) and, optionally, **Mod Menu**.

## Features

- Toggle keybind (default **J**), Cloth Config screen + Mod Menu entry.
- Attack cadence: vanilla-cooldown-timed (default), fixed interval, or
  random interval, with an "always fully charge" safeguard against weak/
  discharged hits.
- Spear support: correctly triggers the spear's piercing thrust (vanilla
  uses a different attack path for it than every other weapon) and always
  waits for a full attack-cooldown charge first, since an uncharged spear
  stab does nothing at all rather than just landing a weaker hit.
- Night-only mode, timed to the Creaking Heart's actual awake window
  (world time 12600-23400, not the generic 13000-23000 night range), with
  Nether/End and duration-freeze options, plus a feedback message whenever
  it pauses/resumes so a forgotten toggle doesn't look like a bug.
- **Adjust to Creakings**: while your crosshair is on a Creaking, temporarily
  switches to the ideal Creaking setup (100-tick interval, night only,
  ignoring Nether/End day-night) without touching your saved settings, then
  reverts the moment it isn't. Announces both transitions.
- Stop conditions: max hits, max duration, min durability (absolute/%),
  hunger safety stop, health safety stop.
- Health safety stop can either hard-disable (default) or, with **Eat food
  to regenerate health** on, pause everything (including all timers),
  force-feed until hunger is full, and wait for health to climb 2 hearts
  above the threshold before resuming - only giving up after 45 seconds.
  Both hunger and health safety can also be set to ignore themselves
  entirely while you have Regeneration (e.g. near a beacon); a **Paranoia
  switch** overrides that specifically for the eat-to-recover path, so
  hunger never goes untended even while regenerating.
- "Use more tools": rotates to another hotbar weapon when the current one's
  durability guard trips - by keyword (default `sword`), by weapon
  category (any material), or requiring an exact item match.
- Entity blacklist/whitelist (players excluded unconditionally - see below).
- Auto-eat with a configurable hunger threshold, either from one fixed
  slot or the first eligible food anywhere in the hotbar, and a choice of
  how much to eat per trigger: one bite, as much as won't waste nutrition
  past a full bar, or straight to full regardless of waste.
- **Presets**: named bundles of duration/durability/tool-rotation/cadence/
  night-only/auto-eat settings, managed via client-side commands
  (`/smartautoattack preset list|apply|save|delete`). Ships with
  `Regular_TP_AEHP`, `Regular_MT_TP_AEHP`, `Creaking_FT_TP_AEHP`, and
  `Creaking_MT_FT_TP_AEHP` - see the bundled README for what each one sets.
- Auto-resumes after a reconnect handled by the separate
  [Smart Auto Reconnect](https://github.com/StefanBraun2001/smart-auto-reconnect)
  mod; optional "resume after manual reconnect" toggle for reconnects you
  initiate yourself.
- Auto-stop sound feedback.

Full feature/config documentation lives in the bundled README shipped
alongside the jars.

## Player targeting

This mod cannot attack players under any configuration -
the exclusion is hardcoded and checked unconditionally (including in
blind-swing mode, where blacklist/whitelist would otherwise be bypassed
entirely). This restriction was originally required by Modrinth Content
Rules 3.3d ("automatic or assisted PvP combat" needs a genuine server-side
opt-in, which this project doesn't implement) and remains in place in this
build.

An **uncensored** variant without this restriction was previously
maintained as a separate `_AP` ("attacks players") build. Past releases
tagged `_AP` remain available on the [Releases](../../releases) page, but
that variant is no longer actively maintained or updated - only the build
above continues to receive new features. Use any
existing `_AP` build only on servers you own or have explicit permission
on; automated combat against other players is very likely to violate a
typical server's rules and get flagged by anti-cheat regardless of what
the mod is called.

## Building from source

```
cd 26.2
./gradlew build
```

Built jar lands in `26.2/build/libs/`. Needs JDK 25.

## Credits

Feature set inspired by Toro Bolin's Auto Attack mod - this is an
independent reimplementation (no shared code), built from scratch with
extra safety/scheduling features layered on top.

## AI disclosure

This mod's code was generated by Claude (Sonnet 5, medium reasoning
effort), based on feature requests and iterative in-game testing feedback
from the repository owner.

## License

MIT - see [LICENSE](LICENSE).

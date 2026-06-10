# Shadow Village Defense

A ninja-themed tower defense game for Android. Defend the hidden Moonshade
Village from waves of rogue ninjas, beasts, and oni warlords using five
elemental Ki adepts. All names, characters, and art are original; the art is
100% code-drawn vector graphics (no bitmap assets).

## Gameplay

- **5 elemental towers**, each with a distinct mechanic and 3 upgrade tiers:
  - **Flamecaller** (Fire) - burns enemies over time
  - **Tidebinder** (Water) - slows enemies
  - **Stormweaver** (Lightning) - chains between grouped enemies
  - **Galeblade** (Wind) - rapid shuriken + periodic knockback; great anti-air
  - **Stonefist** (Earth) - heavy splash damage, but cannot hit flyers
- **8 enemy types**: scouts, ninjas, shadow wolves, armored brutes, flying sky
  raiders, regenerating mistmenders, and two oni bosses
- **2 maps** (River Crossing, Twin Gates), **20 waves** each
- Gold economy: bounties, wave-clear bonuses, 70% sell refund
- 4 targeting modes per tower (First / Last / Strong / Near)
- Pause and 2x speed

Tap a grass tile to build, tap a tower to upgrade/sell/retarget, and send
waves when you're ready.

## Project layout

- **`:core`** - the entire game simulation in pure Kotlin/JVM (no Android
  dependencies): maps, pathing, towers, enemies, waves, economy, win/loss.
  Fully covered by JUnit tests, including deterministic full-game autoplay
  balance tests that prove both maps are winnable.
- **`:app`** - the Android shell: SurfaceView + fixed-timestep game loop,
  Canvas vector rendering with a sprite cache, in-canvas HUD and menus.

`:app` is only included in the build when an Android SDK is available, so
`./gradlew :core:test` works on any JVM machine.

## Building

```bash
# without an Android SDK: run the simulation test suite
./gradlew :core:test

# with an Android SDK (or run scripts/setup-android-sdk.sh first):
./gradlew :app:assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
```

Install on a device with `adb install app-debug.apk` (or sideload it).
Requires Android 7.0+ (API 24).

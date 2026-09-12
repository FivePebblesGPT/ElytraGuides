# Elytra Guides

Client-side Fabric HUD guides for executing precise Elytra pitch maneuvers in Minecraft 26.2.

## Default maneuver

Minecraft pitch uses negative values for looking upward and positive values for looking downward.

- Hold the downward approach around **+32.5°**.
- Snap upward through **-49.0°**.
- Crossing -49° resets a tracking circle to -49°.
- The circle then moves downward toward +32.5° at **10°/second at 20 TPS** (0.5° per tick), with optional effective-TPS compensation.
- The tracking circle changes to the configured on-target color when your pitch is within the tolerance.

The +32.5° and -49° guides are short centered pitch bars. They move vertically on screen as your pitch changes, so the center/crosshair intersects a bar at its exact configured pitch.

## Toggle key

Press **G** by default to toggle Elytra Guides at runtime. The keybind is configurable under Minecraft's Controls menu.

The runtime toggle controls the complete feature set: pitch guides, tracking target, altitude logging, and custom crosshair. The owo-config `enabled` option remains the persistent master switch.

## Crosshair

The custom crosshair has two modes:

- Vanilla; or
- a 2x2 center dot.

The default is **Dot**, active **only while Elytra flying**.

## Altitude analytics

Altitude segments are based on **vertical-speed sign changes**, not sampled all-time maxima:

- positive -> negative vertical speed: local peak / end of ascent;
- negative -> positive vertical speed: local trough / end of descent.

The zero-speed altitude is interpolated between the surrounding samples. Each ascent is measured from the immediately previous trough (or flight start), and each descent from the immediately previous peak (or flight start).

Turn-point messages can go to a small HUD toast, chat, both, or nowhere. An optional flight-end summary reports the flight's maximum Y and cumulative positive altitude gain.

## Configuration

Configuration is provided by **owo-config**. If Mod Menu is installed, Elytra Guides automatically exposes its owo config screen from the Mod Menu configuration button.

Important settings include:

- approach/snap pitch;
- return rate and TPS compensation;
- target tolerance and guide dimensions/colors;
- crosshair style/color and whether it is flight-only;
- peak/trough logging and output destination;
- flight-end summary;
- vertical-speed deadzone and HUD-toast duration.

### TPS compensation

Minecraft 26.2 routinely synchronizes server game time every 20 server ticks. Elytra Guides samples that packet cadence and smooths several samples to estimate effective remote-server TPS. Client tick spacing is used as a fallback until enough server samples are available.

This is intentionally an **effective** TPS estimate rather than a privileged server metric: packet latency/jitter can affect individual samples, which is why the estimator uses a multi-packet window. With compensation enabled, the nominal 0.5°/tick target motion remains approximately tied to server tick progress during slowdowns instead of blindly staying at 10°/real-time second.

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.5+
- Fabric API 0.160.0+26.2
- owo-lib 0.13.1+26.2
- Java 25
- Mod Menu is optional

## Building

The project targets Gradle 9.5.1 (the version used by Fabric's 26.2 example project).

```bash
gradle build copyDistributionJar
```

The CI/package-ready jar is copied to `build/distribution/`.

A GitHub Actions workflow also builds every push and pull request with Java 25 and Gradle 9.5.1.

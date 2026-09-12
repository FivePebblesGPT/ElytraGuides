# Elytra Guides

Client-side Fabric HUD guides for executing precise Elytra pitch maneuvers in Minecraft 26.2.

## Default maneuver

- Hold the approach around **-32.5°**.
- Snap through **+49.0°**.
- Crossing +49° resets a tracking circle to +49°.
- The circle then returns toward -32.5° at **10°/second at 20 TPS** (0.5° per tick), with optional effective-TPS compensation.
- The tracking circle changes to the configured on-target color when your pitch is within the tolerance.

The -32.5° and +49° guides are short centered pitch bars. They move vertically on screen as your pitch changes, so the center/crosshair intersects a bar at its exact configured pitch.

## Crosshair

The vanilla crosshair can be replaced with either:

- a 2 px thick thin cross with a 2x2 center gap; or
- a 2x2 center dot.

The replacement can be global or limited to Elytra flight.

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
- crosshair style/color/arm length;
- peak/trough logging and output destination;
- flight-end summary;
- vertical-speed deadzone and HUD-toast duration.

### TPS compensation note

The mod estimates effective TPS from client simulation tick spacing. This catches local/integrated-server and client tick slowdowns. A vanilla client cannot directly know a remote dedicated server's authoritative TPS without server support, so remote-server TPS cannot be measured exactly.

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
gradle build
```

The built jar is written to `build/libs/`.

A GitHub Actions workflow also builds every push and pull request with Java 25 and Gradle 9.5.1.

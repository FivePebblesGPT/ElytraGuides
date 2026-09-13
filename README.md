# Elytra Guides

Client-side Fabric HUD guides for executing precise Elytra pitch maneuvers in Minecraft 26.2.

## Default maneuver

Minecraft pitch uses negative values for looking upward and positive values for looking downward.

- Hold the downward approach around **+32.5°**.
- Snap upward through **-49.0°**.
- Crossing -49° resets a two-bar tracking target to -49°.
- The target then moves downward toward +32.5° at **10°/second at 20 TPS** (0.5° per tick), with optional effective-TPS compensation.
- The two tracking bars change to the configured on-target color when your pitch is within the tolerance.

The +32.5° and -49° guides are short centered pitch bars. They move vertically on screen as your pitch changes, so the center/crosshair intersects a bar at its exact configured pitch.

A separate horizontal-drift guide tracks the circular-average yaw from the previous **2 seconds**. It draws a horizontal pointer from the current heading toward that rolling average, making unwanted left/right mouse movement easier to notice while executing the maneuver.

## Speed guidance

A compact bar speedometer is shown near the crosshair while Elytra flying. It displays horizontal and vertical speed with **two decimal places** and fills according to horizontal speed.

The default tested optimal envelope is:

- horizontal speed: **42.00–43.00 m/s**;
- vertical speed: **-7.54 ± 0.75 m/s**.

The horizontal optimal band is marked on the speedometer. When both horizontal and vertical speed are inside the configured envelope, the bar switches to the lime on-target color, flashes, and shows upward arrows around the speed readout. Entering the envelope also sends an optional notification such as `Optimal speed • H 42.56 m/s • V -7.48 m/s`.

The horizontal range, vertical target/tolerance, speedometer visibility/color, notification toggle, and notification destination are configurable. Notification hysteresis prevents rapid repeated toasts when speed jitters around the boundary.

## Toggle key

Press **G** by default to toggle Elytra Guides at runtime. The same key mapping can be rebound from either:

- Minecraft's Controls menu; or
- the Elytra Guides owo-config screen, using its inline Toggle key control.

Both interfaces edit the same Minecraft key mapping, so the binding stays synchronized and is saved in the normal Minecraft controls configuration.

The runtime toggle controls the complete feature set: pitch guides, tracking target, horizontal-drift history, speed guidance, altitude logging, and custom crosshair. The owo-config `enabled` option remains the persistent master switch.

## Crosshair

Elytra Guides can replace the vanilla crosshair with a small **2x2 center dot**. The dot is enabled by default and, by default, only replaces the crosshair while Elytra flying.

Disable **Use dot crosshair** to keep the vanilla crosshair. The previous Thin Crosshair mode has been removed.

## Altitude analytics

Turning points are detected from **vertical-speed sign changes** and the zero-speed altitude is interpolated between the surrounding samples.

The primary cycle metric is **peak-to-peak gain**:

- first detected peak establishes the baseline;
- each later peak reports `current peak Y - previous peak Y`;
- positive values mean the tech gained elevation that cycle, negative values mean it lost elevation.

The gain value is color graded from **red at -10 blocks or worse**, through **white at 0**, to the configured **lime on-target color at +10 blocks or better**.

Amplitude is kept separate from gain. When **Show cycle amplitude** is enabled, the same completed-cycle notification also shows:

- `↓` previous peak to trough distance; and
- `↑` trough to current peak distance.

For example: `Cycle gain +4.25 blocks • amplitude ↓36.10 / ↑40.35 • peak Y 128.70`.

This deliberately avoids calling trough-to-peak ascent distance "gain". The flight-end summary reports maximum Y, net peak-to-peak gain, accumulated **horizontal** path distance, average horizontal speed, and elapsed flight time. Horizontal path distance is accumulated sample-by-sample, so flying 1 km north and then 1 km south contributes 2 km rather than cancelling back to zero displacement. Vertical travel is not included in the distance field.

Cycle messages can go to a HUD toast, chat, both, or nowhere. HUD toasts use a translucent background, remain visible for at least 5 seconds, and fade smoothly rather than disappearing abruptly.

## Configuration

Configuration is provided by **owo-config**. If Mod Menu is installed, Elytra Guides exposes a custom owo config screen from the Mod Menu configuration button, including the inline toggle-key rebinder.

Important settings include:

- approach/snap pitch;
- return rate and TPS compensation;
- target tolerance and guide dimensions/colors;
- two-second horizontal-drift guide and color;
- speedometer and optimal horizontal/vertical speed envelope;
- optimal-speed notification and output destination;
- dot crosshair/color and whether it is flight-only;
- toggle key binding;
- cycle-gain logging, amplitude display, and output destination;
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

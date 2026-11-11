# DamageOverlay

A lightweight, client-side damage popup overlay for NeoForge. It renders floating damage numbers in the world at the position you specify, complete with a configurable rise trajectory, fade-out animation, and a small API that other mods can call.

> **Client-side only.** No server-side component, no gameplay changes, no mixins.

## Features

- Floating damage text anchored to any world position
- Smooth rise + fade-out animation
- Random horizontal spread and slope per popup, so multiple hits on one target don't stack on top of each other
- Fully configurable via the in-game config screen
- Tiny public API (`DamageApi`) for other mods to trigger popups

## API Reference

Add DamageOverlay as an **optional** dependency, then call the API. It is safe to guard with `ModList.get().isLoaded("damageoverlay")` if you want a hard fallback.

### Entry point

```java
DamageApi api = DamageApi.get();
```

### Spawning a popup

All overloads delegate to the single abstract method:

```java
void popup(double damageAmount, double x, double y, double z, PopupOptions options);
```

Convenience overloads:

| Overload | Notes |
| --- | --- |
| `popup(amount, int x, int y, int z)` | Block position, anchored at `(x + 0.5, y + 1.0, z + 0.5)` |
| `popup(amount, int x, int y, int z, int color)` | Same, with an explicit colour |
| `popup(amount, int x, int y, int z, PopupOptions options)` | Same, with full options |
| `popup(amount, double x, double y, double z)` | Exact world position, default options |
| `popup(amount, double x, double y, double z, int color)` | Exact position, custom colour |

Example:

```java
DamageApi.get().popup(12.5, entity.getX(), entity.getY() + 1.0, entity.getZ());
```

Calls are thread-safe: they are marshalled onto the client thread internally.

### `PopupOptions`

An immutable record with builder-style copies:

```java
record PopupOptions(int color, float scale, long lifetimeMs, float riseSpeed, boolean appendExclamationMark)
```

- `color` — 0xRRGGBB; **the alpha channel is ignored** (opacity is driven by the fade-out).
- `scale` — text scale multiplier (default `1.0`).
- `lifetimeMs` — total time on screen including the fade, clamped to at least `1`.
- `riseSpeed` — GUI pixels per second of vertical travel.
- `appendExclamationMark` — when `true`, a `!` is appended to the formatted damage value.

Factory and helpers:

```java
PopupOptions.defaults()          // reads the current config values
.withColor(int color)
.withScale(float scale)
.withLifetime(long lifetimeMs)
.withRiseSpeed(float riseSpeed)
.withAppendExclamationMark(boolean appendExclamationMark)
```

Example:

```java
DamageApi.get().popup(42.0, x, y, z,
    PopupOptions.defaults()
        .withColor(0xFFAA00)
        .withScale(1.5F)
        .withLifetime(1000L)
        .withAppendExclamationMark(true));
```

## Configuration

| Key | Type | Default | Description |
| --- | --- | --- | --- |
| `DefaultColor` | int (`0xRRGGBB`) | `0xFF5555` | Text colour used when the caller doesn't supply one. Alpha is ignored. |
| `DropShadow` | boolean | `true` | Draw a drop shadow behind the text. Cheaper to disable on bright backgrounds. |
| `Lifetime` | long (ms) | `1500` | Total time a popup stays on screen, fade included. |
| `RisePixels` | double | `20.0` | Total vertical distance (GUI pixels) a popup travels over its lifetime. |
| `Spread` | double (blocks) | `0.4` | Random horizontal offset applied at spawn so simultaneous hits don't overlap exactly. Set to `0` to disable. |
| `MinSlope` | double | `0.1` | Lower bound of the random slope `k`. |
| `MaxSlope` | double | `10.0` | Upper bound of the random slope `k`; must be strictly greater than `MinSlope`. |

The default `riseSpeed` is derived from `RisePixels` and `Lifetime`:

```
riseSpeed = RisePixels * 1000 / Lifetime   // px per second
```

## How it works

Each popup stores its world position, a random slope `k` in `[MinSlope, MaxSlope]`, and a spawn timestamp. Every frame the renderer:

1. Projects the world position through the level camera's model-view and projection matrices.
2. Computes `age = (now - createdAt) / lifetimeMs`.
3. Moves the text along a straight trajectory where horizontal travel = vertical travel / `k` — so small slopes drift sideways while large slopes rise almost vertically.
4. Fades the alpha from `255` to `0` over the lifetime.
5. Skips anything that lands outside the GUI bounds.

Popups are added and expired on the client thread, so the list is only ever mutated where it's safe to do so.
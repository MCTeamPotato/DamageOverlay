package me.kall.damageoverlay.api;

import me.kall.damageoverlay.DamageOverlay;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record PopupOptions(int color, float scale, long lifetimeMs, float riseSpeed) {
    public PopupOptions {
        lifetimeMs = Math.max(1L, lifetimeMs);
    }

    public static @NotNull PopupOptions defaults() {
        int color = DamageOverlay.DEFAULT_COLOR.getAsInt();
        long lifetime = DamageOverlay.LIFETIME.getAsLong();
        float risePixels = DamageOverlay.RISE_PIXELS.get().floatValue();
        return new PopupOptions(color, 1.0F, lifetime, risePixels * 1000.0F / (float) lifetime);
    }

    @Contract(value = "_ -> new", pure = true)
    public @NotNull PopupOptions withColor(int color) {
        return new PopupOptions(color, this.scale, this.lifetimeMs, this.riseSpeed);
    }

    @Contract(value = "_ -> new", pure = true)
    public @NotNull PopupOptions withScale(float scale) {
        return new PopupOptions(this.color, scale, this.lifetimeMs, this.riseSpeed);
    }

    @Contract(value = "_ -> new", pure = true)
    public @NotNull PopupOptions withLifetime(long lifetimeMs) {
        return new PopupOptions(this.color, this.scale, lifetimeMs, this.riseSpeed);
    }

    @Contract(value = "_ -> new", pure = true)
    public @NotNull PopupOptions withRiseSpeed(float riseSpeed) {
        return new PopupOptions(this.color, this.scale, this.lifetimeMs, riseSpeed);
    }
}

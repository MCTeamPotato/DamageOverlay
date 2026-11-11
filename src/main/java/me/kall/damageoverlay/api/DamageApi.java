package me.kall.damageoverlay.api;

import me.kall.damageoverlay.impl.DamageRenderer;

public interface DamageApi {
    static DamageApi get() {
        return DamageRenderer.INSTANCE;
    }

    default void popup(double damageAmount, int x, int y, int z) {
        this.popup(damageAmount, (double) x + 0.5D, (double) y + 1.0D, (double) z + 0.5D);
    }

    default void popup(double damageAmount, int x, int y, int z, int color) {
        this.popup(damageAmount, (double) x + 0.5D, (double) y + 1.0D, (double) z + 0.5D, color);
    }

    default void popup(double damageAmount, int x, int y, int z, PopupOptions options) {
        this.popup(damageAmount, (double) x + 0.5D, (double) y + 1.0D, (double) z + 0.5D, options);
    }

    default void popup(double damageAmount, double x, double y, double z) {
        this.popup(damageAmount, x, y, z, PopupOptions.defaults());
    }

    default void popup(double damageAmount, double x, double y, double z, int color) {
        this.popup(damageAmount, x, y, z, PopupOptions.defaults().withColor(color));
    }

    void popup(double damageAmount, double x, double y, double z, PopupOptions options);
}

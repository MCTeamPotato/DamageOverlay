package me.kall.damageoverlay;

import me.kall.damageoverlay.impl.DamageRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.NotNull;

@Mod(DamageOverlay.MOD_ID)
public final class DamageOverlay {
    public static final String MOD_ID = "damageoverlay";

    private static final ModConfigSpec CONFIG;

    public static final ModConfigSpec.IntValue DEFAULT_COLOR;
    public static final ModConfigSpec.BooleanValue DROP_SHADOW;
    public static final ModConfigSpec.LongValue LIFETIME;
    public static final ModConfigSpec.DoubleValue RISE_PIXELS, SPREAD, MIN_SLOPE, MAX_SLOPE;

    public DamageOverlay(IEventBus modBus, @NotNull Dist dist, ModContainer container) {
        if (dist.isClient()) {
            container.registerConfig(ModConfig.Type.CLIENT, CONFIG);
            container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

            modBus.addListener(DamageRenderer::setup);
        }
    }

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.translation("damageoverlay.config.section")
                .comment("Settings for the damage popup overlay.")
                .push("DamageOverlay");
        DEFAULT_COLOR = builder
                .comment("Colour of the damage text as 0xRRGGBB, used when the caller does not pass its own colour.",
                        "The alpha channel is ignored: opacity is driven by the fade-out animation.")
                .translation("damageoverlay.config.default_color")
                .defineInRange("DefaultColor", 0xFF5555, Integer.MIN_VALUE, Integer.MAX_VALUE);
        DROP_SHADOW = builder
                .comment("Whether the damage text is drawn with a drop shadow.",
                        "Turning this off makes the text harder to read against bright backgrounds, but slightly cheaper to render.")
                .translation("damageoverlay.config.drop_shadow")
                .define("DropShadow", true);
        LIFETIME = builder
                .comment("How long a popup stays on screen, in milliseconds, fade-out included.")
                .translation("damageoverlay.config.lifetime")
                .defineInRange("Lifetime", 1500L, 1L, Long.MAX_VALUE);
        RISE_PIXELS = builder
                .comment("Total vertical distance, in GUI pixels, a popup travels from its origin over its whole lifetime.")
                .translation("damageoverlay.config.rise_pixels")
                .defineInRange("RisePixels", 20.0D, 0.0D, Double.MAX_VALUE);
        SPREAD = builder
                .comment("Random horizontal offset applied at spawn, in blocks, so several hits on the same block do not overlap exactly.",
                        "Set to 0 to always spawn at the exact origin.")
                .translation("damageoverlay.config.spread")
                .defineInRange("Spread", 0.4D, 0.0D, Double.MAX_VALUE);
        MIN_SLOPE = builder
                .comment("Lower bound of the random slope k used by the popup trajectory y = kx.",
                        "Both bounds must stay positive to keep the motion in the first quadrant.")
                .translation("damageoverlay.config.min_slope")
                .defineInRange("MinSlope", 0.1D, 0D, Double.MAX_VALUE);
        MAX_SLOPE = builder
                .comment("Upper bound of the random slope k.",
                        "Smaller slopes drift further sideways, larger slopes rise more vertically.",
                        "Must be strictly greater than MinSlope.")
                .translation("damageoverlay.config.max_slope")
                .defineInRange("MaxSlope", 10D, 0D, Double.MAX_VALUE);
        builder.pop();
        CONFIG = builder.build();
    }
}

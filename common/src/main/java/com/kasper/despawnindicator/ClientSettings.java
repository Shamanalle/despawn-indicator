package com.kasper.despawnindicator;

import java.util.Properties;

/** What a player can set on their client, in config/despawnindicator-client.properties. */
public final class ClientSettings {

    public static final String FILE = "despawnindicator-client.properties";

    /** How an item, and the death loot timer, warn that time is running out. */
    public enum Style {
        /** Blinks, faster towards the end. */
        BLINK,
        /** Items shrink and grow back, the death loot timer fades in and out. */
        FADE,
        /** Never blinks: items get their time label, the timer only turns red. */
        LABEL
    }

    /** When items show their time label. */
    public enum Labels {
        LOOK_SNEAK, LOOK, SNEAK, NEVER;

        public boolean look() {
            return this == LOOK_SNEAK || this == LOOK;
        }

        public boolean sneak() {
            return this == LOOK_SNEAK || this == SNEAK;
        }
    }

    public static final int MIN_WARN = 5;
    public static final int MAX_WARN = 120;

    public static final String DEFAULTS = ""
            + "# Despawn Indicator, client settings. Changes apply within a second, no restart needed.\n"
            + "\n"
            + "# The timer for the items you dropped when you died: true or false.\n"
            + "death_hud=true\n"
            + "\n"
            + "# How many seconds before it despawns an item starts to warn (" + MIN_WARN + " - " + MAX_WARN + ").\n"
            + "warn_seconds=20\n"
            + "\n"
            + "# How it warns: blink, fade (the item shrinks and grows back, the death loot timer fades in and out)\n"
            + "# or label (no blinking: the item shows its time).\n"
            + "style=blink\n"
            + "\n"
            + "# When items show their time: look_sneak (the item you look at, and all around you while\n"
            + "# sneaking), look, sneak or never.\n"
            + "labels=look_sneak\n"
            + "\n"
            + "# A sound when the death loot timer reaches one minute and 15 seconds: true or false.\n"
            + "sound=false\n";

    public static final ClientSettings DEFAULT = parse(ConfigFile.parse(DEFAULTS));

    public final boolean deathHud;
    public final int warnSeconds;
    public final Style style;
    public final Labels labels;
    public final boolean sound;

    private ClientSettings(boolean deathHud, int warnSeconds, Style style, Labels labels, boolean sound) {
        this.deathHud = deathHud;
        this.warnSeconds = warnSeconds;
        this.style = style;
        this.labels = labels;
        this.sound = sound;
    }

    /** Settings from the file; anything missing or unreadable keeps its default. */
    public static ClientSettings parse(Properties p) {
        return new ClientSettings(
                ConfigFile.bool(p, "death_hud", true),
                ConfigFile.integer(p, "warn_seconds", 20, MIN_WARN, MAX_WARN),
                ConfigFile.choice(p, "style", Style.BLINK),
                ConfigFile.choice(p, "labels", Labels.LOOK_SNEAK),
                ConfigFile.bool(p, "sound", false));
    }
}

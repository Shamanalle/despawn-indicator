package com.kasper.despawnindicator;

/**
 * Names shared by every side and loader: the mod id, the plugin-messaging channels and the vanilla item
 * lifetime. The Bukkit plugin uses the same channels and bytes as the mod.
 */
public final class DespawnIndicator {

    /** Mod id on every loader, the channel namespace and the folder of the language files. */
    public static final String MOD_ID = "despawnindicator";
    public static final String NAME = "Despawn Indicator";

    /** Client to server, once after joining: "I have the mod" ({@link HelloMessage}). */
    public static final String CHANNEL_HELLO = "hello";
    /** Server to client: despawn clocks of items the player sees ({@link ClockMessage}). */
    public static final String CHANNEL_CLOCKS = "clocks";

    /** Ticks a dropped item lies on the ground before it despawns in vanilla (5 minutes). */
    public static final int VANILLA_LIFETIME = 6000;
    /** The age Minecraft gives an item that never despawns ({@code ItemEntity#setUnlimitedLifetime}). */
    public static final int UNLIMITED_AGE = -32768;
    public static final int TICKS_PER_SECOND = 20;

    private DespawnIndicator() {
    }

    /** "despawnindicator:clocks". */
    public static String channel(String path) {
        return MOD_ID + ":" + path;
    }
}

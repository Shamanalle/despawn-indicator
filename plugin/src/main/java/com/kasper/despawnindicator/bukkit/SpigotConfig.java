package com.kasper.despawnindicator.bukkit;

import com.kasper.despawnindicator.DespawnIndicator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * spigot.yml, which Spigot, Paper and Folia all read: how long items lie and how far players see them, per
 * world ({@code world-settings.<world>}, else {@code world-settings.default}).
 */
final class SpigotConfig {

    /** Spigot's default tracking range of items ("misc" entities), in blocks. */
    static final int MISC_TRACKING_RANGE = 32;

    private static volatile YamlConfiguration config;

    private SpigotConfig() {
    }

    /** {@code item-despawn-rate}: the age at which items despawn in {@code world} on Spigot, in ticks. */
    static int itemDespawnRate(World world) {
        return value(world, "item-despawn-rate", DespawnIndicator.VANILLA_LIFETIME);
    }

    /** {@code entity-tracking-range.misc}: how far from a player items are shown to them, in blocks. */
    static int trackingRange(World world) {
        return value(world, "entity-tracking-range.misc", MISC_TRACKING_RANGE);
    }

    /** Reads the file again when next needed. */
    static void reload() {
        config = null;
    }

    private static int value(World world, String key, int fallback) {
        YamlConfiguration spigot = get();
        ConfigurationSection worlds = spigot == null ? null : spigot.getConfigurationSection("world-settings");
        if (worlds == null) {
            return fallback;
        }
        return worlds.getInt(world.getName() + "." + key, worlds.getInt("default." + key, fallback));
    }

    private static YamlConfiguration get() {
        YamlConfiguration c = config;
        if (c == null) {
            c = read();
            config = c;
        }
        return c;
    }

    private static YamlConfiguration read() {
        try {
            return Bukkit.spigot().getConfig();
        } catch (LinkageError | RuntimeException e) {
            // Not in this server's API: the file itself, where servers keep it unless told otherwise
            File file = new File("spigot.yml");
            return YamlConfiguration.loadConfiguration(file);
        }
    }
}

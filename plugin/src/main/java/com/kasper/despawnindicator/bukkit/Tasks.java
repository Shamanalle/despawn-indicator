package com.kasper.despawnindicator.bukkit;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Where code runs. On Paper and Spigot everything runs on the one server thread; on Folia each part of a world
 * has its own thread, and an entity or a spot may only be touched from the thread that owns it.
 */
interface Tasks {

    /** Runs {@code tick} every tick on the thread that owns {@code player}; the returned task stops it. */
    Runnable everyTick(Player player, Runnable tick);

    /** Runs {@code task} once, {@code delay} ticks from now (at least 1), on the thread that owns {@code where}. */
    void at(Location where, long delay, Runnable task);

    /**
     * Runs {@code task} once, next tick, on the thread that owns {@code entity}; {@code gone} instead when the
     * entity left the world (picked up, despawned, or its chunk unloaded) before that.
     */
    void on(Entity entity, Runnable task, Runnable gone);

    /** Folia's when the server is Folia, the Bukkit scheduler's otherwise. */
    static Tasks of(Plugin plugin) {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
        } catch (ClassNotFoundException e) {
            return new BukkitTasks(plugin);
        }
        try {
            return (Tasks) Class.forName("com.kasper.despawnindicator.bukkit.FoliaTasks")
                    .getConstructor(Plugin.class).newInstance(plugin);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not start on Folia", e);
        }
    }
}

package com.kasper.despawnindicator.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/** Paper and Spigot: one server thread. */
final class BukkitTasks implements Tasks {

    private final Plugin plugin;

    BukkitTasks(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Runnable everyTick(Player player, Runnable tick) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, tick, 1, 1);
        return task::cancel;
    }

    @Override
    public void at(Location where, long delay, Runnable task) {
        Bukkit.getScheduler().runTaskLater(plugin, task, Math.max(1, delay));
    }

    @Override
    public void on(Entity entity, Runnable task, Runnable gone) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (entity.isValid()) {
                task.run();
            } else {
                gone.run();
            }
        });
    }
}

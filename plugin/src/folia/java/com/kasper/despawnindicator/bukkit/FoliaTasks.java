package com.kasper.despawnindicator.bukkit;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Folia: a player's and an entity's tasks follow them from region to region, a spot's run in its region. */
public final class FoliaTasks implements Tasks {

    private final Plugin plugin;

    public FoliaTasks(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Runnable everyTick(Player player, Runnable tick) {
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, t -> tick.run(), null, 1, 1);
        return task == null ? () -> { } : task::cancel;
    }

    @Override
    public void at(Location where, long delay, Runnable task) {
        Bukkit.getRegionScheduler().runDelayed(plugin, where, t -> task.run(), Math.max(1, delay));
    }

    @Override
    public void on(Entity entity, Runnable task, Runnable gone) {
        if (entity.getScheduler().run(plugin, t -> {
            if (entity.isValid()) {
                task.run();
            } else {
                gone.run();
            }
        }, gone) == null) {
            gone.run();
        }
    }
}

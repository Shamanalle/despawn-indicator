package com.kasper.despawnindicator.bukkit;

import com.kasper.despawnindicator.ClockMessage;
import com.kasper.despawnindicator.HelloMessage;
import com.kasper.despawnindicator.ItemClock;
import com.kasper.despawnindicator.LootMessage;
import com.kasper.despawnindicator.ModLog;
import com.kasper.despawnindicator.ServerSettings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A player with the mod: every tick on their thread, sends them the clocks of the items they started to see.
 * <p>
 * Bukkit does not say when a player starts to see an entity, so every half a second the items within the
 * distance the server shows items at are looked at, and the new ones get a clock (the client counts down on
 * its own until then, which is right for an item that just fell). Every few seconds all of them get their
 * clock again, as on the mod's servers: the client's ticks drift from the server's when either lags.
 */
final class Viewer {

    /** How often the items around the player are looked at, in ticks. */
    static final int SCAN_EVERY = 10;
    /** How often every item around the player gets its clock again, in ticks. */
    static final int RESYNC_EVERY = 100;
    /**
     * Clocks a message: up to 16 bytes each, so a message stays well under the 32 KiB older servers allow for
     * a plugin message.
     */
    static final int CLOCKS_PER_MESSAGE = 1024;
    /** Items are shown however far above or below the player they are; this covers the tallest world. */
    private static final double HEIGHT = 4096;

    interface Sender {
        /** @return whether it went out */
        boolean send(LootMessage message);
    }

    final Player player;
    final int protocol;
    private final DespawnIndicatorPlugin plugin;
    private final AtomicBoolean hello = new AtomicBoolean(true);
    private final Set<Integer> known = new HashSet<>();
    private volatile Runnable stop;
    private int ticks;

    Viewer(DespawnIndicatorPlugin plugin, Player player, int protocol) {
        this.plugin = plugin;
        this.player = player;
        this.protocol = protocol;
    }

    void start(Tasks tasks) {
        stop = tasks.everyTick(player, this::tick);
    }

    void stop() {
        Runnable s = stop;
        if (s != null) {
            s.run();
        }
    }

    /** The client said hello again (it joined another world, or reconnected): everything is sent anew. */
    void hello() {
        hello.set(true);
    }

    private void tick() {
        if (!player.isOnline()) {
            return;
        }
        ticks++;
        boolean greet = hello.getAndSet(false);
        if (greet || ticks % RESYNC_EVERY == 0) {
            known.clear();
        }
        if (greet || ticks % SCAN_EVERY == 0) {
            scan();
        }
        if (protocol >= HelloMessage.LOOT) {
            plugin.loot().tick(player, greet, this::send);
        }
    }

    /** The items the player sees now; those that were not seen at the last look get their clock. */
    private void scan() {
        World world = player.getWorld();
        double range = range(SpigotConfig.trackingRange(world), viewDistance(world));
        Location at = player.getLocation();
        Set<Integer> seen = new HashSet<>();
        List<Item> fresh = new ArrayList<>();
        for (Entity e : player.getNearbyEntities(range, HEIGHT, range)) {
            if (!(e instanceof Item) || !e.isValid() || e.getTicksLived() < 2) {
                // An item from this very tick may not have reached the client yet
                continue;
            }
            double dx = e.getLocation().getX() - at.getX();
            double dz = e.getLocation().getZ() - at.getZ();
            if (dx * dx + dz * dz > range * range) {
                continue;
            }
            seen.add(e.getEntityId());
            if (!known.contains(e.getEntityId())) {
                fresh.add((Item) e);
            }
        }
        known.clear();
        known.addAll(seen);
        send(fresh);
    }

    /**
     * How far around the player items are surely shown to them: the server shows an item within the tracking
     * range and the view distance less a chunk; one block less keeps away from the edge.
     */
    static double range(int trackingRange, int viewDistance) {
        int r = trackingRange;
        if (viewDistance > 1) {
            r = Math.min(r, (viewDistance - 1) * 16);
        }
        return Math.max(1, r - 1);
    }

    private static int viewDistance(World world) {
        try {
            return world.getViewDistance();
        } catch (LinkageError | RuntimeException e) {
            return Bukkit.getViewDistance();
        }
    }

    private void send(List<Item> items) {
        if (items.isEmpty()) {
            return;
        }
        boolean all = plugin.settings().clocks == ServerSettings.Clocks.ALL;
        UUID me = player.getUniqueId();
        List<ItemClock> clocks = new ArrayList<>();
        for (Item item : items) {
            boolean mine = me.equals(thrower(item));
            // With clocks=own the client keeps its own estimate for everyone else's items
            if (all || mine) {
                clocks.add(ItemClock.of(item.getEntityId(), ItemAccess.age(item), ItemAccess.lifespan(item), false, mine));
            }
        }
        try {
            for (int i = 0; i < clocks.size(); i += CLOCKS_PER_MESSAGE) {
                List<ItemClock> part = clocks.subList(i, Math.min(clocks.size(), i + CLOCKS_PER_MESSAGE));
                player.sendPluginMessage(plugin, DespawnIndicatorPlugin.CLOCKS, ClockMessage.encode(part));
            }
        } catch (RuntimeException e) {
            ModLog.warn("Could not send despawn times to " + player.getName() + ": " + e);
        }
    }

    private boolean send(LootMessage message) {
        if (!player.isOnline()) {
            return false;
        }
        try {
            player.sendPluginMessage(plugin, DespawnIndicatorPlugin.CLOCKS, message.encode());
            return true;
        } catch (RuntimeException e) {
            ModLog.warn("Could not send the death loot timer to " + player.getName() + ": " + e);
            return false;
        }
    }

    /** Who dropped the item, by hand or when they died; null when nobody did or the server cannot say. */
    static UUID thrower(Item item) {
        try {
            return item.getThrower();
        } catch (LinkageError | RuntimeException e) {
            return null;
        }
    }
}

package com.kasper.despawnindicator.bukkit;

import com.kasper.despawnindicator.DespawnIndicator;
import com.kasper.despawnindicator.ItemClock;
import com.kasper.despawnindicator.LootMessage;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The items a player with the mod dropped when they died: which of them are still there and how long the
 * first of them has left, told to that player ({@link LootMessage}) whenever it changes. The same as the
 * mod's server side does, with the Bukkit API.
 * <p>
 * The items are the ones that appear around the player the tick after they die and were dropped by them (or
 * by nobody). Graves and keepInventory leave none, and then the player's earlier loot, if any, stays tracked.
 * An item whose chunk unloads is looked up again by its UUID when the chunk is back; an item that merged into
 * one not from the death, went through a portal or burned counts as gone.
 * <p>
 * On Folia each item is read on the thread of its region, so a loot's fields are only touched while holding
 * the loot.
 */
final class DeathLoot {

    /** How far from the player their death drops land, in blocks. */
    static final double DROP_RADIUS = 4;
    /** How many ticks old an item may be and still count as dropped at the death. */
    static final int FRESH_TICKS = 3;
    /** How often the items are looked at, in ticks. */
    static final int CHECK_EVERY = DespawnIndicator.TICKS_PER_SECOND;
    /** How many looks in a loaded chunk may miss an item before it counts as gone. */
    static final int MISSES = 3;
    /** How far the client's count may drift from the server's before it is told again, in ticks. */
    static final int DRIFT = 2;

    static final class Stack {
        final UUID id;
        Item entity;
        int x;
        int y;
        int z;
        int age;
        boolean aged;
        boolean frozen;
        int lifespan;
        /** The loot's tick when {@link #age} was read; the clock runs on from there until the next read. */
        long readAt;
        int misses;

        Stack(UUID id, int x, int y, int z, int age, int lifespan) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.z = z;
            this.age = age;
            this.lifespan = lifespan;
        }

        boolean unlimited() {
            return age == DespawnIndicator.UNLIMITED_AGE || lifespan <= 0;
        }

        /** Ticks left at the loot's tick {@code ticks}. */
        int left(long ticks) {
            return frozen ? lifespan - age : (int) (lifespan - age - (ticks - readAt));
        }
    }

    static final class Loot {
        final World world;
        final String dimension;
        final List<Stack> stacks;
        LootMessage sent;
        long sentAt;
        long ticks;

        Loot(World world, String dimension, List<Stack> stacks) {
            this.world = world;
            this.dimension = dimension;
            this.stacks = stacks;
        }
    }

    private static Method isEntitiesLoaded;
    private static boolean lookedUp;

    private final Tasks tasks;
    private final Map<UUID, Loot> loot = new ConcurrentHashMap<>();

    DeathLoot(Tasks tasks) {
        this.tasks = tasks;
    }

    /** {@code player} died at {@code where}: their items are looked for next tick, where they land. */
    void died(Player player, Location where) {
        UUID owner = player.getUniqueId();
        tasks.at(where, 1, () -> {
            World world = where.getWorld();
            List<Stack> stacks = new ArrayList<>();
            for (Entity e : world.getNearbyEntities(where, DROP_RADIUS, DROP_RADIUS, DROP_RADIUS)) {
                if (!(e instanceof Item) || !e.isValid() || e.getTicksLived() > FRESH_TICKS) {
                    continue;
                }
                Item item = (Item) e;
                UUID thrower = Viewer.thrower(item);
                if (thrower == null || thrower.equals(owner)) {
                    Location at = item.getLocation();
                    Stack s = new Stack(item.getUniqueId(), at.getBlockX(), at.getBlockY(), at.getBlockZ(),
                            ItemAccess.age(item), ItemAccess.lifespan(item));
                    s.entity = item;
                    stacks.add(s);
                }
            }
            if (!stacks.isEmpty()) {
                loot.put(owner, new Loot(world, dimension(world), stacks));
            }
        });
    }

    /** The item left the world for good: picked up, despawned or merged into another. */
    void gone(UUID item) {
        for (Loot l : loot.values()) {
            synchronized (l) {
                l.stacks.removeIf(s -> s.id.equals(item));
            }
        }
    }

    /** One tick of the owner's: looks at the items now and then, and tells the owner what changed. */
    void tick(Player owner, boolean hello, Viewer.Sender send) {
        Loot l = loot.get(owner.getUniqueId());
        if (l == null) {
            if (hello) {
                send.send(LootMessage.NONE);
            }
            return;
        }
        synchronized (l) {
            l.ticks++;
            if (l.ticks % CHECK_EVERY == 0) {
                check(l);
            }
            LootMessage now = message(l.dimension, l.stacks, l.ticks);
            if (hello || l.sent == null || changed(l.sent, l.ticks - l.sentAt, now)) {
                if (send.send(now)) {
                    l.sent = now;
                    l.sentAt = l.ticks;
                }
            }
            if (!now.present) {
                loot.remove(owner.getUniqueId(), l);
            }
        }
    }

    void clear() {
        loot.clear();
    }

    /** Asks each item where it is now and whether its clock runs, on the item's thread. */
    private void check(Loot l) {
        for (Stack s : new ArrayList<>(l.stacks)) {
            Item e = s.entity;
            if (e != null) {
                tasks.on(e, () -> read(l, s, e), () -> {
                    synchronized (l) {
                        if (s.entity == e) {
                            s.entity = null;
                        }
                    }
                });
            } else {
                tasks.at(new Location(l.world, s.x, s.y, s.z), 1, () -> find(l, s));
            }
        }
    }

    private static void read(Loot l, Stack s, Item e) {
        int age = ItemAccess.age(e);
        int lifespan = ItemAccess.lifespan(e);
        Location at = e.getLocation();
        boolean valid = e.isValid();
        synchronized (l) {
            if (!valid) {
                // Its chunk unloaded, or it is gone and an event says so
                s.entity = null;
                return;
            }
            s.frozen = s.aged && age == s.age;
            s.aged = true;
            s.age = age;
            s.lifespan = lifespan;
            s.readAt = l.ticks;
            s.x = at.getBlockX();
            s.y = at.getBlockY();
            s.z = at.getBlockZ();
        }
    }

    /** An item without its entity: frozen while its chunk is not loaded, looked up by its UUID when it is. */
    private static void find(Loot l, Stack s) {
        if (!entitiesLoaded(l.world, s.x >> 4, s.z >> 4)) {
            synchronized (l) {
                s.frozen = true;
            }
            return;
        }
        Entity found;
        try {
            found = Bukkit.getEntity(s.id);
        } catch (RuntimeException e) {
            found = null;
        }
        if (found instanceof Item && found.isValid() && found.getWorld().equals(l.world)) {
            synchronized (l) {
                s.entity = (Item) found;
                s.misses = 0;
            }
            read(l, s, (Item) found);
            return;
        }
        synchronized (l) {
            if (++s.misses >= MISSES) {
                l.stacks.remove(s);
            }
        }
    }

    /** Whether the chunk's entities are in the world; they load after the chunk itself on 1.17 and newer. */
    private static boolean entitiesLoaded(World world, int cx, int cz) {
        if (!world.isChunkLoaded(cx, cz)) {
            return false;
        }
        Method m = entitiesLoadedMethod();
        if (m == null) {
            return true;
        }
        try {
            Chunk chunk = world.getChunkAt(cx, cz);
            return (boolean) m.invoke(chunk);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return true;
        }
    }

    private static synchronized Method entitiesLoadedMethod() {
        if (!lookedUp) {
            lookedUp = true;
            try {
                isEntitiesLoaded = Chunk.class.getMethod("isEntitiesLoaded");
            } catch (NoSuchMethodException e) {
                isEntitiesLoaded = null;
            }
        }
        return isEntitiesLoaded;
    }

    /**
     * The timer at the loot's tick {@code ticks} for the stack that despawns first; at the first stack when
     * none of them despawns.
     */
    static LootMessage message(String dimension, List<Stack> stacks, long ticks) {
        if (stacks.isEmpty()) {
            return LootMessage.NONE;
        }
        Stack first = null;
        for (Stack s : stacks) {
            if (!s.unlimited() && (first == null || s.left(ticks) < first.left(ticks))) {
                first = s;
            }
        }
        int items = stacks.size();
        if (first == null) {
            Stack s = stacks.get(0);
            return new LootMessage(dimension, s.x, s.y, s.z, items, ItemClock.UNLIMITED, 0, 0);
        }
        return new LootMessage(dimension, first.x, first.y, first.z, items,
                first.frozen ? ItemClock.FROZEN : 0, first.left(ticks), first.lifespan);
    }

    /** Whether the client, counting {@code sent} down for {@code elapsed} ticks, now shows something else. */
    static boolean changed(LootMessage sent, long elapsed, LootMessage now) {
        if (sent.present != now.present || sent.items != now.items || sent.flags != now.flags
                || sent.x != now.x || sent.y != now.y || sent.z != now.z || !sent.dimension.equals(now.dimension)) {
            return true;
        }
        if (!now.present || now.has(ItemClock.UNLIMITED)) {
            return false;
        }
        long expected = now.has(ItemClock.FROZEN) ? sent.remaining : sent.remaining - elapsed;
        return Math.abs(expected - now.remaining) > DRIFT;
    }

    /**
     * The world's dimension id as the client knows it: the world's key, which Spigot before 1.20 does not
     * expose; there the main world and its nether and end are the vanilla dimensions, and other worlds are
     * named after their folder.
     */
    static String dimension(World world) {
        try {
            return world.getKey().toString();
        } catch (LinkageError | RuntimeException e) {
            return dimension(world.getName(), world.getEnvironment(), mainWorld());
        }
    }

    static String dimension(String name, World.Environment environment, String main) {
        if (name.equals(main) && environment == World.Environment.NORMAL) {
            return "minecraft:overworld";
        }
        if (name.equals(main + "_nether") && environment == World.Environment.NETHER) {
            return "minecraft:the_nether";
        }
        if (name.equals(main + "_the_end") && environment == World.Environment.THE_END) {
            return "minecraft:the_end";
        }
        return "minecraft:" + name.toLowerCase(Locale.ROOT);
    }

    private static String mainWorld() {
        List<World> worlds = Bukkit.getWorlds();
        return worlds.isEmpty() ? "world" : worlds.get(0).getName();
    }
}

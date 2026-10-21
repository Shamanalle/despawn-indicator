package com.kasper.despawnindicator;

/**
 * The despawn clock of one item on the ground, as the server sees it: how many ticks it has left and how
 * long it lives in all. The client counts the ticks down by itself until the next message about the item.
 */
public final class ItemClock {

    /** It never despawns (picked up by a mod, {@code setUnlimitedLifetime}, a server rule). */
    public static final int UNLIMITED = 1;
    /** Its chunk does not tick (outside the simulation distance): the clock stands still. */
    public static final int FROZEN = 2;
    /** Dropped by the player this message goes to (their death or a throw). */
    public static final int MINE = 4;

    public final int entityId;
    public final int flags;
    /** Ticks until it despawns; meaningless with {@link #UNLIMITED}. */
    public final int remaining;
    /** Ticks it lives in all (6000 in vanilla); meaningless with {@link #UNLIMITED}. */
    public final int lifespan;

    public ItemClock(int entityId, int flags, int remaining, int lifespan) {
        this.entityId = entityId;
        this.flags = flags;
        this.remaining = remaining;
        this.lifespan = lifespan;
    }

    /**
     * The clock of an item {@code age} ticks old that despawns at {@code lifespan}. Minecraft marks an item
     * that never despawns with age -32768.
     */
    public static ItemClock of(int entityId, int age, int lifespan, boolean frozen, boolean mine) {
        int flags = (frozen ? FROZEN : 0) | (mine ? MINE : 0);
        if (age == DespawnIndicator.UNLIMITED_AGE || lifespan <= 0) {
            return new ItemClock(entityId, flags | UNLIMITED, 0, 0);
        }
        return new ItemClock(entityId, flags, lifespan - age, lifespan);
    }

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ItemClock)) {
            return false;
        }
        ItemClock c = (ItemClock) o;
        return entityId == c.entityId && flags == c.flags && remaining == c.remaining && lifespan == c.lifespan;
    }

    @Override
    public int hashCode() {
        return ((entityId * 31 + flags) * 31 + remaining) * 31 + lifespan;
    }

    @Override
    public String toString() {
        return "ItemClock{" + entityId + (has(UNLIMITED) ? ", unlimited" : ", " + remaining + "/" + lifespan)
                + (has(FROZEN) ? ", frozen" : "") + (has(MINE) ? ", mine" : "") + "}";
    }
}

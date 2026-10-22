package com.kasper.despawnindicator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The server side of the clocks, without Minecraft: who has the mod, and which items each of them needs a
 * clock for at the end of this tick. The loader glue queues an item when a player with the mod starts to
 * see it and sends what {@link #drain()} gives at the end of the server tick, in one message a player, so
 * the client already knows the item from its spawn packet.
 *
 * @param <E> the loader's item entity (entities are equal only to themselves)
 */
public final class ClockSender<E> {

    private final Map<UUID, HelloMessage> withMod = new ConcurrentHashMap<>();
    private final Map<UUID, Set<E>> pending = new LinkedHashMap<>();

    /** A player said hello: from now on they get clocks. */
    public void hello(UUID player, HelloMessage hello) {
        withMod.put(player, hello);
    }

    public void left(UUID player) {
        withMod.remove(player);
        pending.remove(player);
    }

    public boolean hasMod(UUID player) {
        return withMod.containsKey(player);
    }

    /** The protocol {@code player}'s client speaks; 0 without the mod. */
    public int protocol(UUID player) {
        HelloMessage h = withMod.get(player);
        return h == null ? 0 : h.protocol;
    }

    public int playersWithMod() {
        return withMod.size();
    }

    /** {@code player} needs the clock of {@code item}; ignored for players without the mod. */
    public void queue(UUID player, E item) {
        if (hasMod(player)) {
            pending.computeIfAbsent(player, p -> new LinkedHashSet<>()).add(item);
        }
    }

    /** What to send now, by player, each item once; empties the queue. */
    public Map<UUID, List<E>> drain() {
        if (pending.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<UUID, List<E>> out = new LinkedHashMap<>();
        for (Map.Entry<UUID, Set<E>> e : pending.entrySet()) {
            out.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        pending.clear();
        return out;
    }

    /** Forgets every player (the server stopped). */
    public void clear() {
        withMod.clear();
        pending.clear();
    }
}

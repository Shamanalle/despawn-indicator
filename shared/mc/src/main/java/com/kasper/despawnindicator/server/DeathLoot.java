package com.kasper.despawnindicator.server;

import com.kasper.despawnindicator.DespawnIndicator;
import com.kasper.despawnindicator.Dimensions;
import com.kasper.despawnindicator.HelloMessage;
import com.kasper.despawnindicator.ItemClock;
import com.kasper.despawnindicator.LootMessage;
import com.kasper.despawnindicator.ModLog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The items a player with the mod dropped when they died: which of them are still there and how long the
 * first of them has left, told to that player ({@link LootMessage}) whenever it changes.
 * <p>
 * The items are the ones that appear around the player in the tick they die and were dropped by them (or by
 * nobody). Graves and keepInventory leave none, and then the player's earlier loot, if any, stays tracked.
 * An item whose chunk unloads is looked up again by its UUID when the chunk is back; an item that merged into
 * one not from the death, went through a portal or burned counts as gone.
 */
public final class DeathLoot {

    /** How far from the player their death drops land, in blocks. */
    private static final double DROP_RADIUS = 4;
    /** How many ticks old an item may be and still count as dropped at the death. */
    private static final int FRESH_TICKS = 3;
    /** How often the items are looked at, in ticks. */
    private static final int CHECK_EVERY = DespawnIndicator.TICKS_PER_SECOND;
    /** How many looks in a loaded chunk may miss an item before it counts as gone. */
    private static final int MISSES = 3;
    /** How far the client's count may drift from the server's before it is told again, in ticks. */
    private static final int DRIFT = 2;

    private static final class Stack {
        final UUID id;
        ItemEntity entity;
        BlockPos pos;
        int age;
        boolean aged;
        boolean frozen;
        int lifespan;
        int misses;

        Stack(ItemEntity entity) {
            this.id = entity.getUUID();
            this.entity = entity;
            this.pos = entity.blockPosition();
            this.age = entity.getAge();
            this.lifespan = Platform.lifespan(entity);
        }

        boolean unlimited() {
            return age == DespawnIndicator.UNLIMITED_AGE || lifespan <= 0;
        }
    }

    private static final class Loot {
        final ServerLevel level;
        final String dimension;
        final List<Stack> stacks;
        LootMessage sent;
        long sentAt;

        Loot(ServerLevel level, List<Stack> stacks) {
            this.level = level;
            this.dimension = Dimensions.id(level.dimension().toString());
            this.stacks = stacks;
        }
    }

    private static final Map<UUID, Loot> LOOT = new HashMap<>();
    /** Players who were dead at the end of the last tick. */
    private static final Set<UUID> DEAD = new HashSet<>();
    /** Players with the mod who said hello since the last tick: they get their loot, or "none". */
    private static final Set<UUID> ASKED = ConcurrentHashMap.newKeySet();
    private static long ticks;

    private DeathLoot() {
    }

    static boolean speaksLoot(UUID player) {
        return ServerClocks.SENDER.protocol(player) >= HelloMessage.LOOT;
    }

    /** A client said hello; it may be on the network thread. */
    static void hello(UUID player) {
        ASKED.add(player);
    }

    static void left(UUID player) {
        DEAD.remove(player);
        ASKED.remove(player);
    }

    static void stopped() {
        LOOT.clear();
        DEAD.clear();
        ASKED.clear();
    }

    /** The end of a server tick, on the server thread. */
    static void endTick(MinecraftServer server) {
        ticks++;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            if (!player.isDeadOrDying()) {
                DEAD.remove(id);
            } else if (DEAD.add(id) && speaksLoot(id)) {
                died(player);
            }
        }
        boolean look = ticks % CHECK_EVERY == 0;
        for (Iterator<Map.Entry<UUID, Loot>> i = LOOT.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<UUID, Loot> e = i.next();
            Loot loot = e.getValue();
            if (look) {
                check(loot);
            }
            LootMessage now = message(loot);
            if (loot.sent == null || changed(loot.sent, loot.sentAt, now)) {
                if (send(server, e.getKey(), now)) {
                    loot.sent = now;
                    loot.sentAt = ticks;
                }
            }
            if (!now.present) {
                i.remove();
            }
        }
        if (!ASKED.isEmpty()) {
            for (UUID id : new ArrayList<>(ASKED)) {
                ASKED.remove(id);
                Loot loot = LOOT.get(id);
                LootMessage now = loot == null ? LootMessage.NONE : message(loot);
                if (send(server, id, now) && loot != null) {
                    loot.sent = now;
                    loot.sentAt = ticks;
                }
            }
        }
    }

    private static void died(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        List<Stack> stacks = new ArrayList<>();
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(DROP_RADIUS))) {
            if (item.isRemoved() || item.tickCount > FRESH_TICKS) {
                continue;
            }
            Entity owner = item.getOwner();
            if (owner == null || owner.getUUID().equals(player.getUUID())) {
                stacks.add(new Stack(item));
            }
        }
        if (!stacks.isEmpty()) {
            LOOT.put(player.getUUID(), new Loot(level, stacks));
        }
    }

    /** Where each item is now and whether its clock runs; drops the ones that are gone. */
    private static void check(Loot loot) {
        for (Iterator<Stack> i = loot.stacks.iterator(); i.hasNext(); ) {
            Stack s = i.next();
            if (s.entity != null && s.entity.isRemoved()) {
                if (s.entity.getRemovalReason() != Entity.RemovalReason.UNLOADED_TO_CHUNK) {
                    // Picked up, despawned, merged, burned or through a portal
                    i.remove();
                    continue;
                }
                s.entity = null;
            }
            if (s.entity == null) {
                if (!loot.level.areEntitiesLoaded(chunk(s.pos))) {
                    s.frozen = true;
                    continue;
                }
                Entity found = loot.level.getEntity(s.id);
                if (!(found instanceof ItemEntity) || found.isRemoved()) {
                    if (++s.misses >= MISSES) {
                        i.remove();
                    }
                    continue;
                }
                s.entity = (ItemEntity) found;
                s.misses = 0;
            }
            int age = s.entity.getAge();
            s.frozen = s.aged && age == s.age;
            s.aged = true;
            s.age = age;
            s.pos = s.entity.blockPosition();
            s.lifespan = Platform.lifespan(s.entity);
        }
    }

    /** {@code ChunkPos.asLong}, renamed in Minecraft 26.1. */
    private static long chunk(BlockPos pos) {
        return (pos.getX() >> 4 & 0xFFFFFFFFL) | (long) (pos.getZ() >> 4 & 0xFFFFFFFFL) << 32;
    }

    /** The timer for the stack that despawns first; at the first stack when none of them despawns. */
    static LootMessage message(Loot loot) {
        if (loot.stacks.isEmpty()) {
            return LootMessage.NONE;
        }
        Stack first = null;
        for (Stack s : loot.stacks) {
            if (!s.unlimited() && (first == null || s.lifespan - s.age < first.lifespan - first.age)) {
                first = s;
            }
        }
        int items = loot.stacks.size();
        if (first == null) {
            BlockPos p = loot.stacks.get(0).pos;
            return new LootMessage(loot.dimension, p.getX(), p.getY(), p.getZ(), items, ItemClock.UNLIMITED, 0, 0);
        }
        BlockPos p = first.pos;
        return new LootMessage(loot.dimension, p.getX(), p.getY(), p.getZ(), items,
                first.frozen ? ItemClock.FROZEN : 0, first.lifespan - first.age, first.lifespan);
    }

    /** Whether the client, counting {@code sent} down since {@code sentAt}, now shows something else. */
    private static boolean changed(LootMessage sent, long sentAt, LootMessage now) {
        if (sent.present != now.present || sent.items != now.items || sent.flags != now.flags
                || sent.x != now.x || sent.y != now.y || sent.z != now.z || !sent.dimension.equals(now.dimension)) {
            return true;
        }
        if (!now.present || now.has(ItemClock.UNLIMITED)) {
            return false;
        }
        long expected = now.has(ItemClock.FROZEN) ? sent.remaining : sent.remaining - (ticks - sentAt);
        return Math.abs(expected - now.remaining) > DRIFT;
    }

    /** Whether it reached the player: they are online and their client knows loot messages. */
    private static boolean send(MinecraftServer server, UUID id, LootMessage m) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player == null || !speaksLoot(id)) {
            return false;
        }
        try {
            Platform.send(player, m.encode());
            return true;
        } catch (RuntimeException ex) {
            ModLog.warn("Could not send the death loot timer to " + id + ": " + ex);
            return false;
        }
    }
}

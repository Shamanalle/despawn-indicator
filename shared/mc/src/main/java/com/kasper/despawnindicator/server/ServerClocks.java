package com.kasper.despawnindicator.server;

import com.kasper.despawnindicator.ClockMessage;
import com.kasper.despawnindicator.ClockSender;
import com.kasper.despawnindicator.HelloMessage;
import com.kasper.despawnindicator.ItemClock;
import com.kasper.despawnindicator.ModLog;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The server half on every loader: tells players with the mod how long the items they see have left. The
 * loader glue calls these from its events; {@link Platform} holds what differs between loaders.
 */
public final class ServerClocks {

    public static final ClockSender<ItemEntity> SENDER = new ClockSender<>();
    /**
     * How far around a player who just said hello items get a clock. Items further away are not tracked by
     * their client yet; they get one when the player starts to see them.
     */
    private static final double HELLO_RADIUS = 128;
    /**
     * Every few seconds the items close to a player with the mod get their clock again: the client counts
     * down on its own ticks, which drift from the server's when either lags.
     */
    private static final int RESYNC_EVERY = 100;
    private static final double RESYNC_RADIUS = 32;

    /** Hellos that arrived since the last tick, answered at its end, on the server thread. */
    private static final Set<UUID> NEW = ConcurrentHashMap.newKeySet();
    private static int ticks;

    private ServerClocks() {
    }

    /** A client said hello on the despawnindicator:hello channel. */
    public static void hello(ServerPlayer player, byte[] data) {
        HelloMessage hello = HelloMessage.decode(data);
        if (hello == null) {
            return;
        }
        SENDER.hello(player.getUUID(), hello);
        NEW.add(player.getUUID());
    }

    /** {@code player}'s client was just sent {@code entity}. */
    public static void startTracking(Entity entity, ServerPlayer player) {
        if (entity instanceof ItemEntity) {
            SENDER.queue(player.getUUID(), (ItemEntity) entity);
        }
    }

    public static void left(ServerPlayer player) {
        SENDER.left(player.getUUID());
        NEW.remove(player.getUUID());
    }

    public static void stopped() {
        SENDER.clear();
        NEW.clear();
    }

    /** The end of a server tick: clocks for the items queued in it. */
    public static void endTick(MinecraftServer server) {
        if (!NEW.isEmpty()) {
            for (UUID id : new ArrayList<>(NEW)) {
                NEW.remove(id);
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player != null) {
                    queueAround(server, player, HELLO_RADIUS);
                }
            }
        }
        if (++ticks % RESYNC_EVERY == 0 && SENDER.playersWithMod() > 0) {
            for (ServerLevel level : server.getAllLevels()) {
                for (ServerPlayer player : level.players()) {
                    if (SENDER.hasMod(player.getUUID())) {
                        queue(level, player, RESYNC_RADIUS);
                    }
                }
            }
        }
        for (Map.Entry<UUID, List<ItemEntity>> e : SENDER.drain().entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
            if (player == null) {
                continue;
            }
            List<ItemClock> clocks = new ArrayList<>();
            for (ItemEntity item : e.getValue()) {
                if (!item.isRemoved()) {
                    clocks.add(clock(item));
                }
            }
            try {
                for (byte[] message : ClockMessage.encodeAll(clocks)) {
                    Platform.send(player, message);
                }
            } catch (RuntimeException ex) {
                ModLog.warn("Could not send despawn times to " + player.getUUID() + ": " + ex);
            }
        }
    }

    /** The items near a player who just said hello: their client already has them. */
    private static void queueAround(MinecraftServer server, ServerPlayer player, double radius) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.players().contains(player)) {
                queue(level, player, radius);
            }
        }
    }

    private static void queue(ServerLevel level, ServerPlayer player, double radius) {
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(radius))) {
            SENDER.queue(player.getUUID(), item);
        }
    }

    static ItemClock clock(ItemEntity item) {
        return ItemClock.of(item.getId(), item.getAge(), Platform.lifespan(item), false, false);
    }
}
